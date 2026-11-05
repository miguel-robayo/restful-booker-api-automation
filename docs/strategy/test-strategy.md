# Estrategia de Pruebas — Restful-Booker API

**Versión 3.5 · 2026-11-05 · Aprobado · Referencia para la implementación**
Requisitos y trazabilidad: [`requirements-analysis.md`](../analysis/requirements-analysis.md) · Divergencias: [`known-defects.md`](../known-defects.md) · Arquitectura: [`framework-architecture.md`](../architecture/framework-architecture.md)
*v3.2: BDD con Cucumber (D-22), D-16 actualizada a un feature por funcionalidad, job separado para `@known-defect` (D-21). v3.3: categoría Performance (SLA) y resultados de la verificación de la fase 0. v3.4: sintaxis GPath de D-01 verificada. v3.5: convenciones BDD (§3.1), fase explícita de observación posterior (ADR-22), TC-AUTH-02 en dos escenarios y verificaciones de la fase de escenarios.*

## 1. Objetivo y alcance

Verificar de forma automatizada, independiente y reproducible los requisitos de la prueba sobre la API pública Restful-Booker. Hay **24 escenarios**: **10 solicitados** 🟦 por el documento de la prueba y **14 propuestos** 🟨 que agregan seguridad, contrato, límites y monitoreo de divergencias (D-02).

**Fuera de alcance:** `PATCH`, auth Basic, XML, carga y estrés, pruebas de seguridad ofensivas (es una API de terceros sin autorización), token caducado (D-07) y límites sin regla definida (precio ≤ 0, `checkout` anterior a `checkin`). Estos últimos se exploran manualmente, porque automatizarlos obligaría a inventar el resultado esperado.

## 2. Stack y arquitectura

**Stack:** Java 21 · Maven (Wrapper) · Cucumber 7 + Gherkin (español) · JUnit 5 · Rest Assured 5 · Jackson · Lombok · Datafaker · Allure. El detalle está en la [arquitectura](../architecture/framework-architecture.md).

| Capa | Responsabilidad | Regla |
|---|---|---|
| Features (Gherkin) | Comportamiento esperado en lenguaje de negocio (§3.1) | Sin términos técnicos ni datos técnicos |
| Step Definitions | Adaptadores delgados hacia el Actor o `Expect` | Sin HTTP directo ni reglas de negocio |
| Observación posterior (`PostActionObservations` + `@AfterStep`) | Lecturas de evidencia declaradas por el `Cuando` | Solo lecturas; sin aserciones |
| Screenplay: `Actor`, *Abilities*, *Tasks* (escrituras), *Questions* (lecturas) | Intención de negocio | Sin aserciones |
| API Objects (auth, booking) | Endpoint, método y parámetros | Sin lógica ni aserciones |
| `Expect` / `ResponseSpecs` | Aserciones sobre resultados almacenados | Sin peticiones HTTP |
| Hooks de Cucumber + `ScenarioContext` | Ciclo de vida por escenario: actor, limpieza, verificación del entorno | Único mecanismo; sin estado estático |
| JUnit 5 | Tests técnicos del framework (FW-01, FW-02) | No usan el Actor |

**Regla de pasos (ADR-13, ADR-22):** `Dado` → prepara el contexto y obtiene el estado inicial · `Cuando` → **exactamente una** acción (`attemptsTo` o `asksFor`), que además **declara** la evidencia posterior que necesita · **observación posterior** → el hook `@AfterStep` ejecuta esas lecturas justo después del `Cuando`, visibles en Allure · `Entonces` → `Expect`/`ResponseSpecs` sobre la evidencia almacenada, **sin HTTP**.

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

### 3.1 Redacción BDD y fases de un escenario

| ID | Convención | Verificación automática |
|---|---|---|
| C-1 | **Fases:** `Dado` prepara el contexto y el estado inicial · `Cuando` ejecuta **solo** la acción principal y declara la evidencia posterior · **observación posterior** (`@AfterStep`, visible en Allure) · `Entonces` valida la evidencia sin HTTP (ADR-13, ADR-22) | `ArchitectureRulesTest`: un `Cuando` = exactamente una acción HTTP; un `Entonces` = ninguna |
| C-2 | Los códigos HTTP no aparecen en el Gherkin: son constantes con nombre en Java (`HttpStatus`). Los valores esperados no cambian | `FeatureConventionsTest`: ningún código de 3 cifras |
| C-3 | Lenguaje de negocio: sin token, HTTP, JSON, schema, GPath, SLA, Builder, cabeceras… Las notas técnicas de cada escenario viven en esta estrategia (§7, "Notas técnicas") | `FeatureConventionsTest` (títulos, pasos y comentarios) |
| C-4 | Un único actor, **Miguel**, presentado en el primer `Dado`; después se omite el sujeto | `FeatureConventionsTest` |
| C-5 | Métodos Java: verbo imperativo para `Dado`/`Cuando` (`registerBooking`) y `verify…` para `Entonces` | Revisión |
| C-6 | Un único `Cuando` por escenario (un `Y` después del `Cuando` también es acción) y etiquetas `@TC-*` + `@esc-*` en todos | `FeatureConventionsTest` |

