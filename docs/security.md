# Seguridad – CarpoolApp

## Autenticación

- Firebase Auth con correo/contraseña y Google.
- Los proveedores Email/Password y Google deben estar habilitados en Firebase Console > Authentication > Sign-in method.
- No se usa acceso por link de correo.

## Reglas de Firestore

- `firestore.rules` con default deny.
- `request.auth != null` requerido en todas las operaciones.
- Validación de ownership: `conductorId == request.auth.uid`, `pasajeroId == request.auth.uid`.

## Almacenamiento Seguro

| Dato                        | Almacenamiento correcto          |
|-----------------------------|----------------------------------|
| Token FCM                   | Firestore `/users/{uid}.fcmToken`|
| Datos del perfil            | Room (caché local)               |
| Credenciales de Firebase    | `google-services.json` (fuera del repo) |

## Permisos

- `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS` (API 33+).
- No se solicita `ACCESS_BACKGROUND_LOCATION`.

## Ofuscación (R8)

- Minificación y shrinking habilitados en release.
- DTOs de Firestore no se ofuscan.

## Prevención de Fugas

- Logs sensibles solo en DEBUG.
- `FLAG_SECURE` en MainActivity para evitar captura de pantalla.
- Backup excluye datos sensibles.
