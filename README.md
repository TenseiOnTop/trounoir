# Trou-Noir

Mod Minecraft **Java 1.21.1 — Forge 52.1.20**.

Vous êtes le Dieu Trou-Noir, le plus faible des Dieux, condamné par Higgs à vivre parmi les mortels.
Vous absorbez ce que vous tuez. Quand vous mourez, vous renaissez dans le passé, au même point,
sans vos pouvoirs mais avec vos souvenirs. But : tuer Higgs.

## État de vérification — à lire d'abord

Ce projet a été écrit dans un environnement **sans accès aux serveurs de Forge et de Mojang** :
`./gradlew build` et `runClient` **n'ont donc pas pu y être exécutés**. Ce qui a été vérifié à la place :

- syntaxe et cohérence interne de tout le code (compilation tolérante `javac`, avec les sources de Forge 52.1.20) ;
- chaque appel à l'API Forge relu contre les sources de Forge 52.1.20 ;
- les signatures vanilla relues contre les correspondances Parchment 1.21.1 et un mod 1.21.1 réel ;
- tous les identifiants vanilla cités (objets, blocs, entités, attributs, effets, particules, sons, recettes)
  contrôlés contre les registres de la 1.21.1 (`tools/check_ids.py`) ;
- validité de tous les JSON, références croisées du contenu, clés de traduction.

Il faut donc s'attendre à ce que la première compilation réelle signale quelques erreurs à corriger
(un nom de méthode vanilla, un type générique). Elles se corrigent vite une fois le message de `gradlew build` connu.
Le fichier `.github/workflows/build.yml` permet aussi de compiler sur GitHub Actions.

## Installation et lancement

Prérequis : un JDK 21. Le reste (Gradle 9.3.1, ForgeGradle 7, Minecraft, Forge) est téléchargé par le wrapper.

```
./gradlew build        # produit build/libs/trounoir-1.0.0.jar
./gradlew runClient    # lance un client de développement avec le mod
./gradlew runServer    # lance un serveur dédié de développement
```

Pour jouer dans le profil CurseForge existant (1.21.1, Forge 52.1.20) : copier `build/libs/trounoir-1.0.0.jar`
dans le dossier `mods` du profil, puis créer **un nouveau monde** (voir « Mondes préexistants » ci-dessous).

Le build suit le MDK officiel de Forge 52.1.20 (ForgeGradle 7). Si votre installation préfère ForgeGradle 6,
`docs/CHOIX_TECHNIQUES.md` indique quoi changer.

## Les systèmes, en bref

| Système | Ce qu'il fait | Package |
|---|---|---|
| Fragilité divine | Un seul cœur, lenteur, faiblesse, mains gourdes, maladresse, faim. Des modificateurs d'attributs permanents, effacés peu à peu par l'Absorption. | `fragility`, `absorption` |
| Absorption | Tuer donne des **Traits** (passifs), des **Techniques** (actives, 4 touches) et des **Savoirs**. Doublons = niveaux, avec plafond et rendements décroissants. Étiquettes et synergies. | `absorption`, `technique` |
| Rangs | Une puissance lisible et un rang de 0 à X. L'écart de rang se ressent dans les deux sens. | `combat` |
| Personnages | Une seule entité, entièrement décrite en JSON : PNJ, Supernovas, Hypernovas, Segaroth, Dieux. Attaques toujours annoncées au sol. | `character` |
| Suke | Tir systémique sur tout Diviniarth : reflet, visée, tir. Se déjoue par le couvert ou un pas de côté. | `suke` |
| Monde | Nébulosia (Overworld) et Diviniarth (dimension), reliés par la Traversée. 27 lieux bâtis par le mod. | `world` |
| Réactions | Deuil, alerte, traque, vengeance, succession, réputation par faction. | `faction` |
| Renaissance | Mort → Chronique → nom → Écho → retour au premier instant du monde. | `rebirth` |
| Chronique des Vies | Chaque vie est une étoile ; les vies forment une constellation. Épitaphes procédurales. | `chronicle`, `client` |

