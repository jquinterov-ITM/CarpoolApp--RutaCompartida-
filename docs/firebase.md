# Configuración Firebase – CarpoolApp

## Servicios habilitados

| Servicio                     | Uso en CarpoolApp                                              |
|------------------------------|----------------------------------------------------------------|
| **Cloud Firestore**          | Base de datos principal compartida entre todos los usuarios    |
| **Firebase Authentication**  | Login con correo/contraseña y Google                           |
| **Firebase Cloud Messaging** | Notificaciones push de solicitudes aceptadas/rechazadas        |

## Setup en Android

1. Crear proyecto en [Firebase Console](https://console.firebase.google.com).
2. Agregar app Android con el package name `com.carpoolapp`.
3. Descargar `google-services.json` y colocarlo en `app/`.
4. Sincronizar Gradle.

## Modelo de Datos en Firestore

### Colecciones

```
/users/{userId}                    # Perfil del usuario
/trips/{tripId}                    # Viajes publicados
/trips/{tripId}/requests/{reqId}   # Solicitudes (subcollection del viaje)
```

### Esquemas

**`/users/{userId}`**
```json
{
  "nombre": "Ana García",
  "email": "ana@email.com",
  "vehiculo": "Toyota Corolla gris",
  "fcmToken": "dG9rZW4...",
  "createdAt": "Timestamp"
}
```

**`/trips/{tripId}`**
```json
{
  "conductorId": "uid_firebase",
  "conductorNombre": "Ana García",
  "origen": "Envigado",
  "destino": "El Poblado",
  "fechaHora": "Timestamp",
  "asientosDisponibles": 3,
  "tipo": "PROGRAMADO",
  "estado": "PROGRAMADO",
  "createdAt": "Timestamp"
}
```

**`/trips/{tripId}/requests/{requestId}`**
```json
{
  "pasajeroId": "uid_firebase",
  "pasajeroNombre": "Carlos López",
  "estado": "PENDIENTE",
  "createdAt": "Timestamp"
}
```

## Índices de Firestore

| Colección | Campos indexados                          | Tipo       | Uso                              |
|-----------|-------------------------------------------|------------|----------------------------------|
| `trips`   | `estado` ASC + `conductorId` ASC + `fechaHora` ASC | Compuesto | Feed excluyendo viajes propios |
| `trips`   | `conductorId` ASC + `fechaHora` DESC      | Compuesto  | Mis viajes como conductor        |
| `trips/{id}/requests` | `pasajeroId` ASC + `estado` ASC | Compuesto | Viajes como pasajero          |