**Vocabulario de negocio ↔ implementación** (documentado para no inventar reglas): "iniciar sesión" = obtener el token de `POST /auth`; "sesión" = ese token; "cancelar" = `DELETE`, que **elimina** la reserva (no existe un estado "cancelada"), por eso el resultado se expresa como "deja de estar disponible"; "credenciales de acceso válidas" = las configuradas (no hay registro de usuarios); "valor de referencia" = `query.price.threshold`; "tiempo máximo acordado" = `sla.booking.get.ms`; "formato y codificación acordados" = `Content-Type: application/json; charset=utf-8`.

**Por qué existe la observación posterior:** un escenario negativo necesita leer la reserva **después** de la acción para probar que quedó intacta. Un `Y` después del `Cuando` sería otra acción; leer dentro del `Cuando` escondería una segunda operación; leer en el `Entonces` rompería ADR-13; no leer debilitaría la comprobación. La observación declarada por el `Cuando` y ejecutada por el hook es la única opción que respeta las cuatro reglas.

## 4. Datos, autenticación y limpieza

| Tema | Estrategia |
|---|---|
| Aislamiento | Cada test crea sus propios datos. `firstname` = nombre generado + sufijo único (el filtro es exacto, H-02). Sin IDs fijos salvo `9999999` (D-13) |
| Generación | Datafaker (D-04; JavaFaker no tiene mantenimiento y arrastra una dependencia vulnerable). Precio de 1 a 10 000; `checkout` posterior a `checkin`; todos los campos de RC-01. Semilla en el reporte |
| Autenticación | Credenciales desde `RB_USERNAME`/`RB_PASSWORD` (D-05). Un token por actor y por test, inyectado con un `Filter` como `Cookie` |
| Limpieza | La Task de creación registra cada ID en el contexto del escenario **antes** de cualquier aserción; el hook `@After` de Cucumber lo borra, pase o falle el escenario. Usa su propio token |
| Propiedad (D-12) | Antes de borrar se hace un `GET`: si da 404, no hay nada que hacer; si el `firstname` coincide, se borra; si no coincide (el ID fue reutilizado, H-03), **no se borra** |
| Limpieza pendiente (D-08) | No altera el resultado funcional ni genera un resultado de test. Es visible con: un step *broken* y un adjunto en el test afectado; `target/cleanup-pending.json`; un contador en el panel Environment de Allure; un resumen en la consola. **Solo registra `bookingid`, el marcador sintético y el motivo; nunca tokens ni credenciales** |
| Registro por escenario | Cada escenario escribe su propio archivo en `target/cleanup/<directorio de resultados>/` (IDs creados y pendientes; un directorio por job). El hook `@AfterAll` lo consolida: no hay estado compartido en memoria entre escenarios en paralelo |
| Si la JVM muere | Los archivos de `target/cleanup/` conservan los IDs; el reinicio de la API elimina los restos |

## 5. Ejecución

| Tema | Estrategia |
|---|---|
| Ambientes (D-14) | `-Denv=dev\|qa` (`qa` por defecto); todos apuntan a la API pública |
| Configuración | Precedencia: `-D` > variable de entorno > `<env>.properties` > `default.properties`. Fail-fast ante un ambiente desconocido, una credencial ausente o una URL que no sea `https` |
| Paralelismo (D-10) | Cucumber en paralelo sobre JUnit Platform, 4 hilos configurables; cada escenario tiene su propio `ScenarioContext` (PicoContainer). **Prohibido** escribir en `RestAssured.*` estático: se crea una spec nueva por petición. Actor, token, filtros y `Faker` son de cada escenario |
| Jobs (D-21) | **Principal:** `mvn clean test` → FW + escenarios `not @known-defect`. **Defectos conocidos:** `mvn test -Pknown-defects` → solo la suite de Cucumber con `@known-defect`, con directorios Allure y Surefire propios. Su resultado real se conserva y nunca se cuenta como exitoso en el principal |
| Independencia | Se demuestra con orden aleatorio (semilla en el log), cada clase ejecutada sola, la suite ejecutada dos veces y en modo paralelo y secuencial |
| Entorno | `GET /ping` (RC-07) antes de ejecutar; si falla, la ejecución se aborta como Infrastructure |
| Reintentos | Ninguno |

