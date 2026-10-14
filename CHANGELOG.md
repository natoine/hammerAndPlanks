# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versioning sémantique](https://semver.org/lang/fr/).

## [Unreleased]

*(aucun changement en attente)*

## [0.6.0] — 2026-10-14 — Contrôle du bateau repris de l'ancien jeu

### Added
- Trois modes de contrôle du bateau repris de l'ancien jeu (`MovementControlMode`) : position (l'axe désigne un point du rail ; relâcher ramène au centre ; mode par défaut, comme l'ancien jeu), vitesse (relâcher arrête le bateau sur place), force (relâcher laisse le bateau sur sa lancée).
- `SmoothDamp` : portage exact de `Mathf.SmoothDamp` de Unity, lissage de 0,6 s de l'ancien jeu.
- Vitesse maximale de l'ancien jeu (6 unités/s sur un rail de 32, soit 0,375 demi-rail/s) et facteur de vitesse du joueur dans [0, 1] (`ControlSettings`), lus dans les préférences.
- `GameInput` (module `:input`) : entrée commune à tous les modes d'interaction ; le clavier le produit, le bateau le consomme.
- Options de lancement : desktop `--control=position|velocity|force` et `--speed=0..1` (`-Pcontrol`, `-Pspeed` avec Gradle) ; web `?control=` et `?speed=` dans l'URL.
- Tests unitaires (trois modes, butée, facteur de vitesse, changement de mode, lissage indépendant de la fréquence d'images, réglages) et tests Playwright (retour au centre en mode position, arrêt sur place en mode vitesse, lancée en mode force, facteur de vitesse nul).

### Changed
- Déplacement du bateau : le mode position remplace le contrôle en vitesse de la 0.5.0 par défaut. Le bateau va moins vite, à la vitesse de l'ancien jeu. L'inclinaison dans le virage est conservée.
- Modes vitesse et force : l'intention de l'ancien jeu est reprise, pas sa formule. Dans l'ancien code, la cible n'avançait que d'un pas d'image avant le lissage : le bateau parcourait moins d'une unité en 5 s sur un demi-rail de 16 à 60 images/s, et moins encore à fréquence plus élevée.
- README : modes de contrôle, options de lancement, procédure de test.

## [0.5.0] — 2026-10-14 — Déplacement du bateau au clavier

### Added
- Mode clavier : les flèches gauche et droite déplacent le bateau sur l'écran de lancement (desktop, Android avec clavier physique, web). Le bateau accélère, s'incline dans le sens du mouvement, glisse un peu quand on relâche et s'arrête au bord de l'écran. Les deux flèches ensemble s'annulent. Dans les autres modes, les flèches n'ont pas d'effet.
- `KeyboardSteering` (module `:input`) : axe horizontal à partir des flèches.
- `BoatMotion` : position et vitesse en fractions de la largeur d'écran (justes après redimensionnement), accélération, freinage, inclinaison, butée, pas de temps borné après une pause.
- `LaunchLayout.boatLimit` : débattement maximal pour que la coque reste dans l'écran.
- Web : les flèches ne font pas défiler la page ; position du bateau exposée sur le canvas (`data-boat-offset`) pour les tests d'interface.
- Tests unitaires (axe clavier, mouvement, butée, limites d'écran) et tests Playwright (déplacement à gauche et à droite, arrêt, butée, page fixe, flèches inactives hors mode clavier).

### Changed
- README : déplacement au clavier, procédure de test par plateforme.

## [0.4.0] — 2026-10-14 — Manette barrée et imagette de la caméra

### Added
- Manette barrée : en mode manette sans manette branchée, le badge affiche une manette barrée et « Manette non détectée » (annonce web comprise). Présence vérifiée une fois par seconde (`PeriodicCheck`), donc mise à jour au branchement ou au débranchement.
- Détection des manettes par plateforme derrière l'interface `GamepadDetector` : desktop via gdx-controllers 2.2.4 (SDL), Android via `InputDevice` (sources manette ou joystick, hors périphériques virtuels), web via l'API Gamepad.
- Imagette du retour caméra en bas à droite, en mode caméra uniquement : cadre, effet miroir, recadrage sans déformation, format portrait ou paysage selon l'écran, états « démarrage », « accès refusé » et « aucune caméra ». La caméra s'éteint dès qu'on quitte le mode caméra ou que l'application passe en arrière-plan.
- Caméra par plateforme derrière l'interface `CameraThumbnail` : desktop via webcam-capture 0.3.12 (image dessinée par le jeu), Android via CameraX 1.6.2 (PreviewView posée sur la vue du jeu, autorisation demandée au passage en mode caméra), web via getUserMedia (élément vidéo posé sur le canvas, créé seulement au démarrage de la caméra).
- Pictos `input_gamepad_off.png` (dérivé du picto manette pour garder le même dessin) et `camera_off.png`, générés par `assets-src/ui/render_icons.py`.
- Desktop : option `--input-mode` (`-Pmode=motion` avec Gradle) pour imposer le mode d'interaction au lancement.
- Traductions fr et en de la manette absente et des états de la caméra.
- Tests unitaires : mise en page et recadrage de l'imagette, vérification périodique, picto et libellé selon la présence d'une manette, option desktop, sources de manette Android ; tests du script de pictos (picto barré). Tests Playwright avec caméra et manette simulées : manette absente ou branchée, caméra éteinte hors mode caméra, imagette en bas à droite avec flux, flux coupé en sortant du mode, accès refusé, navigateur sans caméra.

### Changed
- Android : la vue du jeu est placée dans un FrameLayout pour superposer l'aperçu caméra ; manifeste avec l'autorisation caméra et les fonctionnalités caméra, tactile et manette déclarées facultatives.
- README : marche à suivre pour tester la caméra et la manette sur chaque plateforme, architecture des interfaces de plateforme.

## [0.3.0] — 2026-10-14 — Mode d'interaction : badge et bascule par Tab

### Added
- Module Kotlin pur `:input` : `InputMode` (clavier, manette, caméra) et `InputModeSelector` (bascule cyclique, clavier par défaut, lecture tolérante d'une valeur sauvegardée).
- Badge du mode d'interaction en bas à gauche de l'écran de lancement : picto, nom du mode traduit, indication pour changer (« Tab » au clavier, « Touchez » sur écran tactile). Tab ou un appui sur le badge passe au mode suivant (desktop, Android, web).
- Sauvegarde du mode choisi entre deux lancements (Preferences libGDX ; localStorage sur le web).
- Web : annonce du mode aux lecteurs d'écran (zone `aria-live` `#input-mode`).
- Pictos clavier, manette et caméra (`assets/ui/input_*.png`) rendus depuis Material Icons (Apache 2.0) par `assets-src/ui/render_icons.py`, avec ses tests.
- Traductions fr et en des modes, des indications et de l'annonce.
- Tests unitaires du sélecteur de mode, de la mise en page du badge et des traductions ; tests Playwright de la bascule par Tab, de l'appui sur le badge, de la sauvegarde et de l'annonce en français et en anglais.

### Fixed
- Mer animée : la taille des vagues se calcule sur le petit côté de l'écran, et non plus sur sa largeur, pour éviter des vagues démesurées en paysage.

### Changed
- README : modes d'interaction implémentés, pictos de l'interface, structure du projet.

## [0.2.0] — 2026-10-14 — Mer animée : vagues et écume

### Added
- Mer animée portée du shader Unity de l'ancien jeu (« Parallax Specular Proc ») : turbulence de bruit 3D, ondulation de la texture par parallaxe, lignes d'écume sur les crêtes, vagues qui évoluent sur place et défilent avec la texture. Shaders GLSL ES 1.0 dans `assets/shaders/sea.vert` et `sea.frag`, compatibles Android, iOS, desktop et WebGL 1.
- Bruit simplex 3D calculé dans le shader (webgl-noise d'Ashima Arts, licence MIT), sans les textures de permutation de l'ancien jeu.
- `SeaWaves` : réglages repris de l'ancien matériau (couleur et bande d'écume, lacunarité, gain, vitesse de phase), 4 octaves au lieu de 5 pour ménager les téléphones.
- `SeaRenderer` : rendu de la mer en un seul quad plein écran, avec repli sur la texture qui défile si le shader ne compile pas sur l'appareil.
- Tests unitaires de la mer (phase, défilement du bruit, parallaxe, formule d'écume, réglages invalides) et de cohérence entre le shader et le code Kotlin (uniforms, borne d'octaves, GLSL ES 1.0).
- Test Playwright : le shader de mer compile dans le navigateur et l'image change d'un instant à l'autre.

### Changed
- Écran de lancement : la mer animée remplace la texture simple.
- README : section assets complétée par le rendu de la mer.

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
- `2026-10-14 | La mer de l'ancien jeu avait un effet de vagues par shader : analyse de l'ancien shader Unity | (aucun fichier modifié)`
  - En tant que joueur, je veux retrouver la mer animée de l'ancien jeu, afin de reconnaître son univers visuel.
- `2026-10-14 | Implémentation de la mer animée (portage du shader de vagues) | assets/shaders/sea.vert, assets/shaders/sea.frag, game/src/main/kotlin/.../sea/SeaWaves.kt, game/src/main/kotlin/.../sea/SeaRenderer.kt, game/src/main/kotlin/.../launch/LaunchScreen.kt, game/src/test/kotlin/.../sea/*, e2e/launch-screen.spec.ts, README.md, CHANGELOG.md, package.json`
  - En tant que joueur, je veux voir des vagues et de l'écume bouger sur la mer dès l'écran de lancement, sur téléphone, ordinateur et navigateur, afin de retrouver l'ambiance de l'ancien jeu.
- `2026-10-14 | Picto du mode d'interaction en bas à gauche, bascule par Tab entre clavier (flèches), manette et caméra | input/**, settings.gradle.kts, game/build.gradle.kts, game/src/main/kotlin/.../HammerAndPlanksGame.kt, game/src/main/kotlin/.../PlatformHooks.kt, game/src/main/kotlin/.../ui/*, game/src/main/kotlin/.../launch/LaunchScreen.kt, game/src/main/kotlin/.../sea/*, assets/shaders/sea.frag, assets/i18n/*, assets/ui/*, assets-src/ui/*, platform/web/**, e2e/input-mode.spec.ts, game/src/test/kotlin/.../ui/*, README.md, CHANGELOG.md, package.json`
  - En tant que joueur, je veux voir en bas à gauche de l'écran quel mode d'interaction j'utilise (clavier, manette ou caméra) et en changer avec la touche Tab, ou en touchant le picto sur téléphone, afin de jouer avec le périphérique de mon choix.
- `2026-10-14 | Manette barrée si aucune manette branchée ; imagette du retour caméra en bas à droite en mode caméra | game/src/main/kotlin/.../platform/*, game/src/main/kotlin/.../ui/*, game/src/main/kotlin/.../util/PeriodicCheck.kt, game/src/main/kotlin/.../launch/LaunchScreen.kt, game/src/main/kotlin/.../HammerAndPlanksGame.kt, game/src/main/kotlin/.../PlatformHooks.kt, platform/desktop/**, platform/android/**, platform/web/**, assets/ui/*, assets/i18n/*, assets-src/ui/*, gradle/libs.versions.toml, playwright.config.ts, e2e/*, README.md, CHANGELOG.md, package.json`
  - En tant que joueur, je veux voir que ma manette n'est pas détectée et, en mode caméra, voir ce que filme la caméra, afin de savoir si mon périphérique est prêt avant de jouer.
- `2026-10-14 | En mode clavier, déplacer le bateau à gauche et à droite avec les flèches | input/src/main/kotlin/.../KeyboardSteering.kt, game/src/main/kotlin/.../launch/BoatMotion.kt, game/src/main/kotlin/.../launch/LaunchLayout.kt, game/src/main/kotlin/.../launch/LaunchScreen.kt, game/src/main/kotlin/.../PlatformHooks.kt, platform/web/src/main/kotlin/.../*, input/src/test/**, game/src/test/**, e2e/boat-keyboard.spec.ts, README.md, CHANGELOG.md, package.json`
  - En tant que joueur au clavier, je veux déplacer mon bateau à gauche et à droite avec les flèches, afin de le piloter.
- `2026-10-14 | Question : le contrôle s'inspire-t-il de l'ancien projet ? Analyse de PlayerHorizontalMovement | (aucun fichier modifié)`
  - En tant que développeur, je veux savoir comment l'ancien jeu pilotait le bateau, afin de retrouver ses sensations.
- `2026-10-14 | Reprise des trois modes de contrôle et du lissage de l'ancien jeu, inclinaison conservée | input/src/main/kotlin/.../GameInput.kt, input/src/main/kotlin/.../KeyboardSteering.kt, game/src/main/kotlin/.../control/*, game/src/main/kotlin/.../util/SmoothDamp.kt, game/src/main/kotlin/.../launch/BoatMotion.kt, game/src/main/kotlin/.../launch/LaunchScreen.kt, game/src/main/kotlin/.../HammerAndPlanksGame.kt, game/src/main/kotlin/.../PlatformHooks.kt, platform/desktop/**, platform/web/**, input/src/test/**, game/src/test/**, e2e/boat-keyboard.spec.ts, README.md, CHANGELOG.md, package.json`
  - En tant que joueur, je veux retrouver les modes de contrôle de l'ancien jeu (position, vitesse, force) et sa douceur de déplacement, afin de piloter le bateau comme avant, avec le réglage adapté à mes capacités.
