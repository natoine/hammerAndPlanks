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
Plateforme	Android (smartphones) en priorité, puis navigateur web sur PC avec webcam en deuxième backend. Pas Steam.
Langage	Kotlin, en Kotlin Multiplatform (KMP) pour partager la logique entre Android et web
Ambiance	Reggae dub, univers de pirates, positionnement clairement antifasciste, question de la violence posée (voir section 1 bis)
Sujet central	Monter une équipe (un équipage) ; le shooter en est la couche d'action
Modes d'interaction	Au choix du joueur : capture de mouvement, écran tactile, clavier, manette. Le gameplay ne dépend d'aucun mode en particulier.
Estimation de pose	InstantHMR (modèle développé par Mohamed Adjel chez NaturalPad), à la place de MediaPipe
Exécution du modèle	En local sur l'appareil, de préférence sur NPU
Niveaux	Générés procéduralement, à partir de blocs réutilisables
Ennemis	Chaque ennemi est un agent (percepts → décision → action), avec un comportement propre à sa nature
Assets 3D	Plutôt sprites pré-rendus depuis des modèles 3D (rendu 2D dans un moteur 2D) [À CONFIRMER]
Moteur de jeu : Kotlin natif avec libGDX
Android + iOS

4. Architecture cible

Modules Gradle indépendants. Le gameplay et les gestes ne dépendent pas d'Android pour rester testables sur PC.

:input       GameInput, InputSource, sélection et bascule du mode (Kotlin pur)
:camera      capture caméra (Android : CameraX ; web : getUserMedia), basse résolution, miroir/rotation
:pose        prétraitement et suivi de ROI (partagés) ; exécution du modèle ONNX (Android : ONNX Runtime ; web : onnxruntime-web)
:gestures    joints → GameInput (Kotlin pur, filtre One Euro)
:levelgen    blocs, générateur procédural, validation (Kotlin pur)
:agents      percepts, décision, comportements ennemis (Kotlin pur)
:game        rendu, boucle de jeu, physique, audio

Flux en mode mocap : Caméra → ROI → InstantHMR (NPU) → joints 3D → lissage + gestes → GameInput → jeu. Flux en mode clavier ou manette : événements Android (KeyEvent, MotionEvent joystick) → mapping → GameInput → jeu. Flux en mode tactile : MotionEvent (toucher) → mapping → GameInput → jeu.

Cibles et partage de code (Kotlin Multiplatform)

Deux cibles : Android (priorité) et navigateur web sur PC avec webcam (deuxième backend, utile pour des démonstrations sans installation). La logique est écrite une seule fois en Kotlin et compilée pour les deux.

Code partagé (commonMain) : :gestures, :levelgen, :agents, :input (GameInput, InputSource), le prétraitement de pose et le suivi de ROI, la logique de jeu et de progression.
Code par plateforme (mécanisme expect/actual de KMP) : capture caméra, exécution du modèle ONNX, entrées spécifiques (événements Android, Gamepad API et clavier côté web), rendu et audio selon le moteur retenu.
Cible web : Kotlin/JS ou Kotlin/Wasm. La caméra passe par getUserMedia, l'inférence par onnxruntime-web (WebGPU ou WASM). Pas de NPU dans le navigateur. Sur un PC avec carte graphique, le GPU devrait suffire (le modèle fait environ 5 ms sur une RTX 4070), à vérifier.
Les quatre modes d'interaction fonctionnent aussi sur le web (clavier, Gamepad API, tactile, webcam).
Points à vérifier tôt : (1) instanthmr.onnx s'exécute-t-il dans onnxruntime-web (opérations supportées, fp16 selon navigateur et GPU) ? C'est le test le plus important, indépendant du moteur ; (2) poids du modèle à télécharger au premier lancement, puis à mettre en cache ; (3) la licence des poids (SAM) est plus exposée quand le modèle est téléchargé par le navigateur ; (4) cadrage d'une webcam de PC (joueur assis, buste).
Ordre : stabiliser le cœur sur Android, puis ajouter le backend web.
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
Les assets de l'ancien jeu Unity sont potentiellement réutilisables

12. Conventions de travail avec Claude Code
Garder :gestures, :levelgen et :agents en Kotlin pur (sans dépendance Android) pour les tester sur PC.
Ne pas embarquer le rendu du mesh MHR ni RF-DETR dans l'application mobile sans accord.
Les mesures de performance doivent préciser l'appareil, le provider d'inférence et la précision numérique (fp16 ou int8).