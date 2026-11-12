# Choix techniques, écarts de design et limites connues

## 0. Ce qui n'a pas pu être fait : compiler et lancer

L'environnement où ce mod a été écrit ne pouvait joindre ni `maven.minecraftforge.net`, ni les serveurs de Mojang, ni Maven Central.
`./gradlew build` et `runClient` n'ont donc **jamais tourné**. Le code n'a été confronté ni au compilateur avec les classes de Minecraft,
ni au jeu. Les vérifications de remplacement sont listées dans le README. Conséquences à prévoir :

- quelques erreurs de compilation probables sur des appels vanilla (nom ou signature) ;
- des défauts de comportement que seul le jeu révèle (rythme des boss, placement des lieux, lisibilité des annonces).

Tout le reste de ce document décrit ce qui a été conçu, pas ce qui a été observé en jeu.

## 1. Outillage

- **Forge 1.21.1-52.1.20**, noms officiels Mojang, Java 21.
- **ForgeGradle 7 + Gradle 9.3.1** : c'est ce que livre le MDK officiel de Forge 52.1.20. Le wrapper est fourni.
- Pour revenir à ForgeGradle 6 (MDK plus ancien) : dans `build.gradle`, plugin `net.minecraftforge.gradle` version `[6.0.24,6.2)`,
  remplacer le bloc `repositories`/`dependencies` par `dependencies { minecraft 'net.minecraftforge:forge:1.21.1-52.1.20' }`,
  ajouter `reobf = false` et `copyIdeResources = true` dans `minecraft { }`, et utiliser Gradle 8.12.1.
- Aucun mixin, aucun access transformer, aucune dépendance autre que Forge.

## 2. Architecture

Un système = un package (`net.trounoir.*`) :

| Package | Rôle |
|---|---|
| `data` | Définitions lues du datapack (traits, techniques, savoirs, synergies, étiquettes, Fragilité, profils, personnages, factions, lieux, épitaphes). |
| `absorption` | Données d'une vie, règles d'absorption, calcul des bonus, de la puissance et du rang. |
| `fragility` | Handicaps qui s'expriment dans le temps, entretien périodique. |
| `technique` | Emplacements, recharges, exécution des techniques, arrêt du temps. |
| `combat` | Écarts de rang, statistiques de combat, faiblesses. |
| `character` | Entité des personnages, attaques annoncées, apparition, paroles. |
| `suke` | Tir systémique et notoriété. |
| `world` | Lieux, plans de construction, Traversée, état du monde. |
| `faction` | Réputation et réactions du monde. |
| `rebirth` | Mémoires persistantes, Renaissance, retour dans le passé, Échos. |
| `chronicle` | Enregistrement des vies, épitaphes. |
| `network` | Canal réseau. |
| `client` | Touches, HUD, écrans, rendu (jamais chargé sur un serveur dédié). |
| `command`, `config`, `registry`, `util` | Commandes de debug, configuration, enregistrements, aides. |

