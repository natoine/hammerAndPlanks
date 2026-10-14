# hammerAndPlanks
A body movement based vertical shooter

1. Vision

Hammer and Planks est un jeu vidéo de type vertical shooter dans un univers de pirates (le joueur pilote un bateau pirate vu de dessus ou en 3/4, l'écran défile verticalement).

La particularité : le jeu se pilote avec le corps, capté par la caméra frontale d'un smartphone (capture de mouvement monoculaire, sans capteur de profondeur). C'est le mode signature, mais le jeu doit rester jouable selon plusieurs modes d'interaction, au choix du joueur : capture de mouvement, écran tactile, clavier ou manette.

Le jeu est développé par NaturalPad (studio français, savoir-faire en interfaces naturelles, mocap et jeux de santé). Il vise le marché grand public du motion gaming.
1 bis. Direction narrative et ambiance

Orientation donnée par Antoine :

Genre et univers : vertical shooter, univers de pirates.
Ambiance sonore et mood : reggae dub.
Violence : le jeu pose la question de la violence sans la glorifier. Règle de conception : la plupart du temps, le jeu propose des alternatives à la violence pour réussir autrement. Le joueur peut toujours se battre, mais il a presque toujours une autre voie.
Positionnement : le jeu est clairement antifasciste.
Cœur du jeu : « avant tout un jeu sur comment monter une équipe ». Le shooter est la couche d'action ; la construction d'une équipe (un équipage) est le sujet central.
2. Décisions prises
Sujet	Décision
Plateforme	Android (smartphones) en priorité, puis navigateur web sur PC avec webcam et iOS. Ordre entre web et iOS [À CONFIRMER]. Pas Steam.
Langage	Kotlin (JVM), une seule base de code partagée entre Android, iOS et web via libGDX
Ambiance	Reggae dub, univers de pirates, positionnement clairement antifasciste, question de la violence posée (voir section 1 bis)
Sujet central	Monter une équipe (un équipage) ; le shooter en est la couche d'action
Modes d'interaction	Au choix du joueur : capture de mouvement, écran tactile, clavier, manette. Le gameplay ne dépend d'aucun mode en particulier.
Estimation de pose	InstantHMR (modèle développé par Mohamed Adjel chez NaturalPad), à la place de MediaPipe
Exécution du modèle	En local sur l'appareil, de préférence sur NPU
Niveaux	Générés procéduralement, à partir de blocs réutilisables
Ennemis	Chaque ennemi est un agent (percepts → décision → action), avec un comportement propre à sa nature
Assets 3D	Plutôt sprites pré-rendus depuis des modèles 3D (rendu 2D dans un moteur 2D) [À CONFIRMER]
Moteur de jeu	libGDX en Kotlin. Lanceurs : Android, iOS (MobiVM, fork maintenu de RoboVM), web (TeaVM via gdx-teavm), desktop LWJGL3 pour le développement
Backend web	TeaVM plutôt que Kotlin/JS ou Kotlin/Wasm : le backend GWT de libGDX n'accepte pas Kotlin, TeaVM compile le bytecode JVM (Kotlin compris) en JS ou Wasm

3. Démarrer, lancer et tester

État actuel : écran de lancement (mer animée, bateau, titre) sur desktop, Android et navigateur ; le bateau se pilote aux flèches du clavier, au stick gauche de la manette ou, en mode caméra, en inclinant le buste ou la tête (InstantHMR). Modules existants : :input, :pose, :pose-onnx, :gestures, :game et les lanceurs :platform:desktop, :platform:android, :platform:web ; les autres modules de la section 4 seront créés au fil des besoins. iOS (MobiVM) demande un Mac pour compiler et n'est pas encore branché.

Prérequis : JDK 17 ou plus (testé avec 21), Node 22 et npm, SDK Android (chemin dans local.properties, non versionné : `sdk.dir=/chemin/vers/Android/Sdk`). Gradle est fourni par le wrapper (./gradlew).

La version du projet est dans package.json, seule source de vérité : Gradle la lit pour l'APK et l'écran de lancement. `npm run version:check` vérifie qu'elle correspond à la dernière release du CHANGELOG.

```bash
npm install                      # ONNX Runtime Web, outils de test d'interface (Playwright)
./gradlew downloadModel          # modèle de pose InstantHMR (77 Mo, non versionné, empreinte vérifiée)
npx playwright install chromium  # navigateur des tests d'interface, une seule fois

npm run desktop                  # fenêtre desktop 540x960, langue du système
./gradlew :platform:desktop:run -Plang=en -Psize=1920x1080   # forcer la langue et la taille
./gradlew :platform:desktop:run -Pscreenshot=/tmp/capture.png  # capture après 3 s puis fermeture
./gradlew :platform:desktop:run -Pmode=motion                  # imposer le mode (keyboard, gamepad, motion)

npm run android                  # installe l'APK de debug sur le téléphone branché (débogage USB activé)
./gradlew :platform:android:assembleDebug   # APK seul : platform/android/build/outputs/apk/debug/

npm run web                      # compile en JavaScript (TeaVM) puis sert sur http://localhost:8080
                                 # (serveur avec en-têtes COOP/COEP : ONNX Runtime Web peut utiliser plusieurs threads)
                                 # le CHANGELOG est servi sur http://localhost:8080/changelog/

npm test                         # tests unitaires (JUnit 5) de tous les modules
npm run test:scripts             # tests des scripts Node (serveur web)
npm run web:build && npm run test:e2e   # tests d'interface Playwright (Chromium desktop et mobile)
```

Comment tester l'écran de lancement, plateforme par plateforme

Résultat attendu partout : la mer défile lentement vers le bas avec des vagues et des lignes d'écume qui ondulent, le bateau (vue 3/4, proue vers le haut) tangue au centre, le titre « Hammer and Planks » puis l'accroche apparaissent en fondu en haut, la version s'affiche en bas à droite. En mode clavier, les flèches gauche et droite déplacent le bateau, qui s'incline dans le virage ; en mode manette, c'est le stick gauche (zone morte de 0,15 comme l'ancien jeu ; à mi-course, en mode position, le bateau s'arrête à mi-chemin du bord). Par défaut (mode de contrôle « élan », arcade), le bateau accélère de plus en plus tant qu'on pousse, glisse un peu quand on relâche puis revient tout seul au centre, un peu plus lentement qu'à l'aller, freine fort si on pousse à contresens et rebondit un peu sur les bords. Modes de l'ancien jeu, au choix : « position » (relâcher ramène au centre), « vitesse » (s'arrête sur place), « force » (garde sa lancée). En bas à gauche, un badge indique le mode d'interaction (clavier par défaut) : Tab passe à manette, caméra, puis de nouveau clavier ; un appui sur le badge fait de même. Le mode choisi est retrouvé au lancement suivant. Sans manette branchée, le mode manette affiche une manette barrée et « Manette non détectée ». En mode caméra, une imagette du retour caméra (effet miroir) s'affiche en bas à droite ; si la caméra est refusée ou absente, l'imagette le dit. La caméra n'est allumée qu'en mode caméra et s'éteint dès qu'on en sort. L'accroche est en français sur un système ou un navigateur en français, en anglais sinon.

