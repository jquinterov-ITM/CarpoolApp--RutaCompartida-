# CarpoolApp

Aplicación Android de carpooling comunitario donde cualquier usuario puede publicar un viaje como conductor o unirse a uno como pasajero. Sin pagos — solo coordinación de rutas compartidas.

## Stack Tecnológico

| Capa         | Tecnología                                    |
|-------------|-----------------------------------------------|
| Lenguaje    | Kotlin 100%                                   |
| UI          | XML Views + ViewBinding                       |
| Arquitectura | MVVM + Clean Architecture (Data / Domain / UI) |
| Backend     | Firebase (Cloud Firestore + Auth + FCM)       |
| Caché local | Room (SQLite)                                 |
| DI          | Hilt                                          |
| Navegación  | Navigation Component (Single Activity)        |
| Async       | Coroutines + Flow / StateFlow                 |
| Listas      | RecyclerView + ListAdapter + DiffUtil         |
| Build       | Gradle con Kotlin DSL + Version Catalog       |

## Estructura del Proyecto

```
app/src/main/java/com/carpoolapp/
├── CarpoolApp.kt              # Application class (@HiltAndroidApp)
├── MainActivity.kt            # Single Activity + Bottom Navigation
├── data/
│   ├── local/
│   │   ├── dao/               # Room DAOs
│   │   ├── db/                # AppDatabase
│   │   └── entity/            # Room entities
│   ├── mapper/                # DTO <-> Domain mappers
│   ├── remote/
│   │   ├── dto/               # Firestore DTOs
│   │   └── firestore/         # Firestore DataSources
│   └── repository/            # Repository implementations
├── domain/
│   ├── model/                 # Domain models (data classes puras)
│   ├── repository/            # Repository interfaces
│   └── usecase/               # Casos de uso (uno por clase)
├── di/                        # Hilt modules
├── notifications/             # FCM Service
└── ui/
    ├── auth/                  # Login con email + OTP
    ├── home/                  # Feed de viajes en tiempo real
    ├── publicar/              # Formulario para crear viaje
    ├── buscar/                # Búsqueda por destino
    ├── detalle/               # Detalle del viaje + solicitar
    ├── mis_viajes/            # Viajes como conductor/pasajero
    ├── solicitudes/           # Conductor acepta/rechaza
    ├── perfil/                # Perfil del usuario
    └── common/                # BaseFragment, utilidades
```

## Requisitos

- Android Studio Hedgehog 2023.1.1 o superior
- JDK 17
- SDK Android: compileSdk 35, minSdk 26, targetSdk 35
- Firebase project (crear en [Firebase Console](https://console.firebase.google.com))

## Configuración Inicial

### 1. Firebase

1. Crear proyecto en [Firebase Console](https://console.firebase.google.com)
2. Agregar app Android con package name `com.carpoolapp`
3. Descargar `google-services.json` y copiar a `app/google-services.json`
4. Activar servicios:
   - **Authentication** → Sign-in method → **Email link (passwordless sign-in)**
   - **Firestore Database** → Create database → modo test (ajustar reglas después)
5. Crear los índices compuestos en Firestore (ver `docs/firebase.md`)

### 2. Emulador Firebase (desarrollo local)

```bash
# Instalar Firebase CLI
npm install -g firebase-tools

# Iniciar emuladores
cd CarpoolApp
firebase emulators:start
```

La app en modo debug se conecta automáticamente a los emuladores (`10.0.2.2:8080` para Firestore, `10.0.2.2:9099` para Auth).

### 3. Build

```bash
./gradlew build          # Build completo
./gradlew test           # Tests unitarios
```

## Modelo de Datos (Firestore)

```
/users/{userId}
/trips/{tripId}
/trips/{tripId}/requests/{requestId}
```

Ver esquema detallado en `docs/firebase.md`.

## Pantallas

| Pantalla       | Fragment                  | Descripción                                    |
|----------------|---------------------------|------------------------------------------------|
| Login          | `AuthFragment`            | Email → OTP por correo → Firebase Auth         |
| Home / Feed    | `HomeFragment`            | Listener Firestore en tiempo real              |
| Publicar viaje | `PublicarViajeFragment`   | Formulario → escribe en Firestore              |
| Buscar viaje   | `BuscarViajeFragment`     | Búsqueda por destino                           |
| Detalle viaje  | `ViajeDetalleFragment`    | Info + botón solicitar                         |
| Mis viajes     | `MisViajesFragment`       | Tabs: conductor / pasajero                     |
| Solicitudes    | `SolicitudesFragment`     | Conductor acepta/rechaza (Transaction)         |
| Perfil         | `PerfilFragment`          | Datos personales y vehículo                    |

## Reglas de Negocio

- **Rol dual**: cualquier usuario puede publicar viajes y ser pasajero
- **Sin pago**: la app solo coordina rutas
- **Cupo limitado**: `Transaction` de Firestore al aceptar solicitud
- **Solicitud única**: un pasajero no puede solicitar dos veces el mismo viaje
- **IDs**: Firestore genera IDs como `String`

## Documentación

- `docs/firebase.md` — Configuración completa de Firebase
- `docs/quality.md` — Estrategia de testing y calidad
- `docs/security.md` — Medidas de seguridad
- `prompts/` — Prompts y skills para desarrollo asistido
