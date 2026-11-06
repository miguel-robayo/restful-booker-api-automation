# Arquitectura del Framework — Restful-Booker API

**Versión 2.5 · 2026-11-05 · Aprobado** (v2.1: verificación de la fase 0 · v2.2: capas HTTP, API Objects y Screenplay; ADR-20 y ADR-21 · v2.3: escenarios, fase explícita de observación posterior y refactorización BDD; ADR-13 precisado, ADR-22 a ADR-24 · v2.4: auditoría de trazabilidad Screenplay; detección de Questions HTTP derivada del código, detalles HTTP de la sesión en `TheSentRequests`, `TheBooking.responseFor`; sin cambios de ADR · v2.5: runner `BookingCreationTests` y ruta real del consolidado de limpieza; sin cambios de ADR)
Estrategia: [`test-strategy.md`](../strategy/test-strategy.md) · Requisitos: [`requirements-analysis.md`](../analysis/requirements-analysis.md) · Defectos: [`known-defects.md`](../known-defects.md)

## 1. Capas y responsabilidades

```mermaid
flowchart TB
    F["Features (Gherkin, español)<br/>comportamiento + tags"] --> S["Step Definitions<br/>adaptadores delgados"]
    H["ScenarioHooks<br/>@BeforeAll · @Before · @AfterStep · @After · @AfterAll"] --> C
    S --> C["ScenarioContext<br/>(PicoContainer, uno por escenario)"] --> A["Actor (Miguel)<br/>wasAbleTo · attemptsTo · asksFor · memoria"]
    S -->|Cuando: declara| O["PostActionObservations<br/>lecturas posteriores (ADR-22)"]
    H -->|@AfterStep ejecuta| O --> A
    S -->|Entonces| E["Expect / ResponseSpecs<br/>sobre evidencia almacenada"]
    A --> T["Tasks (escrituras)"] & Q["Questions (lecturas)"]
    T & Q --> AB["Abilities<br/>CallTheApi · UseAnAuthToken · TrackCreatedBookings · RecordSentRequests"]
    AB --> API["API Objects<br/>AuthApi · BookingApi · HealthApi"]
    API --> SF["SpecFactory + TokenCookieFilter + SentRequestFilter + MaskedAllureFilter"] --> RA["Rest Assured"] --> X[("API REST")]
    H --> SUP["BookingCleaner · EnvironmentHealthCheck · CleanupReport"] --> API
    J["JUnit 5 (Jupiter)<br/>FW-01 · FW-02 · ArchitectureRulesTest · FeatureConventionsTest"] --> CFG["config · http · features"]
```

| Capa | Responsabilidad | Prohibido |
|---|---|---|
| **Features** | Describir el comportamiento **en lenguaje de negocio**, con Miguel como actor, un solo `Cuando` y las etiquetas `@TC-xx`, `@esc-N` y de nivel | Términos técnicos (token, HTTP, JSON, GPath, SLA, códigos…), URLs, datos técnicos |
| **Step Definitions** | Traducir cada paso en llamadas al Actor o a `Expect`; un `Cuando` = **una** acción | HTTP directo, Rest Assured, API Objects, reglas de negocio |
| **ScenarioContext** | Contener a Miguel, los datos de prueba y las observaciones del escenario (instancia nueva por escenario, **sin campos `static`**) | Estado compartido entre escenarios |
| **PostActionObservations** | Lecturas de evidencia **declaradas** por el `Cuando` y **ejecutadas** por `@AfterStep` (ADR-22) | Escrituras, aserciones |
| **Actor** | Ejecutar Tasks y Questions, recordar resultados (`remember`/`recall`) y publicar los steps de Allure | Aserciones |
| **Abilities** | Entregar API Objects con la spec adecuada (base o autenticada) | Lógica de negocio |
| **Tasks** | Una escritura de negocio: guardan la `Response` y registran las reservas creadas | Aserciones, lecturas |
| **Questions** | Una lectura (HTTP o derivada, por ejemplo GPath) que devuelve un valor tipado. Las de memoria (`TheLastResponse`, `TheSentRequests`, `BookingsPricedAbove`) son las únicas permitidas en un `Entonces`; los métodos y rutas HTTP que hacen falta para responder viven aquí, no en los steps | Escrituras, aserciones |
| **API Objects** | Endpoint, método, parámetros → `Response` sin validar | Aserciones, lógica |
| **http** | Spec inmutable, autenticación por filtro, enmascarado, adjuntos de Allure, logs `ifValidationFails` | Estado global de Rest Assured |
| **Expect / ResponseSpecs** | Aserciones reutilizables con prefijos `KD-xx:` y `SLA:` | Peticiones HTTP |
| **support** | Limpieza con verificación de propiedad, health check y consolidación de pendientes | Aserciones |

