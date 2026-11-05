# Estrategia de Pruebas — Restful-Booker API

**Versión 3.1 · 2026-11-05 · Aprobado · Referencia para la implementación**
Requisitos y trazabilidad: [`requirements-analysis.md`](../analysis/requirements-analysis.md) · Divergencias: [`known-defects.md`](../known-defects.md)

## 1. Objetivo y alcance

Verificar de forma automatizada, independiente y reproducible los requisitos de la prueba sobre la API pública Restful-Booker. Hay **24 escenarios**: **10 solicitados** 🟦 por el documento de la prueba y **14 propuestos** 🟨 que agregan seguridad, contrato, límites y monitoreo de divergencias (D-02).

**Fuera de alcance:** `PATCH`, auth Basic, XML, carga y estrés, pruebas de seguridad ofensivas (es una API de terceros sin autorización), token caducado (D-07) y límites sin regla definida (precio ≤ 0, `checkout` anterior a `checkin`). Estos últimos se exploran manualmente, porque automatizarlos obligaría a inventar el resultado esperado.

## 2. Stack y arquitectura

**Stack:** Java 21 · Maven · JUnit 5 · Rest Assured 5 · Jackson · Lombok · Datafaker · Allure.

| Capa | Responsabilidad | Regla |
|---|---|---|
| Tests (5 clases por escenario + `LifecycleTest` + `FrameworkTest`) | Given/When/Then y aserciones | No conocen URLs, headers ni JSON |
| Screenplay: `Actor`, *Abilities*, *Tasks*, *Questions* | Intención de negocio: "crear reserva", "autenticarse" | Componen API Objects; no hacen aserciones |
| API Objects (por recurso: auth, booking) | Endpoint, método, headers y spec | Sin lógica de negocio ni aserciones |
| Modelos (Lombok `@Builder` + Jackson) | Requests y responses tipados | Sin JSON estático |
| Datos (`BookingDataFactory`) | Datos válidos y únicos | Una instancia de `Faker` por test |
| Soporte | Configuración, filtros (token, enmascarado, Allure), extensión de limpieza | Sin estado estático mutable |

Los nombres concretos de clases y paquetes se fijan en la fase 0.

## 3. Enfoque de pruebas

**Niveles de aserción (D-03, D-17):**

| Nivel | Qué afirma | Si falla |
|---|---|---|
| **REQ** | Lo que exige [DOC] o, a falta de este, el contrato de [API-DOC]. Si hay un KD abierto, el test **falla** hasta que se corrija (no se oculta) | Defecto nuevo → Product Defect · defecto ya registrado → Known Defect. Es el único nivel que cuenta como cobertura |
| **MON** | Un comportamiento observado que **ningún** requisito ni contrato define (por ejemplo, códigos de error), ligado a un KD | El comportamiento cambió → Known Defect → **revisar `known-defects.md`**. Nunca cuenta como cobertura |

La clasificación depende del requisito, **no** de lo que hace la API: un escenario no pasa de REQ a MON porque la API no cumpla.

- **Positivas:** toda escritura se confirma con una lectura posterior; lo enviado y lo devuelto se comparan como objetos completos.
- **Negativas:** una sola variable inválida por test (por ejemplo, body válido y token inválido); después del rechazo se verifica que el estado quedó **intacto**. Los tokens y credenciales inválidos se generan, nunca son literales.
- **Rechazo sin código definido (D-20):** se afirma status no 2xx + datos sin cambios; el código recibido se adjunta como evidencia. Aplica a: escrituras sin autorización, `PUT` sobre una reserva inexistente y payloads que incumplen RC-01 (A-13).
- **Filtros combinados (D-19):** el resultado debe cumplir todos los criterios.
- **Contrato (D-15):** `schemas/booking.json` (RC-01) y `schemas/booking-response.json` (RC-02), con todos los campos documentados como obligatorios y con su tipo. **No se agregan** restricciones que [API-DOC] no establece (`additionalProperties: false`, patrón de fecha en el body). Limitación aceptada: no se detectan campos extra.
- **Seguridad:** control de acceso con verificación del estado (TC-NEG-01/02/04), auth sin falsos positivos (TC-AUTH-01/03), secretos enmascarados (FW-01), URL `https://` obligatoria (FW-02).
- **Rendimiento (D-11):** el SLA es una **aserción de requisito no funcional** con umbral configurable (2000 ms por defecto) y **sin reintentos**. Se identifica con el prefijo `SLA:` y la etiqueta `performance`. Se mide del lado del cliente, por eso se reporta el tiempo real.

