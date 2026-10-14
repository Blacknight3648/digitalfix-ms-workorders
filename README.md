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
- Acceso a la base **Oracle Autonomous Database** del proyecto: la **wallet** y las **credenciales** (las entrega el equipo, ver abajo)
- Docker (opcional, para ejecutar el servicio en contenedor)

---

## Secretos: que NO va en el repositorio

> ⚠️ **Nunca subas al repositorio la wallet de Oracle ni el archivo `.env`.** Ambos estan en `.gitignore`.
> Si por error quedan en un commit, avisa de inmediato al equipo: hay que **cambiar la contrasena de la BD y rotar la wallet** en OCI, porque borrar el archivo no lo saca del historial de git.

| Archivo / carpeta | Que contiene | Versionado |
|---|---|---|
| `wallet/` | Certificados y llaves privadas para conectarse a Oracle Autonomous | ❌ No |
| `.env` | Usuario, contrasena y URL reales de la BD, puertos locales | ❌ No |
| `.env.example` | Plantilla de `.env` **sin valores reales** | ✅ Si |
| `src/main/resources/application.yaml` | Solo referencias `${VARIABLE}`, ninguna credencial | ✅ Si |

Las credenciales y la wallet se comparten **solo por un canal privado del equipo** (por ejemplo, un mensaje directo o una carpeta de Drive restringida), nunca por GitHub, issues ni capturas de pantalla.

---

## Configuracion local (paso a paso)

### 1. Wallet de Oracle

La wallet es un `.zip` que se descarga desde la consola de OCI (**Autonomous Database → Database connection → Download wallet**) o que te comparte el equipo.

1. Descomprime el `.zip` en una carpeta llamada **`wallet`** en la raiz del proyecto, de modo que quede asi:

   ```text
   digitalfix-ms-workorders/
   ├── wallet/
   │   ├── cwallet.sso
   │   ├── ewallet.p12
   │   ├── keystore.jks
   │   ├── ojdbc.properties
   │   ├── sqlnet.ora
   │   ├── tnsnames.ora
   │   └── truststore.jks
   ├── .env
   ├── pom.xml
   └── src/
   ```

2. No hace falta editar ninguna ruta: la URL usa `TNS_ADMIN=./wallet`, una ruta **relativa** a la raiz del proyecto, asi que funciona igual en cualquier PC.

> La ruta relativa se resuelve desde el directorio donde arranca la app. Ejecuta siempre desde la raiz del proyecto (`./mvnw spring-boot:run` o el boton *Run* del IDE con el proyecto abierto en esa carpeta).

### 2. Archivo `.env`

```bash
cp .env.example .env          # Windows (cmd): copy .env.example .env
```

Abre `.env` y completa los valores reales:

| Variable | Descripcion | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC con el alias de `wallet/tnsnames.ora` | `jdbc:oracle:thin:@<alias>_high?TNS_ADMIN=./wallet` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base de datos | `<usuario_bd>` |
| `SPRING_DATASOURCE_PASSWORD` | Contrasena de la base de datos | `<contrasena_bd>` |
| `CORS_ALLOWED_ORIGINS` | Origen(es) permitidos para el navegador, separados por coma | `http://localhost:4200` |
| `SERVER_PORT` | Puerto local con `mvnw`/IDE (por defecto `8080`) | `8081` |
| `APP_PORT` | Puerto publicado en el host por docker compose | `8081` |
| `JAVA_OPTS` | Opciones de la JVM dentro del contenedor (opcional) | `-Xms256m -Xmx512m` |

**No hace falta exportar variables a mano.** `application.yaml` importa el `.env` automaticamente al arrancar:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

Si una variable tambien existe en el sistema operativo o en el contenedor, esa tiene prioridad sobre el `.env`.

### 3. Verificar que esta todo listo

```bash
git status        # wallet/ y .env NO deben aparecer en la lista
```

---

## Quick start (desarrollo local)

### Sin Docker

Con la wallet y el `.env` listos (ver "Configuracion local"):

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

El servicio queda disponible en `http://localhost:<SERVER_PORT>` (por ejemplo `http://localhost:8081`). Al iniciar, Flyway aplica las migraciones de `src/main/resources/db/migration`.

> Si aparece `Port 8080 was already in use`, otro programa (XAMPP/Apache, `ms-login`, etc.) ocupa ese puerto: define `SERVER_PORT=8081` (u otro libre) en `.env`.

### Con Docker Compose

Requiere la red `digitalfix-net`, que crea `digitalfix-ms-login`:

```bash
# 1. Una sola vez: crea la red compartida (o levanta digitalfix-ms-login)
docker network create digitalfix-net

# 2. En digitalfix-ms-workorders
docker compose up -d --build
```

Docker Compose lee el mismo `.env` para completar las variables y **monta la carpeta `wallet/` como volumen de solo lectura** (`./wallet:/app/wallet:ro`): la wallet nunca se copia dentro de la imagen. El servicio queda en `http://localhost:<APP_PORT>` (por defecto `8081`; dentro del contenedor sigue siendo el `8080`).

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

La base es compartida por varios microservicios. Para que las migraciones de Flyway no se pisen entre si, este servicio usa su propia tabla de historial: `flyway_schema_history_workorders` (configurado en `application.yaml`).

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

El `Dockerfile` **no copia la wallet ni el `.env`** a la imagen. Ambos compose (`docker-compose.yaml` y `docker-compose.prod.yaml`) toman las credenciales del `.env` y montan `./wallet` en `/app/wallet` como volumen de solo lectura. Asi la imagen se puede construir y compartir sin exponer secretos.

El servicio se une, como red externa, a `digitalfix-net` (la crea `digitalfix-ms-login`, o a mano con `docker network create digitalfix-net`).

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
    │   │   ├── model/        # Entidades JPA (WorkOrder, OrdenServicio, ProveedorServicio)
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
chmod 600 .env
```

Subir la wallet al servidor **desde tu PC** (nunca por git), y protegerla:

```bash
# En tu PC, desde la raiz del proyecto
scp -i <tu-llave>.pem -r wallet ubuntu@<IP-publica-EC2>:~/digitalfix-ms-workorders/

# En el EC2
chmod 700 ~/digitalfix-ms-workorders/wallet
chmod 644 ~/digitalfix-ms-workorders/wallet/*   # el contenedor corre con un usuario no root y debe poder leerla
```

> `git reset --hard` del pipeline **no borra** `wallet/` ni `.env` porque estan en `.gitignore` (git no toca archivos ignorados).

> Este servicio depende de la red Docker `digitalfix-net` que crea
> `digitalfix-ms-login` (o `docker network create digitalfix-net`).

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
archivo `.env` ni la carpeta `wallet/` del servidor: los secretos de base de datos viven solo ahi.

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