## 2. Regla Gherkin → Actor (ADR-13, ADR-22)

| Fase | Dónde | Llamada | Semántica |
|---|---|---|---|
| **Dado** | Step | `wasAbleTo(task…)` o `asksFor(question)` | Prepara el contexto y obtiene el estado inicial. Una precondición fallida → `PreconditionFailedException` (categoría Precondition) |
| **Cuando** | Step | **exactamente una** `attemptsTo(task)` o `asksFor(question HTTP)` | Solo la acción principal. Guarda su respuesta y **declara** (no ejecuta) la evidencia posterior que necesita: `observations.after(descripción, question, clave)` |
| **Observación posterior** | Hook `@AfterStep` | `asksFor(question)` por cada observación declarada | Justo después del `Cuando`, ejecuta las lecturas declaradas, en orden; cada una es un paso visible en Allure ("Post-action observation: …"). Solo `Question` (lecturas), nunca valida |
| **Entonces** | Step | `Expect…(recall(clave))` / `ResponseSpecs` | Verifica **evidencia ya almacenada**. **Nunca provoca HTTP** |

Verificado automáticamente: un `Cuando` contiene exactamente una acción HTTP; un `Entonces`, ninguna, ni directa ni a través de un helper privado del step (`ArchitectureRulesTest`, que deduce del código qué Questions hacen HTTP: las que usan `CallTheApi` o componen otra Question HTTP); un solo `Cuando` por escenario, Miguel solo en el primer `Dado` y sin términos técnicos (`FeatureConventionsTest`).

Ejemplo (TC-NEG-01; los `Antecedentes` de la regla se ejecutan antes de cada escenario, así que cada uno crea su propia reserva):
```gherkin
# language: es
Regla: Sin una sesión válida no se puede modificar una reserva y esta conserva su información

  Antecedentes:
    Dado que Miguel tiene una reserva registrada     # crea la reserva propia (POST)
    Y conoce el estado actual de su reserva          # estado inicial (GET)

  @TC-NEG-01 @esc-5 @requested @req @negative @security
  Escenario: Intentar modificar una reserva sin haber iniciado sesión
    Dado que no ha iniciado sesión
    Cuando intenta modificar su reserva              # única acción (PUT); declara la observación del estado
    Entonces la modificación es rechazada por falta de autorización     # 403, sobre la respuesta guardada
    Y la reserva conserva su información original                       # compara la observación (GET) con el estado inicial
```

## 3. Estructura

```
src/main/java/com/credibanco/qa/restfulbooker/
├── config/      EnvironmentConfig, ConfigResolver, ConfigurationException
├── http/        SpecFactory, JsonMapper, TokenCookieFilter, MaskedAllureFilter, SensitiveDataMasker, MaskingOutputStream, TokenFingerprint, SentRequest, SentRequestFilter
├── reporting/   AllureReporter (steps, adjuntos y step broken, solo con un escenario en curso)
├── api/         BaseApi, AuthApi, BookingApi, HealthApi
├── model/       Booking, BookingDates, BookingResponse, AuthRequest, AuthResponse
├── data/        BookingDataFactory, UniqueMarker
├── screenplay/  core/ (Actor, Ability, Performable, Question, PreconditionFailedException)
│                abilities/ (CallTheApi, UseAnAuthToken, TrackCreatedBookings, RecordSentRequests)
│                tasks/ (Authenticate, CreateBooking, UpdateBooking, DeleteBooking)
│                questions/ (TheLastResponse, TheBooking, BookingIds, BookingDetails, BookingsPricedAbove, HighestBookingId, TheSentRequests)
└── support/     BookingCleaner, CleanupResult, CleanupReport, EnvironmentHealthCheck, InfrastructureException

src/test/java/com/credibanco/qa/restfulbooker/
├── runners/     ApiScenariosSuite   (@Suite · @IncludeEngines("cucumber") · @SelectPackages("features"))
│                BookingCreationTests (solo booking_creation.feature; fuera de los includes *Test/*Suite: solo con -Dtest)
├── context/     ScenarioContext, ScenarioMemory, PostActionObservations
├── hooks/       ScenarioHooks
├── steps/       AuthenticationSteps, BookingSteps, BookingSearchSteps, BookingQualitySteps
├── support/     Expect, ResponseSpecs, Preconditions, HttpStatus
└── framework/   ConfigResolverTest (FW-02), SensitiveDataMaskerTest (FW-01), ModelSerializationTest, BookingDataFactoryTest,
                 SpecFactoryTest, ActorTest, ArchitectureRulesTest, FeatureConventionsTest

src/test/resources/
├── features/    authentication · booking_creation · booking_queries · booking_validation (.feature)
├── config/      default · dev · qa (.properties)
├── schemas/     booking.json
├── fixtures/    respuestas reales de la API para probar la deserialización
├── allure/      categories.json
└── allure.properties, junit-platform.properties
```

