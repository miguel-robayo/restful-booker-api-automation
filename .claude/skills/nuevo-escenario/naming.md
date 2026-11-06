# Guía de nombres

Sacada del código existente: un nombre nuevo debe parecerse a sus vecinos. El código y los identificadores van en inglés; el Gherkin y la documentación, en español. Un nombre debe decir **qué significa para el negocio**, no cómo está implementado.

## Gherkin (español, lenguaje de negocio)

| Elemento | Patrón | Ejemplos reales |
|---|---|---|
| `Característica` | Capacidad de negocio + "Como… quiero… para…" | "Creación de reservas de alojamiento" |
| `Regla` | La regla de negocio como afirmación | "Sin una sesión válida no se puede modificar una reserva y esta conserva su información" |
| `Escenario` | Infinitivo con la intención de Miguel; "Intentar…" en los negativos | "Realizar una reserva de alojamiento" · "Intentar modificar una reserva sin haber iniciado sesión" |
| `Dado` | Estado o contexto de Miguel | "que Miguel tiene una reserva registrada" · "conoce el estado actual de su reserva" |
| `Cuando` | Una acción en presente, en tercera persona | "registra la reserva" · "intenta modificar su reserva" |
| `Entonces` | Resultado observable para el negocio | "la reserva queda confirmada" · "la reserva conserva su información original" |
| Etiquetas | `@TC-<ÁREA>-NN @esc-N` + nivel (`@req`, `@kd-monitor`, `@known-defect`) + tipo (`@negative`, `@security`, `@contract`, `@performance`) | `@TC-NEG-01 @esc-5 @requested @req @negative @security` |
| Archivo | `snake_case.feature` por área de negocio | `booking_creation.feature`, `booking_queries.feature` |

## Java

| Elemento | Patrón | Ejemplos reales |
|---|---|---|
| Clase de pasos | `<Área>Steps` | `BookingSteps`, `BookingSearchSteps`, `AuthenticationSteps`, `BookingQualitySteps` |
| Método `Dado` | Verbo imperativo + objeto de negocio | `registerOwnBooking`, `prepareBookingRequest`, `startSession`, `chooseNonexistentBookingNumber` |
| Método `Cuando` | Verbo imperativo de la acción; `attempt…` en negativos | `registerBooking`, `cancelBooking`, `attemptBookingUpdate`, `retrieveBookingDetails` |
| Método `Entonces` | `verify` + resultado esperado | `verifyBookingConfirmed`, `verifyUpdateRejectedAsUnauthorized`, `verifyBookingKeptOriginalInformation` |
| Task (escritura) | `VerboSustantivo` + fábrica estática que se lee como frase | `CreateBooking.with(booking)`, `UpdateBooking.of(id, booking)`, `DeleteBooking.withId(id)`, `Authenticate.withConfiguredCredentials()` |
| Question (lectura) | Sustantivo de **lo que responde** + fábrica | `TheBooking.withId(id)`, `TheBooking.responseFor(id)`, `BookingIds.withFirstName(name)`, `HighestBookingId.current()`, `TheSentRequests.signIns()` |
| Question solo de memoria | Prefijo `The…` sobre lo ya ocurrido | `TheLastResponse.bodyAs(type)`, `TheSentRequests.sessionUsedToModify(id)` |
| Ability | Frase verbal de lo que **puede** hacer el actor + `as(actor)` | `CallTheApi`, `UseAnAuthToken.withToken(t)`, `TrackCreatedBookings.forThisScenario()` |
| API Object | `<Recurso>Api`; métodos con verbo CRUD, sin aserciones | `BookingApi.create/getById/findIds/update/delete` |
| Clave de `ScenarioMemory` | `UPPER_SNAKE` con valor descriptivo en inglés | `OWN_BOOKING_ID = "own booking id"`, `BOOKING_STATE_AFTER_ACTION`, `NONEXISTENT_BOOKING_RESPONSE` |
| Datos de prueba | `BookingDataFactory.a…` | `aValidBooking()`, `aBookingPricedAt(p)`, `aBookingWithFirstNameAndPrice(n, p)` |
| Constantes | Nombre de negocio, nunca el valor | `HttpStatus.FORBIDDEN`, `AGREED_CONTENT_TYPE`, `BOOKINGS_AROUND_REFERENCE_VALUE` |
| Test técnico | `<Tema>Test` + `@DisplayName` que se lee como una regla | `ArchitectureRulesTest`: "only API Objects start HTTP requests" |

## Nombres que no se usan

`Helper`, `Utils`, `Manager`, `Common` · `doAction`, `process`, `handle`, `test1` · `data`, `resp`, `obj`, `tmp` · abreviaturas (`bkg`, `usr`) · números mágicos (`403`, `2000`) en lugar de constantes con nombre.
