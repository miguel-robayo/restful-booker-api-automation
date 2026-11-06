---
name: nuevo-escenario
description: Use when creating, modifying or splitting a Cucumber scenario, feature file, step definition, Task, Question, API Object method or test data in this Restful-Booker framework, including when the request is phrased as "agrega un caso", "nuevo TC", "automatiza este endpoint" or "cubre este requisito".
---

# Nuevo escenario

## Principio

Un escenario nuevo **reutiliza** lo que existe y **respeta** la arquitectura y las convenciones aprobadas en `docs/`. Si una regla de este skill choca con `docs/`, manda `docs/`. Romper la letra de una regla es romper la regla, aunque "el espíritu" parezca cumplirse.

## Pasos (en orden, sin saltar ninguno)

1. **Trazar el requisito.** Identifica el RF/RC/RNF en `docs/analysis/requirements-analysis.md` y el TC en la matriz de `docs/strategy/test-strategy.md` §7. Decide el nivel por el **requisito**, no por lo que hace la API: REQ, o MON con `@kd-monitor`. Revisa `docs/known-defects.md`: un REQ que falla por un KD abierto lleva `@known-defect`. Si el TC no existe en la estrategia, **detente y pide autorización** para registrarlo; no inventes reglas de negocio.
2. **Buscar qué reutilizar** antes de crear nada: pasos en `src/test/java/.../steps`, Tasks y Questions en `screenplay/`, métodos de `BookingApi`/`AuthApi`, claves en `ScenarioMemory` y métodos de `BookingDataFactory`. Un paso nuevo cuyo texto es casi igual a uno existente es una señal de duplicación.
3. **Escribir la feature** con las convenciones C-1 a C-8 (ver la tabla de abajo).
4. **Escribir el código por capas**, de abajo hacia arriba: método en el API Object → Task (escritura) o Question (lectura) → paso. Los nombres siguen `naming.md`, en este mismo directorio. Léelo antes de crear una clase, un método o una clave.
5. **Datos:** siempre con `BookingDataFactory`, nunca JSON ni literales fijos. Las reservas se crean con `CreateBooking`, que las registra para la limpieza. Los tokens y credenciales inválidos se generan; nunca son literales.
6. **Verificar** con ejecución real y muestra la salida (nunca afirmes que pasó sin ella):
   ```bash
   ./mvnw test -Dtest=ApiScenariosSuite -Dcucumber.execution.dry-run=true
   ./mvnw test -Dtest='FeatureConventionsTest,ArchitectureRulesTest'
   ./mvnw test -Dtest=ApiScenariosSuite -Dcucumber.filter.tags="@TC-XXX-NN" -Denv=qa
   ./mvnw clean verify -Denv=qa
   ```
7. **Documentar**, con autorización porque son documentos aprobados: notas técnicas del TC en `test-strategy.md` §7, fila en `docs/delivery/scenarios.md` y conteos en `README.md` y en `docs/delivery/execution-evidence.md`.

## Referencia rápida de convenciones

| Regla | Qué exige | Se verifica en |
|---|---|---|
| C-1 Fases | `Dado` prepara (`wasAbleTo`, exige 2xx) · `Cuando` = **una** acción (`attemptsTo`/`asksFor`) y declara lecturas posteriores con `observations.after(...)` · `Entonces` = `Expect`/`ResponseSpecs` sobre evidencia guardada, **sin HTTP** | `ArchitectureRulesTest` |
| C-2 | Sin códigos HTTP en el Gherkin; en Java se usan las constantes de `HttpStatus` | `FeatureConventionsTest` |
| C-3 | Solo lenguaje de negocio: sin token, HTTP, JSON, schema, GPath, SLA, Builder, cabeceras | `FeatureConventionsTest` |
| C-4 | Actor único **Miguel**, nombrado solo en el primer `Dado` efectivo ("Dado que Miguel …") | `FeatureConventionsTest` |
| C-5 | Métodos `Entonces` → `verify…`; `Dado`/`Cuando` → verbo imperativo | `ArchitectureRulesTest` |
| C-6 | Un solo `Cuando` (un `Y` tras el `Cuando` es otra acción) · etiquetas `@TC-*` y `@esc-*` | `FeatureConventionsTest` |
| C-7 | `Antecedentes`: solo contexto común a **todos**, solo `Dado`/`Y`, máximo 4 pasos | `FeatureConventionsTest` |
| C-8 | `Regla` solo para ejemplos de una misma regla de negocio · `Esquema del escenario` solo si cambian únicamente los datos | Revisión |
| Vocabulario | "iniciar sesión/sesión" = token · "cancelar" = DELETE ("deja de estar disponible") · "valor de referencia" = `query.price.threshold` · "tiempo máximo acordado" = `sla.booking.get.ms` | Revisión |
| Mensajes de fallo | Prefijo `KD-xx:`, `SLA:`, `PRECONDITION:` o `INFRA:` según la categoría de Allure | Revisión |

## Errores comunes

| Error | Corrección |
|---|---|
| Leer la reserva en el `Entonces` para comprobar que no cambió | El `Cuando` lo declara con `observations.after(...)` y el `Entonces` usa `recall(clave)` |
| Dos acciones en el `Cuando` (o `Cuando` + `Y` con acción) | Separar en dos escenarios con la misma etiqueta `@TC-*`, o pasar la primera acción a un `Dado` |
| Ruta, método o filtro HTTP dentro de un step | Moverlo a una Task o Question; el step solo llama al actor |
| Assert dentro de una Task, Question o API Object | Las aserciones viven solo en `Expect`/`ResponseSpecs`, desde un `Entonces` |
| `RestAssured.baseURI = …` o un campo `static` mutable | Una spec nueva por petición (`SpecFactory`); el estado del escenario va en `ScenarioContext` |
| Pasar un escenario a MON porque la API no cumple | El nivel lo decide el requisito: queda REQ con `@known-defect` |
| Nombre genérico (`Helper`, `Utils`, `doAction`, `data1`) | Usar un nombre de negocio según `naming.md` |
| Usar `Esquema del escenario` para comportamientos distintos | Escenarios separados |

## Antes de dar el trabajo por terminado

- [ ] TC trazado a un requisito y nivel REQ/MON decidido por el requisito
- [ ] Nada duplicado: se reutilizó lo existente
- [ ] Nombres según `naming.md`
- [ ] Dry-run sin pasos indefinidos ni ambiguos
- [ ] `FeatureConventionsTest` y `ArchitectureRulesTest` en verde (salida mostrada)
- [ ] Escenario ejecutado contra la API (salida mostrada) y `clean verify` en verde
- [ ] Documentación actualizada con autorización
