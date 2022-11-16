package dev.justme.busket.feathers

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.*
import com.google.gson.Gson
import dev.justme.busket.SingletonHolder
import dev.justme.busket.feathers.responses.AuthenticationSuccessResponse
import dev.justme.busket.feathers.responses.User
import org.json.JSONObject

class FeathersHttp(private val context: Context) {
    val gson = Gson()
    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    var user: User? = null
    private val cache = DiskBasedCache(context.cacheDir, 1024 * 1024) // 1MB cap
    private val network = BasicNetwork(HurlStack())
    private val requestQueue = RequestQueue(cache, network).apply {
        start()
    }
    private var authentication: AuthenticationSuccessResponse? = null

    companion object : SingletonHolder<FeathersHttp, Context>(::FeathersHttp)

    class Service(
        private val service: String,
        private val gson: Gson,
        private val context: Context,
        private val requestQueue: RequestQueue,
        private val authentication: AuthenticationSuccessResponse? = null,
    ) {
        fun <T : Any, R : Any> find(
            body: T,
            responseClass: Class<R>,
            successCallback: ((R) -> Unit)?,
            errorCallback: ((e: VolleyError) -> Unit)?
        ) {
            makeHttpRequest(
                Request.Method.GET,
                body,
                responseClass,
                { successCallback?.invoke(it) },
                { errorCallback?.invoke(it) })
        }

        fun <T : Any, R : Any> get(
            body: T,
            responseClass: Class<R>,
            successCallback: ((R) -> Unit)?,
            errorCallback: ((e: VolleyError) -> Unit)?
        ) {
            makeHttpRequest(
                Request.Method.GET,
                body,
                responseClass,
                { successCallback?.invoke(it) },
                { errorCallback?.invoke(it) })
        }

        fun <T : Any, R : Any> create(
            body: T,
            responseClass: Class<R>,
            successCallback: ((R) -> Unit)?,
            errorCallback: ((e: VolleyError) -> Unit)?
        ) {
            makeHttpRequest(
                Request.Method.POST,
                body,
                responseClass,
                { successCallback?.invoke(it) },
                { errorCallback?.invoke(it) })
        }

        fun <T : Any, R : Any> update(
            body: T,
            responseClass: Class<R>,
            successCallback: ((R) -> Unit)?,
            errorCallback: ((e: VolleyError) -> Unit)?
        ) {
            makeHttpRequest(
                Request.Method.PUT,
                body,
                responseClass,
                { successCallback?.invoke(it) },
                { errorCallback?.invoke(it) })
        }

        fun <T : Any, R : Any> patch(
            body: T,
            responseClass: Class<R>,
            successCallback: ((R) -> Unit)?,
            errorCallback: ((e: VolleyError) -> Unit)?
        ) {
            makeHttpRequest(
                Request.Method.PATCH,
                body,
                responseClass,
                { successCallback?.invoke(it) },
                { errorCallback?.invoke(it) })
        }

        fun <T : Any, R : Any> remove(
            body: T,
            responseClass: Class<R>,
            successCallback: ((R) -> Unit)?,
            errorCallback: ((e: VolleyError) -> Unit)?
        ) {
            makeHttpRequest(
                Request.Method.DELETE,
                body,
                responseClass,
                { successCallback?.invoke(it) },
                { errorCallback?.invoke(it) })
        }

        private fun <T : Any, R : Any> makeHttpRequest(
            method: Int,
            body: T? = null,
            responseClass: Class<R>,
            successCallback: (R) -> Unit,
            errorCallback: (VolleyError) -> Unit
        ) {
            val url = "https://busket-beta.bux.at/$service"
            val data = if (body == null) null else JSONObject(gson.toJson(body))
            Log.d("Busket data", data.toString())

            val stringRequest = object : JsonObjectRequest(
                method, url, data,
                {
                    val res = gson.fromJson(
                        it.toString(),
                        responseClass
                    )
                    successCallback.invoke(res)
                },
                {
                    errorCallback.invoke(it)
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    if (authentication != null) headers["Authorization"] =
                        authentication.accessToken
                    return headers
                }
            }
            requestQueue.add(stringRequest)
        }
    }

    fun service(service: String): Service {
        return Service(service, gson, context, requestQueue, authentication)
    }

    fun authenticate(
        email: String,
        password: String,
        successCallback: (AuthenticationSuccessResponse) -> Unit,
        errorCallback: ((e: VolleyError) -> Unit)? = null
    ) {
        fun success(auth: AuthenticationSuccessResponse) {
            storeAccessTokenAndSetUser(auth)
            successCallback(auth)
        }

        tryAuthenticateWithAccessToken({
            success(it)
        }, {
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
        errorCallback: ((e: VolleyError) -> Unit)? = null,
        storeTokenAndUser: Boolean = true,
    ) {

        val url = "https://busket-beta.bux.at/authentication"
        val data =
            "{\n\"strategy\":\"local\",\n\"email\":\"$email\",\n\"password\":\"$password\"\n}"

        val stringRequest = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(data),
            {
                val auth = gson.fromJson(
                    it.toString(),
                    AuthenticationSuccessResponse::class.java
                )
                if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                successCallback?.invoke(auth)
            },
            {
                errorCallback?.invoke(it)
            })

        requestQueue.add(stringRequest)
    }

    fun tryAuthenticateWithAccessToken(
        successCallback: ((AuthenticationSuccessResponse) -> Unit)? = null,
        errorCallback: ((e: VolleyError) -> Unit)? = null,
        storeTokenAndUser: Boolean = true,
    ) {
        val storedAccessToken = EncryptedSharedPreferences.create(
            context,
            "encrypted_shared_prefs",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).getString("access_token", null)

        val url = "https://busket-beta.bux.at/authentication"
        if (storedAccessToken != null) {
            val data = "{\"strategy\":\"jwt\",\"accessToken\":\"$storedAccessToken\"}"

            val stringRequest = JsonObjectRequest(
                Request.Method.POST, url, JSONObject(data),
                {
                    val auth = gson.fromJson(
                        it.toString(),
                        AuthenticationSuccessResponse::class.java
                    )
                    if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                    successCallback?.invoke(auth)
                },
                {
                    errorCallback?.invoke(it)
                })

            requestQueue.add(stringRequest)
            return
        }

        errorCallback?.invoke(AuthFailureError())
    }

    fun storeAccessTokenAndSetUser(auth: AuthenticationSuccessResponse) {
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