**Piloté par les données.** Le code ne connaît aucun trait, aucune créature, aucun personnage, aucun lieu par son nom.
Il connaît des *types* : huit types de techniques (`dash`, `leap`, `blink`, `projectile`, `aoe`, `beam`, `buff`, `time_stop`),
trois formes d'attaque (cercle, ligne, cône), neuf plans de lieux, sept types de réactions. Les seuls identifiants cités dans le code sont
`trounoir:suke`, sa tour et la faction `azalee` (le système de Suke), `higgs` (la victoire) et `trounoir:essence_inconnue`
(gain par défaut d'un mob sans profil).

**Une seule entité** (`trounoir:character`) pour tous les humanoïdes. Son identité vient d'un identifiant synchronisé vers une définition JSON.
Les demi-dieux des repaires sont des **mobs vanilla renforcés** (attribut d'échelle, PV, dégâts) marqués d'une étiquette.

**Un seul type de message réseau** (type + JSON compressé), dans les deux sens. Peu d'octets en jeu (synchronisations ponctuelles),
et aucun champ à maintenir en double. Chaque valeur venant du client est revalidée côté serveur.

**Sons, particules, effets** : désignés par identifiant vanilla et résolus par les registres, avec repli neutre. Aucun son propre au mod.

## 3. Le retour dans le passé

### Approche retenue : effacement et régénération, serveur arrêté

L'état initial d'un monde est une fonction de sa graine. Plutôt que de copier un instantané à la création puis de le restaurer,
le mod **efface ce qui a été vécu** et laisse le monde se régénérer à l'identique :

1. À la mort : la vie est inscrite dans la Chronique, **hors du monde** (`<jeu>/trounoir/memoires/<monde>.json`).
2. Le joueur choisit son nom et son Écho (écran de Renaissance).
3. Solo : le client quitte le monde ; le serveur intégré s'arrête. Une fois les fichiers fermés (`ServerStoppedEvent`),
   le mod supprime `region`, `entities`, `poi`, `DIM-1`, `DIM1`, `dimensions`, `data`, `playerdata`, `advancements`, `stats`, `trounoir`
   et remet `level.dat` à zéro (horloge, météo, joueur, dragon, événements). Graine, règles du jeu, datapacks et `serverconfig` sont conservés.
4. Le client rouvre le même monde : terrain et structures vanilla se régénèrent, le mod rebâtit ses lieux aux mêmes positions
   (déduites de la graine et mises en cache hors du monde), les personnages uniques sont de nouveau vivants.
5. Le joueur apparaît au point de renaissance exact, avec ses capacités de base et ses Échos.

Pourquoi pas un instantané : copier des fichiers de région pendant que le serveur écrit dedans n'est pas fiable, et l'instantané d'un monde neuf
ne contient presque rien que la graine ne redonne. L'effacement serveur arrêté n'a pas ce défaut.

### Garde-fous

- **Mondes préexistants.** L'effacement n'est autorisé que si le monde est né avec le mod (temps de jeu inférieur à 30 s au premier démarrage).
  Sinon le mod l'inscrit comme « non lié » et n'utilise que la renaissance douce. `/trounoir status` affiche l'état.
- **Échec d'effacement** (fichier verrouillé…) : le monde reste jouable, et la renaissance douce termine le travail à la connexion suivante.
- **Autre graine sous le même nom de dossier** : les mémoires de l'ancien monde sont archivées, le nouveau repart d'une page blanche.

### Renaissance douce (mode `SOFT`, et repli)

Réinitialisation contrôlée de l'état géré par le mod, sans arrêter le serveur : vie du joueur, inventaire, coffre de l'Ender, expérience,
personnages (retirés, y compris ceux des tronçons non chargés à leur prochain chargement), morts d'uniques, réputations, alertes,
événements programmés, horloge et météo. Les lieux restent bâtis et sont repeuplés.

### Limites

- **Le terrain modifié ne revient pas en renaissance douce.** Constructions, coffres et objets tombés à la mort restent.
- **Serveur dédié** : en mode `AUTO` il utilise la renaissance douce, et l'état du monde y est commun à tous les joueurs
  (la renaissance de l'un ressuscite les uniques pour tous). En `WORLD_RESET`, le serveur expulse les joueurs, s'arrête et efface le monde :
  il faut un script de relance automatique.
- **Solo ouvert au réseau local à plusieurs** : renaissance douce.
- **Mods de génération non déterministes** : la régénération ne serait pas identique.
- **Le dossier du monde** garde son nom et son `level.dat` : l'écran de sélection affiche toujours le même monde.
- Les statistiques et progrès vanilla sont effacés avec le reste (ils font partie de la vie).
- La réouverture passe par l'écran de chargement habituel ; si Minecraft demande une confirmation
  (réglages expérimentaux), elle s'affichera aussi à ce moment.

## 4. Les deux continents

- **Nébulosia = l'Overworld.** Le point de renaissance est le point d'apparition du monde.
- **Diviniarth = une dimension** (`trounoir:diviniarth`), définie en JSON : générateur de l'Overworld avec le relief « grands biomes ».
  Même graine, terrain différent, terres plus vastes. C'est la solution la plus robuste : aucun générateur personnalisé à maintenir,
  et un océan infranchissable « gratuit » entre les deux.
- **La Traversée** : clic droit sur le gouvernail d'un navire, dans un port. Le lieu d'arrivée est bâti avant la téléportation.
- Écart de design : la traversée n'est pas un voyage en mer joué, mais un fondu (cécité brève, titre). Le saut d'échelle est porté par les rangs :
  le premier légionnaire rencontré vaut un Supernova.

## 5. Les lieux

- Chaque lieu a une position théorique (distance et angle depuis l'origine du continent). Le mod cherche autour un terrain émergé et à peu près plat
  en interrogeant le générateur (hauteur de base), **sans générer de tronçon**. À défaut, le lieu est posé sur ses propres fondations.
- Un lieu est bâti quand un joueur s'en approche (150 blocs par défaut), bloc à bloc, par un plan procédural (`StructureBuilders`).
  Les plans sont volontairement simples et lisibles ; la palette de blocs vient du JSON du lieu.
- Écart de design : ce ne sont pas des structures de génération vanilla (pas de `/locate`, pas de gabarits `.nbt`). Avantage : positions stables,
  repeuplement possible, aucune dépendance à des fichiers binaires qu'on ne pouvait pas produire sans le jeu.
- Limites : la construction d'un grand lieu (cité) peut provoquer une brève saccade ; un lieu peut couper un relief ou une structure vanilla ;
  le port n'est pas forcément sur une côte (il creuse son propre bassin).

## 6. Combat et boss

- **Annonces.** Toute attaque de personnage est précédée d'une forme dessinée au sol en particules (cercle, ligne, cône), figée au début de l'annonce.
  Les dernières 0,3 s passent en flammes. Sortir de la forme, c'est esquiver. Les phases changent à des seuils de vie fixes, avec un signal sonore.
- Écart de design : les **demi-dieux** utilisent l'IA de leur mob vanilla (pas d'annonces). Ils sont dangereux par leurs statistiques et leur rang.
- **Arrêt du temps** : dans la zone, les créatures passent en « sans IA » et les projectiles sont figés, puis rétablis. Les êtres de très haut rang
  ne sont que ralentis. L'arrêt du temps *subi* (Trou-Blanc) fige le joueur par des effets (lenteur, faiblesse) : il peut encore regarder et utiliser des objets.
- **Rangs** : appliqués à tout le monde, créature contre créature comprise.
- **Suke** : le reflet n'est montré qu'au joueur visé ; la visée se fige 0,35 s avant le tir. Le tir est un tracé instantané, arrêté par les blocs.
  L'œil rouge du HUD (notoriété) est un indicateur d'interface, pas un élément du monde.

## 7. Absorption

- Les morts ne comptent que si le joueur (ou son projectile, ou sa technique) est l'auteur du coup fatal **et** a retiré au moins la moitié de la vie
  de la victime. Les mobs de spawner (classique ou d'épreuve) sont marqués à l'apparition.
- Les modificateurs d'attributs sont **transitoires** et recalculés à chaque changement : rien de persistant n'est écrit dans le joueur.
- Les effets passifs (vision nocturne…) sont des effets vanilla réappliqués sans icône ni particules.
- Un mob d'un autre mod, sans profil, donne « Essence inconnue ».

## 8. Chronique et épitaphes

- L'épitaphe est composée **côté serveur**, dans la langue du client. En solo, les noms sont traduits ; sur serveur dédié, les noms de créatures
  vanilla sortent en anglais (le serveur ne connaît que cette langue).
- La position d'une étoile dépend de l'ordre et du nom de la vie : elle ne bouge pas quand d'autres vies s'ajoutent.

## 9. Assets

- Textures de personnages : skins 64×64 générés par script (palette par personnage, étoile sur la poitrine des Supernovas et Hypernovas,
  couronnes, poussière d'étoiles pour les Dieux). Ce sont des **substituts propres et cohérents**, pas des illustrations finales.
- Les Dieux sont des humanoïdes agrandis (attribut d'échelle), pas des modèles propres.
- Aucun son propre : sons vanilla.

## 10. Autres limites connues

- Les personnages n'ouvrent pas les portes et ne suivent pas de routine quotidienne ; les civils fuient, les gardes gardent.
- Les « alliés » de la Chronique sont les factions dont la réputation atteint le seuil d'alliance ; il n'y a pas de compagnon recrutable.
- Le savoir de type « recette » débloque des recettes vanilla dans le livre de recettes ; il n'y a pas de recette propre au mod.
- La vengeance d'un unique le fait surgir près du joueur ; sa copie restée au foyer est retirée.
- La règle `doImmediateRespawn` court-circuite l'écran de mort vanilla : l'écran de Renaissance s'ouvre quand même, mais en renaissance douce
  le joueur a déjà réapparu derrière lui.
