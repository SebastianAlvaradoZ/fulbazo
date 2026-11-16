# Fulbazo ⚽

> **README provisional.** Se irá completando a medida que avance el proyecto.

**Fulbazo** es una red social para encontrar gente con quién jugar fútbol en las
**losas deportivas gratuitas de Arequipa**. Puedes ver los partidos cercanos,
crear uno como anfitrión, invitar a tus amigos, pedir unirte a partidos de
otros y calificar a tus compañeros después de jugar.

Proyecto final del curso de Desarrollo de Aplicaciones Móviles.

---

## Funcionalidades

### Implementadas

- **Cuenta:** registro (nombre, apellido, apodo, fecha de nacimiento con edad
  mínima de 13 años, distrito de Arequipa, celular peruano, correo, contraseña y
  foto opcional), inicio de sesión, edición del perfil y cierre de sesión.
- **Mapa (pantalla principal):** **Google Maps** centrado en Arequipa con un
  marcador por losa (verde si tiene partidos próximos) y la lista de partidos
  próximos, con filtro por losa; botón flotante para crear un partido.
- **Ubicación:** con el permiso de ubicación, el mapa muestra dónde estás y los
  partidos se ordenan **del más cercano al más lejano** ("a 2.4 km"). Si no lo
  das, un aviso explica para qué sirve y permite activarlo (o abrir Ajustes), y
  los partidos se ordenan por fecha.
- **Detalle de losa:** al tocar un marcador del mapa (o desde el filtro) se abre
  una hoja con su dirección, si tiene iluminación y sus próximos partidos, y el
  botón **"Crear partido aquí"** con la losa ya elegida.
- **Partidos:**
  - Crear partido (losa, fecha, hora, cupos, nivel y descripción). Quien lo crea
    es el anfitrión.
  - Detalle del partido con la lista de jugadores.
  - Solicitudes para unirse: el anfitrión acepta o rechaza, respetando los cupos.
  - Salir de un partido.
  - **Mis partidos:** próximos, mis solicitudes e historial.
- **Amigos y grupos:** buscar jugadores por apodo, enviar, aceptar y rechazar
  solicitudes de amistad, y crear grupos para invitar a varios a la vez.
- **Invitaciones:** invitar amigos o grupos a un partido, bandeja de
  invitaciones y **notificación del sistema** al recibir una.
- **Estadísticas:**
  - *Mis estadísticas:* pierna buena, posición y autoevaluación del 1 al 5
    (7 habilidades de campo y 5 de arquero).
  - *Evaluar jugadores:* después de un partido, cada jugador califica a sus
    compañeros como jugador de campo o como arquero.
  - El perfil muestra **"Cómo me veo"** (autoevaluación) y **"Cómo me ven"**
    (promedio de la comunidad, separado en campo y arquero, con número de votos).
- **Perfil de otros jugadores:** sus estadísticas, partidos jugados juntos,
  botón de amistad e invitación a uno de tus partidos.
- **Animaciones de Compose:** transiciones entre pantallas, fundido entre
  estados de carga, error y contenido, listas animadas y números que cambian
  suavemente.

> Cuentas, partidos, solicitudes, amigos, grupos, invitaciones y estadísticas
> se guardan en **Firebase** y se actualizan en tiempo real entre teléfonos. Las
> losas todavía son una lista local hasta que lleguen de Retrofit + Room.

### Pendientes

- **Notificaciones push** con la app cerrada (FCM). Hoy la invitación llega
  como notificación mientras la app está abierta.
- **Losas de Arequipa** registradas a mano, consumidas con **Retrofit** y
  guardadas en **Room** para usar la app sin conexión.
- **IA (Gemini)** para armar equipos balanceados según las estadísticas de los
  jugadores.
- **Diseño propio** ("verde cancha") con Material 3 Expressive.

---

## Tecnologías

