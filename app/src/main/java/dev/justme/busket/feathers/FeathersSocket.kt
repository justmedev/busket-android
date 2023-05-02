package dev.justme.busket.feathers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import dev.justme.busket.feathers.responses.AuthenticationSuccessResponse
import dev.justme.busket.feathers.responses.User
import dev.justme.busket.helpers.SingletonHolder
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
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
    private val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private var authentication: AuthenticationSuccessResponse? = null
    //endregion

    //region publics
    companion object : SingletonHolder<FeathersSocket, Context>(::FeathersSocket) {
        private const val TAG = "Busket Socket"
    }

    val gson = Gson()
    val socket: Socket = IO.socket("http://localhost:3030", options)
    var user: User? = null
    //endregions

    //region connection
    private fun connect(connectedCallback: (() -> Unit)?) {
        options.path = "/socket.io/"
        options.transports = arrayOf(WebSocket.NAME)
        socket.connect()

        socket.io().on(Manager.EVENT_RECONNECT) {
            tryAuthenticateWithAccessToken({
                connectedCallback?.invoke()
            }, {
                connectedCallback?.invoke()
            })
        }

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected. Invoking callback")
            connectedCallback?.invoke()
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected.")
            socket.connect()
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "Socket error.")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "Socket connection error.")
        }
    }

    fun requireConnected(fn: () -> Unit) {
        if (!socket.connected()) connect(fn)
        else fn.invoke()
    }
    //endregion

    //region service methods
    fun isSuccessCode(statusCode: Int): Boolean {
        var code = statusCode
        while (code > 9) code /= 10
        return code == 2
    }

    fun service(path: FeathersService.Service): FeathersService {
        return FeathersService(this, path)
    }

    fun service(path: String): FeathersService {
        return FeathersService(this, path)
    }
    //endregion

    // region authentication
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
        val jData = JSONObject()
        jData.put("strategy", "local")
        jData.put("email", email)
        jData.put("password", password)

        service(FeathersService.Service.AUTHENTICATION).create(jData) { data, err ->
            if (err != null) {
                errorCallback?.invoke(err)
                return@create
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
        val storedAccessToken = getEncryptedSharedPreferences().getString("access_token", null)

        if (storedAccessToken != null) {
            val jData = JSONObject()
            jData.put("strategy", "jwt")
            jData.put("accessToken", storedAccessToken)

            service(FeathersService.Service.AUTHENTICATION).create(jData) { data, err ->
                if (err != null) {
                    errorCallback?.invoke(err)
                    return@create
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

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            "encrypted_shared_prefs",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun storeAccessTokenAndSetUser(auth: AuthenticationSuccessResponse) {
        getEncryptedSharedPreferences().edit().putString("access_token", auth.accessToken).apply()

        authentication = auth
        user = auth.user
    }

    fun getAuthentication(): AuthenticationSuccessResponse? {
        return authentication
    }

    fun logout() {
        getEncryptedSharedPreferences().edit().putString("access_token", null).apply()

        requireConnected {
            service(FeathersService.Service.AUTHENTICATION).remove(JSONObject(), null)
        }
    }
    //endregion
}