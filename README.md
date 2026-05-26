# CarpoolApp

Aplicación Android de carpooling comunitario (Kotlin, MVVM). Este README está actualizado al estado actual del desarrollo y contiene instrucciones rápidas de prueba y debugging.

## Estado actual (resumen)
- Optimistic UI implementada para `Publicar viaje`: el repositorio emite eventos locales inmediatamente usando `MutableSharedFlow` (replay=1, extraBufferCapacity=64). Archivo: `app/src/main/java/com/carpoolapp/data/repository/ViajeRepositoryImpl.kt`.
- `MisViajesViewModel` parcheado para aislar collectors en jobs separados y evitar que un fallo en una query cancele otros collectors. Archivo: `app/src/main/java/com/carpoolapp/ui/mis_viajes/MisViajesViewModel.kt`.
- `firestore.rules.copy` añadido con reglas sugeridas para staging; publicar en la consola de Firestore es necesario para permisos correctos.
- `firestore.indexes.json` incluido para deploy de índices necesarios (collectionGroup queries). Ver `firestore.indexes.json` en la raíz del proyecto.
- APK debug instalada y probada en emulador(s) con `./gradlew installDebug`.

## Stack Tecnológico (rápido)

- Kotlin, Android (XML + ViewBinding)
- MVVM + Clean-ish layers (data/domain/ui)
- Firebase: Firestore, Auth, FCM
- Room (local cache)
- Hilt (DI)
- Coroutines + Flow / StateFlow
- Gradle (Kotlin DSL)

## Estructura relevante

```
app/src/main/java/com/carpoolapp/
├── data/repository/    # Implementaciones (incluye optimist notifier)
├── data/remote/firestore/  # Firestore data sources
├── domain/repository/  # Interfaces
└── ui/
        ├── mis_viajes/     # ViewModel y Fragment claves para collectors
        └── publicar/       # PublicarViajeFragment / ViewModel
    ├── auth/                  # Login con correo/contraseña + Google
    ├── home/                  # Feed de viajes en tiempo real
    ├── publicar/              # Formulario para crear viaje
    ├── buscar/                # Búsqueda por destino
    ├── detalle/               # Detalle del viaje + solicitar
    ├── mis_viajes/            # Viajes como conductor/pasajero
    ├── solicitudes/           # Conductor acepta/rechaza
    ├── perfil/                # Perfil del usuario
    └── common/                # BaseFragment, utilidades
```

## Cómo probar rápidamente (pasos reproducibles)

1) Compilar e instalar debug (desde la carpeta `CarpoolApp`):

```powershell
cd CarpoolApp
.\gradlew installDebug
```

2) Reiniciar la app o abrir en el emulador:

```powershell
adb -s emulator-5554 shell am force-stop com.carpoolapp
adb -s emulator-5554 shell am start -n com.carpoolapp/.MainActivity
```

3) Ver logs filtrados (útil para validar optimistic UI y collectors):

```powershell
adb -s emulator-5554 logcat -v time ViajeRepo:D MisViajesVM:D Firestore:V *:S
```

4) Para pruebas con dos emuladores (dos cuentas):

```powershell
# listar AVDs
%CANDROID_SDK_ROOT%\\emulator\\emulator.exe -list-avds

# iniciar AVD con puerto alterno (ej. 5556)
%CANDROID_SDK_ROOT%\\emulator\\emulator.exe -avd NombreDelAVD -port 5556 -netdelay none -netspeed full

# instalar APK en el segundo emulador
adb -s emulator-5556 install -r app\\build\\outputs\\apk\\debug\\app-debug.apk

# logcat del segundo emulador
adb -s emulator-5556 logcat -v time ViajeRepo:D MisViajesVM:D Firestore:V *:S
```

5) Qué buscar en los logs (pégalo en la conversación si pides ayuda):

- `D/ViajeRepo: emitted created event id=...`  (repo registró emisión optimista)
- `D/MisViajesVM: Received created event for uid=... id=...`  (ViewModel recibió evento optimista)
- `FAILED_PRECONDITION` (falta índice para collectionGroup → revisar `firestore.indexes.json`/Console)
- `PERMISSION_DENIED` (reglas Firestore no publicadas o mal configuradas)

## Firestore — reglas e índices

- Reglas sugeridas para staging: `CarpoolApp/firestore.rules.copy` (pégalas y publica en Firebase Console para entornos de staging).
- Índices compuestos: `firestore.indexes.json` incluido. Deploy con:

```bash
firebase deploy --only firestore:indexes --project <your-project-id>
```

Verificar en la consola que el índice `collectionGroup` esté en estado `READY` antes de ejecutar queries que lo necesiten.

## Problemas conocidos y soluciones rápidas

- Problema: `collectionGroup` queries lanzaban `FAILED_PRECONDITION` y pueden cancelar collectors.
    - Solución: crear índice en Firebase Console o con `firebase deploy` y esperar a que compile (READY).
- Problema: `PERMISSION_DENIED` al leer/escribir.
    - Solución: publicar `firestore.rules.copy` o ajustar reglas de acuerdo al entorno.
- Problema: emitted event en repo pero no recibido por ViewModel.
    - Causas comunes: versión de la app en el emulador no actualizada (reinstalar con `installDebug`), collector cancelado por excepción (patchado en `MisViajesViewModel`), o race-condition de arranque.

## Qué revisar si algo falla (checklist rápido)

1. Reinstalar APK: `./gradlew installDebug`.
2. Ver logs en el emulador (pasos arriba). Pegar las líneas relevantes.
3. Verificar índices en Firebase Console (`collectionGroup` → READY).
4. Verificar reglas publicadas (Firestore → Rules).
5. Confirmar que el `ViajeRepositoryImpl` no se instancia múltiples veces inesperadamente (Hilt scope: singleton por diseño).

## Archivos importantes a revisar

- `app/src/main/java/com/carpoolapp/data/repository/ViajeRepositoryImpl.kt` — emisión optimista con `MutableSharedFlow`.
- `app/src/main/java/com/carpoolapp/ui/mis_viajes/MisViajesViewModel.kt` — collectors aislados, manejo de errores.
- `CarpoolApp/firestore.rules.copy` — reglas sugeridas para staging (pegar en Console).
- `firestore.indexes.json` — índices Firestore.
- `app/src/main/java/com/carpoolapp/data/remote/firestore/FirestoreViajeDataSource.kt` — queries que usan `collectionGroup`.

---

Si quieres, creo un archivo `DEV_STATUS.md` con este resumen en la raíz del repo (no lo añadiré sin tu confirmación). También puedo abrir los logs en ambos emuladores y capturar una sesión si quieres continuar ahora.

Gracias — listo para seguir cuando digas.
