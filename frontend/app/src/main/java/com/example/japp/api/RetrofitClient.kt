package com.example.japp.api

import android.R.attr.type
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.japp.api.responses.ActivityType
import com.example.japp.api.responses.Currency
import com.example.japp.api.responses.ExpenseCategory
import com.example.japp.api.responses.GroupRole
import com.example.japp.api.responses.HealthResponse
import com.example.japp.api.responses.MessageType
import com.example.japp.api.responses.SettlementStatus
import com.example.japp.api.responses.SplitType
import com.example.japp.api.responses.UserStatus
import com.example.japp.api.responses.activity.ActivityService
import com.example.japp.api.responses.auth.AuthService
import com.example.japp.api.responses.expense.ExpenseService
import com.example.japp.api.responses.group.GroupService
import com.example.japp.api.responses.message.MessageService
import com.example.japp.api.responses.settlement.SettlementService
import com.example.japp.api.responses.user.UserService
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import java.lang.reflect.Type
import java.util.Date
import retrofit2.Response as RetrofitResponse


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

class AuthInterceptor(val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = CredentialsStorage.load(context)

        if (credentials == null) {
            return chain.proceed(chain.request())
        }

        val newReq = chain.request().newBuilder().apply {
            credentials?.let {
                header("Authorization", "Bearer ${it.accessToken}")
            }
        }.build()

        return chain.proceed(newReq)
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
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(interceptor)
            .build()

        val enums = listOf(
            SplitType::class.java,
            Currency::class.java,
            UserStatus::class.java,
            GroupRole::class.java,
            ActivityType::class.java,
            SettlementStatus::class.java,
            ExpenseCategory::class.java,
            MessageType::class.java
        )

        val builder = GsonBuilder()

        val adapter = JsonDeserializer { json, type, _ ->
            val enumClass = type as Class<*>
            val fromString = enumClass.getMethod("fromString", String::class.java)
            fromString.invoke(null, json.asString)
        }

        for (e in enums) {
            builder.registerTypeAdapter(e, adapter)
        }

        val gson = builder.create()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val jappService: JappService
        get() = retrofit!!.create(JappService::class.java)

    val activityService: ActivityService
        get() = retrofit!!.create(ActivityService::class.java)

    val authService: AuthService
        get() = retrofit!!.create(AuthService::class.java)

    val expenseService: ExpenseService
        get() = retrofit!!.create(ExpenseService::class.java)

    val groupService: GroupService
        get() = retrofit!!.create(GroupService::class.java)

    val messageService: MessageService
        get() = retrofit!!.create(MessageService::class.java)

    val settlementService: SettlementService
        get() = retrofit!!.create(SettlementService::class.java)

    val userService: UserService
        get() = retrofit!!.create(UserService::class.java)
}