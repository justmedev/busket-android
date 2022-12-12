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

data class SocketError(
    val name: String,
    val message: String,
    val code: Int,
    val className: String,

    val data: Any?,
    val errors: List<Any>?,
)

class FeathersSocket(private val context: Context) {
    //region privates
    private val options = IO.Options()
    private val gson = Gson()
    private val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private var authentication: AuthenticationSuccessResponse? = null
    //endregion

    //region publics
    companion object : SingletonHolder<FeathersSocket, Context>(::FeathersSocket)

    val socket = IO.socket("http://localhost:43005", options)
    var user: User? = null
    //endregions

    enum class Method {
        FIND, GET, CREATE, UPDATE, PATCH, REMOVE
    }

    //region connection
    private fun connect(connectedCallback: (() -> Unit)?) {
        options.path = "/socket.io"
        options.transports = arrayOf(WebSocket.NAME)
        socket.connect();

        socket.on(Socket.EVENT_CONNECT) {
            Log.d("Busket Socket", "Socket connected. Invoking callback")
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
        socket.on(Socket.EVENT_CONNECT_TIMEOUT) {
            Log.d("Busket Socket", "Socket connection timeout error.")
        }

        socket.on(Socket.EVENT_RECONNECT_FAILED) {
            Log.d("Busket Socket", "Socket reconnection failed.")
        }

        socket.on(Socket.EVENT_RECONNECT_ERROR) {
            Log.d("Busket Socket", "Socket reconnection error.")
        }
    }

    fun requireConnected(fn: () -> Unit) {
        if (!socket.connected()) connect(fn)
    }
    //endregion

    //region service methods
    fun service(
        name: String,
        method: Method,
        data: JSONObject,
        callback: (data: JSONObject?, error: SocketError?) -> Unit
    ) {
        service(name, method.toString(), data, callback)
    }

    fun service(
        name: String,
        method: String,
        data: JSONObject,
        callback: (data: JSONObject?, error: SocketError?) -> Unit
    ) {
        requireConnected {
            socket.emit("%s::%s".format(name, method.lowercase()), data, Ack {
                var foundResponse = false
                for (res in it) {
                    if (res != null) {
                        foundResponse = true

                        val json = res as JSONObject
                        if (!json.has("code")) json.put("code", 200);

                        if (json.getInt("code") != 200) {
                            val errorObj = gson.fromJson(it[0].toString(), SocketError::class.java)
                            callback.invoke(json, errorObj)
                            return@Ack
                        }

                        callback.invoke(json, null)
                        return@Ack
                    }
                }

                if (!foundResponse) {
                    callback.invoke(
                        null,
                        SocketError(
                            "Unknown error",
                            "An unknown networking error occurred",
                            -1,
                            "",
                            null,
                            null
                        )
                    )
                    return@Ack
                }
            })
        }
    }
    //endregion

    //region authentication
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
        val jData = JSONObject();
        jData.put("strategy", "local")
        jData.put("email", email)
        jData.put("password", password)

        service("authentication", "create", jData) { data, err ->
            if (err != null) {
                errorCallback?.invoke(err)
                return@service
            }

            val auth = gson.fromJson(
                data.toString(), AuthenticationSuccessResponse::class.java
            )
            if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
            successCallback?.invoke(auth)
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
            val jData = JSONObject()
            jData.put("strategy", "jwt");
            jData.put("accessToken", storedAccessToken);

            service("authentication", Method.CREATE, jData) { data, err ->
                if (err != null) {
                    errorCallback?.invoke(err)
                    return@service
                }

                val auth = gson.fromJson(
                    data.toString(), AuthenticationSuccessResponse::class.java
                )
                if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                successCallback?.invoke(auth)
            }
        } else {
            errorCallback?.invoke(
                SocketError(
                    "forbidden",
                    "No accesstoken found in SharedPreferences",
                    403,
                    "Forbidden",
                    null,
                    null,
                )
            )
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
    //endregion
}