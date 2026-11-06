# Los cinco escenarios de la prueba técnica

**Entrega técnica · 2026-11-05** · Fuente de los resultados esperados: el documento de la prueba [DOC] y, donde este no define nada, la documentación de la API [API-DOC] (D-17). Matriz completa y decisiones: [`test-strategy.md`](../strategy/test-strategy.md) §7 · Requisitos: [`requirements-analysis.md`](../analysis/requirements-analysis.md).

Los cinco escenarios del documento se implementan con **10 casos de prueba** (`@TC-xx`) en **11 escenarios de Cucumber**: TC-AUTH-02 se divide en dos porque cada escenario tiene un solo `Cuando` (C-6). Todos llevan la etiqueta `@esc-N` del escenario del documento, `@requested` y `@req` (nivel REQ: cuentan como cobertura).

| # | Tema | Casos | Feature | Ejecutar |
|---|---|---|---|---|
| 1 | Autenticación y reutilización del token | TC-AUTH-01, TC-AUTH-02 ×2 | `authentication.feature` | `mvn test -Dtest=ApiScenariosSuite "-Dcucumber.filter.tags=@esc-1" -Denv=qa` |
| 2 | Creación dinámica | TC-CRE-01 | `booking_creation.feature` | `mvn test -Dtest=BookingCreationTests -Denv=qa` |
| 3 | Búsqueda y filtro GPath | TC-QRY-01 | `booking_queries.feature` | `mvn test -Dtest=ApiScenariosSuite "-Dcucumber.filter.tags=@esc-3" -Denv=qa` |
| 4 | Contrato, SLA y headers | TC-CON-01, TC-CON-02, TC-CON-03 | `booking_validation.feature` | `mvn test -Dtest=ApiScenariosSuite "-Dcucumber.filter.tags=@esc-4" -Denv=qa` |
| 5 | Casos negativos | TC-NEG-01, TC-NEG-02, TC-NEG-03 | `authentication.feature`, `booking_queries.feature` | `mvn test -Dtest=ApiScenariosSuite "-Dcucumber.filter.tags=@esc-5" -Denv=qa` |

**Comunes a todos:**

- El actor es Miguel. Cada escenario crea sus propios datos, con un `firstname` generado más un sufijo único, y su propia sesión.
- Las reservas creadas se registran antes de cualquier aserción. El hook `@After` las borra pase o falle el escenario, después de comprobar que son propias (D-12).
- Request y response se adjuntan a Allure con los secretos enmascarados.
- Si no se puede preparar el escenario (crear la reserva o iniciar sesión), el resultado es *broken / Precondition* y no un fallo funcional.

---

## Escenario 1 — Autenticación y reutilización del token

**Objetivo:** obtener un token con las credenciales configuradas y demostrar que **el mismo token** autoriza la modificación (`PUT`) y la cancelación (`DELETE`) de una reserva sin volver a autenticarse.
**Requisitos:** RF-01, RF-02, RF-08, RF-09, RF-12, RC-04, RC-05.

| Caso | Escenario | Endpoints | Resultado esperado |
|---|---|---|---|
| TC-AUTH-01 | Iniciar sesión con credenciales válidas | `POST /auth` | 200, `token` no vacío y deserializado a `AuthResponse`, sin `reason`. Ese mismo `token` del POJO es el que `TokenCookieFilter` envía como `Cookie: token=<valor>` |
| TC-AUTH-02 (a) | Modificar una reserva después de iniciar sesión | `PUT /booking/{id}`, `GET` | 200, el body devuelto es igual al enviado y el `GET` posterior confirma los cambios |
| TC-AUTH-02 (b) | Cancelar una reserva modificada sin volver a iniciar sesión | `PUT`, `DELETE /booking/{id}`, `GET` | `DELETE` 201 · `GET` 404 · el `PUT` y el `DELETE` usaron el mismo token · un solo `POST /auth` |

