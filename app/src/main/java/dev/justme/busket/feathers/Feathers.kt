package dev.justme.busket.feathers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.*
import com.google.gson.Gson
import dev.justme.busket.feathers.responses.AuthenticationSuccessResponse
import dev.justme.busket.feathers.responses.User
import org.json.JSONObject
import kotlin.reflect.typeOf

enum class BackendService {

}

class Feathers(context: Context) {
    private val cache = DiskBasedCache(context.cacheDir, 1024 * 1024) // 1MB cap
    private val network = BasicNetwork(HurlStack())
    val requestQueue = RequestQueue(cache, network).apply {
        start()
    }
    private val context = context;
    private val gson = Gson()
    private var user: User? = null;

    public fun authenticate(
        email: String,
        password: String,
        successCallback: (AuthenticationSuccessResponse) -> Unit,
        errorCallback: ((e: VolleyError) -> Unit)? = null
    ) {
        val mainKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        fun success(auth: AuthenticationSuccessResponse) {
            EncryptedSharedPreferences.create(
                context,
                "encrypted_shared_prefs",
                mainKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).edit().putString("access_token", auth.accessToken).apply()

            user = auth.user;
            successCallback(auth)
        }

        tryAuthenticateWithAccessToken(mainKey, {
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
        successCallback: (AuthenticationSuccessResponse) -> Unit,
        errorCallback: ((e: VolleyError) -> Unit)? = null
    ) {

        val url = "https://busket-beta.bux.at/authentication"
        val data =
            "{\n\"strategy\":\"local\",\n\"email\":\"$email\",\n\"password\":\"$password\"\n}"

        val stringRequest = JsonObjectRequest(
            Request.Method.POST, url, JSONObject(data),
            {
                successCallback(
                    gson.fromJson(
                        it.toString(),
                        AuthenticationSuccessResponse::class.java
                    )
                )
            },
            {
                errorCallback?.invoke(it)
            })

        requestQueue.add(stringRequest);
    }

    private fun tryAuthenticateWithAccessToken(
        mainKey: MasterKey,
        successCallback: (AuthenticationSuccessResponse) -> Unit,
        errorCallback: ((e: VolleyError) -> Unit)? = null
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
                    successCallback(
                        gson.fromJson(
                            it.toString(),
                            AuthenticationSuccessResponse::class.java
                        )
                    )
                },
                {
                    errorCallback?.invoke(it)
                })

            requestQueue.add(stringRequest);
        }
    }
}