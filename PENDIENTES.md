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
- [ ] **MobSF**: revisar el primer informe (`mobsf-report`), corregir lo que proceda y poner
  `MOBSF_FAIL_ON_HIGH: 'true'` para que un hallazgo alto bloquee. Fijar la versión de la imagen.
- [ ] Generar un SBOM de la release y adjuntarlo junto a los instalables.
- [ ] Atestación de procedencia de los instalables (`actions/attest-build-provenance`).

## Hecho

- [x] Job `mobsf`: análisis de seguridad del APK release con MobSF en cada push y PR (de momento solo informa).
- [x] `permissions` globales de solo lectura y `timeout-minutes` en todos los jobs.
