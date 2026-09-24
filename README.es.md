# 📱 FinanceTracker Android

[English](README.md) · **Español**

[![CI](https://img.shields.io/github/actions/workflow/status/antonicr1986/financetracker-android/ci.yml?branch=main&style=for-the-badge&label=CI&logo=githubactions&logoColor=white)](https://github.com/antonicr1986/financetracker-android/actions)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)

Cliente Android de [FinanceTracker](https://github.com/antonicr1986/FinanceTracker),
una API REST de finanzas personales escrita en .NET 8.

**Este es el segundo cliente de esa API.** El primero es
[financetracker-web](https://github.com/antonicr1986/financetracker-web), en
Next.js. Escribir un segundo consumidor es lo que convierte una API en un
contrato: los mismos endpoints, los mismos codigos de error y las mismas reglas
de negocio, alcanzados desde otro lenguaje y otra plataforma.

Hay una **cuenta de demostracion publica**, la misma que usa el cliente web, a
un toque desde la pantalla de acceso. La API se duerme tras 20 minutos sin uso,
asi que la primera entrada del dia tarda unos segundos mientras despiertan el
servicio y la base de datos — la pantalla lo explica mientras espera.

## ✨ Que hace

- **Acceso con JWT**, con entrada directa a la cuenta de demostracion.
- **Registro de cuenta**, validado igual que en el cliente web; despues inicia
  sesion con los mismos datos y abre el panel.
- **Selector de meses**: un chip por cada mes con datos, de modo que el
  historico entero esta a mano y no solo el mes en curso.
- **Totales del mes elegido** — ingresos, gastos y balance — derivados en el
  dispositivo a partir del historico completo.
- **Lista de movimientos** con categoria, fecha e importe con signo.
- **Alta de un movimiento**, con selector de fecha nativo y un desplegable de
  categorias filtrado por el tipo elegido: la API rechaza un gasto con
  categoria de ingresos, asi que ni se ofrece.
- **Editar y borrar un movimiento**: al pulsarlo se abre relleno en el mismo
  formulario; borrar pide confirmacion antes.
- **Deslizar para recargar**, y boton de reintentar cuando la carga falla.
- **La misma estetica que el cliente web**: la paleta slate de Tailwind
  llevada a los roles de Material 3, tarjetas blancas sobre fondo gris y una
  barra superior comun en todas las pantallas.
- **Tema claro y oscuro**, que se cambia desde la barra superior y se recuerda;
  mientras no se elige ninguno, sigue al del sistema.
- **Espanol e ingles**, que se cambian desde la barra superior. Los textos, las
  fechas y los importes siguen al idioma (`es-ES` / `en-GB`, siempre en euros),
  igual que en el cliente web.

## 🚧 Que no hace

El cliente completo es el web. Este es deliberadamente mas pequeno, construido
alrededor de lo que un movil hace bien: consultar rapido y anotar en el momento.

- No hay presupuestos, ni filtros, ni desglose por categoria.
- Las categorias tienen que existir ya: la aplicacion las ofrece pero no puede
  crear ninguna.

## 🧰 Stack

- **Kotlin**, minSdk 24
- **Retrofit 3** con Gson, sobre OkHttp
- **Corrutinas**, para que cada llamada de red se lea de arriba abajo
- **Material 3** y view binding
- **JUnit** para las pruebas unitarias

## 📁 Estructura del proyecto

    app/src/main/java/.../
      data/           Cliente de Retrofit, sesion y repositorio
        model/        Los DTOs de la API
      domain/         Agrupacion por mes y totales — Kotlin puro
      ui/             Barra superior, tema, idioma, adaptador y formatos
      BaseActivity    Cambio de tema e idioma sin parpadeo
      LoginActivity, RegisterActivity, MainActivity, NewTransactionActivity

`domain/` no tiene ni una referencia a Android, y es a proposito. Eso es lo que
permite que sus pruebas corran en la JVM en milisegundos y sin emulador, y es la
misma separacion que hace el cliente web con `derive.ts`.

## 🧠 Decisiones que merece la pena leer

**Las fechas nunca se convierten en objetos de fecha.** La API devuelve
`2026-09-22T00:00:00` y el mes se saca recortando la cadena. Construir una fecha
aplicaria la zona horaria del dispositivo y meteria el dia 1 de un mes en el
anterior a quien este al oeste de Greenwich. El selector de fecha es el mismo
problema por el otro lado: devuelve la medianoche **UTC**, asi que ese valor se
formatea con un formateador fijado a UTC.

**El token vive en SharedPreferences normales.** Google deprecio
`EncryptedSharedPreferences` en 2025 en favor de las APIs de la plataforma. El
almacenamiento privado de una aplicacion ya esta aislado por el sandbox; lo que
anadia el cifrado era proteccion frente a copias de seguridad y extraccion
fisica. Lo primero se cubre con `android:allowBackup="false"`, y frente a lo
segundo la mitigacion real es que el token caduca en 60 minutos.

**Un 401 significa dos cosas distintas.** En la pantalla de acceso todavia no
habia sesion, asi que es una contrasena incorrecta. En el panel si habia token y
la API lo ha rechazado, asi que la sesion ha caducado y se vuelve al acceso.
Mismo codigo de estado, dos mensajes.

**Cambiar de tema o de idioma no recrea la pantalla.** Android los aplica con
`recreate()`, que quita la ventana vieja antes de dibujar la nueva, y ese hueco
se ve como un parpadeo. Las pantallas declaran `uiMode|locale` en
`configChanges`, asi que Android solo les avisa, y `BaseActivity` las reinicia
con un fundido del sistema, llevando su estado guardado en el intent. El panel
reutiliza los movimientos que ya tenia en lugar de volver a llamar a la API.

**El idioma usa los idiomas por aplicacion de AppCompat.** Lo guarda el sistema
en Android 13+ (y aparece tambien en los ajustes de la aplicacion) y
`AppLocalesMetadataHolderService` en versiones anteriores, asi que la app no
guarda copia propia. El tema, que no tiene equivalente en el sistema, va en su
propio fichero de preferencias, separado de la sesion, para que cerrar sesion
no lo reinicie.

## 🧪 Pruebas

`./gradlew test` — 10 pruebas unitarias, sin emulador.

Seis cubren las derivaciones por mes. Cuatro cubren el bucle de paginacion
contra una API falsa, incluido el caso que el mundo real esconde: con menos de
100 movimientos nunca se pide la segunda pagina, asi que un fallo ahi solo
apareceria el dia que un usuario acumulara datos.

## 🔄 Automatizacion

- **CI** en cada push y pull request: escaneo de secretos, pruebas unitarias y
  un APK de depuracion, descargable desde la propia ejecucion.
- **Escaneo de secretos** con gitleaks sobre el historial completo, con la misma
  configuracion que los demas repositorios de este proyecto.
- El JDK esta fijado al mismo con el que se desarrolla, de modo que el CI y el
  escritorio compilan igual.

## ⚙️ Ejecutar en local

Android Studio, JDK 17 o superior, y un dispositivo o emulador con Android 7.0+.

    ./gradlew test
    ./gradlew assembleDebug

La URL de la API se fija al compilar en `app/build.gradle.kts`, como
`API_BASE_URL`. Apunta a la API desplegada; se cambia ahi para dirigir una
compilacion a una local.

## ✍️ Autor

Antonio Company - [GitHub](https://github.com/antonicr1986) ·
[LinkedIn](https://www.linkedin.com/in/antoniocompany/)