## 4. Dependencias

| Dependencia | Versión | Fase | Motivo |
|---|---|---|---|
| `junit-bom` → `junit-jupiter`, `junit-platform-suite` | 5.14.4 | 0 | Tests técnicos + runner `@Suite` para Cucumber |
| `cucumber-bom` → `cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer` | 7.34.9 | 0 | BDD sobre JUnit Platform; contexto por escenario |
| `allure-bom` → `allure-cucumber7-jvm` | 2.35.5 | 0 | **Único** adaptador de Allure |
| `rest-assured`, `json-schema-validator` | 5.5.7 | 1 | HTTP y contrato |
| `jackson-bom` → `jackson-databind`, `jackson-datatype-jsr310` · `lombok` (`provided`, procesador explícito) · `datafaker` | 2.22.4 · 1.18.48 · 2.7.0 | 1 | Modelos y datos |
| `allure-java-commons` (`compile`, versión del `allure-bom`) | 2.35.5 | 1 | Steps y adjuntos enmascarados desde `src/main` (ADR-05, ADR-06) |
| `slf4j-simple` (`test`, nivel `warn`) | 2.0.17 | 1 | Proveedor SLF4J para los logs internos de Allure: sus errores quedan visibles |
| Fijadas por convergencia | `jsr305` 3.0.2 · `libphonenumber` 9.0.33 | 1 | Conflictos transitivos de `json-schema-validator` (Enforcer) |
| **Plugins:** compiler 3.16.0 · surefire 3.6.0 · resources 3.5.0 · clean 3.5.0 · enforcer 3.6.3 · allure-maven 3.1.0 · Maven Wrapper (Maven 3.9.14) | | 0 | Build reproducible |

**Excluidas a propósito:** `allure-junit5` (duplicaría los escenarios), `aspectjweaver` (ADR-06), `allure-rest-assured` (ADR-05) y cualquier librería de logging para el código del framework (ADR-07; `slf4j-simple` solo atiende a Allure).

## 5. Ciclo de ejecución

| Momento | Qué ocurre |
|---|---|
| `mvn clean test` | Surefire ejecuta los tests técnicos de Jupiter (FW) y `ApiScenariosSuite` con `cucumber.filter.tags = not @known-defect`. Resultados en `target/allure-results` y `target/surefire-reports` |
| `mvn test -Pknown-defects` | **Solo** `ApiScenariosSuite`, con `@known-defect`; sin FW-01 ni FW-02. Resultados en `target/allure-results-known-defects` y `target/surefire-reports-known-defects`. Se conserva el resultado real (sin `testFailureIgnore`) |
| `@BeforeAll` (una vez) | `EnvironmentHealthCheck` → `GET /ping`; si falla → `InfrastructureException` (`INFRA:`) |
| Por escenario | PicoContainer crea `ScenarioContext` (Miguel, datos, observaciones) → `@Before` adjunta la semilla → pasos; después de cada paso, `@AfterStep` ejecuta las observaciones que haya declarado el `Cuando` (solo lo hace él) → `@After` (siempre): `BookingCleaner` hace un `GET` de propiedad y un `DELETE` solo si es propia (D-12); si algo queda pendiente, step **broken** con adjunto, sin cambiar el resultado del escenario (D-08) |
| Pendientes de limpieza | Cada escenario escribe **su propio** archivo en `target/cleanup/<directorio de resultados>/` (nombre único; un directorio por job): sin estado compartido en memoria |
| `@AfterAll` | `CleanupReport` consolida → `target/cleanup/<directorio de resultados>/cleanup-pending.json`, panel Environment de Allure (`cleanup.pending.count`) y aviso en consola si hay pendientes |
| Paralelismo | `cucumber.execution.parallel.enabled=true`, `fixed`, 4 hilos (configurable). Un escenario se ejecuta entero en un hilo con su propio contexto |

