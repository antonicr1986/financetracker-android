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

    /**
     * Que hacer cuando la API rechaza la sesion (401 con token): caducada o
     * cerrada desde otro dispositivo. Lo fija FinanceTrackerApp para llevar al
     * acceso; aqui no se conoce ninguna pantalla.
     */
    @Volatile
    var onSessionRejected: (() -> Unit)? = null

    /**
     * Que hacer cuando la API responde 426: esta version ya no esta soportada.
     * Lo fija FinanceTrackerApp para mostrar el aviso de actualizar.
     */
    @Volatile
    var onUpgradeRequired: (() -> Unit)? = null

    fun get(context: Context): FinanceTrackerApi {
        return instance ?: build(context.applicationContext).also { instance = it }
    }

    /**
     * Solo para los tests de las pantallas: sustituye la API por una falsa
     * (null vuelve a la real). Asi se prueban sin red ni servidor.
     */
    internal fun replaceForTests(api: FinanceTrackerApi?) {
        instance = api
    }

    private fun build(context: Context): FinanceTrackerApi {
        val session = SessionStore(context)

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor(session) { onSessionRejected?.invoke() })
            .addInterceptor(clientVersionInterceptor(BuildConfig.VERSION_NAME) { onUpgradeRequired?.invoke() })
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
     *
     * Interno y no privado para poder probarlo (ApiClientAuthTest).
     */
    internal fun authInterceptor(
        session: SessionStore,
        onSessionRejected: () -> Unit = {},
    ) = Interceptor { chain ->
        val token = session.token

        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(request)

        // Un 401 con token: la API ya no acepta esta sesion. Se cierra aqui, en
        // un solo sitio, para cualquier pantalla (antes solo lo hacia el panel).
        // Sin token (login, registro) un 401 es otra cosa y no se toca. Si hay
        // varias peticiones en vuelo, solo la primera avisa: las demas ya no
        // encuentran el mismo token.
        if (response.code == 401 && token != null) {
            val rejected = synchronized(session) {
                (session.token == token).also { if (it) session.clear() }
            }
            if (rejected) onSessionRejected()
        }

        response
    }

    /**
     * Dice a la API que app y que version son (X-Client-Platform y
     * X-Client-Version), para que pueda exigir una version minima. Avisa en
     * cada 426: si el usuario cierra el aviso y vuelve a intentarlo, lo ve otra
     * vez. Que no se abran varios a la vez lo controla FinanceTrackerApp.
     *
     * Interno y no privado para poder probarlo (ApiClientVersionTest).
     */
    internal fun clientVersionInterceptor(
        version: String,
        onUpgradeRequired: () -> Unit = {},
    ) = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("X-Client-Platform", "android")
            .header("X-Client-Version", version)
            .build()

        val response = chain.proceed(request)
        if (response.code == 426) onUpgradeRequired()
        response
    }
}
