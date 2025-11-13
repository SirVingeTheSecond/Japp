package com.example.japp.api

import android.content.Context
import com.example.japp.api.responses.HealthResponse
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.Response as RetrofitResponse
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.Date
import androidx.core.content.edit
import com.example.japp.api.responses.auth.AuthService
import com.example.japp.api.responses.expense.ExpenseService
import com.example.japp.api.responses.group.GroupService
import okhttp3.ResponseBody
import retrofit2.Converter


data class ErrorResponse(
    val error: String?,
    val message: String?,
    val timestamp: Long? = null
)

object ErrorUtils {
    fun parseError(response: RetrofitResponse<*>): ErrorResponse? {
        val retrofit = RetrofitClient.retrofit ?: return null

        return try {
            // Explicitly declare <ErrorResponse>
            val converter: Converter<ResponseBody, ErrorResponse> =
                retrofit.responseBodyConverter(
                    ErrorResponse::class.java,
                    arrayOfNulls(0)
                )

            response.errorBody()?.let { converter.convert(it) }
        } catch (e: Exception) {
            null
        }
    }
}

data class Credentials(
    val accessToken: String,
    val expiresAt: Date,
)

object CredentialsStorage {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_EXPIRES_AT = "expires_at"

    fun save(context: Context, credentials: Credentials) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                putString(KEY_ACCESS_TOKEN, credentials.accessToken)
                putLong(KEY_EXPIRES_AT, credentials.expiresAt.time)
                apply()
            }
    }

    fun load(context: Context): Credentials? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = Date(prefs.getLong(KEY_EXPIRES_AT, 0))
        return Credentials(token, expiresAt)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { clear() }
    }
}

interface JappService {
    @GET("health")
    suspend fun getHealth(): HealthResponse
}

// https://medium.com/@ratko.kostov21/jwt-authentication-in-android-using-retrofit-and-authenticator-b7b66e231295
// https://notificare.com/blog/2023/04/21/android-retrofit-refresh-authentication/

class OAuthAuthenticator(private val appContext: Context) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) {
            val credentials = CredentialsStorage.load(appContext) ?: return null

            // NOTE: this can be improved by checking the expiration date locally instead of
            // sending a request to the API which will result in a 401.

            // Adding the access token to the request.
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${credentials.accessToken}")
                .build()
        }

        // Use the authenticated original request.
        return response.request
    }

    private fun getCredentials(): Credentials {
        return Credentials("Idk?", Date())
        // TODO("Get the user credentials from local storage.")
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://japp-app-api.itnerd.net/api/"
    var retrofit: Retrofit? = null

    fun init(context: Context) {
        if (retrofit != null) return // already initialized

        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .authenticator(OAuthAuthenticator(context))
            .addInterceptor(interceptor)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService
        get() = retrofit!!.create(AuthService::class.java)

    val jappService: JappService
        get() = retrofit!!.create(JappService::class.java)

    val groupService: GroupService
        get() = retrofit!!.create(GroupService::class.java)

    val expenseService: ExpenseService
        get() = retrofit!!.create(ExpenseService::class.java)
}