## 4. Datos, autenticación y limpieza

| Tema | Estrategia |
|---|---|
| Aislamiento | Cada test crea sus propios datos. `firstname` = nombre generado + sufijo único (el filtro es exacto, H-02). Sin IDs fijos salvo `9999999` (D-13) |
| Generación | Datafaker (D-04; JavaFaker no tiene mantenimiento y arrastra una dependencia vulnerable). Precio de 1 a 10 000; `checkout` posterior a `checkin`; todos los campos de RC-01. Semilla en el reporte |
| Autenticación | Credenciales desde `RB_USERNAME`/`RB_PASSWORD` (D-05). Un token por actor y por test, inyectado con un `Filter` como `Cookie` |
| Limpieza | Una extensión de JUnit registra cada ID **antes** de cualquier aserción y lo borra en `afterEach`, pase o falle el test. Usa su propio token |
| Propiedad (D-12) | Antes de borrar se hace un `GET`: si da 404, no hay nada que hacer; si el `firstname` coincide, se borra; si no coincide (el ID fue reutilizado, H-03), **no se borra** |
| Limpieza pendiente (D-08) | No altera el resultado funcional ni genera un resultado de test. Es visible con: un step *broken* y un adjunto en el test afectado; `target/cleanup-pending.json`; un contador en el panel Environment de Allure; un resumen en la consola. **Solo registra `bookingid`, el marcador sintético y el motivo; nunca tokens ni credenciales** |
| Si la JVM muere | Los IDs quedan en `target/created-bookings.log`; el reinicio de la API elimina los restos |

## 5. Ejecución

| Tema | Estrategia |
|---|---|
| Ambientes (D-14) | `-Denv=dev\|qa` (`qa` por defecto); todos apuntan a la API pública |
| Configuración | Precedencia: `-D` > variable de entorno > `<env>.properties` > `default.properties`. Fail-fast ante un ambiente desconocido, una credencial ausente o una URL que no sea `https` |
| Paralelismo (D-10) | JUnit 5 concurrente, 4 hilos configurables. **Prohibido** escribir en `RestAssured.*` estático: se crea una spec nueva por petición. Actor, token, filtros y `Faker` son de cada test |
| Independencia | Se demuestra con orden aleatorio (semilla en el log), cada clase ejecutada sola, la suite ejecutada dos veces y en modo paralelo y secuencial |
| Entorno | `GET /ping` (RC-07) antes de ejecutar; si falla, la ejecución se aborta como Infrastructure |
| Reintentos | Ninguno |

## 6. Resultados y defectos en Allure

**Evidencia por test:** request y response con los secretos enmascarados · steps de Screenplay · etiquetas (`requested`/`proposed`, escenario, `known-defect`, `performance`) · `@Issue("KD-xx")` cuando aplica · tiempos · IDs creados y su limpieza.

**Categorías de Allure (D-03; las cinco aprobadas, sin una sexta):**

| Categoría | Estado | Clasificación | Acción |
|---|---|---|---|
| **Product Defect** | failed | Aserción REQ **funcional** fallida, sin KD registrado (sin prefijo `KD-xx:` ni `SLA:`) | Analizar y registrar el defecto |
| **Known Defect** | failed | Prefijo `KD-xx:`: un REQ que incumple un KD abierto, o un MON cuyo comportamiento observado cambió | Consultar y actualizar `known-defects.md` |
| **Precondition** | broken | `PreconditionFailedException` (prefijo `PRECONDITION:`) (D-09) | Revisar la capacidad base (`/auth`, `POST /booking`) |
| **Infrastructure** | broken | Conexión, TLS, DNS, timeout o `/ping` caído (prefijo `INFRA:`) | Reintentar la ejecución completa más tarde |
| **Test Defect** | broken | Cualquier otro error | Corregir el framework |

**Performance/SLA (requisito no funcional):** un fallo de SLA **no es un fallo funcional**, así que queda **excluido** de Product Defect por su prefijo `SLA:`. Se identifica por el prefijo `SLA:` y la etiqueta `performance`, no por una categoría propia.

