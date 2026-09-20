package com.antoniocompany.financetracker.data

import android.content.Context
import com.antoniocompany.financetracker.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Construye el cliente de la API, una sola vez por proceso.
 *
 * Los tiempos de espera son generosos a proposito: la API corre en un plan
 * gratuito de Azure con base de datos serverless que se pausa sola, asi que la
 * primera peticion tras un rato de inactividad puede tardar bastante en
 * responder mientras todo despierta.
 */
object ApiClient {

    private var instance: FinanceTrackerApi? = null

    fun get(context: Context): FinanceTrackerApi {
        return instance ?: build(context.applicationContext).also { instance = it }
    }

    private fun build(context: Context): FinanceTrackerApi {
        val session = SessionStore(context)

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor(session))
            .apply {
                // Solo en depuracion: en una release esto escribiria el token
                // en los logs del dispositivo, donde cualquier aplicacion con
                // permisos de lectura de logs podria verlo.
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinanceTrackerApi::class.java)
    }

    /**
     * Anade la cabecera Authorization cuando hay sesion. Se lee el token en
     * cada peticion y no al construir el cliente, porque cambia al entrar y al
     * salir y el cliente se crea una sola vez.
     */
    private fun authInterceptor(session: SessionStore) = Interceptor { chain ->
        val token = session.token

        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        chain.proceed(request)
    }
}
