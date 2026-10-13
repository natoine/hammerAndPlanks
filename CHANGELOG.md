# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versioning sémantique](https://semver.org/lang/fr/).

## [Unreleased]

### Added
- `.gitignore` adapté à la stack Kotlin / Gradle / libGDX / Android / web, excluant notamment les poids ONNX (licence SAM) et les keystores.

### Changed
- README : iOS ajouté au périmètre, backend web en TeaVM (gdx-teavm) au lieu de Kotlin/JS ou Wasm, partage de code par interfaces libGDX au lieu d'expect/actual KMP, arborescence des modules et règles de dépendance documentées.

### Prompt log
- `2026-10-13 | Proposition d'une architecture à partir du README et initialisation du .gitignore | .gitignore, CHANGELOG.md`
  - En tant que développeur du projet, je veux une architecture de modules adaptée au README et un .gitignore initialisé, afin de démarrer le code sur une base saine.
- `2026-10-13 | Récapitulatif des changements à commiter | CHANGELOG.md`
- `2026-10-13 | Validation d'iOS dans le périmètre et de TeaVM pour le web | README.md, CHANGELOG.md`
  - En tant que joueur sur iPhone ou sur navigateur, je veux pouvoir jouer à Hammer and Planks, afin de ne pas être limité à Android.