```gherkin
Regla: Una misma sesión permite modificar y cancelar una reserva sin volver a iniciar sesión
  Antecedentes:
    Dado que Miguel inició sesión
    Y tiene una reserva registrada

  Escenario: Cancelar una reserva modificada sin volver a iniciar sesión
    Dado que ya modificó esa reserva durante la misma sesión
    Cuando cancela su reserva
    Entonces la cancelación queda confirmada
    Y la reserva deja de estar disponible
    Y la modificación y la cancelación se realizaron con la misma sesión
    Y no fue necesario iniciar sesión nuevamente
```

**Notas técnicas:**

- El token viaja como cabecera `Cookie: token=…`, inyectada por `TokenCookieFilter` (ADR-20).
- "Misma sesión" se prueba comparando la huella SHA-256 de la cabecera **realmente enviada** en el `PUT` y en el `DELETE` con la del token obtenido (ADR-23); el token no aparece en el reporte.
- KD-01: la API responde 200 incluso con credenciales inválidas, así que la autenticación nunca se valida solo por el status.

## Escenario 2 — Creación dinámica de una reserva

**Objetivo:** crear una reserva con un payload construido con `Booking.builder()` y datos generados con Datafaker, sin JSON estático, y verificar que lo devuelto coincide con lo enviado.
**Requisitos:** RF-03, RF-04, RNF-07.

| Caso | Endpoints | Resultado esperado |
|---|---|---|
| TC-CRE-01 | `POST /booking` | 200, `bookingid` presente y `booking` igual al request (comparación del objeto completo) |

```gherkin
Escenario: Realizar una reserva de alojamiento
  Dado que Miguel tiene los datos de una nueva reserva de alojamiento
  Cuando registra la reserva
  Entonces la reserva queda confirmada
  Y recibe un número de reserva
  Y la reserva registrada contiene exactamente los datos ingresados
```

**Notas técnicas:**

- `BookingDataFactory`: precio entero de 1 a 10 000 (KD-09), `checkout` posterior a `checkin` y todos los campos de RC-01.
- La semilla se adjunta como "Data seed", así que una ejecución se puede reproducir.
- Runner dedicado: `BookingCreationTests`.

## Escenario 3 — Búsqueda por nombre y filtro GPath por precio

**Objetivo:** filtrar reservas por query params y aplicar GPath para obtener, sin bucles `for`, los IDs de las reservas con `totalprice` mayor que el valor de referencia (`query.price.threshold`, 500 por defecto).
**Requisitos:** RF-05, RF-06, RF-07 (parcial, D-01), RC-03.

| Caso | Endpoints | Resultado esperado |
|---|---|---|
| TC-QRY-01 | `GET /booking?firstname=`, `GET /booking/{id}` | La búsqueda devuelve exactamente las 4 reservas propias · GPath identifica solo las de `T+1` y `T+500` (el valor límite `T` queda excluido) · el detalle mapeado a `Booking` coincide con lo registrado |

```gherkin
Escenario: Identificar sus reservas de valor superior al de referencia
  Dado que Miguel tiene cuatro reservas a su nombre con valores alrededor del valor de referencia
  Cuando busca entre sus reservas las de valor superior al de referencia
  Entonces la búsqueda por su nombre encuentra exactamente sus cuatro reservas
  Y solo se identifican las reservas que superan el valor de referencia
  Y el detalle de la reserva identificada coincide con el registrado
```

**Notas técnicas:**

- `GET /booking` solo devuelve `bookingid` (contradicción C-01 del documento). Por eso GPath (`findAll { it.booking.totalprice > T }`) se aplica a la colección de detalles de las reservas propias, lo que hace la prueba determinista (D-01).
- Se crean 4 reservas con precios `T-1`, `T`, `T+1` y `T+500` para cubrir el límite.

## Escenario 4 — Contrato, SLA y headers

**Objetivo:** validar que `GET /booking/{id}` cumple el JSON Schema, responde dentro del SLA y devuelve los headers acordados.
**Requisitos:** RNF-04, RNF-05, RNF-06, RC-01.