Desktop (le plus rapide, pour le développement)
1. `npm run desktop` : une fenêtre portrait 540x960 s'ouvre.
2. Flèches gauche et droite : le bateau accélère de plus en plus et s'incline ; relâchées, il glisse un peu puis revient au centre ; à contresens, il freine fort ; il rebondit sur les bords (mode élan). Modes de l'ancien jeu : `./gradlew :platform:desktop:run -Pcontrol=position` (ou `velocity`, `force`), facteur de vitesse `-Pspeed=0.5`.
2 bis. Redimensionner la fenêtre (y compris en paysage) : le bateau et les textes se recalent sans se chevaucher.
3. Tester l'autre langue : `./gradlew :platform:desktop:run -Plang=en` (ou `-Plang=fr`).
3 bis. Tester la caméra : `./gradlew :platform:desktop:run -Pmode=motion` ; la webcam du PC s'affiche en bas à droite (Linux : l'utilisateur doit avoir accès à /dev/video0). Pilotage par le corps (modèle téléchargé par `./gradlew downloadModel`) : se placer en buste face à la webcam, rester droit pendant « Tenez-vous droit... » (mesure de la posture neutre), puis pencher le buste vers sa droite ou sa gauche ; le bateau suit. Suivre la tête plutôt que les épaules : `-Ptilt=head`. Journal des mesures chaque seconde (état, posture neutre, angle, axe) : `-PdebugPose`. Tester la manette : `./gradlew :platform:desktop:run -Pmode=gamepad`, brancher une manette USB ou Bluetooth ; le picto n'est plus barré et le stick gauche déplace le bateau ; la débrancher remet le picto barré en une seconde environ.
4. Tester une taille précise : `-Psize=360x640` (petit téléphone), `-Psize=1920x1080` (écran PC).

Android
1. Sur le téléphone : Paramètres > À propos > appuyer 7 fois sur « Numéro de build », puis Options pour les développeurs > activer « Débogage USB ».
2. Brancher le téléphone en USB, accepter l'autorisation sur le téléphone, vérifier qu'il apparaît avec `adb devices` (adb est dans platform-tools du SDK Android).
3. `npm run android` installe l'application « Hammer and Planks » ; la lancer depuis le téléphone.
4. Sans câble : `./gradlew :platform:android:assembleDebug`, puis copier `platform/android/build/outputs/apk/debug/android-debug.apk` sur le téléphone et l'ouvrir (autoriser l'installation d'applications inconnues).
5. Sans téléphone : créer un émulateur dans Android Studio (Device Manager), le démarrer, puis `npm run android`.
6. Tester l'autre langue : changer la langue du téléphone, puis relancer l'application.
7. Tester la caméra : toucher le badge jusqu'au mode caméra ; Android demande l'autorisation, puis l'aperçu de la caméra frontale apparaît en bas à droite. Refuser l'autorisation affiche « Accès à la caméra refusé ». Pilotage par le corps : le modèle est embarqué dans l'APK s'il a été téléchargé avant la compilation (`./gradlew downloadModel`) ; se tenir droit pendant la calibration, puis pencher le buste. L'APK de debug pèse environ 235 Mo (modèle de 77 Mo et ONNX Runtime pour quatre architectures).
8. Tester la manette : connecter une manette Bluetooth ou USB ; en mode manette, le picto n'est plus barré et le stick gauche déplace le bateau.
9. Tester le déplacement au clavier : connecter un clavier Bluetooth ou USB, rester en mode clavier et utiliser les flèches gauche et droite.