## 6. Configuración, autenticación, datos, errores y logs

| Tema | Diseño |
|---|---|
| Configuración | Precedencia `-D` > `RB_*` > `<env>.properties` > `default.properties`; `-Denv=dev\|qa`; URL `https` validada al cargar; credenciales `${RB_USERNAME}`/`${RB_PASSWORD}` validadas **al primer uso** |
| Autenticación | Task `Authenticate` → token en `UseAnAuthToken` (exige `token`; un 200 con `reason` falla, KD-01). `CallTheApi` usa `SpecFactory.authenticated(token)` (spec nueva + `TokenCookieFilter`, que envía la **cabecera** `Cookie: token=…`, ADR-20). La limpieza usa su propio actor |
| Datos | `BookingDataFactory` (Datafaker, semilla por escenario, adjunta como "Data seed"); `firstname` único; serialización `NON_NULL`; los payloads inválidos de los escenarios negativos (`InvalidBookingPayload`) se crearán solo en `src/test` con la ronda de escenarios propuestos (**aún no implementada**); por eso `CreateBooking`, `UpdateBooking` y `BookingApi` aceptan `Object` |
| Identidad del token | `TokenFingerprint`: SHA-256 truncado de la cabecera `Cookie` **realmente enviada**, registrado por `SentRequestFilter` en `RecordSentRequests` y mostrado en los adjuntos como `[token sha256:…]`. Permite comparar sesiones sin revelar el token |
| Excepciones (unchecked) | `AssertionError` → failed · `PreconditionFailedException` / `InfrastructureException` / otras → broken. `ConfigurationException` → Test Defect. Nadie silencia excepciones |
| Logs | Request/response enmascarados como adjuntos de Allure del paso · `log().ifValidationFails()` por spec, con `Cookie`/`Set-Cookie`/`Authorization` en lista negra y escrito a través de `MaskingOutputStream`, que enmascara `password`/`token` de los bodies línea a línea (ADR-20) · `System.Logger` del JDK para el framework |
| Paralelismo seguro | Sin escrituras en `RestAssured.*`; specs inmutables; actor, `Faker` y tracker por escenario. Compartidos e inmutables: `EnvironmentConfig`, `JsonMapper` |

## 7. Decisiones de arquitectura (ADR)

