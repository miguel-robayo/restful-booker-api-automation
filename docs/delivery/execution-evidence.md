# Evidencias de ejecución

**Ejecución real · 2026-11-05 (UTC-5)** · Rama `fix/phase-1-false-positives` más los cambios de esta entrega (sin commit) · API pública `https://restful-booker.herokuapp.com` · ambiente `qa`.

## 1. Entorno

| Elemento | Valor |
|---|---|
| SO | Windows 10 Home 10.0.19045 |
| JDK | Eclipse Temurin **21.0.13** (`JAVA_HOME` apuntando a `jdk-21.0.13.9-hotspot`) |
| Maven | 3.9.14 (`mvn` instalado y Maven Wrapper, la misma versión) |
| Credenciales | `RB_USERNAME` / `RB_PASSWORD` como variables de entorno del usuario; no se escriben en archivos ni en la consola |
| Enforcer | `RequireJavaVersion`, `RequireMavenVersion` y `DependencyConvergence`: **passed** |

> Con el JDK 17 del `PATH` el build se detiene en el Enforcer, como está previsto. Por eso todas las ejecuciones usan el JDK 21.

## 2. Comandos solicitados

| # | Comando | Resultado | Duración |
|---|---|---|---|
| 1 | `mvn clean test -Denv=qa` | ✅ `Tests run: 104, Failures: 0, Errors: 0, Skipped: 0` · **BUILD SUCCESS** | 25.7 s |
| 2 | `mvn test -Dtest=BookingCreationTests -Denv=qa` | ✅ `Running …runners.BookingCreationTests` · `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` · **BUILD SUCCESS** | 13.4 s |
| 3 | `mvn allure:serve` | ✅ `Local preview server started at <http://127.0.0.1:64494>` | — |
| — | `mvn allure:report` | ✅ `Report successfully generated to …\target\site\allure-maven-plugin` | — |
| — | `./mvnw clean verify -Denv=qa` (comando del CI) | ✅ `Tests run: 104, Failures: 0, Errors: 0, Skipped: 0` · **BUILD SUCCESS** | 30.7 s |
| — | `mvn clean test -Pknown-defects -Denv=qa` | ✅ BUILD SUCCESS · `Tests run: 11, Skipped: 11`: no hay escenarios `@known-defect` implementados | 17.0 s |

Desglose del comando 1: **93 tests técnicos** de JUnit 5 y **11 escenarios** de Cucumber (`ApiScenariosSuite`).

| Clase técnica | Tests | | Clase técnica | Tests |
|---|---|---|---|---|
| FW-01 Sensitive data masking | 7 | | Screenplay actor | 16 |
| FW-02 Configuration resolution | 10 | | Model serialization | 13 |
| Architecture rules | 9 | | Booking data factory | 8 |
| Feature conventions | 6 | | Spec factory | 4 |
| HTTP Questions: not found vs. unexpected | 9 | | Tasks as preconditions | 5 |
| Then steps on incomplete evidence | 6 | | **Total** | **93** |

## 3. Resultado por escenario (Allure, ejecución final)

Reporte: **11 total · 11 passed · 0 failed · 0 broken · 0 skipped**. Sin categorías de defecto. Panel Environment: `cleanup.pending.count=0`, es decir, todas las reservas creadas se borraron.

| Esc. | Caso | Escenario | Estado | Duración |
|---|---|---|---|---|
| 1 | TC-AUTH-01 | Iniciar sesión con credenciales válidas | ✅ passed | 0.24 s |
| 1 | TC-AUTH-02 | Modificar una reserva después de iniciar sesión | ✅ passed | 1.83 s |
| 1 | TC-AUTH-02 | Cancelar una reserva modificada sin volver a iniciar sesión | ✅ passed | 1.81 s |
| 2 | TC-CRE-01 | Realizar una reserva de alojamiento | ✅ passed | 1.36 s |
| 3 | TC-QRY-01 | Identificar sus reservas de valor superior al de referencia | ✅ passed | 4.87 s |
| 4 | TC-CON-01 | Recibir la información completa de una reserva | ✅ passed | 1.75 s |
| 4 | TC-CON-02 | Consultar una reserva en un tiempo aceptable | ✅ passed | 1.26 s |
| 4 | TC-CON-03 | Recibir la información en el formato acordado | ✅ passed | 1.55 s |
| 5 | TC-NEG-01 | Intentar modificar una reserva sin haber iniciado sesión | ✅ passed | 1.81 s |
| 5 | TC-NEG-02 | Intentar modificar una reserva con una sesión no válida | ✅ passed | 2.08 s |
| 5 | TC-NEG-03 | Consultar una reserva inexistente | ✅ passed | 1.00 s |

Las duraciones son del escenario completo, con su preparación y su limpieza; se ejecutan en paralelo con 4 hilos. El SLA de TC-CON-02 se mide aparte, sobre un único `GET /booking/{id}`.

## 4. Seguridad de las evidencias

| Revisión | Resultado |
|---|---|
| Valor de la contraseña en los logs de Maven (5 ejecuciones) | 0 apariciones |
| Valor de la contraseña en `target/allure-results`, `target/site`, `target/surefire-reports` | 0 archivos |
| Tokens en los adjuntos de Allure | 12 apariciones, todas como `"token":"***"` |
| Credenciales en el repositorio | Ninguna: solo `${RB_USERNAME}` / `${RB_PASSWORD}` |

## 5. Reporte de errores (fallo forzado)

Comprueba que un fallo real deja la ejecución en rojo y llega clasificado al reporte:

```text
mvn clean test -Denv=qa -Dtest=ApiScenariosSuite "-Dcucumber.filter.tags=@TC-CON-02" -Dsla.booking.get.ms=1

java.lang.AssertionError: SLA: GET /booking/{id} took 241 ms; required < 1 ms
[ERROR] Tests run: 11, Failures: 1, Errors: 0, Skipped: 10
[INFO] BUILD FAILURE                                   → exit code 1
```

| Señal | Resultado |
|---|---|
| Exit code de Maven | `1`: en CI, el job `main` queda en rojo |
| Resumen (`test-summary.sh`) | `❌ failed · TC-CON-02 esc-4 · Consultar una reserva en un tiempo aceptable · SLA: GET /booking/{id} took 241 ms; required < 1 ms` |
| Categoría Allure | `Performance (SLA)`: 1 failed |

Después del fallo forzado se repitió la ejecución completa (`./mvnw clean verify -Denv=qa`, 104/104) para que `target/` contenga la evidencia real.

## 6. Dónde están las evidencias

| Evidencia | Ruta local (no versionada, `target/` está en `.gitignore`) | En CI |
|---|---|---|
| Resultados Allure | `target/allure-results/` | artifact `evidence-main-*` |
| Reporte HTML | `target/site/allure-maven-plugin/index.html` (`mvn allure:serve` para verlo) | artifact `evidence-main-*` |
| Reportes JUnit | `target/surefire-reports/` | artifact `evidence-main-*` |
| Limpieza de datos | `target/cleanup/allure-results/cleanup-pending.json` (`[ ]`) | artifact `evidence-main-*` |
| Resumen por escenario | `bash .github/scripts/test-summary.sh "Main job" target/surefire-reports target/allure-results` | Página del run (*Summary*) |

## 7. Pendiente

- **Primera ejecución en GitHub Actions:** depende de un push que esta entrega no incluye. Al hacerlo, conviene añadir aquí el enlace al run y el nombre de sus artifacts.
- Los resultados dependen de una API pública compartida: un *cold start* de Heroku puede hacer fallar TC-CON-02 (Performance (SLA)) en una ejecución aislada.
