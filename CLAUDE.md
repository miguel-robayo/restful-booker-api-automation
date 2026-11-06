# CLAUDE.md

Este archivo orienta a Claude Code (claude.ai/code) cuando trabaja con el código de este repositorio.

## Proyecto

Framework de automatización de pruebas de la API REST pública Restful-Booker (prueba técnica). Java 21, Maven, Cucumber 7 (Gherkin en español) sobre JUnit Platform, Screenplay propio y ligero, patrón API Object, Rest Assured 5, Allure 2.

El diseño aprobado está en `docs/` y es la fuente de verdad; léelo antes de cambiar comportamiento:
- `docs/analysis/requirements-analysis.md`: requisitos (RF/RC/RNF), hallazgos y trazabilidad.
- `docs/strategy/test-strategy.md`: 24 escenarios (TC-xx, FW-xx), niveles REQ/MON, categorías de Allure, decisiones D-01..D-22.
- `docs/architecture/framework-architecture.md`: capas, regla Dado/Cuando/Entonces, ADR-01..24.
- `docs/known-defects.md`: divergencias de la API (KD-xx) y cómo se prueban.

## Comandos

JDK 21 es obligatorio (si no, Maven Enforcer falla). Usa el Wrapper.

```bash
./mvnw clean verify                         # job principal: tests del framework + escenarios "not @known-defect"
./mvnw clean test -Denv=dev                 # elegir ambiente (dev|qa, por defecto qa)
./mvnw test -Pknown-defects                 # job separado: solo escenarios @known-defect, con sus propios directorios de resultados
./mvnw test -Dtest=ConfigResolverTest       # una sola clase de test JUnit
./mvnw test -Dtest=BookingCreationTests -Denv=qa   # solo booking_creation.feature (escenario 2); no está en los includes por defecto
./mvnw test -Dtest=ApiScenariosSuite -Dcucumber.filter.tags="@TC-NEG-01"   # escenarios de un TC (requiere RB_USERNAME/RB_PASSWORD)
./mvnw test -Dtest=ApiScenariosSuite -Dcucumber.execution.dry-run=true      # detecta pasos sin definir o ambiguos sin llamar a la API
./mvnw allure:serve                         # abre el reporte de target/allure-results
./mvnw allure:serve -Pknown-defects         # abre el reporte del job de defectos conocidos
```

Las credenciales vienen solo de las variables de entorno `RB_USERNAME` / `RB_PASSWORD` (o de `-Dauth.username` / `-Dauth.password`). Nunca se hace commit de ellas.

## Escenarios nuevos o modificados

Para crear o modificar un escenario, feature, paso, Task, Question, método de API Object o dato de prueba, usa **siempre** el skill `/nuevo-escenario` (`.claude/skills/nuevo-escenario/`), con su guía de nombres `naming.md`. Las reglas de abajo siguen aplicando; el skill da el procedimiento y los nombres.

## Reglas de arquitectura (no romper)

- Features → Step Definitions → Actor → Tasks (escrituras) / Questions (lecturas) → Abilities (`CallTheApi`) → API Objects → Rest Assured. Los API Objects cumplen el papel de las Interactions de Screenplay (ADR-02).
- Los detalles HTTP (rutas, métodos, filtrado de peticiones enviadas) viven en Questions/Tasks, nunca en los step definitions. Los pasos "Entonces" solo usan evidencia almacenada o Questions de memoria (`TheLastResponse`, `TheSentRequests`, `BookingsPricedAbove`); `ArchitectureRulesTest` deduce del código qué Questions hacen HTTP.
- `Dado` = preparar el contexto y el estado inicial (`wasAbleTo`, precondición, exige 2xx); `Cuando` = **exactamente una** acción (`attemptsTo` o `asksFor`), que puede **declarar** evidencia posterior con `observations.after(...)`; el hook `@AfterStep` ejecuta esas lecturas justo después (ADR-22); `Entonces` = `Expect` / `ResponseSpecs` sobre la evidencia almacenada, **sin HTTP en los pasos Entonces**.
- Los step definitions son adaptadores delgados: sin Rest Assured, sin API Objects, sin reglas de negocio.
- Solo los API Objects construyen peticiones HTTP; no existe una capa genérica de "Interactions" HTTP.
- Nunca se escribe en el estado estático `RestAssured.*`; se construye una spec nueva por petición. Sin estado estático mutable; un `ScenarioContext` por escenario (PicoContainer).
- Los hooks de Cucumber son el único mecanismo de ciclo de vida de los escenarios. JUnit Jupiter es solo para los tests técnicos del framework (FW-xx).
- REQ/MON lo decide el requisito, nunca lo que la API hace hoy. `@known-defect` = REQ que falla por un KD abierto (job separado); `@kd-monitor` = MON (job principal).
- Los prefijos de los mensajes de aserción definen las categorías de Allure: `KD-xx:`, `SLA:`, `PRECONDITION:`, `INFRA:`.
- Un único adaptador de Allure: `allure-cucumber7-jvm`. No agregar `allure-junit5`.

## Reglas de redacción BDD (las verifica `FeatureConventionsTest`)

- Solo lenguaje de negocio en las features (títulos, pasos, comentarios): sin token, HTTP, JSON, schema, GPath, SLA, Builder, cabeceras ni códigos de estado. Las notas técnicas van en `docs/strategy/test-strategy.md` §7.
- Un actor, **Miguel**, presentado en el primer `Dado` que se ejecuta ("Dado que Miguel …"), que es el de `Antecedentes` cuando hay antecedentes; los pasos siguientes omiten el sujeto. Las comprobaciones se hacen sobre los pasos efectivos (feature + antecedentes de la regla + escenario).
- `Antecedentes` solo para precondiciones de negocio comunes a todos los escenarios de su feature o `Regla`: solo `Dado`/`Y`, máximo 4 pasos, nunca la acción ni resultados (C-7). `Regla` solo para ejemplos de una misma regla de negocio; `Esquema del escenario` solo cuando cambian únicamente los datos (C-8).
- Exactamente un `Cuando` por escenario (un `Y` después del `Cuando` cuenta como otra acción). Los comportamientos independientes se separan en escenarios distintos que conservan la misma etiqueta `@TC-*`.
- Todo escenario lleva las etiquetas `@TC-*` y `@esc-*`. Vocabulario: "iniciar sesión/sesión" = token de autenticación, "cancelar" = DELETE (la reserva deja de existir).
- Métodos Java de los pasos: verbo imperativo para `Dado`/`Cuando`, `verify…` para `Entonces` (lo verifica `ArchitectureRulesTest`); los códigos de estado esperados vienen de `HttpStatus`.

## Acuerdo de trabajo

- Comunicarse en español. Flujo: ANALIZAR → PLANIFICAR → IMPLEMENTAR → VERIFICAR → REVISAR → DOCUMENTAR; presentar el plan, los archivos afectados, los riesgos y los criterios de aceptación antes de implementar.
- No inventar reglas de negocio; los resultados esperados salen del documento de la prueba y, después, de la documentación de la API; el comportamiento observado solo sirve para identificar divergencias.
- No modificar documentos aprobados sin autorización explícita.
- Ningún commit, push, merge ni rebase sin mostrar el diff y obtener autorización explícita.
- No agregar dependencias sin justificarlo; las versiones fijadas deben tener al menos dos semanas de publicadas.
- Nunca afirmar que un test pasó sin la salida real de la ejecución.
