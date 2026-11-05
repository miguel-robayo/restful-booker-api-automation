# Defectos Conocidos — Restful-Booker API

**Versión 2.1 · 2026-11-05** · Registro versionado de divergencias (D-18). Requisitos: [`requirements-analysis.md`](analysis/requirements-analysis.md) · Escenarios: [`test-strategy.md`](strategy/test-strategy.md)

**Reglas:**
- Se registra lo que se aparta del documento de la prueba, del contrato de [API-DOC] o, donde nada está definido, de la semántica HTTP estándar. Lo observado **nunca** se convierte en requisito.
- **REQ:** si un requisito o contrato define el comportamiento, el escenario lo afirma y **falla mientras el KD siga abierto**.
- **MON:** solo para comportamientos que nada define (por ejemplo, códigos de error). Nunca cuenta como cobertura.
- Ambos llevan `@Issue("KD-xx")` y el prefijo `KD-xx:`, así que sus fallos aparecen en la categoría **Known Defect** de Allure, que es la señal para revisar esta tabla.
- Los IDs no se reutilizan.

| ID | Comportamiento observado | Referencia esperada | Impacto | Tratamiento | Estado |
|---|---|---|---|---|---|
| KD-01 | `POST /auth` con credenciales inválidas → **200** `{"reason":"Bad credentials"}` | 401 (HTTP; no hay contrato de error) | Un test que solo valide el status daría un falso positivo | **MON** TC-AUTH-03 · los REQ de auth validan siempre la presencia del `token` | Abierto |
| KD-03 | `PUT` o `DELETE` sobre un ID inexistente → **405** | 404 (HTTP) | Código engañoso para el cliente | **REQ** TC-NEG-06 (no 2xx y no se crea; pasa) + **MON** TC-NEG-06 (405) | Abierto |
| KD-04 | `POST /booking` con `{}` → **500** | 400 (HTTP); el rechazo exigido por RC-01 sí se cumple | Error de servidor ante una entrada del cliente | **REQ** TC-NEG-07 (no 2xx; pasa) + **MON** TC-NEG-07 (500) | Abierto |
| KD-05 | `totalprice` string → **200**; se guarda `null` y la respuesta incumple RC-01 | Rechazo (RC-01: `Number`; D-20) | Datos corruptos persistidos | **REQ** TC-NEG-08 (no 2xx) → **falla mientras esté abierto** | Abierto |
| KD-07 | Sin `additionalneeds` → **200** y la respuesta omite el campo obligatorio | Rechazo (RC-01: obligatorio; D-20) | Contrato documentado incumplido | **REQ** TC-CRE-02 (no 2xx) → **falla mientras esté abierto** | Abierto |
| KD-06 | Sin headers de seguridad; `x-powered-by: Express` expuesto | Headers recomendados por OWASP (RNF-06) | Divulgación de información | **Exclusión justificada:** hallazgo sin test (D-06). RNF-06 pide headers "de seguridad **o** servidor" y la parte de servidor se cumple (TC-CON-03); ningún requisito exige headers de seguridad concretos | Hallazgo |
| KD-08 | `Accept: application/xml` → `text/html` | XML (RC-06) | Contrato de XML incumplido | **Exclusión justificada:** XML fuera de alcance | Hallazgo |

**Retirado:** KD-02 (`DELETE` → 201). Se registró por error: está documentado en [API-DOC] (RC-04) y es el resultado esperado.
**No son defectos:** `GET /booking` sin `totalprice` (coincide con RC-03; es la contradicción C-01 del documento de la prueba) y `checkout` anterior a `checkin` aceptado (ninguna fuente define esa regla).
