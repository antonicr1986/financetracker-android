# Pendientes

Mejoras identificadas que quedan para más adelante. Al terminar una, se marca
con `[x]` y se anota la fecha o el commit.

## Repositorio y pipeline

- [ ] **SAST con CodeQL** (Settings → Code security → CodeQL, Default setup).
- [ ] Dependabot alerts + security updates, y secret scanning con push protection.
- [ ] Branch protection en `main` (PR obligatoria, checks en verde, sin force push).
- [ ] Fijar las actions por SHA y añadir `.github/dependabot.yml` con `github-actions`.
- [ ] Verificar el checksum de gitleaks al descargarlo.
- [ ] `SECURITY.md` con cómo reportar vulnerabilidades.
- [ ] `azure-pipelines.yml`: añadir el mismo análisis de dependencias que en GitHub Actions y `timeoutInMinutes`.
- [ ] Generar un SBOM de la release y adjuntarlo junto a los instalables.
- [ ] Atestación de procedencia de los instalables (`actions/attest-build-provenance`).

## Hecho

- [x] `permissions` globales de solo lectura y `timeout-minutes` en todos los jobs.
