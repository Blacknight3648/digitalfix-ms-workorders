# DigitalFix - ms-workorders

Microservicio de gestion de ordenes de trabajo del sistema **DigitalFix**, una plataforma de mantencion electrica. Expone un CRUD basico sobre las ordenes de trabajo (creacion, consulta, edicion, cambio de estado y eliminacion), construido con **Spring Boot** y **Spring Data JPA** sobre una base de datos **Oracle**.

Corresponde al microservicio de dominio `ms-digitalfix-workorders` del caso, dentro del flujo `Angular (MSAL) -> AWS API Gateway -> ms-digitalfix-bff -> microservicios de dominio`.

---

## Alcance actual

Este servicio implementa el CRUD de ordenes de trabajo, nada mas. Dos cosas quedan fuera a proposito, para las siguientes etapas del caso:

- **Validacion de JWT de Azure AD**: en la arquitectura objetivo, el unico componente expuesto directamente detras del API Gateway es `ms-digitalfix-bff`, que es quien valida el token antes de reenviar la peticion a los microservicios de dominio. Por eso este servicio no incluye Spring Security ni valida el JWT por su cuenta (a diferencia de `digitalfix-ms-login`, que si lo hace porque fue construido para probarse de forma independiente).
- **Timeline / auditoria de eventos**: el historial de eventos de una orden (quien la creo, asigno, etc.) le corresponde a `ms-digitalfix-audit`, que lo construye consumiendo eventos de Kafka. Este servicio no persiste ni expone ese historial.
- **Reglas de negocio del flujo de estados**: el caso exige que no se pueda pasar a `EN_EJECUCION` sin haber asignado un tecnico antes. Ese control todavia no esta implementado en el servidor (ver nota en `WorkOrderService.updateStatus`); por ahora el cambio de estado es libre.

---

## Stack tecnologico

- **Framework**: Spring Boot (spring-boot-starter-parent 4.0.8)
- **Lenguaje**: Java 21
- **Persistencia**: Spring Data JPA + Oracle Database (driver `ojdbc17`)
- **Migraciones de base de datos**: Flyway (`flyway-database-oracle`)
- **Documentacion de API**: springdoc-openapi (Swagger UI)
- **Utilitarios**: Lombok
- **Build**: Maven (con Maven Wrapper)

---

## Requisitos

- JDK 21
- Maven (o usar el wrapper `mvnw` / `mvnw.cmd` incluido en el proyecto)
- Una base de datos Oracle accesible
- Docker (opcional, para ejecutar el servicio en contenedor)

---

## Base de datos centralizada

Este servicio **no levanta su propio contenedor de Oracle**: usa el mismo Oracle que ya levanta `digitalfix-ms-login`, para que ambos microservicios (y los que se agreguen despues) compartan una sola base de datos en el entorno de desarrollo local, tal como lo pide el caso.

Para evitar que las migraciones de Flyway de distintos microservicios se pisen entre si (ambos comparten el mismo usuario/schema de Oracle, `system`), cada servicio usa su propia tabla de historial:

- `digitalfix-ms-login` -> `flyway_schema_history` (la tabla por defecto)
- `digitalfix-ms-workorders` -> `flyway_schema_history_workorders` (configurado en `application.yaml`)

---

## Configuracion

Variables de entorno leidas en [`src/main/resources/application.yaml`](src/main/resources/application.yaml):

| Variable | Descripcion | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de la base de datos Oracle | `jdbc:oracle:thin:@//oracle-db:1521/FREEPDB1` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `system` |
| `SPRING_DATASOURCE_PASSWORD` | Contrasena de la base de datos | `system` |
| `CORS_ALLOWED_ORIGINS` | Origen(es) permitidos para llamadas del navegador (separados por coma) | `http://localhost:4200` |

---

## Quick start (desarrollo local)

### Con Docker Compose (recomendado)

Este servicio depende de la red y el contenedor de Oracle que crea `digitalfix-ms-login`. El orden importa:

```bash
# 1. Primero, en digitalfix-ms-login: crea la red "digitalfix-net" y levanta Oracle
cd ../digitalfix-ms-login
docker compose up -d

# 2. Despues, en digitalfix-ms-workorders: se une a esa misma red
cd ../digitalfix-ms-workorders
docker compose up -d --build
```

El servicio queda disponible en `http://localhost:8081` (el host usa el puerto 8081 porque `ms-login` ya ocupa el 8080; puertas adentro del contenedor sigue siendo el 8080).

### Sin Docker

```bash
export SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
export SPRING_DATASOURCE_USERNAME=system
export SPRING_DATASOURCE_PASSWORD=system

./mvnw spring-boot:run
```

