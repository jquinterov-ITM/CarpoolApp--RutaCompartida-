# Calidad – CarpoolApp

## Estrategia de Testing

### Pirámide de Tests

- **Unitarios** (`test/`): casos de uso, ViewModels, mappers. Mockean el DataSource.
- **Integración** (`androidTest/`): DataSources reales contra el emulador local de Firebase.
- **UI / E2E** (`androidTest/`): flujos críticos con Espresso.

## Tests Unitarios (JUnit4 + MockK + Turbine)

### Casos mínimos

| Módulo       | Happy path                                           | Casos de error / borde                              |
|--------------|------------------------------------------------------|-----------------------------------------------------|
| Viajes       | Publicar viaje → repositorio recibe el modelo correcto | Asientos ≤ 0, destino vacío                       |
| Viajes       | Buscar por destino → filtra correctamente            | Sin resultados → lista vacía (no Error)             |
| Viajes       | Cancelar viaje → actualiza estado                    | Cancelar viaje completado → excepción               |
| Solicitudes  | Enviar solicitud → estado PENDIENTE                  | Segunda solicitud → excepción                       |
| Solicitudes  | Aceptar solicitud → transaction                      | Cupo = 0 → excepción                                |
| Mappers      | Dto → Domain y viceversa sin pérdida                | Enums desconocidos → excepción controlada           |

## Estándares de Código

| Herramienta       | Alcance        | Propósito                                   |
|-------------------|----------------|---------------------------------------------|
| `Detekt`          | Kotlin         | Linter estático                             |
| `Android Lint`    | Kotlin + XML   | Errores de API, rendimiento                 |
| `JaCoCo`          | Kotlin         | Cobertura de código                         |

### Umbrales de Cobertura

| Capa             | Cobertura mínima |
|------------------|-----------------|
| Casos de uso     | ≥ 90%           |
| ViewModels       | ≥ 80%           |
| Mappers          | 100%            |

## Comandos

| Comando                                              | Descripción                                          |
|------------------------------------------------------|------------------------------------------------------|
| `./gradlew test`                                     | Tests unitarios                                      |
| `./gradlew connectedAndroidTest`                     | Tests de integración y UI                            |
| `./gradlew jacocoTestReport`                         | Reporte de cobertura                                 |
| `./gradlew detekt`                                   | Análisis estático                                    |
| `./gradlew lint`                                     | Android Lint                                         |
