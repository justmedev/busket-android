package dev.justme.busket.feathers

import android.content.Context
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

class Feathers(private val context: Context) {
    private val cache = DiskBasedCache(context.cacheDir, 1024 * 1024) // 1MB cap
    private val network = BasicNetwork(HurlStack())
    private val requestQueue = RequestQueue(cache, network).apply {
        start()
    }
    private val gson = Gson()
    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    var user: User? = null

    companion object : SingletonHolder<Feathers, Context>(::Feathers)

    class Service(service: String, gson: Gson) {
        private val service = service
        private val gson = gson

        fun find(data: String) {

        }

        fun get(data: String) {

        }

        fun <T> create(data: T) {
            gson.toJson(data)

        }

        fun update(data: String) {

        }

        fun patch(data: String) {

        }

        fun remove(data: String) {

        }

        private fun <T> makeHttpRequest(method: Int, body: Class<T>? = null): T {
            val url = "https://busket-beta.bux.at/$service"
            val data = if (body == null) null else JSONObject(gson.toJson(body))

            val stringRequest = JsonObjectRequest(
                method, url, data,
                {
                    gson.fromJson(
                        it.toString(),
                        Class<T>::class.java
                    )
                    if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                    successCallback?.invoke(auth)
                },
                {
                    errorCallback?.invoke(it)
                })

            requestQueue.add(stringRequest)
        }
    }

    fun service(service: String): Service {
        return Service(service, gson)
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

        user = auth.user
    }
}