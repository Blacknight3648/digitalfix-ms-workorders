# Documento de Diseno y Especificacion Tecnica: Microservicio de Ordenes de Trabajo (ms-digitalfix-workorders)

## 1. Informacion del Proyecto y Control de Versiones

### 1.1 Ficha Tecnica
* Nombre del Proyecto: DigitalFix - Plataforma para Ordenes de Trabajo de Mantencion Electrica
* Componente: ms-digitalfix-workorders (Microservicio de Gestion Operativa y Ordenes de Trabajo)
* Version del Documento: 1.1.0
* Estado: En Desarrollo / Calidad de Entrega Semestral
* Clasificacion: Confidencial - Uso Academico y Corporativo

### 1.2 Control de Cambios
| Version | Fecha | Autor | Descripcion del Cambio |
|---|---|---|---|
| 1.0.0 | 2026-10-13 | Equipo de Desarrollo DigitalFix | Definicion inicial de persistencia JPA y CRUD base de ordenes de trabajo. |
| 1.1.0 | 2026-10-17 | Equipo de Desarrollo DigitalFix | Estandarizacion de endpoints al idioma espanol, incorporacion de los modulos de Clientes, Proveedores, Ordenes de Servicio, Cuadrillas de Terreno bajo principios SOLID/ACID y documentacion OpenAPI/Swagger. |

---

## 2. Declaracion de Alcance del Proyecto (Project Scope Statement)

### 2.1 Proposito y Justificacion
El sistema DigitalFix responde a la necesidad de estandarizar la coordinacion y ejecucion de servicios de mantenimiento electrico y field service para una red de 20 empresas contratistas. El microservicio `ms-digitalfix-workorders` constituye el nucleo transaccional de dominio responsable de:
1. Administrar el registro y ciclo de vida de los clientes demandantes de servicio.
2. Mantener el maestro de proveedores de repuestos e insumos tecnicos.
3. Gestionar las Ordenes de Servicio (solicitudes contractuales de los clientes con tarifas asociadas).
4. Coordinar y despachar Cuadrillas de Terreno para ejecucion de faenas conjuntas bajo condiciones de concurrencia segura.
5. Gestionar las Ordenes de Trabajo (OT) tecnicas, controlando la asignacion de mano de obra y el consumo de repuestos en terreno.

### 2.2 Limites del Alcance (In Scope / Out of Scope)
* **Dentro del Alcance (In Scope):**
  * Persistencia relacional en base de datos Oracle Autonomous Database bajo esquema normalizado (Tercera Forma Normal - 3FN).
  * Validacion de integridad de datos chilenos (RUT con algoritmo modulo 11, sanitizacion de entradas y telefonos en formato E.164).
  * Maquina de estados finitos para Ordenes de Servicio y Ordenes de Trabajo.
  * Coordinacion y despacho atómico de Cuadrillas de Terreno con bloqueo pesimista a nivel de base de datos.
  * Exposicion de contratos REST en idioma espanol y documentacion viva mediante OpenAPI (Swagger UI).
* **Fuera del Alcance (Out of Scope - Delegado a otros componentes):**
  * Validacion perimetral del token JWT de Azure AD: recae tambien en el API Gateway y en `ms-digitalfix-bff`. Ademas, este servicio valida el JWT por su cuenta (defensa en profundidad), ver seccion 4.4.
  * Consumo analitico y auditoria de eventos: La emision y agregacion de eventos para reporteria y timeline es delegada a los microservicios `ms-digitalfix-report` y `ms-digitalfix-audit` mediante la infraestructura Kafka.
  * Notificaciones en tiempo real al cliente y despacho al tecnico: Delegado a `ms-digitalfix-notify` a traves de colas RabbitMQ.

---

## 3. Arquitectura del Sistema y Principios de Ingenieria

