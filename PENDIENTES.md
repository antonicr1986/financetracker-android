# Pendientes

Mejoras identificadas que quedan para más adelante. Al terminar una, se marca
con `[x]` y se anota la fecha o el commit.

## Repositorio y pipeline

- [x] **SAST con CodeQL** (Settings → Code security → CodeQL, Default setup).
- [x] Dependabot alerts + security updates, y secret scanning con push protection.
- [x] Ruleset `proteger-main`: sin borrado ni force push en `main` (sin PR obligatoria: se trabaja con push directo).
- [ ] Fijar las actions por SHA y añadir `.github/dependabot.yml` con `github-actions`.
- [ ] Verificar el checksum de gitleaks al descargarlo.
- [ ] `SECURITY.md` con cómo reportar vulnerabilidades.
- [ ] `azure-pipelines.yml`: añadir el mismo análisis de dependencias que en GitHub Actions y `timeoutInMinutes`.
- [x] **MobSF bloquea**: falla con hallazgos altos que no estén en `.mobsf/accepted.txt` (firma en el
  CI y `minSdk 24`, aceptados con su motivo).
- [ ] Fijar la versión de la imagen de MobSF (ahora `latest`).
- [ ] Generar un SBOM de la release y adjuntarlo junto a los instalables.
- [ ] Atestación de procedencia de los instalables (`actions/attest-build-provenance`).

## Hecho

- [x] Job `mobsf`: análisis de seguridad del APK release con MobSF en cada push y PR (de momento solo informa).
- [x] `permissions` globales de solo lectura y `timeout-minutes` en todos los jobs.

## Cobertura de tests

- [x] **Medir la cobertura en el CI** con Kover (plugin de JetBrains para Kotlin): informe
  de los tests unitarios en el resumen del job y un % mínimo, igual que en la API (excluyendo
  clases generadas como `databinding` y `BuildConfig`). Fijar el mínimo con la cobertura
  medida menos 5 puntos. Hecho (nov 2026) con JaCoCo, el que trae el plugin de Android (Kover no se integra bien con el Kotlin de AGP 9): 12,6 % de líneas; con los tests de las pantallas de acceso, 37,3 % y mínimo 30 %.
- [x] **Tests de las pantallas de acceso** (Robolectric con una API falsa): login, registro y
  recuperar contraseña (72-88 % de líneas cada una). Hecho (nov 2026).
- [ ] **Tests del resto de pantallas**: panel (`MainActivity`), movimiento, presupuesto y
  Seguridad (`MfaActivity`), todas al 0 %; después, subir el mínimo.
