# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versioning sémantique](https://semver.org/lang/fr/).

## [Unreleased]

*(aucun changement en attente)*

## [0.1.1] — 2026-10-14 — README : procédure de test

### Changed
- README : marche à suivre pas à pas pour tester l'écran de lancement sur desktop, Android (téléphone, APK ou émulateur) et navigateur, avec le résultat attendu et le changement de langue.

## [0.1.0] — 2026-10-14 — Écran de lancement et squelette multi-plateforme

### Added
- Écran de lancement : la mer de l'ancien jeu défile en boucle, le bateau joueur en vue 3/4 tangue au centre, le titre, l'accroche et la version apparaissent en fondu. Mise en page proportionnelle à l'écran, portrait comme paysage.
- Internationalisation : textes dans `assets/i18n/messages.properties` (anglais, par défaut) et `messages_fr.properties`, langue choisie d'après le système (desktop, Android) ou le navigateur (web), repli sur l'anglais.
- Projet Gradle (Kotlin DSL, Gradle 9.8.1, versions centralisées dans `gradle/libs.versions.toml`) avec les modules `:game` (cœur libGDX 1.14.0 partagé), `:platform:desktop` (LWJGL3), `:platform:android` et `:platform:web` (TeaVM via gdx-teavm 1.4.0).
- Desktop : options `--lang`, `--size` et `--screenshot` (capture PNG puis fermeture) pour tester sans interaction.
- Web : page d'accueil avec viewport mobile, langue de la page et libellé accessible du canvas traduits, CHANGELOG publié sur la route `/changelog`.
- `package.json` racine : version unique du projet (lue par Gradle, l'APK et l'écran de lancement) et scripts npm (`desktop`, `android`, `web`, `test`, `test:e2e`, `version:check`).
- Tests unitaires JUnit 5 (choix de langue, chargement des traductions, parité des clés fr/en, mise en page sur plusieurs tailles d'écran, animations, images du bateau, options du lanceur desktop).
- Tests d'interface Playwright sur la version web (Chromium desktop et mobile) : textes fr, en et repli, rendu du canvas plein écran, absence de défilement horizontal, route `/changelog`.
- Assets : texture de mer de l'ancien jeu réduite (`assets/sea/sea.jpg`, source dans `assets-src/sea/`), polices libres Pirata One et Nunito (licence OFL, fichiers de licence joints).
- `.gitignore` adapté à la stack Kotlin / Gradle / libGDX / Android / web, excluant notamment les poids ONNX (licence SAM) et les keystores.

- Script Blender `assets-src/blender/render_boat.py` : rendu des bateaux FBX de l'ancien jeu en sprites PNG transparents (vue de dessus ou 3/4, rotations multiples, éclairage cartoon).
- Tests du script de rendu (`assets-src/blender/test_render_boat.py`, unittest) : cas nominal, plusieurs angles, coque seule, erreurs d'arguments.
- Sources du bateau joueur copiées depuis l'ancien jeu Unity dans `assets-src/boats/player/`.
- Sprites du bateau joueur en vue 3/4, 8 rotations de 45°, dans `assets/boats/player/`.

### Changed
- README : structure du projet, commandes de lancement et de test par plateforme.
- README : pipeline Blender vers sprites et commandes de rendu et de test documentées.
- README : droits de réutilisation des assets de l'ancien jeu Unity confirmés.
- README : iOS ajouté au périmètre, backend web en TeaVM (gdx-teavm) au lieu de Kotlin/JS ou Wasm, partage de code par interfaces libGDX au lieu d'expect/actual KMP, arborescence des modules et règles de dépendance documentées.

### Prompt log
- `2026-10-13 | Proposition d'une architecture à partir du README et initialisation du .gitignore | .gitignore, CHANGELOG.md`
  - En tant que développeur du projet, je veux une architecture de modules adaptée au README et un .gitignore initialisé, afin de démarrer le code sur une base saine.
- `2026-10-13 | Récapitulatif des changements à commiter | CHANGELOG.md`
- `2026-10-13 | Validation d'iOS dans le périmètre et de TeaVM pour le web | README.md, CHANGELOG.md`
  - En tant que joueur sur iPhone ou sur navigateur, je veux pouvoir jouer à Hammer and Planks, afin de ne pas être limité à Android.
- `2026-10-13 | Recherche dans l'ancien projet Unity des assets pour un écran de lancement (océan et bateau) | CHANGELOG.md`
  - En tant que joueur, je veux voir au lancement un bateau sur l'océan, afin d'entrer tout de suite dans l'univers du jeu.
- `2026-10-13 | Droits des assets de l'ancien jeu confirmés, question sur l'usage de Blender par Claude | README.md, CHANGELOG.md`
  - En tant que développeur, je veux transformer les modèles 3D de l'ancien jeu en sprites de façon scriptée, afin de réutiliser les assets sans travail manuel.
- `2026-10-13 | Blender installé : rendu du bateau joueur en sprite via script | assets-src/blender/render_boat.py, assets-src/blender/test_render_boat.py, assets-src/boats/player/*, README.md, CHANGELOG.md`
  - En tant que développeur, je veux générer les sprites des bateaux depuis les modèles 3D de l'ancien jeu par une commande, afin de pouvoir les régénérer à chaque changement de rendu.
- `2026-10-14 | Reprise de session, choix de la vue 3/4 pour les sprites du bateau | assets/boats/player/*, README.md, CHANGELOG.md`
  - En tant que joueur, je veux voir le bateau en vue 3/4 avec sa voile, afin de percevoir son volume dès l'écran de lancement.
- `2026-10-14 | Création du package.json et de l'écran de lancement, avec la marche à suivre pour le tester | package.json, settings.gradle.kts, build.gradle.kts, gradle/*, gradlew*, game/**, platform/**, assets/i18n/*, assets/sea/*, assets/fonts/*, assets-src/sea/*, e2e/*, playwright.config.ts, scripts/check-version.mjs, README.md, CHANGELOG.md`
  - En tant que joueur, je veux voir au lancement du jeu mon bateau naviguer sur l'océan avec le titre dans ma langue, sur téléphone, sur ordinateur et dans le navigateur, afin d'entrer tout de suite dans l'univers du jeu.
- `2026-10-14 | Ajout des informations de lancement et de test dans le README | README.md, CHANGELOG.md, package.json`
  - En tant que membre de l'équipe, je veux trouver dans le README comment lancer et tester le jeu sur chaque plateforme, afin de vérifier l'écran de lancement sans aide.