| Caso | Escenario | Resultado esperado |
|---|---|---|
| TC-CON-01 | Recibir la información completa de una reserva | La respuesta cumple `schemas/booking.json` (`matchesJsonSchemaInClasspath`); todos los campos de RC-01 son obligatorios y tienen su tipo |
| TC-CON-02 | Consultar una reserva en un tiempo aceptable | Tiempo **estrictamente menor** que `sla.booking.get.ms` (2000 ms) |
| TC-CON-03 | Recibir la información en el formato acordado | `Content-Type: application/json; charset=utf-8` y header `Server` presente |

**Notas técnicas:**

- **SLA:** se mide del lado del cliente con Rest Assured, sin reintentos, y se valida con `time(lessThan(...))` (`ResponseSpecs.responseTimeBelow`). Una respuesta sin tiempo medido no cumple el SLA. El tiempo real se adjunta antes de comparar y un fallo lleva el prefijo `SLA:`, que lo lleva a la categoría *Performance (SLA)*.
- **Headers:** la API no envía headers de seguridad (KD-06, hallazgo). RNF-06 acepta "de seguridad **o** servidor", así que se valida la presencia de `Server`.
- **Schema:** no incluye restricciones que la documentación de la API no establece, como `additionalProperties` (D-15).

## Escenario 5 — Casos negativos

**Objetivo:** comprobar que sin token o con un token inválido no se puede modificar una reserva y que esta **queda intacta**, y que un ID inexistente devuelve 404.
**Requisitos:** RF-10, RF-12. RF-11 (token caducado) no es automatizable, porque la API no permite generar uno (D-07).

| Caso | Escenario | Endpoints | Resultado esperado |
|---|---|---|---|
| TC-NEG-01 | Intentar modificar una reserva sin haber iniciado sesión | `PUT /booking/{id}` sin token, `GET` | 403 y la reserva conserva su estado inicial |
| TC-NEG-02 | Intentar modificar una reserva con una sesión no válida | `PUT` con token generado, `GET` | 403 y la reserva conserva su estado inicial |
| TC-NEG-03 | Consultar una reserva inexistente | `GET /booking/9999999` | 404 |

```gherkin
Regla: Sin una sesión válida no se puede modificar una reserva y esta conserva su información
  Antecedentes:
    Dado que Miguel tiene una reserva registrada
    Y conoce el estado actual de su reserva

  Escenario: Intentar modificar una reserva sin haber iniciado sesión
    Dado que no ha iniciado sesión
    Cuando intenta modificar su reserva
    Entonces la modificación es rechazada por falta de autorización
    Y la reserva conserva su información original
```

**Notas técnicas:**

- Cada caso cambia **una sola variable**: body válido con token ausente o inválido.
- El estado posterior se lee en la observación posterior (`@AfterStep`, ADR-22) y se compara con el estado inicial. Así se evitan los falsos positivos de un rechazo con efecto colateral.
- El token inválido se genera; nunca es un literal.
- TC-NEG-03 usa `9999999` solo si es mayor que el ID más alto existente (`GPath bookingid.max()`); si no, el resultado es Precondition.

---

## Trazabilidad requisito → caso

| Requisito | Casos |
|---|---|
| RF-01 token y POJO | TC-AUTH-01 |
| RF-02 token dinámico en `PUT`/`DELETE` | TC-AUTH-02 |
| RF-03, RF-04 creación dinámica y eco | TC-CRE-01 |
| RF-05, RF-06, RF-07 mapeo, filtro y GPath | TC-QRY-01 |
| RF-08, RF-09 `PUT` completo, `DELETE` | TC-AUTH-02 |
| RF-10 403 sin token o con token inválido | TC-NEG-01, TC-NEG-02 |
| RF-12 404 | TC-NEG-03, TC-AUTH-02 (b) |
| RNF-04 SLA · RNF-05 schema · RNF-06 headers | TC-CON-02 · TC-CON-01 · TC-CON-03 |
| RNF-03 secretos y configuración · RNF-10 enmascarado | FW-01, FW-02 (JUnit) |

Los 14 casos propuestos (🟨) de la estrategia amplían esta cobertura y pertenecen a una ronda posterior. Hoy no hay escenarios `@known-defect` ni `@kd-monitor` implementados.