Rien n'est expliqué par un tutoriel : l'interface apparaît à mesure qu'elle a un sens
(le rang à la première absorption, les emplacements à la première technique, la Chronique à la première mort).

## Touches (modifiables dans Options → Contrôles → Trou-Noir)

| Touche | Action |
|---|---|
| R, V, G, B | Techniques 1 à 4 |
| K | Écran d'Absorption (après la première absorption) |
| J | Chronique des Vies (après la première mort) |

Traversée : clic droit sur le **gouvernail** du navire, au port (Havre-Gris à Nébulosia, Port-Écume à Diviniarth).

## Commandes de debug (`/trounoir`, niveau opérateur 2)

| Commande | Effet |
|---|---|
| `/trounoir status` | Vie en cours, rang, puissance, synergies, Fragilité restante, mode de renaissance. |
| `/trounoir absorb <id> [nombre]` | Absorbe un trait, une technique ou un savoir (ex. `trounoir:maitrise_de_cledith`). |
| `/trounoir fragility on\|off` | Active ou suspend la Fragilité divine pour cette vie. |
| `/trounoir rebirth` | Renaissance douce immédiate (sans mourir, sans effacer le monde). |
| `/trounoir site list` | Liste les lieux, leur position et s'ils sont bâtis. |
| `/trounoir site tp <id>` | Bâtit le lieu si besoin et s'y téléporte (ex. `trounoir:aiguille_de_veille`). |
| `/trounoir spawn <personnage>` | Fait apparaître un personnage (ex. `trounoir:cledith`). |
| `/trounoir reputation <faction> [delta]` | Lit ou modifie la réputation (`nebulosia`, `azalee`, `pantheon`, `sauvage`). |
| `/trounoir notoriety <0-100>` | Fixe la notoriété (attention de Suke). |
| `/trounoir hunt <faction>` | Déclenche une escouade de traque. |
| `/trounoir cross` | Effectue la Traversée sans passer par le port. |
| `/trounoir chronicle clear` | Efface Chronique, Échos et souvenirs du joueur. |

`/reload` recharge tout le contenu JSON sans redémarrer.

## Équilibrer sans recompiler

- Contenu : `src/main/resources/data/trounoir/trounoir/` (ou un datapack qui le surcharge)
  — `traits/`, `techniques/`, `knowledge/`, `synergies/`, `tags/`, `fragility/`, `profiles/` (mobs vanilla),
  `characters/`, `factions/`, `sites/`, `epitaphs/`.
- Réglages globaux : `config/trounoir-common.toml` (créé au premier lancement).
- Tables de valeurs et formules : `docs/EQUILIBRAGE.md`.

Le contenu d'origine est produit par `python3 tools/gen/gen.py` à partir des tables `tools/gen/content_*.py`
(pratique pour des changements en masse ; nécessite Pillow pour les textures). Les JSON restent modifiables à la main.

## Où sont les sauvegardes

- Vie en cours et état du monde : `<monde>/trounoir/` — effacés à chaque Renaissance.
- Chronique, Échos, souvenirs : `<dossier du jeu>/trounoir/memoires/<nom du monde>.json` — **hors du monde**, jamais effacés.

## Mondes préexistants

Le retour dans le passé efface réellement ce qui a été vécu dans le monde. Par sécurité, il n'est activé que pour
les mondes **créés avec le mod**. Sur un monde plus ancien, le mod passe en « renaissance douce »
(l'état géré par le mod est réinitialisé, le terrain reste). Détails et limites : `docs/CHOIX_TECHNIQUES.md`.

## Documentation

- `docs/AJOUTS_AU_CANON.md` — tout ce qui a été inventé pour compléter l'univers.
- `docs/EQUILIBRAGE.md` — formules, réglages, tables de valeurs.
- `docs/CHOIX_TECHNIQUES.md` — décisions techniques, écarts de design et limites connues.