Navigateur (version web)
1. `npm run web` compile le jeu en JavaScript puis le sert ; ouvrir http://localhost:8080 (arrêt du serveur : Ctrl+C).
2. Format mobile : outils de développement du navigateur (F12) > mode appareil, choisir un téléphone, recharger la page.
2 bis. Flèches gauche et droite : le bateau se déplace (cliquer d'abord dans la page si elle n'a pas le focus). Autres modes de contrôle par l'URL : http://localhost:8080/?control=position (ou velocity, force), et ?speed=0.5 pour le facteur de vitesse.
3. Tester l'autre langue : changer la langue préférée du navigateur (paramètres > langues), puis recharger.
4. Le CHANGELOG est consultable sur http://localhost:8080/changelog/.
5. Tester la caméra : Tab deux fois (mode caméra) ; le navigateur demande l'autorisation (localhost est accepté, sinon HTTPS est requis), puis la webcam s'affiche en bas à droite. Pilotage par le corps : le navigateur télécharge le modèle (77 Mo) au premier passage en mode caméra ; se tenir droit pendant la calibration, puis pencher le buste. Suivre la tête : http://localhost:8080/?tilt=head. Aucune image ne quitte le navigateur.
6. Tester la manette : brancher une manette et appuyer sur l'un de ses boutons (les navigateurs ne signalent une manette qu'après un premier appui) ; en mode manette, le picto n'est plus barré et le stick gauche déplace le bateau.

