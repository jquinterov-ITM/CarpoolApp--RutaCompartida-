# FICHA TÉCNICA — CarpoolApp (RutaCompartida)

## 1. DATOS GENERALES

| Campo | Valor |
|---|---|
| **Nombre del proyecto** | CarpoolApp / RutaCompartida |
| **Versión actual** | `beta 0.9` (versionCode: 3) |
| **Package** | `com.carpoolapp` |
| **Lenguaje** | Kotlin |
| **Arquitectura** | MVVM + Clean Architecture (3 capas: UI / Domain / Data) |
| **Inyección de dependencias** | Dagger Hilt |
| **Build System** | Gradle 9.0.0 + Kotlin DSL |
| **Version Catalog** | `gradle/libs.versions.toml` |

## 2. PLATAFORMA ANDROID

| Campo | Valor |
|---|---|
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 35 |
| **Compile SDK** | 35 |
| **Java Version** | 17 |
| **Kotlin Version** | 2.1.0 |
| **AGP (Android Gradle Plugin)** | 8.8.2 |
| **KSP** | 2.1.0-1.0.29 |
| **ViewBinding** | Habilitado |
| **ProGuard / R8** | Habilitado en release + shrinkResources |

## 3. DEPENDENCIAS PRINCIPALES

### Firebase (BOM 33.10.0)
| Servicio | Uso |
|---|---|
| **Firebase Authentication** | Email/Password + Google Sign-In |
| **Cloud Firestore** | Base de datos principal (colecciones: `trips`, `users`, `notifications`) |
| **Cloud Messaging (FCM)** | Notificaciones push + deep links |
| **Crashlytics** | Reporte de crashes |
| **Analytics** | Analíticas de uso |

### AndroidX / Jetpack
| Librería | Versión |
|---|---|
| Navigation (Fragment + UI + Safe Args) | 2.8.7 |
| Lifecycle (ViewModel + Runtime) | 2.8.7 |
| Room (Runtime + KTX + Compiler) | 2.6.1 |
| Core KTX | 1.15.0 |
| AppCompat | 1.7.0 |
| Material Design | 1.12.0 |
| ConstraintLayout | 2.2.1 |
| RecyclerView | 1.4.0 |
| CardView | 1.0.0 |
| SwipeRefreshLayout | 1.1.0 |

### Otras librerías
| Librería | Versión | Uso |
|---|---|---|
| **Hilt** | 2.53.1 | Inyección de dependencias |
| **Coroutines** | 1.9.0 | Programación asíncrona |
| **Coil** | 2.5.0 | Carga de imágenes |
| **OkHttp** | 4.12.0 | Peticiones HTTP (FCM server) |
| **Play Services Auth** | 20.7.0 | Google Sign-In |
| **Chrome Custom Tabs** | 1.7.0 | OAuth en navegador |

### Testing
| Librería | Versión |
|---|---|
| JUnit | 4.13.2 |
| MockK | 1.13.14 |
| Turbine | 1.2.0 |
| Espresso | 3.6.1 |
| Fragment Testing | 1.8.7 |

## 4. FIREBASE — CONFIGURACIÓN

| Campo | Valor |
|---|---|
| **Project ID** | `rutacompartida-32385` |
| **Project Number** | 658053612578 |
| **Storage Bucket** | `rutacompartida-32385.firebasestorage.app` |
| **Android App ID** | `1:658053612578:android:298990866d5585ceb3b67b` |

### Colecciones Firestore
| Colección | Subcolección | Contenido |
|---|---|---|
| `trips` | `requests` | Viajes publicados + solicitudes por viaje |
| `users` | — | Perfiles de usuario |
| `notifications` | — | Registro de notificaciones push |

## 5. PERMISOS

| Permiso | Justificación |
|---|---|
| `INTERNET` | Red / Firebase |
| `ACCESS_FINE_LOCATION` | Geolocalización (futura) |
| `ACCESS_COARSE_LOCATION` | Geolocalización aproximada |
| `POST_NOTIFICATIONS` | Notificaciones push (Android 13+) |