El servicio queda disponible en `http://localhost:8080`. Al iniciar, Flyway aplica las migraciones ubicadas en `src/main/resources/db/migration`.

### Compilar y empaquetar

```bash
./mvnw clean package
```

### Ejecutar pruebas

```bash
./mvnw test
```

---

## Endpoints

Todos bajo el prefijo `/api/workorders`. Ninguno requiere autenticacion por ahora (ver "Alcance actual").

| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/workorders` | Crea una orden de trabajo. El estado inicial siempre es `CREADA`. |
| GET | `/api/workorders` | Lista todas las ordenes. Admite el filtro opcional `?estado=` (ej. `?estado=ASIGNADA`). |
| GET | `/api/workorders/{id}` | Obtiene una orden por id. `404` si no existe. |
| PUT | `/api/workorders/{id}` | Edita cliente, servicio y/o tecnico de una orden existente. |
| PUT | `/api/workorders/{id}/status` | Cambia el estado de la orden. |
| DELETE | `/api/workorders/{id}` | Elimina una orden. `204` sin cuerpo. |

### Estados posibles

`CREADA`, `ASIGNADA`, `EN_DESPLAZAMIENTO`, `EN_EJECUCION`, `CERRADA`, `CANCELADA` (mismos valores que `WorkOrderStatus` en el frontend).

### POST /api/workorders

Cuerpo de la peticion ([`WorkOrderRequest`](src/main/java/df/digitalfix_ms_workorders/dto/WorkOrderRequest.java)):

```json
{
  "cliente": "Comercial Rojas Ltda.",
  "servicio": "Revision tablero electrico",
  "tecnico": "P. Munoz"
}
```

`cliente` y `servicio` son obligatorios; `tecnico` es opcional. Respuesta `201 Created` con la orden creada ([`WorkOrderResponse`](src/main/java/df/digitalfix_ms_workorders/dto/WorkOrderResponse.java)):

```json
{
  "id": "1",
  "cliente": "Comercial Rojas Ltda.",
  "servicio": "Revision tablero electrico",
  "tecnico": "P. Munoz",
  "estado": "CREADA",
  "fechaCreacion": "2026-09-14T05:28:10.000807"
}
```

Nota: `id` se devuelve como texto (no como numero) a proposito, para calzar directo con el modelo `WorkOrder` ya usado en el frontend Angular.

### PUT /api/workorders/{id}/status

```json
{ "estado": "ASIGNADA" }
```

### Errores

Los errores de validacion (`400`) y los recursos no encontrados (`404`) se devuelven con el mismo formato ([`ErrorResponse`](src/main/java/df/digitalfix_ms_workorders/dto/ErrorResponse.java)), manejado por [`GlobalExceptionHandler`](src/main/java/df/digitalfix_ms_workorders/exception/GlobalExceptionHandler.java):

```json
{
  "status": 400,
  "message": "Error de validacion de los datos",
  "timestamp": "2026-09-14T10:00:00",
  "errors": ["El cliente es obligatorio"]
}
```

---

## Base de datos

La migracion inicial [`V1__crear_tabla_work_orders.sql`](src/main/resources/db/migration/V1__crear_tabla_work_orders.sql) crea la tabla `WORK_ORDERS`:

| Columna | Tipo | Descripcion |
|---|---|---|
| `ID` | `NUMBER(19,0)` | Identidad autogenerada, llave primaria. |
| `CLIENTE` | `VARCHAR2(150)` | Cliente que solicita el servicio. |
| `SERVICIO` | `VARCHAR2(200)` | Descripcion del servicio tecnico. |
| `TECNICO` | `VARCHAR2(150)` | Tecnico asignado (puede ser nulo). |
| `ESTADO` | `VARCHAR2(30)` | Estado actual de la orden. |
| `FECHA_CREACION` | `TIMESTAMP` | Fecha y hora de creacion. |

Incluye un indice `IDX_WORK_ORDERS_ESTADO` sobre `ESTADO` para acelerar el filtro por estado.

---

## Dockerizacion

El `Dockerfile` es identico en estructura al de `digitalfix-ms-login`: build multi-stage con Maven (`maven:3.9.8-eclipse-temurin-21-alpine`) y runtime liviano (`eclipse-temurin:21-jre-alpine`) corriendo con un usuario no root, expuesto en el puerto `8080`.

El `docker-compose.yaml` de este servicio **no define su propio Oracle**: se une, como red externa, a `digitalfix-net`, la red que crea el `docker-compose.yaml` de `digitalfix-ms-login`. Por eso ese proyecto debe levantarse primero (ver "Quick start").

---

## Estructura del proyecto

```text
digitalfix-ms-workorders/
├── Dockerfile
├── docker-compose.yaml
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/df/digitalfix_ms_workorders/
    │   │   ├── config/       # CORS (WebConfig)
    │   │   ├── controller/   # Endpoints REST del CRUD
    │   │   ├── dto/          # Objetos de transferencia (request/response/error)
    │   │   ├── entity/       # Entidad JPA WorkOrder
    │   │   ├── enums/        # WorkOrderStatus
    │   │   ├── exception/    # Manejo global de excepciones
    │   │   ├── repository/   # Repositorio JPA
    │   │   ├── service/      # Logica de negocio del CRUD
    │   │   └── DigitalfixMsWorkordersApplication.java
    │   └── resources/
    │       ├── application.yaml
    │       └── db/migration/ # Scripts de Flyway
    └── test/
        └── java/df/digitalfix_ms_workorders/