## 6. Resultados y defectos en Allure

**Evidencia por escenario** (adaptador único `allure-cucumber7-jvm`): pasos Gherkin con las Tasks y Questions anidadas · request y response con los secretos enmascarados · etiquetas (`@TC-xx`, `@esc-N`, `@requested`/`@proposed`, `@req`/`@kd-monitor`/`@known-defect`, funcionales) · `@issue=KD-xx` cuando aplica (verificado: Allure lo muestra como enlace de tipo *issue* hacia `known-defects.md`, gracias a `allure.link.issue.pattern`) · tiempos · IDs creados y su limpieza. FW-01 y FW-02 se reportan en Surefire.

**Categorías de Allure (D-03):**

| Categoría | Estado | Clasificación | Acción |
|---|---|---|---|
| **Product Defect** | failed | Aserción REQ **funcional** fallida, sin KD registrado (sin prefijo `KD-xx:` ni `SLA:`) | Analizar y registrar el defecto |
| **Performance (SLA)** | failed | Prefijo `SLA:`: requisito **no funcional** de rendimiento incumplido | Revisar el tiempo medido frente al umbral; no es un defecto funcional |
| **Known Defect** | failed | Prefijo `KD-xx:`: un REQ que incumple un KD abierto, o un MON cuyo comportamiento observado cambió | Consultar y actualizar `known-defects.md` |
| **Precondition** | broken | `PreconditionFailedException` (prefijo `PRECONDITION:`) (D-09) | Revisar la capacidad base (`/auth`, `POST /booking`) |
| **Infrastructure** | broken | Conexión, TLS, DNS, timeout o `/ping` caído (prefijo `INFRA:`) | Reintentar la ejecución completa más tarde |
| **Test Defect** | broken | Cualquier otro error | Corregir el framework |

**Por qué seis categorías (v3.3):** la verificación de la fase 0 mostró que, con cinco, Allure envía un fallo `SLA:` a su categoría interna *"Product defects"*, con un nombre casi idéntico al de los defectos funcionales. La categoría explícita **Performance (SLA)** lo separa (aprobada como A1); además lleva la etiqueta `performance`.

**Verificado en la fase 0** (ejecución real con escenarios de prueba): AssertionError → failed y cualquier otra excepción → broken; `PRECONDITION:` → Precondition; `INFRA:` y `ConnectException` → Infrastructure; cada escenario en exactamente una categoría y ninguno sin categoría.

**Limitación:** si una reserva desaparece por un reinicio de la API a mitad de un test, el 404 resultante cae en Product Defect y requiere revisión manual.

## 7. Matriz de escenarios

**Distribución (D-16):** un feature por funcionalidad. La trazabilidad con el documento de la prueba se mantiene con las etiquetas `@TC-xx` y `@esc-N`.

| Feature | Escenarios |
|---|---|
| `authentication.feature` | TC-AUTH-01, TC-AUTH-02, TC-AUTH-03, TC-NEG-01, TC-NEG-02, TC-NEG-04 |
| `booking_creation.feature` | TC-CRE-01, TC-CON-04 |
| `booking_queries.feature` | TC-QRY-01, TC-QRY-02, TC-QRY-03, TC-QRY-04, TC-NEG-03 |
| `booking_validation.feature` | TC-CON-01, TC-CON-02, TC-CON-03, TC-CRE-02, TC-NEG-05, TC-NEG-06, TC-NEG-07, TC-NEG-08 |
| `booking_lifecycle.feature` | TC-INT-01 |
| JUnit 5 (`framework/`) | FW-01, FW-02 |

**TC-AUTH-02 = dos escenarios del mismo requisito** (C-6 obliga a un único `Cuando`): "Modificar una reserva después de iniciar sesión" (modificación confirmada y persistida) y "Cancelar una reserva modificada sin volver a iniciar sesión" (cancelación, misma sesión en la modificación y en la cancelación, un solo inicio de sesión). Ambos llevan `@TC-AUTH-02` y son **independientes**: cada uno inicia su propia sesión y crea su propia reserva. No duplican cobertura: reparten las comprobaciones del requisito. Por eso la suite tiene **11 escenarios de Cucumber para 10 TC solicitados**.

**Notas técnicas de los escenarios implementados** (salieron de los features por C-3):

