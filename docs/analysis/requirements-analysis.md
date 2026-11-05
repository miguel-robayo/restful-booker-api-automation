# Análisis de Requisitos — Restful-Booker API

**Versión 3.1 · 2026-11-05 · Aprobado**
Cómo se prueba: [`test-strategy.md`](../strategy/test-strategy.md) · Divergencias de la API: [`known-defects.md`](../known-defects.md)

## 1. Objetivo y alcance

Construir un framework de automatización de APIs REST (Java 21, Rest Assured, Screenplay) sobre [Restful-Booker](https://restful-booker.herokuapp.com) que demuestre cinco escenarios: autenticación, creación dinámica, filtrado con GPath, contrato/SLA/headers y casos negativos.

**Endpoints en alcance:** `POST /auth`, `POST /booking`, `GET /booking`, `GET /booking/{id}`, `PUT /booking/{id}`, `DELETE /booking/{id}`.

**Fuentes y jerarquía (D-17):**
1. **[DOC]**, el documento de la prueba: define el comportamiento esperado.
2. **[API-DOC]**, la documentación pública (`apidoc/api_data.js`): define el contrato técnico donde [DOC] no especifica nada.
3. **[OBS]**, el comportamiento observado: solo identifica divergencias; nunca sustituye a un requisito.

## 2. Requisitos funcionales [DOC]

| ID | Requisito | ¿Validable? |
|---|---|---|
| RF-01 | `POST /auth` con las credenciales predeterminadas devuelve un token, que se deserializa a un POJO | ✅ |
| RF-02 | El token se reutiliza dinámicamente (`Filter` o `Cookie: token=<val>`) en `PUT` y `DELETE` | ✅ |
| RF-03 | `POST /booking` con un payload dinámico (POJO `@Builder` + datos generados), sin JSON estático | ✅ |
| RF-04 | Crear responde 200 y lo devuelto (`BookingResponse`) coincide con lo enviado | ✅ |
| RF-05 | `GET /booking/{id}` se mapea a `Booking.class` | ✅ |
| RF-06 | `GET /booking` filtra por query params | ✅ |
| RF-07 | GPath filtra, **sobre la respuesta de `GET /booking`**, las reservas con `totalprice` mayor que un umbral y extrae los IDs sin bucles `for` | ⚠️ Parcial: la API no devuelve `totalprice` en esa respuesta (C-01) |
| RF-08 | `PUT /booking/{id}` actualiza la reserva **completa**; requiere token | ✅ |
| RF-09 | `DELETE /booking/{id}` elimina; requiere token | ✅ |
| RF-10 | `PUT` sin token o con token inválido → **403** | ✅ |
| RF-11 | `PUT` con token caducado → **403** | ❌ No se puede generar (C-02) |
| RF-12 | ID inexistente (`9999999`) → **404** | ✅ |

## 3. Contrato técnico [API-DOC]

| ID | Contrato |
|---|---|
| RC-01 | `Booking`: `firstname`, `lastname`, `additionalneeds` (String), `totalprice` (Number), `depositpaid` (Boolean), `bookingdates.checkin` y `.checkout` (Date). **Todos obligatorios** en el request y en la response |
| RC-02 | La respuesta de creación tiene `bookingid` (Number) + `booking` (RC-01) |
| RC-03 | `GET /booking` devuelve `[{bookingid}]`. Filtros: *"Return bookings with a specific firstname"* (y `lastname`); `checkin` y `checkout`: fecha **mayor o igual**, formato `CCYY-MM-DD` |
| RC-04 | Un `DELETE` exitoso responde **201 Created** |
| RC-05 | `PUT` y `DELETE` se autorizan con `Cookie: token=` o con `Authorization: Basic` |
| RC-06 | `Accept` y `Content-Type` son obligatorios en `POST`/`PUT`; `Accept` también en `GET /booking/{id}` |
| RC-07 | `GET /ping` responde **201** (health check) |

**No documentado:** códigos de error, vigencia del token, reinicio de datos, rate limit.

## 4. Requisitos no funcionales [DOC]

| ID | Requisito |
|---|---|
| RNF-01 | Stack: Java 21, Rest Assured 5.x, JUnit 5, Maven, Jackson, Lombok, Allure |
| RNF-02 | Arquitectura: Screenplay + API Object + Builder; contratos, datos y aserciones separados |
| RNF-03 | Configuración por ambiente (`-Denv=qa`); credenciales en la configuración sin secretos versionados; solo la API pública |
| RNF-04 | SLA: `GET /booking/{id}` en menos de 2000 ms |
| RNF-05 | Contrato con JSON Schema (`matchesJsonSchemaInClasspath`) |
| RNF-06 | Headers: `Content-Type: application/json; charset=utf-8` + "headers de seguridad o servidor" |
| RNF-07 | Datos de prueba dinámicos |
| RNF-08 | DRY (`RequestSpecBuilder`/`ResponseSpecBuilder`) y `log().ifValidationFails()` |
| RNF-09 | Ejecución paralela con tests independientes |
| RNF-10 | Allure con las peticiones y respuestas adjuntas |
| RNF-11 | Entregables: repositorio público con historial limpio y README con comandos |

## 5. Hallazgos de la API que afectan las pruebas

| ID | Hallazgo | Impacto en el diseño |
|---|---|---|
| H-01 | `GET /booking` solo devuelve `bookingid`, sin `totalprice` | El Escenario 3 no se puede cumplir literalmente (C-01 → D-01) |
| H-02 | El filtro `firstname` es exacto, distingue mayúsculas y es consistente al instante (verificado con 4 reservas propias) | Un `firstname` único aísla los datos de cada test |
| H-03 | La API **reinicia sus datos y reutiliza IDs** (2635 → 1379 reservas el mismo día; los IDs nuevos fueron menores) | Antes de borrar se verifica la propiedad (D-12) |
| H-04 | No hay contrato de errores; `/auth` inválido → 200, `POST {}` → 500, ID inexistente en `PUT` → 405 | Rechazo exigido sin código → REQ con D-20; códigos sin requisito → MON (KD-01, KD-03, KD-04) |
| H-05 | La API acepta payloads que violan RC-01 (`totalprice` string → se guarda `null`; sin `additionalneeds`), y la respuesta resultante también incumple RC-01 | TC-NEG-08 y TC-CRE-02 son REQ y **fallan mientras KD-05 y KD-07 sigan abiertos** |
| H-06 | No envía headers de seguridad; expone `x-powered-by` | KD-06, hallazgo sin test (D-06) |

## 6. Ambigüedades y contradicciones resueltas

| ID | Tema | Resolución |
|---|---|---|
| C-01 | Filtrar `GET /booking` por `totalprice` (que no lo devuelve) | **D-01:** GPath sobre la colección de detalles de las reservas propias, obtenidas con `?firstname=` |
| C-02 | Token caducado no generable | **D-07:** se cubre el token inválido |
| C-03 | "Headers de seguridad" ausentes en la API | **D-06:** se valida `Content-Type` + `Server`; la ausencia queda como KD-06 |
| C-04 | Credenciales en archivos frente a no versionar secretos | **D-05:** placeholder en el `.properties` y valor desde el entorno |
| C-05 | `-Denv` con una sola API pública | **D-14:** `dev`/`qa` apuntan a la misma URL |
| A-01 | Screenplay (título) frente a API Object + Builder (cuerpo) | Screenplay orquesta sobre los API Objects |
| A-02 | "5 casos" frente a varias verificaciones por escenario | **D-16:** una clase por escenario, con varios `@Test` |
| A-05 | Contrato "estricto" sin schema entregado | **D-15:** schema según RC-01/RC-02, sin restricciones no documentadas |
| A-12 | Semántica de los filtros combinados | **D-19:** RC-03 define cada parámetro como una condición, así que se deben cumplir todos |
| A-13 | Rechazo exigido sin código definido: escrituras sin autorización (RF-09, RC-05), `PUT` sobre una reserva inexistente (RF-08: actualiza una reserva existente) y payload que incumple RC-01 | **D-20:** status no 2xx + estado intacto + código recibido como evidencia |

## 7. Trazabilidad resumida

**REQ** = el escenario verifica el requisito; si existe un KD abierto, el escenario falla hasta que se corrija · **MON** = registra un comportamiento que **ningún** requisito ni contrato define; **nunca cuenta como cobertura**. El detalle de cada escenario está en la matriz de la estrategia (§7).

| Requisito | Escenarios REQ | MON | Estado |
|---|---|---|---|
| RF-01 | TC-AUTH-01, TC-AUTH-02, TC-INT-01 | — | ✅ |
| RF-02 | TC-AUTH-02, TC-INT-01 | — | ✅ |
| RF-03, RF-04 | TC-CRE-01, TC-INT-01 | — | ✅ |
| RF-05 | TC-QRY-01, TC-INT-01 | — | ✅ |
| RF-06 | TC-QRY-01 a TC-QRY-04, TC-INT-01 | — | ✅ |
| RF-07 | TC-QRY-01 (alternativa D-01) | — | ⚠️ **Parcial.** Se cubren GPath `findAll`, la extracción de IDs sin `for` y el mapeo a `Booking`. El filtrado sobre la respuesta de `GET /booking` no es posible (H-01) |
| RF-08 | TC-AUTH-02, TC-NEG-05, TC-NEG-06, TC-INT-01 | — | ✅ |
| RF-09 | TC-AUTH-02, TC-NEG-04, TC-INT-01 | — | ✅ |
| RF-10 | TC-NEG-01, TC-NEG-02 | — | ✅ |
| RF-11 | — | — | ❌ **No cubierto.** No hay forma de obtener un token caducado ni se conoce su vigencia (C-02). TC-NEG-02 prueba un token **inválido** (nunca emitido), que es otra condición. Riesgo residual aceptado en D-07: si la API aceptara tokens caducados, no se detectaría |
| RF-12 | TC-NEG-03, TC-AUTH-02 | — | ✅ |
| RC-01 | **Respuesta:** TC-CON-01, TC-CON-04 · **Entrada (D-20):** TC-NEG-05, TC-NEG-07, TC-CRE-02, TC-NEG-08 | — | ⚠️ Respuestas conformes. En la entrada, TC-NEG-05 y TC-NEG-07 pasan; **TC-CRE-02 (KD-07) y TC-NEG-08 (KD-05) fallan** |
| RC-02 | TC-CON-04 | — | ✅ |
| RC-03 | TC-QRY-01 a TC-QRY-04 | — | ✅ (TC-QRY-04 por verificar) |
| RC-04 | TC-AUTH-02, TC-INT-01 | — | ✅ |
| RC-05 | TC-NEG-04 | — | ✅ Cookie; Basic fuera de alcance |
| RC-06, RC-07 | Por diseño (headers siempre enviados; `/ping` como verificación del entorno) | — | ✅ |
| RNF-03 | FW-01, FW-02 | — | ✅ |
| RNF-04 | TC-CON-02 | — | ✅ |
| RNF-05 | TC-CON-01, TC-CON-04 | — | ✅ |
| RNF-06 | TC-CON-03 | — | ⚠️ Sin headers de seguridad (KD-06) |
| RNF-07 | TC-CRE-01 (y todos los que crean datos) | — | ✅ |
| RNF-10 | FW-01 + evidencia de todos los escenarios | — | ✅ |
| RNF-01, 02, 08, 09, 11 | Configuración, diseño y revisión (estrategia §2 y §5) | — | Por revisión |
| — (código de error no definido) | — | TC-AUTH-03 (KD-01: 200 ante credenciales inválidas), TC-NEG-06 (KD-03: 405), TC-NEG-07 (KD-04: 500) | Monitoreado; no es cobertura |

## 8. No verificable con la API actual

Token caducado (RF-11) · headers de seguridad (ausentes) · frecuencia del reinicio de datos · rate limit · historial de commits (RNF-11, criterio de proceso).