**Limitaciones sin verificar todavía:**
- **No está comprobado** cómo presenta Allure un failed que no coincide con ninguna categoría (el fallo de SLA). Hasta verificarlo en una ejecución real, la forma garantizada de localizarlo es filtrar por la etiqueta `performance` o por el prefijo `SLA:`.
- También queda por comprobar la diferencia broken/failed y el orden y la exclusividad de las reglas.
- Si una reserva desaparece por un reinicio de la API a mitad de un test, el 404 resultante cae en Product Defect y requiere revisión manual.

## 7. Matriz de escenarios

**Fuente del esperado:** [D] = documento de la prueba · [A] = documentación de la API.

| ID | Clase | Objetivo | Endpoint · método | Resultado esperado | Nivel | Requisitos |
|---|:-:|---|---|---|:-:|---|
| TC-AUTH-01 | 🟦 | Token con credenciales válidas | `/auth` POST | 200, `token` no vacío, sin `reason` [D][A] | REQ | RF-01 |
| TC-AUTH-02 | 🟦 | El mismo token autoriza `PUT` y `DELETE` | `/booking/{id}` PUT, DELETE, GET | PUT 200 con el body actualizado · DELETE 201 · GET 404 [D][A] | REQ | RF-01, RF-02, RF-08, RF-09, RF-12, RC-04 |
| TC-CRE-01 | 🟦 | Creación con Builder y datos generados | `/booking` POST | 200 y `booking` igual al request [D] | REQ | RF-03, RF-04, RNF-07 |
| TC-QRY-01 | 🟦 | `?firstname=` + GPath `totalprice > T` + mapeo a `Booking` | `/booking` GET, `/booking/{id}` GET | 4 IDs propios; GPath → `T+1` y `T+500` (`T` excluido) [D][A] | REQ (RF-07 parcial, D-01) | RF-05, RF-06, RF-07, RC-03 |
| TC-CON-01 | 🟦 | Contrato de la respuesta | `/booking/{id}` GET | Cumple `booking.json` [D][A] | REQ | RNF-05, RC-01 |
| TC-CON-02 | 🟦 | SLA | `/booking/{id}` GET | < 2000 ms [D] | REQ | RNF-04 |
| TC-CON-03 | 🟦 | Headers | `/booking/{id}` GET | `application/json; charset=utf-8` + `Server` presente [D] | REQ | RNF-06 |
| TC-NEG-01 | 🟦 | `PUT` sin token | `/booking/{id}` PUT, GET | 403 y estado intacto [D] | REQ | RF-10 |
| TC-NEG-02 | 🟦 | `PUT` con token inválido (generado) | `/booking/{id}` PUT, GET | 403 y estado intacto [D] | REQ | RF-10 |
| TC-NEG-03 | 🟦 | ID inexistente | `/booking/9999999` GET | 404 [D] | REQ | RF-12 |
| TC-AUTH-03 | 🟨 | Credenciales inválidas | `/auth` POST | 200 + `reason`, sin token | MON KD-01 | — |
| TC-CRE-02 | 🟨 | Creación sin `additionalneeds` | `/booking` POST | No 2xx (D-20) [A] · **falla mientras KD-07 esté abierto** | REQ (KD-07) | RC-01 |
| TC-CON-04 | 🟨 | Contrato de la respuesta de creación | `/booking` POST | Cumple `booking-response.json` [A] | REQ | RNF-05, RC-01, RC-02 |
| TC-QRY-02 | 🟨 | Filtro sin coincidencias | `/booking` GET | 200 y array vacío [A] | REQ | RF-06, RC-03 |
| TC-QRY-03 | 🟨 | `firstname` + `lastname` | `/booking` GET | Solo el ID que cumple ambos [A] (D-19) | REQ | RF-06, RC-03 |
| TC-QRY-04 | 🟨 | `checkin` mayor o igual (límite) | `/booking` GET | `D` y `D-1` incluidos; `D+1` excluido [A] (por verificar) | REQ | RF-06, RC-03 |
| TC-NEG-04 | 🟨 | `DELETE` sin token | `/booking/{id}` DELETE, GET | No 2xx y la reserva persiste (D-20) | REQ | RF-09, RC-05 |
| TC-NEG-05 | 🟨 | `PUT` con body parcial | `/booking/{id}` PUT, GET | No 2xx y estado intacto (D-20) | REQ | RF-08, RC-01 |
| TC-NEG-06 | 🟨 | `PUT` sobre un ID inexistente | `/booking/9999999` PUT, GET | REQ: no 2xx y no se crea (D-20) · MON: 405 | REQ + MON KD-03 | RF-08 |
| TC-NEG-07 | 🟨 | `POST` con body vacío | `/booking` POST | REQ: no 2xx (D-20) · MON: 500 | REQ + MON KD-04 | RC-01 |
| TC-NEG-08 | 🟨 | `totalprice` no numérico | `/booking` POST | No 2xx (D-20) [A] · **falla mientras KD-05 esté abierto** | REQ (KD-05) | RC-01 |
| TC-INT-01 | 🟨 | Ciclo de vida completo | Todos | Coherencia paso a paso; DELETE 201; GET final 404 [D][A] | REQ | RF-01 a RF-06, RF-08, RF-09, RC-04 |
| FW-01 | 🟨 | Enmascarado de secretos (sin red) | — | `password`, `token`, `Cookie` y `Authorization` aparecen como `***` | REQ | RNF-03, RNF-10 |
| FW-02 | 🟨 | Configuración y fail-fast (sin red) | — | Precedencia correcta; error descriptivo en cada caso inválido | REQ | RNF-03 |