| TC | Nota |
|---|---|
| TC-AUTH-02 | La misma sesión se prueba comparando la huella SHA-256 de la cabecera `Cookie` realmente enviada en el `PUT` y en el `DELETE` con la del token obtenido (ADR-23); además, un solo `POST /auth` |
| TC-NEG-02 | Un token caducado no se puede generar (RF-11, D-07): se usa un token generado que la API nunca emitió |
| TC-QRY-01 | `GET /booking` solo devuelve `bookingid` (H-01, C-01). La búsqueda por nombre único es la acción; los detalles reales, la selección GPath (`findAll { it.booking.totalprice > T }`, valor límite excluido) y el detalle de la reserva identificada son observaciones posteriores (D-01). No se simula ningún dato |
| TC-NEG-03 | El ID configurado (`9999999`) solo se usa si es mayor que el ID más alto existente (`GET /booking`, GPath `bookingid.max()`); si no, Precondition |
| TC-CON-02 | Alcance: un `GET /booking/{id}`, tiempo del lado del cliente medido por Rest Assured; umbral estrictamente menor que `sla.booking.get.ms`; el tiempo real se adjunta antes de comparar |
| TC-CON-03 | RNF-06 pide headers "de seguridad o servidor": no hay headers de seguridad (KD-06), así que `Server` se valida como cabecera técnica (presencia, no valor) |

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
| Alcance y fuentes | **D-02** 24 escenarios (10 + 14) · **D-16** un feature por funcionalidad, con `@TC-xx` y `@esc-N` (actualizada en v3.2; antes: una clase por escenario del documento) · **D-17** [DOC] > [API-DOC] > [OBS] · **D-22** BDD con Cucumber y Gherkin en español; código en inglés | Trazabilidad por etiquetas; lo observado nunca se convierte en requisito; escenarios legibles para la exposición |
| Escenario 3 | **D-01** GPath sobre la colección de detalles de las reservas propias | `GET /booking` no devuelve `totalprice` (C-01); los datos propios lo hacen determinista |
| Aserciones y defectos | **D-03** REQ/MON (clasificados por requisito, no por lo observado) + 6 categorías (Performance (SLA) agregada en v3.3) · **D-18** registro versionado · **D-15** schema sin restricciones no documentadas · **D-19** filtros combinados · **D-20** rechazo sin código · **D-06** headers de seguridad como hallazgo · **D-07** token caducado no automatizable | Distinguir defectos nuevos de los conocidos sin ocultar ninguno, y sin afirmar más de lo que dicen las fuentes |
| Datos y autenticación | **D-04** Datafaker · **D-05** credenciales por el entorno · **D-12** propiedad antes de borrar · **D-08** limpieza con visibilidad propia · **D-13** ID `9999999` | Seguridad, aislamiento y protección de datos ajenos |
| Ejecución | **D-09** precondición = broken · **D-10** paralelo con 4 hilos · **D-11** SLA configurable sin reintentos · **D-14** ambientes `dev`/`qa` · **D-21** `@known-defect` en un job separado, con resultados propios | Diagnóstico claro, sin falsos verdes y respetando una API compartida; el job principal no queda en rojo por defectos ya registrados, que siguen visibles |

## 10. Verificaciones con ejecución real

| Ítem | Estado |
|---|---|
| Propagación de `-Denv`/`-D` a Surefire y resolución de la configuración | ✅ Fase 0 |
| Compatibilidad `allure-cucumber7-jvm` + Cucumber 7.34.9; un resultado de Allure por escenario; filtrado por etiquetas a través de `@Suite`; `@issue=KD-xx` | ✅ Fase 0 |
| Estados broken/failed y categorías (incluido el SLA) | ✅ Fase 0 |
| `@BeforeAll` una sola vez; un contexto por escenario en paralelo; `@After` tras un paso fallido | ✅ Fase 0 |
| Job `-Pknown-defects`: solo `@known-defect`, sin FW, con directorios propios y resultado real | ✅ Fase 0 |
| Sintaxis GPath de D-01 (`findAll { it.<campo> > T }.<id>` con el `JsonPath` de Rest Assured 5.5.7) | ✅ Verificada: devuelve exactamente los IDs por encima del umbral, con el valor límite excluido |
| Semántica del filtro `checkin` (TC-QRY-04) | Al implementar el escenario (ronda de escenarios propuestos) |
| Step broken de limpieza pendiente dentro de un escenario aprobado y panel Environment (D-08) | ✅ Ejecución forzada: escenario aprobado, step "Data cleanup pending" broken con adjunto, `cleanup.pending.count=1` |
| Consolidación concurrente de la limpieza; ausencia de condiciones de carrera | ✅ 4 ejecuciones en paralelo: 0 pendientes, IDs y marcadores únicos, un token por inicio de sesión |
| Observación posterior (ADR-22) | ✅ Visible en Allure, justo después del `Cuando` y antes del primer `Entonces`, en los 5 escenarios que la usan |