| Área | Herramientas |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose, Material 3, animaciones de Compose |
| Arquitectura | MVVM, `ViewModel`, `StateFlow`, corrutinas |
| Inyección de dependencias | Hilt (con KSP) |
| Backend | Firebase Authentication y Cloud Firestore |
| Navegación | Navigation Compose |
| Mapa y ubicación | Google Maps (`maps-compose`), Fused Location Provider |
| Imágenes | Coil (fotos de perfil) y Photo Picker del sistema |
| Notificaciones | `NotificationCompat` con permiso `POST_NOTIFICATIONS` |
| Pruebas | JUnit 4, `kotlinx-coroutines-test` y pruebas instrumentadas de Compose |

---

## Arquitectura

La app sigue **MVVM** por capas:

- Las **pantallas** (Composables) solo dibujan el estado y envían eventos.
- Los **ViewModels** exponen un `StateFlow<UiState>` (carga, contenido, vacío y
  error), validan los datos y lanzan el trabajo en `viewModelScope`.
- Los ViewModels dependen de **interfaces de repositorio** (`domain/repository`).
  Hoy las implementan repositorios de Firebase (`Firebase*` y `Firestore*`);
  las pruebas usan los mismos ViewModels con repositorios en memoria (`Fake*`).
- **Hilt** crea los repositorios (`@Singleton`, en los módulos de `di/`) y los
  ViewModels (`@HiltViewModel`); las pantallas los piden con `hiltViewModel()`.

```
app/src/main/java/com/vapsa/fulbazo/
├── data/
│   ├── local/            # Room (pendiente)
│   ├── remote/           # Utilidades de Firestore (colecciones, fechas, Flows)
│   └── repository/       # Firebase*/Firestore* (app) y Fake* (pruebas)
├── di/                   # Módulos de Hilt (repositorios y scope de la app)
├── domain/
│   ├── model/            # Usuario, partido, losa, estadísticas, invitaciones…
│   ├── repository/       # Interfaces de repositorio
│   └── validation/       # Reglas de validación del usuario
├── notifications/        # Notificación local de invitaciones
└── ui/
    ├── auth/             # Login y registro
    ├── components/       # Componentes reutilizables
    ├── friends/          # Amigos y grupos
    ├── invitations/      # Bandeja de invitaciones
    ├── map/              # Pantalla principal
    ├── match/            # Crear, detalle, mis partidos y evaluar
    ├── navigation/       # Rutas, NavHost y transiciones
    ├── player/           # Perfil de otro jugador
    ├── profile/          # Mi perfil y editar perfil
    ├── stats/            # Mis estadísticas
    └── theme/            # Tema de Material 3
```

---

## Cómo ejecutar

### Requisitos

- Android Studio compatible con **AGP 9.3**.
- **SDK de Android 37** (compileSdk y targetSdk 37). El mínimo es **Android 8.0
  (minSdk 26)**.
- JDK 11 o superior.

### Pasos

1. Clona el repositorio:
   ```bash
   git clone https://github.com/SebastianAlvaradoZ/fulbazo.git
   ```
2. Ábrelo en Android Studio y espera la sincronización de Gradle.
3. Ejecuta la configuración **app** en un emulador o un teléfono.

### Clave de Google Maps (`MAPS_API_KEY`)