## 6. COMPONENTES DE LA APP

### Activity
| Clase | Descripción |
|---|---|
| `MainActivity` | Host con 4 NavHostFragments por tabs + BottomNavigationView. Gestiona auth state, permisos de notificación, deep links FCM. |

### Fragments (7)
| Fragment | Tab | Funcionalidad |
|---|---|---|
| `AuthFragment` | — | Login/Registro con Email/Password y Google Sign-In |
| `HomeFragment` | Home | Feed de viajes activos (listener Firestore en tiempo real) |
| `BuscarViajeFragment` | Buscar | Búsqueda de viajes por destino con filtrado en tiempo real |
| `ViajeDetalleFragment` | (compartido) | Detalle de viaje con transición MaterialContainerTransform. Vista conductor: solicitudes; vista pasajero: solicitar/cancelar |
| `PublicarViajeFragment` | Home | Formulario de publicación: origen, destino, asientos, tipo (inmediato/programado) |
| `MisViajesFragment` | Mis Viajes | Viajes agrupados por secciones: conductor, pasajero, finalizados |
| `SolicitudesFragment` | Mis Viajes | Lista de solicitudes de un viaje con aceptar/rechazar |
| `PerfilFragment` | Perfil | Edición de perfil: avatar, nombre, toggle conductor/pasajero, formulario de vehículo, estadísticas |

### ViewModels (7)
Todos usan `StateFlow<UiState>` + sealed classes para estados de UI.

| ViewModel | Estados UI |
|---|---|
| `AuthViewModel` | Idle, Enviando, EmailEnviado, Autenticado, Error, RegistroExitoso |
| `HomeViewModel` | Loading, Success, Error |
| `BuscarViajeViewModel` | Idle, Loading, Resultado, Error |
| `ViajeDetalleViewModel` | Loading, Success, EnviandoSolicitud, SolicitudExitosa, Finalizando, Cancelando, Error |
| `PublicarViajeViewModel` | Idle, Publicando, Exitoso, Error |
| `MisViajesViewModel` | Loading, Success, Error |
| `PerfilViewModel` | Loading, Success, Error, VehiculoActualizado, FotoActualizada, NombreActualizado, ConductorActualizado |

## 7. CAPA DE DOMINIO

### Modelos
| Modelo | Campos clave |
|---|---|
| `Usuario` | id, nombre, email, fotoUrl, vehiculo, esConductor, calificacion, viajesCompletados |
| `Viaje` | id, conductorId, origen, destino, fechaHora, asientosDisponibles, precio, tipo, estado, vehiculoConductor |
| `Solicitud` | id, tripId, pasajeroId, asientosSolicitados, mensaje, estado, createdAt |
| `Vehiculo` | marca, modelo, ano, color, placa, fotoUrl |

### Enums
| Enum | Valores |
|---|---|
| `TipoViaje` | INMEDIATO, PROGRAMADO |
| `ViajeEstado` | PROGRAMADO, ACTIVO, EN_PROGRESO, COMPLETADO, CANCELADO |
| `SolicitudEstado` | PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA |
| `MarcaVehiculo` | 23 marcas (TOYOTA...TESLA, OTRO) |
| `ColoresVehiculo` | 15 colores |

### Casos de Uso (7)
| Caso de Uso | Descripción |
|---|---|
| `GetFeedUseCase` | Obtiene feed de viajes activos (excluye propios y cancelados) |
| `PublicarViajeUseCase` | Crea un viaje y retorna su ID |
| `BuscarViajesUseCase` | Filtra feed por destino (case-insensitive) |
| `EnviarSolicitudUseCase` | Envía solicitud de ride, valida duplicados pendientes |
| `AceptarSolicitudUseCase` | Acepta solicitud con transacción atómica (decrementa asientos + agrega pasajero) |
| `CancelarViajeUseCase` | Cambia estado del viaje a CANCELADO |
| `FinalizarViajeUseCase` | Finaliza viaje, incrementa contadores de conductor y pasajeros |

