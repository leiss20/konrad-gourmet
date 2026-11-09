# Konrad Gourmet · Sistema de abastecimiento (v3, sobre el esquema de 21 tablas)

Arquitectura en capas con los patrones **Facade**, **Adapter** y **Repository**. Java 21, Spring Boot 4.1, PostgreSQL 18, Flyway y Spring Security.

## Ejecutar
1. En pgAdmin crea una base **nueva** llamada `konrad_gourmet2`.
2. En `src/main/resources/application.properties` pon tu contraseña de PostgreSQL.
3. Abre esta carpeta (la que tiene `pom.xml`) en VS Code y ejecuta `AbastecimientoApplication` (botón Run).
4. Flyway crea las 21 tablas y los datos iniciales. Abre http://localhost:8080

## Usuarios de prueba (clave: Konrad2026)
| Correo | Rol | Puede |
|---|---|---|
| jefe@konrad.co | Jefe de cocina | Crear platos y solicitudes de alimentos |
| mesero@konrad.co | Mesero | Solo registrar pedidos |
| auxiliar@konrad.co | Auxiliar de compras | Consultar solicitudes y pedir cotizaciones |
| dircompras@konrad.co | Director de compras | Validar cotizaciones y generar órdenes de compra |
| dircomercial@konrad.co | Director comercial | Tablero BAM |
| admin@konrad.co | Administrador | Parametrización, auditoría y logs |

Los permisos se validan en el servidor (`@PreAuthorize`), no solo en el menú.

## Estructura
`web` (controladores) → `facade` → `service` (reglas) → `repository` (SQL sobre el esquema real, Spring JdbcClient) · `adapter` (Ministerio, simulado) · `config` (seguridad).
Migraciones en `src/main/resources/db/migration` (V1 esquema, V2 datos, V3 parámetros). Para cambiar el esquema, crea V4, V5… nunca edites una migración ya ejecutada.

## Pendiente
Inventario y alarmas del 25 %, promociones por tendencias, correos y PDF, cliente SOAP real del Ministerio, CSRF y JWT.