### 3.1 Pila Tecnologica (Tech Stack)
* Lenguaje de Programacion: Java 21 (LTS)
* Framework Base: Spring Boot 4.0.8 (Spring Data JPA, Spring Web MVC, Spring Validation)
* Motor de Base de Datos: Oracle Autonomous Database (Oracle 23ai Enterprise)
* Driver de Conexion: Oracle JDBC Driver (ojdbc17 version 23.9.0.25.07) con soporte PKI/TNS
* Gestion de Migraciones: Flyway Migration (flyway-database-oracle version 11.14.1)
* Documentacion de API: springdoc-openapi-starter-webmvc-ui version 3.1.0 (Swagger 3)
* Herramienta de Construccion: Apache Maven 3.9+ (con Maven Wrapper integrado)
* Contenedorizacion: Docker / Docker Compose con imagenes multi-etapa

### 3.2 Implementacion de Principios SOLID
* **Single Responsibility Principle (SRP):** Cada clase de dominio encapsula exclusivamente su regla de negocio. `Cuadrilla` vela por la disponibilidad y quorum del equipo; `OrdenTrabajo` rige el ciclo de vida de terreno; `Rut` valida el modulo 11 de forma inmutable; `SanitizadorTexto` limpia marcados sin alterar la logica de persistencia.
* **Open/Closed Principle (OCP):** La politica de transicion de estados (`PoliticaTransicionEstado`) desacopla las reglas de negocio de los controladores, permitiendo incorporar nuevas transiciones o restricciones sin modificar el codigo cliente.
* **Liskov Substitution Principle (LSP):** Todas las entidades heredan de `EntidadAuditable`, manteniendo el contrato uniforme de control de concurrencia mediante `@Version` y estampas temporales auditables.
* **Interface Segregation Principle (ISP):** Los repositorios de datos exponen contratos granulares adaptados a las consultas especificas por cliente o estado, evitando exponer metodos genericos sobrecargados.
* **Dependency Inversion Principle (DIP):** Los servicios de aplicacion dependen exclusivamente de abstracciones de repositorio inyectadas a traves del constructor, facilitando el desacoplamiento y la elaboracion de pruebas unitarias.

### 3.3 Garantias Transaccionales ACID
* **Atomicidad (Atomicity):** Cada caso de uso de actualizacion o asignacion se encuentra delimitado con la anotacion `@Transactional`. Ante excepciones de regla de negocio o violacion de restricciones, Spring revierte la totalidad de la transaccion (Rollback).
* **Consistencia (Consistency):** Se hace cumplir tanto en capa logica (Agregados DDD que protegen invariantes) como en capa fisica mediante restricciones relacionales de base de datos (`FOREIGN KEY`, `CHECK`, `UNIQUE constraints`).
* **Aislamiento (Isolation):** 
  * Se implementa bloqueo pesimista (`LockModeType.PESSIMISTIC_WRITE` -> `SELECT ... FOR UPDATE`) en `CuadrillaRepository` para evitar condiciones de carrera cuando dos supervisores intentan despachar la misma cuadrilla de manera simultanea.
  * Se implementa bloqueo optimista (`@Version`) en todas las entidades principales para evitar escrituras concurrentes que puedan sobreescribir datos obsoletos (`OptimisticLockingFailureException`).
* **Durabilidad (Durability):** La ejecucion de `saveAndFlush()` asegura la persistencia en disco y en los Redo Logs de Oracle Database tras la confirmacion de la transaccion.

---

## 4. Gestion de Configuracion y Seguridad de Secretos

### 4.1 Politica de Proteccion de Credenciales
De acuerdo con las politicas de seguridad del proyecto, ningun secreto, certificado o contrasena de acceso debe quedar versionado en el repositorio Git. Los siguientes artefactos estan formalmente excluidos a traves de `.gitignore`:
* Directorio `wallet/`: Contiene los certificados privados, llaves SSL y el archivo `tnsnames.ora`.
* Archivo `.env`: Contiene las credenciales reales de la base de datos y puertos locales.