## 8. CAPA DE DATOS

### Room (Base de datos local)
| Elemento | Detalle |
|---|---|
| **DB Name** | `carpoolapp.db` |
| **Entidades** | `UsuarioEntity` (4 campos), `ViajeEntity` (6 campos) |
| **DAOs** | `UsuarioDao` (get, insert, delete), `ViajeDao` (search, insertAll, deleteAll) |

### Firestore DataSources
- `FirestoreUsuarioDataSource` — CRUD de usuarios
- `FirestoreViajeDataSource` — CRUD de viajes + listener en tiempo real + seeding de datos demo
- `FirestoreSolicitudDataSource` — CRUD de solicitudes con transacciones atómicas
- `FirestoreAuthUtils` + `FirestoreSafe` — Wrappers de seguridad con logging de Crashlytics

### DTOs y Mappers
- `ViajeDto`, `SolicitudDto`, `UsuarioDto`, `VehiculoDto` — deserialización manual (`fromDocument()`)
- `ViajeMapper`, `SolicitudMapper`, `UsuarioMapper` — DTO ↔ Domain con Timestamp ↔ Long

### Data Seeder
- `DataSeeder.seedIfEmpty()` — Crea 5 viajes demo con conductores demo si Firestore está vacío
- `FirestoreViajeDataSource.seedDemoDataIfNeeded()` — 5 viajes adicionales en CDMX

## 9. NOTIFICACIONES

| Clase | Rol |
|---|---|
| `CarpoolFirebaseMessagingService` | Recibe mensajes FCM, muestra notificación de sistema con deep link a detalle de viaje |
| `SolicitudNotificationManager` | Monitorea viajes del conductor con listener Firestore, notifica nuevas solicitudes PENDIENTES |
| `UsuarioNotificationManager` | Escucha colección `notifications` para notificaciones de solicitud aceptada/rechazada |

## 10. NAVEGACIÓN

- **4 NavHosts independientes** para aislamiento entre tabs (Home, Buscar, MisViajes, Perfil)
- **NavGraphs**: `nav_graph.xml` (auth), `nav_home.xml`, `nav_buscar.xml`, `nav_mis_viajes.xml`, `nav_perfil.xml`
- Transiciones compartidas con **MaterialContainerTransform** en ViajeDetalleFragment

## 11. INTERFAZ DE USUARIO

### Layouts (14)
- `activity_main.xml` — ConstraintLayout + FrameLayout + BottomNavigationView
- `fragment_auth.xml` — Logo, formulario email/password, botones Google Sign-In
- `fragment_home.xml` — RecyclerView + FAB
- `fragment_buscar.xml` — Card de búsqueda + RecyclerView
- `fragment_viaje_detalle.xml` — Cards: conductor, ruta, detalles, pasajeros, solicitudes
- `fragment_publicar_viaje.xml` — Cards: ruta, detalles, toggle tipo viaje, date picker
- `fragment_perfil.xml` — Cards: avatar, nombre, stats, toggle conductor, vehículo
- `fragment_mis_viajes.xml` — RecyclerView con secciones
- `fragment_solicitudes.xml` — RecyclerView de solicitudes
- `item_viaje.xml` — Card: avatar, conductor, estado, ruta con iconos, asientos
- `item_solicitud.xml` — Card: pasajero, calificación, botones aceptar/rechazar
- `item_section_header.xml` — TextView de sección

### Adapters
| Adapter | ViewTypes |
|---|---|
| `ViajeAdapter` (DiffUtil) | Tipo único |
| `MisViajesAdapter` (DiffUtil) | Header + ViajeItem |
| `SolicitudAdapter` (DiffUtil) | Tipo único |
| `SolicitudesDetalleAdapter` | Tipo único (inline) |