**Precondición común:** los escenarios que necesitan una reserva la crean y registran ellos mismos; si esa creación falla, el resultado es Precondition y no un fallo funcional.

## 8. Riesgos principales

| Riesgo | P × I | Mitigación |
|---|:-:|---|
| Entorno compartido: terceros alteran datos | Alta × Alta | Datos propios con `firstname` único |
| Reutilización de IDs tras un reinicio: la limpieza podría borrar datos ajenos | Media × Alta | Verificar la propiedad antes de borrar (D-12) |
| Estado estático de Rest Assured en paralelo | Media × Alta | Spec por petición; prohibición verificada en CI |
| Secretos en logs y reportes | Alta × Media | Enmascarado (FW-01) |
| Falsos positivos (auth con 200; rechazo con efecto colateral) | Alta × Media | Afirmar el contenido y el estado posterior |
| SLA afectado por red o *cold start* | Media × Media | Umbral configurable; tiempo real reportado; sin reintentos |

## 9. Decisiones técnicas

| Tema | Decisiones | Justificación |
|---|---|---|
| Alcance y fuentes | **D-02** 24 escenarios (10 + 14) · **D-16** una clase por escenario del documento · **D-17** [DOC] > [API-DOC] > [OBS] | Trazabilidad directa; lo observado nunca se convierte en requisito |
| Escenario 3 | **D-01** GPath sobre la colección de detalles de las reservas propias | `GET /booking` no devuelve `totalprice` (C-01); los datos propios lo hacen determinista |
| Aserciones y defectos | **D-03** REQ/MON (clasificados por requisito, no por lo observado) + 5 categorías · **D-18** registro versionado · **D-15** schema sin restricciones no documentadas · **D-19** filtros combinados · **D-20** rechazo sin código · **D-06** headers de seguridad como hallazgo · **D-07** token caducado no automatizable | Distinguir defectos nuevos de los conocidos sin ocultar ninguno, y sin afirmar más de lo que dicen las fuentes |
| Datos y autenticación | **D-04** Datafaker · **D-05** credenciales por el entorno · **D-12** propiedad antes de borrar · **D-08** limpieza con visibilidad propia · **D-13** ID `9999999` | Seguridad, aislamiento y protección de datos ajenos |
| Ejecución | **D-09** precondición = broken · **D-10** paralelo con 4 hilos · **D-11** SLA configurable sin reintentos · **D-14** ambientes `dev`/`qa` | Diagnóstico claro, sin falsos verdes y respetando una API compartida |

## 10. Pendientes de verificar con una ejecución real

| Ítem | Cuándo |
|---|---|
| Propagación de `-Denv`/`-D` a Surefire y resolución de la configuración | Fase 0 |
| Sintaxis GPath (D-01) y semántica del filtro `checkin` (TC-QRY-04) | Al implementar cada escenario |
| Estados broken/failed, reglas de las categorías, presentación de un failed de SLA fuera de las categorías, step broken dentro de un test aprobado y panel Environment | Primera ejecución con Allure |

**Pendiente de decisión:** TC-CRE-02 y TC-NEG-08 (REQ con KD-07 y KD-05 abiertos) fallarán en cada ejecución hasta que se corrija la API. Falta definir si la ejecución por defecto (la que decide si el build es verde) los incluye o los excluye por la etiqueta `known-defect`, ejecutándolos en un job aparte.
