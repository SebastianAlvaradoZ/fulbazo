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
- **Mapa (pantalla principal):** vista previa de las losas y lista de partidos
  próximos; botón flotante para crear un partido.
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

> Por ahora los datos son **de prueba y viven en memoria** (repositorios
> `Fake*`). Al cerrar la app se pierden los cambios.

### Pendientes

- **Mapa real** con la ubicación del usuario y los partidos ordenados por
  distancia (Google Maps u OpenStreetMap).
- **Firebase:** autenticación, base de datos (Firestore) y notificaciones push
  reales entre dispositivos.
- **Losas de Arequipa** registradas a mano, consumidas con **Retrofit** y
  guardadas en **Room** para usar la app sin conexión.
- **IA (Gemini)** para armar equipos balanceados según las estadísticas de los
  jugadores.
- **Diseño propio** ("verde cancha") con Material 3 Expressive.
- **Hilt** para la inyección de dependencias.

---

## Tecnologías

| Área | Herramientas |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose, Material 3, animaciones de Compose |
| Arquitectura | MVVM, `ViewModel`, `StateFlow`, corrutinas |
| Navegación | Navigation Compose |
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
  Hoy las implementan repositorios en memoria; más adelante se cambian por
  Firebase, Retrofit y Room sin tocar las pantallas.

```
app/src/main/java/com/vapsa/fulbazo/
├── data/
│   ├── local/            # Room (pendiente)
│   ├── remote/           # Retrofit / Firebase (pendiente)
│   └── repository/       # Implementaciones (por ahora Fake* en memoria)
├── di/                   # AppContainer (antes de migrar a Hilt)
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

### Claves y configuración

Las claves que se agreguen más adelante (por ejemplo la API key de Maps) irán en
`local.properties`, y `google-services.json` en `app/`. Ambos archivos están
en `.gitignore`: **nunca se suben al repositorio**.

### Usuario de prueba

Mientras se usan los datos de prueba hay un usuario ya creado, con estadísticas,
amigos e historial de partidos. Su correo y contraseña están en
`app/src/main/java/com/vapsa/fulbazo/data/repository/FakeAuthRepository.kt`.
También puedes crear una cuenta nueva desde **Regístrate**.

Para probar sin backend, la app simula a otros jugadores:

- Unos segundos después de iniciar sesión, un amigo de prueba te invita a un partido.
- Al crear un partido, dos jugadores de prueba piden unirse.

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

---

## Capturas

_Próximamente._

## Video de demostración

_Próximamente (YouTube)._

## Equipo

_Próximamente._