Tests automatisés
1. `npm test` : tests unitaires (langue, traductions, mise en page sur plusieurs tailles d'écran, animations, sprites, options desktop).
2. `npm run web:build && npm run test:e2e` : tests Playwright de la version web, en Chromium desktop et mobile, avec des navigateurs en français, en anglais et en allemand (repli sur l'anglais). La caméra est simulée par Chromium (mire), les manettes par un faux navigator.getGamepads.
3. `npm run version:check` : la version de package.json correspond à la dernière release du CHANGELOG.

Structure actuelle :

```
input/                Kotlin pur : GameInput, InputMode, InputModeSelector, KeyboardSteering, GamepadSteering, MotionInput
pose/                 Kotlin pur : Mhr70, PosePreprocessor, RoiTracker, PoseEstimator, ImageRotation, PoseSource
pose-onnx/            InstantHMR avec ONNX Runtime Java (commun au desktop et à Android)
gestures/             Kotlin pur : OneEuroFilter, BodyTilt, MotionSteering, PoseMotionInput
game/                 cœur libGDX partagé : HammerAndPlanksGame, PlatformHooks, Languages, Texts,
                      launch/ (LaunchScreen, LaunchLayout, LaunchAnimation, BoatFrames, BoatMotion),
                      sea/ (SeaRenderer, SeaWaves), ui/ (InputModeBadge, CameraPanel),
                      platform/ (GamepadDetector, CameraThumbnail : interfaces implémentées par les lanceurs),
                      control/ (MovementControlMode, ControlSettings), util/ (SmoothDamp, PeriodicCheck)
platform/desktop/     lanceur LWJGL3 (options --lang, --size, --screenshot, --input-mode, --control, --speed), manettes (gdx-controllers), webcam (webcam-capture)
platform/android/     lanceur Android (portrait, plein écran immersif), manettes (InputDevice, gdx-controllers), caméra (CameraX)
platform/web/         lanceur TeaVM, interop navigateur (langue, accessibilité, API Gamepad, getUserMedia), webapp/index.html,
                      webapp/pose.js (InstantHMR avec onnxruntime-web)
models/               instanthmr.onnx (non versionné, ./gradlew downloadModel), checksums.txt
scripts/              check-version.mjs, serve-web.mjs (serveur statique COOP/COEP)
assets/               sprites, mer, shaders, pictos ui/, polices OFL, i18n/messages*.properties (en par défaut, fr)
e2e/                  tests Playwright de la version web
package.json          version unique et scripts
```

Internationalisation : aucun texte en dur dans le code. Les textes sont dans assets/i18n/messages.properties (anglais, langue de repli) et messages_fr.properties. La langue vient du système (desktop, Android) ou du navigateur (web) ; toute langue non supportée retombe sur l'anglais. Un test vérifie que toutes les langues ont les mêmes clés.

Polices : Pirata One (titre) et Nunito (textes), sous licence SIL OFL, fichiers de licence dans assets/fonts/. Les polices de l'ancien jeu (MV Boli, Aparajita…) sont des polices Microsoft non redistribuables et ne sont pas reprises.

4. Architecture cible

Modules Gradle indépendants. Le gameplay et les gestes ne dépendent ni d'Android, ni d'iOS, ni de libGDX pour rester testables sur PC.

```
hammerAndPlanks/
├── settings.gradle.kts, build.gradle.kts
├── gradle/libs.versions.toml     versions centralisées
├── build-logic/                  conventions Gradle (module Kotlin pur, Android, tests)
│
├── input/      Kotlin pur : GameInput, InputSource, InputMode, routeur avec hot-swap
├── camera/     Kotlin pur : interfaces CameraSource, CameraFrame
├── pose/       Kotlin pur : PoseSource, prétraitement (crop 1,2×, cliff_cond),
│               suivi de ROI, squelette MHR70, ReplayPoseSource, interface PoseModel
├── gestures/   Kotlin pur : filtre One Euro, normalisation morphologique, MotionInputSource
├── levelgen/   Kotlin pur : blocs (JSON), générateur à graine, validateur
├── agents/     Kotlin pur : Agent, Percepts, Intent, FSM / arbres / utility AI / boids
├── game/       cœur libGDX : boucle, écrans, rendu sprites, physique, audio, i18n
│
├── platform/
│   ├── android/   lanceur, CameraX, ONNX Runtime (QNN puis CPU/XNNPACK), KeyEvent/MotionEvent
│   ├── ios/       lanceur MobiVM, AVFoundation, ONNX Runtime (CoreML execution provider)
│   ├── web/       lanceur TeaVM, getUserMedia, onnxruntime-web, Gamepad API
│   └── desktop/   lanceur LWJGL3 pour le dev : webcam, rejeu de sessions, clavier
│
├── tools/
│   ├── pose-golden/   test de parité Python ↔ Kotlin sur des images de référence
│   ├── pose-bench/    mesures par appareil, provider et précision
│   └── level-viewer/  visualiser un niveau à partir d'une graine
├── assets/       sprites, sprite sheets, audio, fichiers i18n fr/en
├── assets-src/   fichiers .blend et scripts de rendu Blender
└── models/       instanthmr.onnx, non versionné (licence SAM), téléchargé par une tâche Gradle
```

Règles de dépendance :

- :game ne dépend que de :input, :levelgen et :agents. Il ne connaît ni la caméra, ni la pose, ni les gestes.
- La chaîne mocap :camera → :pose → :gestures → :input n'est assemblée que par les lanceurs de plateforme, et seulement si le joueur choisit ce mode.
- Les modules Kotlin purs n'ont aucune dépendance à Android, iOS ou libGDX. TeaVM et MobiVM les compilent tels quels.
- Les agents reçoivent une vue du monde (WorldView), jamais l'état du jeu ni le mode d'entrée.

Flux en mode mocap : Caméra → ROI → InstantHMR (NPU) → joints 3D → lissage + gestes → GameInput → jeu. Flux en mode clavier ou manette : événements natifs (KeyEvent, MotionEvent joystick, GCController, Gamepad API) → mapping → GameInput → jeu. Flux en mode tactile : événements tactiles → mapping → GameInput → jeu.

Cibles et partage de code (libGDX)

Trois cibles : Android (priorité), iOS, et navigateur web sur PC avec webcam (utile pour des démonstrations sans installation). Un lanceur desktop LWJGL3 sert au développement. La logique est écrite une seule fois en Kotlin (JVM) et partagée par toutes les cibles.

Code partagé : :input, :pose (prétraitement, ROI), :gestures, :levelgen, :agents, et :game (logique, rendu, audio via libGDX).
Code par plateforme : modèle libGDX, interfaces dans le code partagé et implémentations injectées par chaque lanceur (pas d'expect/actual KMP). Concerne la capture caméra, l'exécution du modèle ONNX et les entrées spécifiques.
Cible iOS : MobiVM (compilation AOT). Caméra via AVFoundation, inférence via ONNX Runtime avec le CoreML execution provider pour viser le Neural Engine. Les bindings ONNX Runtime pour MobiVM sont à écrire (Bro) : point de risque à évaluer tôt.
Cible web : TeaVM via gdx-teavm. La caméra passe par getUserMedia, l'inférence par onnxruntime-web (WebGPU ou WASM), appelés via l'interop JS de TeaVM. Pas de NPU dans le navigateur. Sur un PC avec carte graphique, le GPU devrait suffire (le modèle fait environ 5 ms sur une RTX 4070), à vérifier.
Les quatre modes d'interaction fonctionnent sur toutes les cibles (clavier, manette, tactile, caméra).
Points à vérifier tôt : (1) instanthmr.onnx s'exécute-t-il dans onnxruntime-web (opérations supportées, fp16 selon navigateur et GPU) ? C'est le test le plus important ; (2) instanthmr.onnx avec le CoreML execution provider sur iPhone, et faisabilité des bindings MobiVM ; (3) poids du modèle à télécharger au premier lancement, puis à mettre en cache ; (4) la licence des poids (SAM) est plus exposée quand le modèle est téléchargé par le navigateur ; (5) cadrage d'une webcam de PC (joueur assis, buste).
Ordre : stabiliser le cœur sur Android, puis ajouter les backends web et iOS (ordre [À CONFIRMER]).

Abstraction des entrées

Le jeu ne connaît que GameInput, jamais le périphérique. Chaque mode est une InputSource :

kotlin
data class GameInput(
    val move: Vec2,          // axe de déplacement normalisé [-1, 1]
    val fire: Boolean,       // tir principal
    val special: Boolean,    // tir spécial
    val interact: Boolean,   // actions non violentes (parlementer, négocier, aider...), à préciser
    val pause: Boolean,
    val extras: Map<String, Float> = emptyMap() // bonus propres à un mode (ex. se pencher), jamais obligatoires
)

interface InputSource { val inputs: Flow<GameInput>; val mode: InputMode }
enum class InputMode { MOTION, TOUCH, KEYBOARD, GAMEPAD }
Dénominateur commun : tout le gameplay doit être jouable avec un axe 2D et trois boutons (tir, tir spécial, interaction). Les actions non violentes passent par interact ; en capture de mouvement, un geste dédié (par exemple les mains ouvertes ou levées) doit pouvoir le déclencher. Un mode peut ajouter des bonus facultatifs (par exemple se pencher vers l'écran pour un tir spécial en mocap) via extras, mais aucun niveau ne doit les exiger.
Sélection et bascule : le joueur choisit son mode au premier lancement et peut le changer depuis le menu, voire en cours de partie (hot-swap).
Implémenté (0.3.0) : module :input avec InputMode { KEYBOARD, GAMEPAD, MOTION } et InputModeSelector. Le clavier (flèches gauche et droite) est le mode par défaut. Un badge en bas à gauche de l'écran affiche le picto et le nom du mode courant ; Tab fait défiler clavier, manette, caméra, et un appui sur le badge fait de même sur les écrans tactiles. Le choix est sauvegardé (Preferences libGDX, localStorage sur le web). Sur le web, chaque changement est annoncé aux lecteurs d'écran (zone aria-live). Le mode tactile (TOUCH) n'est pas encore dans la liste. Implémenté (0.4.0) : détection de manette par plateforme (desktop : gdx-controllers, Android : InputDevice, web : API Gamepad), picto barré sans manette ; imagette du retour caméra en mode caméra (desktop : webcam-capture, dessinée par le jeu ; Android : CameraX PreviewView posée sur la vue du jeu ; web : élément vidéo posé sur le canvas). Interfaces GamepadDetector et CameraThumbnail dans :game, implémentées par chaque lanceur. Implémenté (0.6.0) : en mode clavier, KeyboardSteering (:input) produit un GameInput (axe horizontal [-1, 1]) ; BoatMotion (:game) déplace le bateau selon les trois modes de contrôle de l'ancien jeu (PlayerHorizontalMovement) : position (l'axe désigne un point du rail, portage exact, mode par défaut), vitesse et force. Lissage SmoothDamp de 0,6 s (portage de Mathf.SmoothDamp), vitesse maximale de l'ancien jeu (6 unités/s sur un rail de 32, soit 0,375 demi-rail/s), facteur de vitesse du joueur [0, 1]. Les modes vitesse et force reprennent l'intention et non la formule de l'ancien code, où le bateau ne bougeait presque pas et dépendait de la fréquence d'images. Ajout : inclinaison dans le virage. Réglages (ControlSettings) lus dans les préférences ou imposés au lancement, en attendant un menu d'options. Implémenté (0.9.3) : mode de contrôle « élan » (MOMENTUM), par défaut pour le clavier, la manette et le corps. Poussée de 0,625 demi-rail/s² qui croît jusqu'à 3 fois plus après 1,2 s d'appui continu dans la même direction (dosée par l'axe analogique), vitesse de pointe 0,75 demi-rail/s (deux fois l'ancien jeu ; du centre au bord en 2 à 3 s), sans appui rappel vers le centre (8/s²) amorti par le frottement de l'eau (5/s), vitesse de retour plafonnée à 60 % de la vitesse de pointe : légère glisse puis retour au centre sans le dépasser, plus lent que l'aller (centre vers bord 1,7 s en poussant, bord vers centre 2,4 s en relâchant) ; à contresens, poussée ×6 et frottement ; rebond de 35 % sur les bords ; inclinaison qui suit la vitesse. Réglages dans MomentumTuning (game/.../launch/BoatMotion.kt). Les modes position, vitesse et force de l'ancien jeu restent disponibles (`-Pcontrol`, `?control=`). Implémenté (0.7.0) : en mode manette, GamepadSteering (:input) transforme le stick gauche en GameInput, avec la zone morte de l'ancien jeu (0,15 par axe, remise à l'échelle, ProjectSettings/InputManager.asset) et le vecteur 2D borné à 1 (PlayerHorizontalMovement.getLateralAxisInput). Lecture du stick : desktop et Android via gdx-controllers, web via l'API Gamepad (mapping standard, axes 0 et 1). Les trois modes de contrôle s'appliquent aussi à la manette. Implémenté (0.8.0) : en mode caméra, InstantHMR estime la pose et l'inclinaison latérale du buste (ligne des épaules, par défaut) ou de la tête (ligne des oreilles) pilote le bateau, avec les trois modes de contrôle. Chaîne assemblée par chaque lanceur (règle de dépendance respectée : :game ne voit qu'un MotionInput) : images caméra → :pose (prétraitement porté d'instanthmr/inference.py, suivi de la zone par les points du haut du corps, sans détecteur) → modèle (desktop et Android : ONNX Runtime Java via :pose-onnx ; web : onnxruntime-web en WebAssembly via webapp/pose.js) → :gestures (BodyTilt, calibration de la posture neutre sur 10 images, filtre One Euro, zone morte 3°, axe à fond à 20°) → GameInput. Pencher vers sa droite envoie le bateau à droite de l'écran (l'imagette est en miroir). Le badge affiche l'état : chargement, « Tenez-vous droit... », consigne, modèle absent ou erreur.
Caméra à la demande : la permission caméra, la calibration et le chargement du modèle de pose ne se déclenchent que si le mode capture de mouvement est choisi. Un joueur au tactile, au clavier ou à la manette n'a donc jamais à accorder l'accès à sa caméra.
Tactile : mode par défaut sur un smartphone sans périphérique.
Clavier et manette : sur Android, ce sont des périphériques Bluetooth ou USB. Les gérer via les événements d'entrée standard. Le clavier et la manette concernent surtout les tablettes, les appareils Android TV, les ChromeOS et les smartphones avec manette clipsable.

Principes :

Interface PoseSource avec plusieurs implémentations : InstantHmrPoseSource, MediaPipePoseSource (comparaison), ReplayPoseSource (rejeu de sessions enregistrées en JSON).
Un thread dédié à la caméra, un dispatcher mono-thread pour l'inférence, un Flow conflated vers le jeu. Le rendu à 60 fps lit toujours la dernière pose et n'attend jamais le modèle.
Les modes tactile, clavier et manette sont aussi des modes de jeu à part entière, et servent à déboguer sans caméra.
5. Estimation de pose : InstantHMR

Dépôt : https://github.com/mohamdev/InstantHMR (Apache-2.0 pour le code). Auteur : Mohamed Adjel.

Régresseur MHR léger exportable en ONNX (backbone RepViT-M1.5, décodeur à attention croisée sur 9 tokens, conditionnement caméra CLIFF).
Entrées : image (N, 3, 224, 224), cliff_cond (N, 3).
Sorties : mhr_params (204), shape_params (45), cam_trans (3), joints_2d (70, 2) (coordonnées normalisées du crop), joints_3d (70, 3) (mètres, centré sur le corps, Y vers le bas). Les 70 keypoints suivent l'ordre MHR70 (voir instanthmr/skeleton.py).
Perf annoncée : environ 5 ms par frame (fp16, CUDA, RTX 4070) ; environ 25 fps sur CPU. Mohamed indique que le modèle est surtout performant sur les NPU de smartphones [À CONFIRMER : appareils, runtime et quantification utilisés pour ses mesures].
Le projet n'a besoin que des joints : ne pas embarquer le rendu du mesh MHR (Python 3.12, pymomentum) dans l'application.
Points d'attention
Détecteur de personne : la démo du dépôt utilise RF-DETR, bien plus lourd que le modèle de pose. Sur mobile, ne pas l'embarquer. Utiliser un suivi de ROI (boîte déduite des joints_2d précédents, légèrement élargie), initialisé par un écran de calibration, avec un petit détecteur seulement en cas de perte de suivi.
Parité avec Python : le prétraitement (crop carré 1,2×, normalisation, cliff_cond) doit être identique à celui du dépôt. Prévoir un test « golden » : mêmes images de référence, mêmes sorties à tolérance près.
Focale : le modèle est conditionné par la focale. Calculer la focale en pixels depuis CameraCharacteristics (longueur focale et taille du capteur) plutôt que l'approximation de la démo (1,05 × diagonale).
Providers ONNX Runtime à tester dans l'ordre : QNN (Snapdragon), NNAPI (déprécié depuis Android 15, ne pas bâtir dessus), puis CPU/XNNPACK. LiteRT avec delegates NPU est une alternative, mais la conversion du décodeur à attention est risquée.
NPU : formes statiques et quantification (fp16 ou int8) souvent requises ; les opérations non supportées retombent sur CPU/GPU. Benchmarker sur 3 ou 4 téléphones (Snapdragon, Tensor, Exynos, entrée de gamme).
Cadrage : un joueur à 50 cm d'un téléphone est surtout visible en buste. Tester le modèle sur du cadrage haut du corps.
Thermique et batterie : plafonner l'inférence à 15-20 Hz avec interpolation.
Mesures (0.8.0, modèle publié fp16, une personne, image 640×480, prétraitement compris) :
- PC de développement (Linux, processeur x86-64), ONNX Runtime Java 1.31 CPU, 4 threads : environ 50 ms par image ; plafonné à 20 Hz.
- Chromium sans GPU (tests Playwright), onnxruntime-web 1.30 WebAssembly : environ 190 ms par image avec plusieurs threads (page isolée COOP/COEP), 450 ms sur un seul thread.
- Téléphones : non mesurés (aucun appareil branché). Le modèle tourne sur le processeur ; QNN ou NNAPI restent à évaluer.

Licence : le code est en Apache-2.0, mais les poids sont sous licence SAM (entraînés sur le dataset de Meta). Les licences de RF-DETR et des assets MHR s'appliquent aussi si utilisés. Valider la redistribution commerciale avant publication.
Vie privée : tout tourne en local, aucune image n'est envoyée. À mettre en avant dans les stores et pour le RGPD.
6. Interprétation des gestes (:gestures)

Module Kotlin pur, séparé du moteur, que l'on doit pouvoir remapper sans toucher au gameplay. Il ne produit qu'un GameInput, comme les autres modes.

L'équité entre modes (classements, difficulté par mode) est hors périmètre pour l'instant. Seule contrainte retenue : la validation des niveaux (section 7) se fait avec la marge de précision du mode le moins précis, la capture de mouvement.

Entrée : joints 3D normalisés. Normaliser les seuils par la morphologie (largeur d'épaules, longueur du buste) plutôt qu'en valeurs absolues, car la profondeur est estimée et non mesurée.
Lissage : filtre One Euro (adapté à ce type de signal), pas de simple moyenne mobile.
Exemples de mapping (à valider avec de vrais joueurs) : position des poignets ou inclinaison du buste → direction du bateau ; geste de tir ; lever les bras ou se pencher vers l'écran → tir spécial.
Les repères d'InstantHMR (Y vers le bas, ordre MHR70) diffèrent de la Kinect (25 joints) : prévoir une table de correspondance si l'on reprend de la logique de l'ancien jeu.
Outil de réglage : enregistrer des sessions de poses (JSON) et les rejouer, pour régler les gestes sans refaire les mouvements à chaque test.
Tests : les mêmes poses doivent produire les mêmes GameInput, sans téléphone.
7. Niveaux procéduraux (:levelgen)

Les niveaux sont des suites de blocs réutilisables qui défilent verticalement.

Bloc : une donnée (JSON ou DSL Kotlin), pas du code. Il décrit sa hauteur, son terrain (îles, récifs, courants), ses emplacements d'apparition d'ennemis (spawn slots) typés, ses emplacements de bonus, et des ancres d'entrée et de sortie (sockets) pour que deux blocs s'enchaînent sans coupure.
Étiquettes : difficulté, biome (mer ouverte, tempête, port, bataille navale), rôle (respiration, pic de tension, boss).
Générateur : assemble une séquence de blocs selon une courbe de tension cible (montée, pic, repos), avec une graine (seed) pour rejouer ou partager un niveau. Grammaire ou tirage pondéré suffisent.
Validation automatique : le niveau doit être jouable (zones d'esquive suffisantes, aucun passage infranchissable). Important car le pilotage par gestes est moins précis qu'une manette.
Le générateur choisit quels agents placer dans les spawn slots selon la courbe de tension.
Voies alternatives : un bloc de rencontre doit déclarer ses résolutions possibles (combat, négociation, contournement, aide, etc.) et les conditions pour y accéder (rôle d'équipage requis, ressource). La validation vérifie qu'une rencontre garde en général au moins une voie non violente jouable.
8. Ennemis agentifiés (:agents)

Chaque ennemi est un agent : boucle percevoir → décider → agir.

kotlin
interface Agent {
    fun perceive(world: WorldView): Percepts
    fun decide(percepts: Percepts): Intent
}
Percepts : ce que l'agent a le droit de savoir, via un module de perception et non un accès direct à l'état du jeu. Exemples : position et vitesse du bateau du joueur dans un rayon, projectiles proches, alliés proches, état de sa propre coque. Les agents ne perçoivent jamais le mode d'entrée ni le geste brut du joueur, seulement l'état du bateau : leur comportement doit être identique quel que soit le mode choisi.
Intent : une intention (se déplacer vers, tirer, se replier, etc.). Le moteur exécute les intents, ce qui garde la physique centralisée.
Résolutions non violentes : un agent ennemi ne se limite pas à attaquer ou fuir. Ses décisions incluent aussi parlementer, se rendre, accepter un accord, demander de l'aide, rejoindre l'équipage. Les percepts doivent donc couvrir les signaux d'ouverture du joueur (par exemple une action interact tendue vers lui), l'état de l'agent (moral, besoins, liens avec d'autres agents) et la composition de l'équipage du joueur. Ces voies sont à concevoir dès le départ pour chaque type d'ennemi, pas ajoutées après coup.
Décision propre à la nature de l'agent : le formalisme dépend du type d'ennemi. Proposition de départ [À CONFIRMER] :
barque ou kamikaze : machine à états simple ;
frégate : arbre de comportement (garder la distance, tirer en bordée, se replier) ;
capitaine ou boss : planification par utilité (utility AI) ou GOAP ;
bancs de créatures ou escadre : comportement de groupe (boids) avec un rôle par agent.
Fréquence : la décision tourne à 5-10 Hz, le mouvement est interpolé à chaque frame, pour ménager le budget laissé par l'inférence de pose.
Tests : module Kotlin pur, testable sans moteur ni téléphone.
9. Assets et direction artistique
Modèles 3D créés dans Blender, rendus en sprites ou sprite sheets (rotation du bateau, tir, explosion) pour un rendu 2D dans un moteur 2D. Aucun changement d'architecture ; coût de perf minimal.
Passer à de la vraie 3D temps réel (Three.js, Unity 3D, etc.) n'est justifié que pour une caméra dynamique, une mer qui ondule en temps réel ou de la lumière dynamique. C'est un saut de périmètre.
Les assets de l'ancien jeu Unity (dépôt HammerAndPlanks) sont réutilisables : droits confirmés par NaturalPad. Seuls les textes dessinés dans les images sont écartés (contraire à l'i18n). Les modèles 3D (FBX) sont rendus en sprites via des scripts Blender en ligne de commande.

Mer animée

La mer reprend le shader de l'ancien jeu Unity (Assets/Shaders/PerlinNormalHeight.shader) : une turbulence de bruit 3D donne une hauteur de vague qui fait onduler la texture (parallaxe) et dessine l'écume sur les crêtes ; la 3e dimension du bruit fait évoluer les vagues sur place. Portage en GLSL ES 1.0 dans assets/shaders/sea.frag (Android, iOS, desktop, WebGL 1), bruit simplex calculé dans le shader (webgl-noise, MIT). Les réglages sont dans game/.../sea/SeaWaves.kt ; ceux de l'ancien matériau sont conservés (couleur et bande d'écume, lacunarité 2, gain 0,5, phase 0,2/s). Seules différences : 4 octaves au lieu de 5 et pas d'éclairage, effet négligeable en vue de dessus. Si le shader ne compile pas sur un appareil, la mer s'affiche sans vagues (repli journalisé). Coût GPU à mesurer sur un téléphone d'entrée de gamme : le nombre d'octaves est le premier levier.

Pictos de l'interface

Les pictos des modes d'interaction (assets/ui/input_*.png, dont la manette barrée input_gamepad_off.png, et camera_off.png pour l'imagette, blancs, 128 px, teintés au dessin) sont rendus depuis la police Material Icons (Google, Apache 2.0, source et licence dans assets-src/ui/). La police n'est pas embarquée : libGDX ne sait pas générer une police sans lettres.

```bash
python3 assets-src/ui/render_icons.py           # régénère assets/ui/input_*.png
python3 assets-src/ui/test_render_icons.py -v   # tests du script (Pillow)
```

Pipeline Blender → sprites

Les sources 3D sont dans assets-src/boats/<bateau>/ (FBX coque et voile, textures). Le script assets-src/blender/render_boat.py les rend en PNG transparents, avec un éclairage cartoon à 3 paliers qui reprend le shader de l'ancien jeu. Les rendus vont dans assets-src/**/render/, ignoré par git. Les sprites retenus sont ensuite copiés dans assets/.

```bash
# Prérequis : Blender 4.2+ (testé en 5.2 LTS), installé par exemple avec `sudo snap install blender --classic`
blender -b --python assets-src/blender/render_boat.py -- \
  --hull assets-src/boats/player/Boat_player.fbx --hull-tex assets-src/boats/player/Boat_player.png \
  --sail assets-src/boats/player/Boat_player_Voile.fbx --sail-tex assets-src/boats/player/Boat_player_voile.png \
  --view top --size 512 --angles 1 --out assets-src/boats/player/render/boat_player

# Tests du script (Python 3 + Pillow, Blender dans le PATH)
python3 assets-src/blender/test_render_boat.py -v
```

Options : --view top (dessus) ou 34 (caméra inclinée à 35°), --angles N pour N rotations autour de l'axe vertical (sprite sheet de virage), --size en pixels.

Vue retenue : 3/4. Le sprite du bateau joueur est rendu en 8 rotations de 512 px, copiées dans assets/boats/player/boat_player_34_00.png à _07.png. L'image 00 a la proue vers le haut de l'écran. Chaque image suivante tourne de 45° dans le sens inverse des aiguilles d'une montre : 02 proue à gauche, 04 proue en bas, 06 proue à droite.

```bash
blender -b --python assets-src/blender/render_boat.py -- \
  --hull assets-src/boats/player/Boat_player.fbx --hull-tex assets-src/boats/player/Boat_player.png \
  --sail assets-src/boats/player/Boat_player_Voile.fbx --sail-tex assets-src/boats/player/Boat_player_voile.png \
  --view 34 --size 512 --angles 8 --out assets-src/boats/player/render/boat_player
cp assets-src/boats/player/render/boat_player_34_0*.png assets/boats/player/
```

12. Conventions de travail avec Claude Code
Garder :input, :camera, :pose, :gestures, :levelgen et :agents en Kotlin pur (sans dépendance Android, iOS ni libGDX) pour les tester sur PC.
Ne pas introduire dans le code partagé de bibliothèque JVM non validée sous TeaVM et MobiVM (réflexion, java.nio avancé, kotlinx.coroutines dont dépend InputSource via Flow).
Ne pas embarquer le rendu du mesh MHR ni RF-DETR dans l'application mobile sans accord.
Les mesures de performance doivent préciser l'appareil, le provider d'inférence et la précision numérique (fp16 ou int8).