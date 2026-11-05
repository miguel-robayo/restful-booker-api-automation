# Restful-Booker API Automation

Framework de automatización de la API REST pública [Restful-Booker](https://restful-booker.herokuapp.com/apidoc/index.html) con **Java 21, Rest Assured, Cucumber (Gherkin en español), Screenplay y Allure**.

> Estado: fase 0 (base del proyecto). Los escenarios se incorporan en las fases siguientes.

## Requisitos previos

- **JDK 21** (por ejemplo, Eclipse Temurin 21) con `JAVA_HOME` apuntando a él. El build falla con cualquier otra versión.
- Maven **no** es necesario: el proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`, Maven 3.9.14).
- Variables de entorno con las credenciales de la API:

  ```powershell
  $env:RB_USERNAME = "<usuario>"
  $env:RB_PASSWORD = "<contraseña>"
  ```

  ```bash
  export RB_USERNAME=<usuario> RB_PASSWORD=<contraseña>
  ```

## Ejecución

| Objetivo | Comando |
|---|---|
| Job principal (tests técnicos + escenarios, sin `@known-defect`) | `./mvnw clean verify` |
| Elegir ambiente (`dev`, `qa`; por defecto `qa`) | `./mvnw clean test -Denv=qa` |
| Job de defectos conocidos (solo `@known-defect`, resultados aparte) | `./mvnw test -Pknown-defects` |
| Reporte Allure del job principal | `./mvnw allure:serve` |
| Reporte Allure del job de defectos conocidos | `./mvnw allure:serve -Pknown-defects` |

En Windows, usa `mvnw.cmd` en lugar de `./mvnw`.

## Configuración

Precedencia (de mayor a menor): `-Dclave` > variable `RB_CLAVE` > `config/<env>.properties` > `config/default.properties`. Las credenciales nunca se guardan en el repositorio.

## Documentación

| Documento | Contenido |
|---|---|
| [Análisis de requisitos](docs/analysis/requirements-analysis.md) | Requisitos, hallazgos de la API y trazabilidad |
| [Estrategia de pruebas](docs/strategy/test-strategy.md) | 24 escenarios, niveles REQ/MON, categorías Allure, decisiones |
| [Arquitectura](docs/architecture/framework-architecture.md) | Capas, regla Dado/Cuando/Entonces, ADR |
| [Defectos conocidos](docs/known-defects.md) | Divergencias de la API y su tratamiento |

## Notas

- En Windows, si el proyecto está en una unidad distinta a la del directorio temporal, Surefire deja un archivo `*.dumpstream` con el aviso *"'other' has different root"*. Es inofensivo: Surefire usa el classpath completo y los tests se ejecutan con normalidad.
