package dev.justme.busket.feathers

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.github.nkzawa.engineio.client.transports.WebSocket
import com.github.nkzawa.socketio.client.Ack
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import dev.justme.busket.SingletonHolder
import dev.justme.busket.feathers.responses.AuthenticationSuccessResponse
import dev.justme.busket.feathers.responses.User
import org.json.JSONObject

data class SocketError(val name: String, val message: String, val code: Int, val className: String, val errors: JSONObject)

class FeathersSocket(private val context: Context) {
    private val options = IO.Options()
    private val gson = Gson()
    private val mainKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private var authentication: AuthenticationSuccessResponse? = null

    val socket = IO.socket("http://localhost:43005", options)
    var user: User? = null

    companion object : SingletonHolder<FeathersSocket, Context>(::FeathersSocket)

    private fun connect(connectedCallback: (() -> Unit)?) {
        options.path = "/socket.io"
        options.transports = arrayOf(WebSocket.NAME)
        socket.connect().on(Socket.EVENT_CONNECT) {
            connectedCallback?.invoke()
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d("Busket Socket", "Socket disconnected.")
            socket.connect()
        }

        socket.on(Socket.EVENT_ERROR) {
            Log.d("Busket Socket", "Socket error.")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d("Busket Socket", "Socket connection error.")
        }
    }

    fun requireConnected(fn: () -> Unit) {
        if (!socket.connected()) connect(fn)
    }

    fun req(context: Context) {
        socket.emit("list::find", FeathersHttp.getInstance(context).gson.toJson(object {
            val owner = FeathersHttp.getInstance(context).user?.uuid
        }), Ack {
            for (a in it) {
                Log.d("WS ACK it", a.toString())
            }
        })
    }

    fun authenticate(
        email: String,
        password: String,
        successCallback: (AuthenticationSuccessResponse) -> Unit,
        errorCallback: ((e: SocketError) -> Unit)? = null,
    ) {
        fun success(auth: AuthenticationSuccessResponse) {
            storeAccessTokenAndSetUser(auth)
            successCallback(auth)
        }

        tryAuthenticateWithAccessToken({ success(it) }, {
            authenticateWithCredentials(email, password, {
                success(it)
            }, {
                errorCallback?.invoke(it)
            })
        })
    }

    private fun authenticateWithCredentials(
        email: String,
        password: String,
        successCallback: ((AuthenticationSuccessResponse) -> Unit)? = null,
        errorCallback: ((e: SocketError) -> Unit)? = null,
        storeTokenAndUser: Boolean = true,
    ) {
        val data = gson.toJson(object {
            val strategy = "local"
            val email = email
            val password = password
        })

        requireConnected {
            socket.emit("authentication::create", JSONObject(data), Ack {
                val json = it[0] as JSONObject
                if (json.getInt("code") != 200) {
                    errorCallback?.invoke(gson.fromJson(it[0].toString(), SocketError::class.java))
                    return@Ack
                }

                Log.d("WS AUTH ACK ack:", it.contentDeepToString())
                val auth = gson.fromJson(
                    it.toString(), AuthenticationSuccessResponse::class.java
                )
                if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                successCallback?.invoke(auth)
            })
        }
    }

    fun tryAuthenticateWithAccessToken(
        successCallback: ((AuthenticationSuccessResponse) -> Unit)? = null,
        errorCallback: ((e: SocketError) -> Unit)? = null,
        storeTokenAndUser: Boolean = true,
    ) {
        val storedAccessToken = EncryptedSharedPreferences.create(
            context,
            "encrypted_shared_prefs",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).getString("access_token", null)

        if (storedAccessToken != null) {
            val data = "{\"strategy\":\"jwt\",\"accessToken\":\"$storedAccessToken\"}"

            socket.emit("create", "authentication", data, Ack {
                val json = it[0] as JSONObject
                if (json.getInt("code") != 200) {
                    errorCallback?.invoke(gson.fromJson(it[0].toString(), SocketError::class.java))
                    return@Ack
                }

                Log.d("WS AUTH ACK (tryAuthWAT) arr:", it.contentDeepToString())
                val auth = gson.fromJson(
                    it.toString(), AuthenticationSuccessResponse::class.java
                )
                if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                successCallback?.invoke(auth)
            })
        } else {
            errorCallback?.invoke(SocketError("forbidden", "No accesstoken found in SharedPreferences", 403, "Forbidden", JSONObject()))
        }
    }

    private fun storeAccessTokenAndSetUser(auth: AuthenticationSuccessResponse) {
        EncryptedSharedPreferences.create(
            context,
            "encrypted_shared_prefs",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).edit().putString("access_token", auth.accessToken).apply()

        authentication = auth
        user = auth.user
    }

    fun getAuthentication(): AuthenticationSuccessResponse? {
        return authentication
    }
}