| ADR | Decisión | Trade-off |
|---|---|---|
| 01 | Screenplay **propio** y ligero, no Serenity | Control y pocas dependencias, a cambio de mantener unas 6 clases núcleo |
| 02 | **Sin Interactions HTTP** y **sin HTTP en los steps**: solo los API Objects hablan HTTP | Elimina la capa duplicada |
| 03 | Escrituras = Tasks, lecturas = Questions; las Questions se ejecutan en el paso que expresa la lectura y su resultado se almacena | Un solo camino por operación; los `Entonces` son puros |
| 04 | Los API Objects devuelven `Response` sin validar | Los negativos necesitan la respuesta cruda |
| 05 | `MaskedAllureFilter` propio sobre `allure-java-commons` | Enmascara secretos (FW-01); se pierden las plantillas HTML de `allure-rest-assured` |
| 06 | `Allure.step(lambda)` anidado en los pasos Gherkin | Sin AspectJ; algo más explícito |
| 07 | `System.Logger` del JDK | Cero dependencias; menos configurable |
| 08 | DTOs con Lombok `@Value @Builder @Jacksonized` | Cumple el requisito de Builder; los *records* no tienen builder |
| 09 | Jackson tolerante (`FAIL_ON_UNKNOWN_PROPERTIES=false`) + `NON_NULL` | El contrato lo valida el JSON Schema (D-15) |
| 10 | `LocalDate` + `jackson-datatype-jsr310` | Fechas seguras a cambio de un módulo más |
| 11 | Framework en `src/main`, escenarios y tests técnicos en `src/test` | Capas separadas; dependencias en scope `compile` |
| **12** | **Reemplazado:** el ciclo de vida de los escenarios lo dan **solo** los hooks de Cucumber + `ScenarioContext`. Se eliminan las extensiones JUnit de la versión 1.0 | Un único mecanismo; los tests técnicos de Jupiter no usan Actor |
| **13** | Dado = `wasAbleTo` o lectura del estado inicial · Cuando = **exactamente una** acción (`attemptsTo` o `asksFor`) · Entonces = `Expect` sobre la evidencia almacenada, **sin HTTP** (precisado en v2.3) | D-09 aplicado con una regla genérica; ningún `Entonces` hace HTTP; ningún `Cuando` esconde varias operaciones |
| 14 | Cucumber **7.34.9**, no 8 (este último está compilado contra JUnit 6) | Compatible con JUnit 5.14 y Allure 2.35; migrar a 8 implica JUnit 6 + Allure 3 |
| 15 | PicoContainer para el contexto por escenario | Ligero y propio de Cucumber; descartados un `ThreadLocal` manual (frágil) y Spring (pesado) |
| 16 | Un solo adaptador de Allure (`allure-cucumber7-jvm`); FW-01/02 se reportan solo en Surefire | Imposible duplicar; los tests técnicos no aparecen en Allure |
| 17 | Jobs separados por `cucumber.filter.tags` y perfil `known-defects` limitado a la suite, con directorios propios | Filtrado nativo de Cucumber, independiente de `groups` de Surefire |
| 18 | Maven Wrapper (Maven 3.9.14) | Build reproducible para evaluadores y CI; 3 archivos más |
| 19 | Gherkin en español (`# language: es`), código en inglés | Legible para la exposición; dos idiomas en el repositorio |
| 20 | El token viaja como **cabecera** `Cookie: token=…` (no como cookie de Rest Assured) y el log condicional se escribe a través de `MaskingOutputStream` | Verificado en integración: Rest Assured imprimía las cookies y los bodies (contraseña de `/auth`) sin poder ocultarlos. En la red la petición es idéntica (RC-05) |
| 21 | `BaseApi` como base de los API Objects y `AllureReporter` que solo reporta con un escenario en curso | `BaseApi` usa las specs que `SpecFactory` construye (no la duplica). Sin el guard, Allure registra errores fuera de un escenario (tests de Jupiter) |
| **22** | **Fase explícita de observación posterior**: el `Cuando` declara las lecturas de evidencia y el hook `@AfterStep` las ejecuta justo después, como pasos visibles en Allure | Única forma de cumplir a la vez "un solo `Cuando`", "el `Cuando` solo hace la acción principal", ADR-13 y "no debilitar comprobaciones" (análisis en la estrategia §3.1). Coste: `allure-cucumber7-jvm` muestra el hook también (vacío) después de los demás pasos |
| 23 | Identidad del token por huella SHA-256 de la cabecera **realmente enviada** (`SentRequestFilter` + `RecordSentRequests`) | Prueba que la modificación y la cancelación usan la misma sesión sin revelar el token; se compara en memoria, no en el reporte |
| 24 | Sin `Allure.parameter`: valores como la semilla o la medición del SLA se adjuntan | Con `allure-cucumber7-jvm 2.35.5`, `Allure.parameter` lanza `UnsupportedOperationException` dentro de un paso y registra "test case not found" en los hooks (verificado) |

## 8. Verificación de la fase 0 (ejecución real con escenarios de prueba, sin HTTP)

| Verificado | Resultado |
|---|---|
| `allure-cucumber7-jvm 2.35.5` + Cucumber 7.34.9 | ✅ Compatibles; `allure-maven 3.1.0` genera el reporte con la herramienta Java 2.46.1 (no Node.js) |
| Un resultado de Allure por escenario | ✅ 12 resultados para 12 escenarios; FW no aparece en Allure |
| Filtrado por etiquetas a través de `@Suite` | ✅ `not @known-defect` en el principal; `-Pknown-defects` ejecuta solo `@known-defect`, sin FW y con directorios propios |
| Paralelismo y aislamiento | ✅ 4 hilos, un `ScenarioContext` distinto por escenario, `@BeforeAll` una sola vez, `@After` tras un paso fallido |
| Estados y categorías | ✅ failed/broken correctos; cada escenario en una sola categoría. Con cinco categorías, el SLA caía en la categoría interna "Product defects" de Allure → se agregó **Performance (SLA)** |
| `@issue=KD-xx` | ✅ Enlace de tipo *issue*; navega a `known-defects.md` gracias a `allure.link.issue.pattern` |
| Selector de features | `@SelectClasspathResource` sobre el directorio genera un aviso de Cucumber → se usa `@SelectPackages("features")`. Sobre **un solo archivo** (`BookingCreationTests`, v2.5) no genera aviso (verificado) |

**Verificado después, con la API real:** step broken de limpieza pendiente dentro de un escenario aprobado y panel Environment (ejecución forzada con la limpieza fallida); consolidación concurrente de `target/cleanup/` en 4 ejecuciones en paralelo (0 pendientes); observaciones posteriores visibles en Allure, entre el `Cuando` y el primer `Entonces`.