### 4.2 Matriz de Variables de Entorno
| Variable de Entorno | Descripcion Tecnica | Valor por Defecto / Formato |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Cadena de conexion JDBC Oracle con alias TNS y ruta de wallet | `jdbc:oracle:thin:@<alias>_high?TNS_ADMIN=./wallet` |
| `SPRING_DATASOURCE_USERNAME` | Usuario administrador de esquema de la base de datos | `ADMIN` |
| `SPRING_DATASOURCE_PASSWORD` | Contrasena del usuario de base de datos | Credencial privada de entorno |
| `SERVER_PORT` | Puerto HTTP para ejecucion local | `8085` |
| `APP_PORT` | Puerto expuesto en el host mediante Docker Compose | `8085` |
| `CORS_ALLOWED_ORIGINS` | Lista blanca de origenes permitidos para navegadores | `http://localhost:4200` |
| `AZURE_TENANT_ID` | Tenant de Microsoft Entra ID que emite los tokens | `3fb8463e-...` |
| `AZURE_CLIENT_ID` | Client ID de la API (audiencia `aud` del JWT) | `9494b59c-...` |
| `MS_USER_URL` | URL de ms-user, de donde se obtiene el rol del usuario | `http://localhost:8082` |

**Puerto asignado:** `8085` en desarrollo local, unico dentro del ecosistema (ver la [tabla de puertos](https://github.com/SolgreyDuocUC/digitalfix-infrastructure#86-puertos-de-los-microservicios) en `digitalfix-infrastructure`). Dentro del contenedor la aplicacion siempre escucha en `8080`; Docker Compose la publica en `8085` en local y `docker-compose.prod.yaml` en `8081` en la EC2 de produccion.

### 4.3 Procedimiento de Configuracion Local
1. Obtener el archivo comprimido de la Wallet oficial entregado por el administrador de base de datos (por ejemplo, `Wallet_CLX44JOCT2DJMLZ9.zip`).
2. Descomprimir el contenido en el directorio `./wallet` en la raiz del microservicio. Debe contener:
   * `cwallet.sso`
   * `ewallet.p12`
   * `keystore.jks`
   * `ojdbc.properties`
   * `sqlnet.ora`
   * `tnsnames.ora`
   * `truststore.jks`
3. Crear el archivo `.env` a partir de la plantilla:
   ```bash
   cp .env.example .env
   ```
4. Completar los valores de conexion en `.env`. La aplicacion cargara este archivo automaticamente al arrancar mediante la directiva `spring.config.import: optional:file:.env[.properties]`.

---

### 4.4 Seguridad: validacion del JWT y autorizacion por rol

El servicio es un **OAuth2 Resource Server** con la misma configuracion que `ms-login` y `ms-user` ([`SecurityConfig`](src/main/java/df/digitalfix_ms_workorders/security/SecurityConfig.java)):

* **Validacion del token:** firma con las claves publicas del tenant, vigencia (`exp`/`nbf`), issuer del tenant (v1 o v2) y audiencia `AZURE_CLIENT_ID` o `api://AZURE_CLIENT_ID`.
* **Roles:** claim `roles` del token y rol del usuario en `ms-user` (`GET /api/user/me` con el mismo JWT, cache de 5 minutos). `SUPERADMIN` hereda todos los roles.
* **Errores:** `401` (token ausente, invalido o vencido) y `403` (rol insuficiente), en JSON.

| Recurso | Consultar (GET) | Crear / editar datos | Operar (estado, tecnico, cuadrilla, servicios, repuestos) y borrar |
|---|---|---|---|
| `/api/ordenes-trabajo`, `/api/ordenes-servicio` | Admin, Supervisor, Tecnico, Cliente | Admin, Supervisor, Cliente | Admin, Supervisor |
| `/api/clientes` | Admin, Supervisor, Cliente | Admin, Supervisor | Admin, Supervisor |
| `/api/cuadrillas`, `/api/proveedores` | Admin, Supervisor | Admin, Supervisor | Admin, Supervisor |

Swagger UI y el preflight CORS (`OPTIONS`) son publicos.

---

## 5. Especificacion de Interfaces de Servicio (API REST)

Todos los endpoints estan configurados en idioma espanol bajo el prefijo `/api/`.

### 5.1 Documentacion Interactiva OpenAPI (Swagger)
Una vez iniciada la aplicacion, la documentacion tecnica interactiva y el esquema OpenAPI se encuentran disponibles en:
* Interfaz de Usuario Swagger: `http://localhost:8085/swagger-ui.html`
* Especificacion OpenAPI (JSON): `http://localhost:8085/v3/api-docs`

### 5.2 Modulo de Clientes (`/api/clientes`)
| Metodo | Ruta | Descripcion | Codigo Exito |
|---|---|---|---|
| POST | `/api/clientes` | Registra un nuevo cliente validando RUT chileno (modulo 11). | 201 Created |
| GET | `/api/clientes` | Obtiene la nomina completa de clientes. | 200 OK |
| GET | `/api/clientes/{id}` | Retorna el detalle del cliente especificado por identificador. | 200 OK |
| PUT | `/api/clientes/{id}` | Actualiza datos de contacto y nombres del cliente. | 200 OK |
| DELETE | `/api/clientes/{id}` | Elimina el registro de un cliente si no posee historial. | 204 No Content |

### 5.3 Modulo de Proveedores (`/api/proveedores`)
| Metodo | Ruta | Descripcion | Codigo Exito |
|---|---|---|---|
| POST | `/api/proveedores` | Registra un nuevo proveedor de repuestos validando RUT de empresa. | 201 Created |
| GET | `/api/proveedores` | Obtiene el catalogo de proveedores registrados. | 200 OK |
| GET | `/api/proveedores/{id}` | Retorna informacion comercial y de contacto del proveedor. | 200 OK |
| PUT | `/api/proveedores/{id}` | Modifica la razon social, giro o datos de contacto. | 200 OK |
| DELETE | `/api/proveedores/{id}` | Elimina un proveedor si no tiene repuestos asociados a OTs. | 204 No Content |

### 5.4 Modulo de Cuadrillas de Terreno (`/api/cuadrillas`)
| Metodo | Ruta | Descripcion | Codigo Exito |
|---|---|---|---|
| POST | `/api/cuadrillas` | Registra una cuadrilla asignando su tecnico lider y miembros. | 201 Created |
| GET | `/api/cuadrillas` | Lista cuadrillas. Admite filtro por estado operativo (`?estado=DISPONIBLE`). | 200 OK |
| GET | `/api/cuadrillas/{id}` | Consulta el detalle y nomina de integrantes de la cuadrilla. | 200 OK |
| PUT | `/api/cuadrillas/{id}` | Actualiza datos de la cuadrilla (no permite desactivar si esta en terreno). | 200 OK |
| POST | `/api/cuadrillas/{id}/miembros/{idTecnico}` | Agrega un tecnico de apoyo al equipo. | 200 OK |
| DELETE | `/api/cuadrillas/{id}/miembros/{idTecnico}` | Remueve un trabajador (restringido para el tecnico lider). | 200 OK |
| DELETE | `/api/cuadrillas/{id}` | Elimina la cuadrilla (solo si no esta en terreno). | 204 No Content |

### 5.5 Modulo de Ordenes de Servicio (`/api/ordenes-servicio`)
| Metodo | Ruta | Descripcion | Codigo Exito |
|---|---|---|---|
| POST | `/api/ordenes-servicio` | Genera una orden de servicio comercial vinculada a un cliente. | 201 Created |
| GET | `/api/ordenes-servicio` | Lista ordenes de servicio. Permite filtros `?estado=` e `?idCliente=`. | 200 OK |
| GET | `/api/ordenes-servicio/{id}` | Detalle de la orden de servicio con desglose de lineas y monto total. | 200 OK |
| PUT | `/api/ordenes-servicio/{id}` | Modifica las observaciones de la orden (mientras sea editable). | 200 OK |
| PUT | `/api/ordenes-servicio/{id}/estado` | Cambia el estado de la orden respetando la matriz de transiciones. | 200 OK |
| PUT | `/api/ordenes-servicio/{id}/tecnico` | Asigna el tecnico responsable y transiciona el estado a `ASIGNADA`. | 200 OK |
| POST | `/api/ordenes-servicio/{id}/detalles` | Agrega un servicio contratado con cantidad y precio unitario pactado. | 201 Created |
| DELETE | `/api/ordenes-servicio/{id}/detalles/{idDetalle}` | Remueve una linea de servicio de la orden. | 200 OK |
| DELETE | `/api/ordenes-servicio/{id}` | Elimina la orden de servicio (restringido a estado `CREADA`). | 204 No Content |

### 5.6 Modulo de Ordenes de Trabajo (`/api/ordenes-trabajo`)
| Metodo | Ruta | Descripcion | Codigo Exito |
|---|---|---|---|
| POST | `/api/ordenes-trabajo` | Crea una Orden de Trabajo en terreno (estado inicial `CREADA`). | 201 Created |
| GET | `/api/ordenes-trabajo` | Lista ordenes de trabajo con soporte de filtros `?estado=` e `?idCliente=`. | 200 OK |
| GET | `/api/ordenes-trabajo/{id}` | Consulta de orden con servicios, mano de obra, cuadrilla y repuestos. | 200 OK |
| PUT | `/api/ordenes-trabajo/{id}` | Actualiza observaciones tecnicas de la OT. | 200 OK |
| PUT | `/api/ordenes-trabajo/{id}/estado` | Actualiza el estado operativo de terreno (requiere tecnico/cuadrilla para `ASIGNADA`). | 200 OK |
| PUT | `/api/ordenes-trabajo/{id}/cuadrilla` | Asigna cuadrilla mediante bloqueo pesimista y avanza a `ASIGNADA`. | 200 OK |
| POST | `/api/ordenes-trabajo/{id}/servicios` | Agrega una labor tecnica detallando precio de mano de obra y tecnico. | 201 Created |
| PUT | `/api/ordenes-trabajo/{id}/servicios/{idDetalle}/tecnico` | Asigna o reasigna un tecnico a un item especifico de mano de obra. | 200 OK |
| DELETE | `/api/ordenes-trabajo/{id}/servicios/{idDetalle}` | Quita un item de mano de obra de la orden. | 200 OK |
| POST | `/api/ordenes-trabajo/{id}/repuestos` | Registra repuestos utilizados asociando proveedor y cantidad consumida. | 201 Created |
| DELETE | `/api/ordenes-trabajo/{id}/repuestos/{idDetalle}` | Elimina un registro de repuesto de la OT. | 200 OK |
| DELETE | `/api/ordenes-trabajo/{id}` | Elimina la OT (solo habilitado si permanece en estado `CREADA`). | 204 No Content |

---

## 6. Matriz de Estados y Reglas de Negocio

### 6.1 Ciclo de Vida de la Orden de Trabajo
1. `CREADA`: Estado inicial al registrar la solicitud. Unicamente en este estado la orden puede eliminarse fisicamente de la base de datos.
2. `ASIGNADA`: Requiere contar obligatoriamente con una cuadrilla asignada o un tecnico asociado a los servicios tecnicos.
3. `EN_DESPLAZAMIENTO`: La cuadrilla o tecnico se traslada fisicamente hacia las dependencias del cliente.
4. `EN_EJECUCION`: El trabajo electrico se esta ejecutando en terreno. No se puede ingresar a este estado sin previa asignacion.
5. `CERRADA`: Estado terminal de finalizacion exitosa. Se estampa la fecha de cierre y se libera la cuadrilla asignada a estado `DISPONIBLE`.
6. `CANCELADA`: Estado terminal de anulacion. Puede invocarse desde estados previos y libera los recursos asociados.

### 6.2 Matriz de Transiciones Permitidas
| Estado Origen | CREADA | ASIGNADA | EN_DESPLAZAMIENTO | EN_EJECUCION | CERRADA | CANCELADA |
|---|---|---|---|---|---|---|
| **CREADA** | No | Si (con tecnico/cuadrilla) | No | No | No | Si |
| **ASIGNADA** | No | No | Si | Si | No | Si |
| **EN_DESPLAZAMIENTO** | No | No | No | Si | No | Si |
| **EN_EJECUCION** | No | No | No | No | Si | Si |
| **CERRADA** | No | No | No | No | No | No |
| **CANCELADA** | No | No | No | No | No | No |

---

## 7. Esquema de Base de Datos y Persistencia

La persistencia se gestiona en Oracle Database mediante Hibernate y Flyway. Cada microservicio del ecosistema DigitalFix mantiene una tabla de control independiente para migraciones; este servicio utiliza `flyway_schema_history_workorders`.

### 7.1 Modelo de Entidades Principales
| Tabla Relacional | Entidad JPA | Proposito en el Negocio |
|---|---|---|
| `CLIENTE` | `Cliente` | Datos de identificacion, RUN validado, contacto y direccion del cliente. |
| `PROVEEDOR` | `Proveedor` | Identificacion comercial, RUT de empresa y contacto de distribuidores de repuestos. |
| `CUADRILLA` | `Cuadrilla` | Agrupacion operativa de trabajadores de terreno, lider tecnico y estado de disponibilidad. |
| `CUADRILLA_MIEMBRO` | Coleccion `@ElementCollection` | Relacion de tecnicos asignados a cada cuadrilla de terreno. |
| `ORDEN_SERVICIO` | `OrdenServicio` | Solicitud contractual y valorizacion de servicios para el cliente. |
| `DETALLE_ORDEN_SERVICIO` | `DetalleOrdenServicio` | Desglose de servicios, cantidades y precios unitarios. |
| `ORDEN_TRABAJO` | `OrdenTrabajo` | Ejecucion tecnica de terreno, estado, cuadrilla asignada y observaciones. |
| `DETALLE_OT_SERVICIO` | `DetalleOtServicio` | Desglose de actividades tecnicas, mano de obra y tecnico asignado. |
| `DETALLE_OT_REPUESTO` | `DetalleOtRepuesto` | Registro de materiales y repuestos utilizados vinculados a su proveedor. |

---

## 8. Guia de Operacion y Despliegue

### 8.1 Ejecucion Local sin Contenedores
Requisitos: JDK 21 instalado, Maven Wrapper y Wallet configurada.
```bash
# Ejecucion directa de la aplicacion
./mvnw spring-boot:run

# En sistemas operativos Windows:
mvnw.cmd spring-boot:run
```

### 8.2 Compilacion y Verificacion de Calidad
```bash
# Compilacion y ejecucion de pruebas de verificacion
./mvnw clean test

# Generacion del paquete ejecutable JAR
./mvnw clean package -DskipTests
```

### 8.3 Ejecucion con Docker y Docker Compose
El microservicio incluye definicion de contenedores multi-etapa asegurando que las imagenes no contengan secretos embebidos:
```bash
# 1. Asegurar la existencia de la red interna compartida del ecosistema
docker network create digitalfix-net

# 2. Construir y levantar el contenedor en modo detach
docker compose up -d --build
```
El contenedor monta el directorio local `./wallet` en `/app/wallet:ro` como volumen de solo lectura, manteniendo el aislamiento seguro de las credenciales de base de datos.

### 8.4 Despliegue Continuo en AWS EC2 (Pipeline CI/CD)
El proyecto cuenta con un flujo automatizado de despliegue continuo mediante GitHub Actions ([`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)) configurado sobre la rama `deploy`:
1. El pipeline establece conexion segura mediante protocolo SSH hacia la instancia EC2 de AWS.
2. Descarga la version autorizada en el servidor destino.
3. Ejecuta `docker compose -f docker-compose.prod.yaml up -d --build`.
4. El contenedor se reinicia de manera transparente sin alterar el archivo `.env` ni los certificados de la carpeta `wallet/` alojados de manera aislada en el host EC2.
5. En la EC2 el servicio queda publicado en el puerto `8081` (abierto en el Security Group), a diferencia del `8085` de desarrollo local.

Swagger: http://localhost:8085/swagger-ui.html
Especificación OpenAPI: http://localhost:8085/v3/api-docs
Infraestructura de microservicios: docker network create digitalfix-net