1. En [Google Cloud Console](https://console.cloud.google.com/), crea un
   proyecto, activa **Maps SDK for Android** y crea una **API key**.
2. Restringe la clave a apps Android con el paquete `com.vapsa.fulbazo` y la
   huella SHA-1 de tu certificado de depuración (`./gradlew signingReport`).
3. Agrega esta línea a `local.properties`, en la raíz del proyecto:
   ```properties
   MAPS_API_KEY=tu_clave_aqui
   ```
4. Sincroniza Gradle y vuelve a ejecutar la app.

`local.properties` está en `.gitignore`: **la clave nunca se sube al
repositorio**. `app/build.gradle.kts` la lee de ahí y la pone en el
`AndroidManifest.xml` (`com.google.android.geo.API_KEY`). Si falta, se usa el
valor de `local.defaults.properties` (`DEFAULT_API_KEY`) y la app no se cae:
muestra un aviso y una vista simple de las losas, que se pueden seguir tocando.

### Firebase (`google-services.json`)

La cuenta y el perfil del jugador usan **Firebase Authentication** (correo y
contraseña) y **Cloud Firestore**.

1. En la [consola de Firebase](https://console.firebase.google.com/), crea un
   proyecto y agrega una app Android con el paquete `com.vapsa.fulbazo` y la
   huella SHA-1 de depuración (`./gradlew signingReport`).
2. Descarga `google-services.json` y ponlo en `app/`. Está en `.gitignore`:
   **no se sube al repositorio**.
3. En **Authentication**, activa el proveedor **Correo electrónico/contraseña**.
4. En **Firestore Database**, crea la base en modo producción (ubicación
   `southamerica-west1`) y publica las reglas de [`firestore.rules`](firestore.rules)
   en la pestaña **Reglas**.

Colecciones de Firestore (las reglas de `firestore.rules` revisan cada escritura):

| Colección | Contenido |
|---|---|
| `usuarios/{uid}` | Perfil público: nombre, apodo, distrito y foto |
| `usuarios/{uid}/privado/datos` | Celular, correo y fecha de nacimiento (solo los ve su dueño) |
| `apodos/{apodo}` | Reserva del apodo para que no se repita |
| `partidos/{id}` | Losa, anfitrión, fecha, cupos, nivel, estado y jugadores |
| `solicitudes/{partido}_{usuario}` | Pedido para unirse; solo el anfitrión lo acepta o rechaza |
| `invitaciones/{partido}_{invitado}` | Invitación a un partido |
| `solicitudesAmistad/{de}_{para}` | Solicitud de amistad |
| `amistades/{a}_{b}` | Amistad aceptada (`miembros`) |
| `grupos/{id}` | Grupos de amigos (solo los ve su dueño) |
| `estadisticas/{uid}` | Autoevaluación ("Cómo me veo") |
| `calificaciones/{partido}_{evaluador}_{evaluado}` | Calificación de un compañero; con ellas se calcula "Cómo me ven" |

La foto de perfil se comprime (JPEG de 256 px) y se guarda dentro del perfil,
sin Firebase Storage. Las fechas se guardan con la hora de Perú.

### Probar con dos cuentas

Crea dos cuentas desde **Regístrate** (por ejemplo, una en el emulador y otra en
tu teléfono). Con una crea un partido e invita a la otra: la invitación llega al
instante, con una notificación si la app está abierta. Con la otra pide unirte a
un partido y acepta la solicitud desde la primera.

---

## Pruebas

```bash
# Pruebas unitarias (ViewModels, validaciones y repositorios)
./gradlew testDebugUnitTest

# Pruebas instrumentadas de Compose (necesitan un emulador o teléfono conectado)
./gradlew connectedDebugAndroidTest
```

Si hay varios dispositivos conectados, elige uno con `ANDROID_SERIAL`; por
ejemplo, `ANDROID_SERIAL=emulator-5554`.

Las pruebas instrumentadas no usan Firebase: con Hilt testing
(`FakeRepositoryModule`) todos los repositorios son los `Fake*` en memoria, con
un usuario de prueba (`demo@fulbazo.com`) que ya tiene amigos, partidos y
estadísticas. Así no necesitan internet ni escriben en la base real.

Las pruebas conceden los permisos de ubicación y notificaciones. Para la prueba
del orden por distancia, el emulador necesita una ubicación, por ejemplo la
plaza de Cayma:

```bash
adb emu geo fix -71.545 -16.372
```

---

## Capturas

_Próximamente._

## Video de demostración

_Próximamente (YouTube)._

## Equipo

_Próximamente._