### Temas y Recursos
- Tema: Material3 DayNight (`Theme.CarpoolApp`)
- 35 colores definidos (primario #1A73E8, secundario #34A853, fondo #F8F9FA, error #D93025)
- 14 drawables (shape backgrounds, iconos vectoriales)
- 45 strings (español)
- Íconos: `ic_launcher` personalizado (auto con 4 personas)
- Network Security: cleartext permitido para localhost/10.0.2.2/127.0.0.1

## 12. PATRONES ARQUITECTÓNICOS

1. **Hilt DI** — `@HiltAndroidApp` → `@AndroidEntryPoint` → `@HiltViewModel` → `@Module/@InstallIn(SingletonComponent)`
2. **MVVM** — Fragment observa `StateFlow<UiState>` con `repeatOnLifecycle(STARTED)`
3. **Repository Pattern** — Interfaces en Domain, implementaciones en Data, binding con Hilt
4. **CallbackFlow** — Listeners Firestore en tiempo real con `addSnapshotListener` + `awaitClose{}`
5. **Transacciones Firestore** — `runTransaction` para aceptar solicitud (atómico)
6. **FirestoreSafe** — Wrapper con logging Crashlytics para errores de permisos/precondiciones
7. **Persistent Notifications** — SharedPreferences para tracking de IDs ya notificados
8. **Multi-NavHost** — 4 NavHostFragments ocultos/mostrados, no recreados al cambiar tabs
9. **Imágenes Base64** — Fotos de perfil como data URIs en Firestore (no Firebase Storage)

## 13. ESTRUCTURA DE DIRECTORIOS

```
app/src/main/java/com/carpoolapp/
├── CarpoolApp.kt                          # @HiltAndroidApp
├── MainActivity.kt                        # Main entry point
├── config/
│   └── ClerkConfig.kt
├── data/
│   ├── local/ (dao/, db/, entity/)        # Room
│   ├── mapper/                            # DTO ↔ Domain
│   ├── remote/
│   │   ├── dto/                           # DTOs
│   │   └── firestore/                     # DataSources
│   ├── repository/                        # Implementaciones
│   └── seed/                              # DataSeeder
├── di/                                    # Hilt Modules
├── domain/
│   ├── model/                             # Models + Enums
│   ├── repository/                        # Interfaces
│   └── usecase/                           # Casos de uso
├── notifications/                         # FCM + Notification Managers
└── ui/
    ├── auth/                              # AuthFragment + ViewModel
    ├── buscar/                            # BuscarViajeFragment + ViewModel
    ├── common/                            # BaseFragment
    ├── detalle/                           # ViajeDetalleFragment + ViewModel
    ├── home/                              # HomeFragment + ViewModel
    ├── mis_viajes/                        # MisViajesFragment + ViewModel
    ├── perfil/                            # PerfilFragment + ViewModel
    ├── publicar/                          # PublicarViajeFragment + ViewModel
    └── solicitudes/                       # SolicitudesFragment + ViewModel
```

## 14. TESTS

| Test | Framework | Cobertura |
|---|---|---|
| `GetFeedUseCaseTest` | JUnit + runTest | Caso de uso retorna feed |
| `HomeViewModelTest` | MockK + Turbine | cargaFeed emite Success/Error |
| `PerfilViewModelTest` | MockK + Turbine | Carga perfil y actualiza vehículo |

---

**Resumen:** App Android nativa en Kotlin para compartir viajes (carpooling) con arquitectura MVVM + Clean Architecture, Firebase como backend (Auth, Firestore, FCM, Crashlytics), Room como caché local, Hilt para DI, y 7 fragments con sus ViewModels cubriendo el flujo completo: registro/login, publicación de viajes, búsqueda, solicitudes, gestión de viajes y perfil de usuario.