```

---

## Comandos disponibles

```bash
./mvnw spring-boot:run       # ejecutar el servicio en modo desarrollo
./mvnw clean package         # compilar y generar el jar (con pruebas)
./mvnw clean package -DskipTests  # generar el jar sin ejecutar pruebas
./mvnw test                  # ejecutar las pruebas
```

---

## Despliegue en AWS EC2 (Ubuntu)

El servicio se despliega con Docker Compose sobre una instancia EC2 Ubuntu ya
existente, y se actualiza automaticamente via GitHub Actions con cada push a
la rama `deploy`.

### 1. Preparar la instancia EC2 (una sola vez)

En la instancia Ubuntu (por SSH):

```bash
# Docker + plugin de compose
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Permite correr docker sin sudo (cierra sesion y vuelve a entrar despues)
sudo usermod -aG docker $USER

# Clonar el repo en la ruta que se usara para el despliegue
git clone https://github.com/Blacknight3648/digitalfix-ms-workorders.git
cd digitalfix-ms-workorders
git checkout deploy

# Variables de entorno reales del servidor (nunca se suben a git)
cp .env.example .env
nano .env   # completar SPRING_DATASOURCE_URL/USERNAME/PASSWORD, CORS_ALLOWED_ORIGINS, APP_PORT
```

> Este servicio depende de la red Docker `digitalfix-net` y el contenedor
> `oracle-db` que crea `digitalfix-ms-login`. Ese proyecto debe estar
> levantado primero en el mismo EC2 (o el host de Oracle en `.env` debe
> apuntar a una base accesible desde la instancia).

Primer levantamiento manual, para validar que todo funciona:

```bash
docker compose -f docker-compose.prod.yaml up -d --build
```

El servicio queda disponible en `http://<IP-publica-EC2>:<APP_PORT>` (por
defecto `8081`).

### 2. Abrir el puerto en el Security Group

En la consola de AWS, en el Security Group asociado a la instancia, agregar
una regla de entrada (Inbound rule) para el puerto definido en `APP_PORT`
(TCP, `8081` por defecto), idealmente restringida al origen que realmente
necesita llegar al servicio (por ejemplo, la IP del API Gateway o del BFF).

### 3. Configurar GitHub Actions (despliegue automatico)

El workflow [`​.github/workflows/deploy.yml`](.github/workflows/deploy.yml)
se conecta por SSH al EC2 con cada push a `deploy` y corre
`docker compose -f docker-compose.prod.yaml up -d --build`. No toca el
archivo `.env` del servidor: los secretos de base de datos viven solo ahi.

En GitHub: **Settings → Secrets and variables → Actions → New repository
secret**, y crear:

| Secret | Descripcion | Ejemplo |
|---|---|---|
| `EC2_HOST` | IP publica o DNS de la instancia | `54.210.xxx.xxx` |
| `EC2_USERNAME` | Usuario SSH | `ubuntu` |
| `EC2_SSH_KEY` | Contenido completo de la llave privada `.pem` usada para conectarse | `-----BEGIN RSA PRIVATE KEY-----...` |
| `EC2_PORT` | Puerto SSH (opcional, por defecto 22) | `22` |
| `EC2_DEPLOY_PATH` | Ruta absoluta del repo clonado en el EC2 | `/home/ubuntu/digitalfix-ms-workorders` |

A partir de ahi, cada `git push origin deploy` reconstruye y reinicia el
contenedor en EC2 automaticamente. También se puede disparar manualmente
desde la pestaña **Actions** del repositorio (`workflow_dispatch`).

### 4. Actualizaciones posteriores

No se necesita hacer nada manual: basta con mergear/pushear a `deploy`. Para
un despliegue manual (sin esperar al pipeline), en el EC2:

```bash
cd digitalfix-ms-workorders
git pull origin deploy
docker compose -f docker-compose.prod.yaml up -d --build
```
