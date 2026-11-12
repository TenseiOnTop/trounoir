# Équilibrage

Deux endroits où régler le jeu, sans recompiler :

1. **`config/trounoir-common.toml`** — les réglages globaux (tableau ci-dessous).
2. **Les JSON du datapack** — `data/trounoir/trounoir/…` : chaque trait, technique, créature, personnage, lieu.
   Après une modification en jeu : `/reload`.

Les valeurs n'ont pas été éprouvées par des parties réelles (le mod n'a pas pu être lancé dans l'environnement où il a été écrit) :
elles sont cohérentes entre elles et calculées, mais attendez-vous à devoir les ajuster.

## Principes

- **Des nombres lisibles.** Dégâts et points de vie restent dans l'échelle de Minecraft (un cœur = 2). Higgs a 1000 PV, un coup de Dieu fait 30 à 50.
  La puissance du joueur est un seul nombre, son rang un chiffre romain.
- **Une vie compte plus que les Échos.** Un Écho vaut la moitié d'un trait, plafonné au niveau II, sans compter pour les synergies.
- **Aucune voie dominante.** Chaque étiquette a sa synergie ; les plafonds (réduction de dégâts 60 %, recharge 50 %, discrétion 80 %) empêchent l'empilement.
- **Pas de ferme.** Niveau maximal par trait, créatures de spawner exclues, part de dégâts du joueur exigée.

## Formules

| Sujet | Formule |
|---|---|
| Niveau d'un trait | atteint après 1, 3, 6, 10, 15 absorptions (niveau × (niveau + 1) / 2), jusqu'à `max_level` |
| Valeur d'un niveau | valeur × (1 + d + d² + …), `d = levelDecay` (0,7) → niveaux I à V : ×1 ; ×1,7 ; ×2,19 ; ×2,53 ; ×2,77 |
| Puissance | somme des `power` × même facteur, + puissance des synergies actives, + Échos à `echoStrength` |
| Rang | nombre de seuils de `rankPowerThresholds` atteints |
| Dégâts infligés | base × (1 + bonus de statistiques) × faiblesse (×1,5) × multiplicateur d'écart de rang |
| Dégâts subis | base × multiplicateur d'écart de rang × (1 − réduction, plafonnée) |
| Technique | dégâts × (1 + 15 % par niveau au-delà du premier) × (1 + `magic_power` %) ; recharge × (1 − réduction) × (1 − 6 % par niveau) |
| Handicap restant | 1 − (points de soulagement / `relief_needed`) |

### Rangs

Seuils de puissance par défaut : **8, 25, 60, 120, 200, 300, 420, 560, 720, 900** (rangs I à X).

Écart de rang (attaquant − cible), de −3 ou moins à +3 ou plus :

| Écart | −3 | −2 | −1 | 0 | +1 | +2 | +3 |
|---|---|---|---|---|---|---|---|
| Dégâts infligés par le joueur | ×0,1 (« vos coups ne portent pas ») | ×0,4 | ×0,75 | ×1 | ×1,25 | ×1,6 | ×2,5 |
| Dégâts subis par le joueur (écart = attaquant − joueur) | ×0,3 | ×0,55 | ×0,8 | ×1 | ×1,25 | ×1,6 | ×2,5 |

Repères : au rang 0, un chevalier (III) est intouchable et un loup (I) fait 25 % de dégâts en plus.
Les mobs vanilla au maximum mènent vers le rang V ou VI ; Nébulosia entière vers VI ; les demi-dieux vers VII ;
l'Empire entier vers IX ; il faut plusieurs Dieux pour atteindre X (voir le budget de puissance en fin de document).

### Statistiques du mod (clé `stats` des JSON)

`melee_damage_pct`, `ranged_damage_pct`, `all_damage_pct`, `backstab_pct` (dégâts en plus, en %) ·
`magic_power` (puissance des techniques, %) · `cooldown_reduction_pct` (plafond 50) · `damage_reduction_pct` (plafond 60) ·
`explosion_reduction_pct`, `fall_reduction_pct` · `lifesteal_pct` (4 PV au plus par coup) · `thorns` (dégâts renvoyés) ·
`regen` (PV toutes les 5 s) · `stealth` (réduction de la distance de détection, plafond 80) · `perception_range` (blocs ; fait luire les créatures) ·
`fire_immunity`, `fire_touch`, `frost_touch`, `poison_touch`, `wither_touch`, `sniper` (actifs à partir de 1).

## Réglages (`trounoir-common.toml`)

| Clé | Défaut | Rôle |
|---|---|---|
| `absorption.levelDecay` | 0.7 | Rendement décroissant des niveaux. |
| `absorption.spawnerAbsorptionChance` | 0.0 | Chance d'absorber un mob de spawner. |
| `absorption.minPlayerDamageShare` | 0.5 | Part de la vie de la victime que le joueur doit avoir retirée. |
| `fragilite.hungerExhaustionPerSecond` | 0.12 | Faim supplémentaire tant que le handicap n'est pas soulagé. |
| `echos.echoStrength` | 0.5 | Force d'un Écho. |
| `echos.echoMaxLevel` | 2 | Niveau maximal conservé. |
| `echos.echoCooldownFactor` | 2.0 | Recharge d'une technique tenue seulement par un Écho. |
| `echos.echoSlotThresholds` | 0, 3, 7, 12, 20, 30 | Morts nécessaires pour chaque emplacement d'Écho. |
| `rangs.rankPowerThresholds` | 8 … 900 | Puissance des rangs I à X. |
| `rangs.outgoingGapMultipliers` / `incomingGapMultipliers` | voir tableau | Écarts de rang. |
| `rangs.damageReductionCap` / `cooldownReductionCap` | 60 / 50 | Plafonds. |
| `rangs.techniqueCooldownScale` | 1.0 | Multiplicateur global des recharges. |
| `suke.threshold` | 20 | Notoriété à partir de laquelle Suke tire. |
| `suke.damage` | 18 | Dégâts d'un tir avant écart de rang (Suke est rang VI : ×2,5 pour un joueur de rang III ou moins). |
| `suke.telegraphTicks` | 50 | Durée du reflet (2,5 s). |
| `suke.minInterval` / `maxInterval` | 240 / 1200 | Délai entre deux tirs à notoriété 100 / au seuil. |
| `suke.duelRadius` | 70 | Près de sa tour, Suke se bat en personne. |
| `monde.siteDistanceScale` | 1.0 | Rapproche ou éloigne tous les lieux. |
| `monde.siteBuildRadius` | 150 | Distance de construction d'un lieu. |
| `monde.hostileReputation` / `allyReputation` | −30 / 30 | Seuils de réputation. |
| `monde.huntsEnabled` | true | Escouades de traque. |
| `renaissance.mode` | AUTO | AUTO, WORLD_RESET ou SOFT (voir CHOIX_TECHNIQUES.md). |
| `renaissance.familiarityTelegraphBonus` | 0.2 | Annonces plus longues pour un boss dont la faiblesse est connue. |

## Notoriété et réputation

- Notoriété : +4 par citoyen tué, +6 par légionnaire ou tirailleur, +10 par prétorien ou magistère, +30 par Hypernova, +50 pour Segaroth ;
  +2 toutes les 10 s dans un lieu impérial quand l'Empire est hostile ; −1 toutes les 10 s loin des lieux impériaux.
- Réputation : de −100 à +100 par faction. Tuer un paysan −10, un soldat −12, un chevalier ou un mage −15, un Supernova −40 puis −20 de réaction,
  le Roi −60 puis −40. Tuer un monstre près d'un lieu habité : +1. Tuer Gorgath ou la Veuve Blême : +25 auprès de Nébulosia.

## Attaques des personnages

Chaque attaque (`moves` dans `characters/*.json`) : `shape` (circle, line, cone), `size`, `width`, `angle`, `telegraph` (ticks d'annonce),
`cooldown`, `min_range` / `max_range`, `damage`, `knockback`, `pull`, `effects`, `pre` (teleport, dash, retreat), `projectile`, `summon`,
`heal`, `time_stop`, `min_phase` / `max_phase`, `weight`. Allonger `telegraph` rend un boss plus lisible ; c'est le premier réglage à essayer.

<!-- TABLES -->
<!-- Tableaux générés par tools/gen/gen.py : ne pas modifier à la main, relancer le script. -->

### Traits

| Trait | Étiquettes | Niv. max | Puissance / niv. | Par niveau (avant rendement décroissant) |
|---|---|---|---|---|
| Essence inconnue | bete | 3 | 0.5 | PV +1 |
| Chair rustique | endurance, bete | 5 | 1 | PV +2, soulage faim 1 |
| Vigueur du mort | mort, endurance | 5 | 1.5 | PV +2, soulage faim 1, soulage faiblesse 0.5 |
| Cuir épais | endurance, bete | 4 | 1.5 | PV +2, armure +1 |
| Carapace | endurance | 4 | 2 | armure +2, rés. recul +0.05 |
| Masse de fer | endurance | 2 | 6 | PV +6, rés. recul +0.2, dégâts +2, soulage faiblesse 1 |
| Colosse | endurance, bete | 4 | 3 | PV +4, dégâts +1, soulage faiblesse 1 |
| Patte légère | vitesse, bete | 5 | 1 | vitesse +4 %, soulage lenteur 1 |
| Galop | vitesse, bete | 4 | 1.5 | vitesse +5 %, enjambée +0.1, soulage lenteur 1.5 |
| Instinct de meute | bete, vitesse | 5 | 1.5 | dégâts +1, vitesse +3 %, soulage faiblesse 1, soulage lenteur 1, soulage maladresse 1 |
| Nage | bete | 3 | 0.5 | nage +0.2, souffle +1 |
| Souffle marin | bete, endurance | 2 | 1.5 | PV +2, water_breathing (niv. 1+) |
| Croc et griffe | bete, lame | 5 | 1.5 | dégâts +1, soulage faiblesse 1.5 |
| Force brute | endurance, lame | 5 | 2 | dégâts +1.5, soulage faiblesse 1.5, soulage maladresse 0.5 |
| Fouisseur | bete | 4 | 1 | minage +10 %, soulage engourdissement 1.5 |
| Labeur humain | endurance | 4 | 1 | minage +15 %, soulage engourdissement 2, soulage faim 1, soulage maladresse 1 |
| Yeux de nuit | perception, ombre | 3 | 1.5 | perception_range +6, night_vision (niv. 2+) |
| Écholocation | perception | 3 | 1.5 | perception_range +12 |
| Œil d'aigle | perception, distance | 5 | 1.5 | ranged_damage_pct +6 |
| Pas feutré | ombre | 5 | 1.5 | vit. accroupi +0.08, stealth +10 |
| Voile d'Ender | ombre, arcane | 3 | 2.5 | stealth +8, magic_power +4 |
| Sang de braise | feu | 3 | 2.5 | durée de brûlure -25 %, fire_immunity +0.46, fire_resistance (niv. 3+) |
| Os secs | mort, distance | 5 | 1.5 | ranged_damage_pct +6, soulage maladresse 0.5 |
| Givre errant | givre | 3 | 2 | armure +1, frost_touch +0.5 |
| Venin | venin, bete | 3 | 2 | poison_touch +0.6 |
| Flétrissure | mort | 3 | 3 | dégâts +1, wither_touch +0.6 |
| Sang arcanique | arcane | 5 | 2 | magic_power +8, cooldown_reduction_pct +3 |
| Esprit malin | arcane, vitesse | 3 | 1.5 | vitesse +2 %, magic_power +4 |
| Poudre vive | feu | 3 | 1.5 | explosion_reduction_pct +20 |
| Ressort | vitesse, bete | 4 | 1 | saut +0.05, chute sûre +1.5, fall_reduction_pct +10, soulage lenteur 0.5 |
| Régénération lente | endurance | 4 | 2 | regen +0.5 |
| Sens du troc | perception | 3 | 0.5 | chance +0.5, soulage faim 0.5 |
| Épines | endurance | 3 | 1.5 | thorns +1 |
| Souffle vif | vitesse | 3 | 2 | vitesse +3 %, saut +0.04, fall_reduction_pct +20, soulage lenteur 1 |
| Vol plané | vitesse, perception | 3 | 1.5 | chute sûre +3, fall_reduction_pct +20 |
| Regard ancien | perception, arcane | 1 | 8 | thorns +2, perception_range +16, magic_power +8 |
| Ossature abyssale ★ | endurance, ombre | 1 | 14 | PV +10, armure +4, rés. recul +0.3 |
| Couronne flétrie ★ | mort, arcane | 1 | 14 | PV +8, magic_power +20, wither_touch +1 |
| Sang de dragon ★ | feu, arcane | 1 | 18 | PV +12, dégâts +3, fall_reduction_pct +100, damage_reduction_pct +5 |
| Discipline | lame, endurance | 5 | 2 | armure +1, dégâts +0.5, soulage faiblesse 1, soulage maladresse 1 |
| Art de l'épée | lame | 5 | 3 | vit. d'attaque +3 %, melee_damage_pct +6, soulage maladresse 1 |
| Discipline impériale | lame, endurance | 5 | 3.5 | PV +2, armure +1, melee_damage_pct +4 |
| Canalisation | arcane | 5 | 3.5 | magic_power +10, cooldown_reduction_pct +4 |
| Tir tendu | distance | 5 | 3 | ranged_damage_pct +8 |
| Sang royal ★ | astral | 1 | 4 | PV +4, chance +1 |
| Flamme d'Aldric ★ | lame, feu, astral | 1 | 8 | dégâts +3, melee_damage_pct +15, fire_touch +1 |
| Givre de Lysmène ★ | givre, arcane, astral | 1 | 8 | armure +2, magic_power +20, frost_touch +1 |
| Ombre de Corvin ★ | ombre, vitesse, astral | 1 | 8 | vitesse +10 %, stealth +25, backstab_pct +40 |
| Corps de fer de Hestia ★ | endurance, astral | 1 | 8 | PV +10, armure +4, rés. recul +0.3, damage_reduction_pct +8 |
| Ambition de Garmond ★ | lame, astral | 1 | 6 | dégâts +2, melee_damage_pct +8 |
| Fureur de Gorgath ★ | bete, endurance | 1 | 16 | PV +12, dégâts +3, rés. recul +0.3, soulage faiblesse 6 |
| Toile de la Veuve Blême ★ | venin, ombre | 1 | 16 | PV +6, vitesse +6 %, poison_touch +1, stealth +20 |
| Chaînes du Geôlier ★ | ombre, endurance | 1 | 24 | PV +16, armure +4, damage_reduction_pct +10, perception_range +24 |
| Cœur de Sombrefer ★ | endurance | 1 | 24 | PV +20, armure +6, rés. recul +0.5, dégâts +4 |
| Sceptre du Roi Flétri ★ | mort, lame | 1 | 24 | dégâts +5, wither_touch +1, lifesteal_pct +8, melee_damage_pct +15 |
| Maîtrise de Cledith ★ | lame, astral | 1 | 14 | dégâts +6, vit. d'attaque +25 %, balayage +1, melee_damage_pct +50, soulage maladresse 4, soulage faiblesse 6 |
| Œil de Suke ★ | distance, perception, astral | 1 | 14 | allonge +2, ranged_damage_pct +60, perception_range +96, sniper +1 |
| Brasier d'Ignarelle ★ | feu, arcane, astral | 1 | 14 | magic_power +35, fire_immunity +1, fire_touch +1 |
| Linceul de Noctalys ★ | ombre, lame, astral | 1 | 14 | vitesse +12 %, stealth +50, backstab_pct +80, night_vision (niv. 1+) |
| Rempart de Bragor ★ | endurance, astral | 1 | 14 | PV +20, armure +8, rés. recul +0.6, damage_reduction_pct +15, thorns +3 |
| Foudre de Zéphyrine ★ | vitesse, lame, astral | 1 | 14 | vitesse +20 %, vit. d'attaque +20 %, saut +0.1, fall_reduction_pct +100, melee_damage_pct +15, soulage lenteur 6 |
| Géométrie d'Oriel ★ | arcane, astral | 1 | 14 | portée blocs +2, cooldown_reduction_pct +20, magic_power +20 |
| Commandement de Valdrek ★ | endurance, lame, astral | 1 | 14 | PV +10, armure +3, all_damage_pct +12, regen +1 |
| Audace de Kael ★ | lame, astral | 1 | 10 | dégâts +3, PV +6, melee_damage_pct +12 |
| Immortalité de Segaroth ★ | endurance, arcane, astral | 1 | 30 | PV +30, regen +3, damage_reduction_pct +12, lifesteal_pct +6 |
| Balance de Saturne ★ | divin | 1 | 40 | PV +16, thorns +6, damage_reduction_pct +12 |
| Rouille et sang de Mars ★ | divin, lame | 1 | 40 | dégâts +8, lifesteal_pct +12, melee_damage_pct +30, wither_touch +1 |
| Voile de Séléné ★ | divin, ombre | 1 | 40 | PV +12, stealth +60, regen +3, cooldown_reduction_pct +15, night_vision (niv. 1+) |
| Cadence de Pulsar ★ | divin, vitesse | 1 | 40 | vit. d'attaque +50 %, vitesse +15 %, cooldown_reduction_pct +25 |
| Sillage de Comète ★ | divin, vitesse, givre | 1 | 40 | vitesse +35 %, saut +0.2, fall_reduction_pct +100, frost_touch +1, soulage lenteur 6 |
| Couronne de Sol ★ | divin, feu | 1 | 48 | PV +16, fire_immunity +1, fire_touch +1, magic_power +40, all_damage_pct +15, night_vision (niv. 1+) |
| Berceau de Nébula ★ | divin, arcane | 1 | 48 | PV +30, regen +4, magic_power +30 |
| Voracité de Quasar ★ | divin, distance | 1 | 48 | ranged_damage_pct +80, lifesteal_pct +10, all_damage_pct +20, perception_range +48 |
| Gémellité ★ | divin, arcane | 1 | 48 | PV +20, cooldown_reduction_pct +20, damage_reduction_pct +15 |
| Masse primordiale ★ | divin, endurance | 1 | 70 | PV +40, armure +10, rés. recul +1, dégâts +10, damage_reduction_pct +20 |

★ = capacité signature d'un personnage unique.

### Techniques

| Technique | Type | Recharge (s) | Faim | Puissance | Paramètres principaux |
|---|---|---|---|---|---|
| Bond | leap | 8 | 0 | 1.5 | strength 0.9 |
| Charge | dash | 10 | 1 | 2 | damage 5, strength 1.5 |
| Détonation | aoe | 25 | 2 | 3 | damage 12, radius 4.5, knockback 1 |
| Clignement | blink | 12 | 1 | 3 | range 12 |
| Salve de braise | projectile | 9 | 1 | 3 | count 3 |
| Boule de feu | projectile | 14 | 2 | 3.5 |  |
| Crâne flétri | projectile | 10 | 2 | 5 |  |
| Onde sonique ★ | beam | 20 | 3 | 8 | damage 16, range 18 |
| Souffle du dragon ★ | aoe | 18 | 3 | 8 | damage 14, radius 9, cone_angle 50 |
| Rayon du gardien | beam | 12 | 2 | 4 | damage 8, range 20 |
| Rugissement | aoe | 20 | 2 | 4 | damage 4, radius 6, knockback 1.6 |
| Crocs d'évocateur | beam | 10 | 1 | 3.5 | damage 7, range 10 |
| Voile illusoire | buff | 45 | 2 | 3 |  |
| Rafale | aoe | 10 | 1 | 3 | damage 2, radius 5, knockback 2 |
| Souffle de givre | aoe | 12 | 1 | 3 | damage 5, radius 7, cone_angle 60 |
| Breuvage | buff | 40 | 1 | 3 | heal 6 |
| Estoc | dash | 8 | 1 | 3 | damage 7, strength 1.3 |
| Trait arcanique | beam | 6 | 1 | 3 | damage 7, range 24 |
| Carreau lourd | projectile | 5 | 0 | 3 | damage 6 |
| Lame solaire ★ | aoe | 10 | 1 | 4 | damage 12, radius 5, cone_angle 120, ignite 5 |
| Prison de glace ★ | aoe | 18 | 2 | 4 | damage 8, radius 7 |
| Pas de l'ombre ★ | blink | 6 | 0 | 4 | range 16 |
| Peau de fer ★ | buff | 40 | 2 | 4 |  |
| Mille lames ★ | aoe | 9 | 1 | 6 | damage 22, radius 6, knockback 0.6 |
| Tir Hypernova ★ | beam | 15 | 1 | 6 | damage 40, range 256 |
| Fournaise ★ | aoe | 16 | 2 | 6 | damage 18, radius 9, ignite 8 |
| Éclipse ★ | buff | 30 | 1 | 6 |  |
| Bastion ★ | buff | 45 | 2 | 6 | heal 10 |
| Éclair blanc ★ | dash | 5 | 0 | 6 | damage 16, strength 2.6 |
| Pli spatial ★ | blink | 4 | 0 | 6 | range 28 |
| Ordre de charge ★ | buff | 40 | 2 | 6 |  |
| Mille ans ★ | buff | 90 | 3 | 10 | heal 40 |
| Verdict ★ | aoe | 20 | 2 | 15 | damage 30, radius 10 |
| Marée de sang ★ | aoe | 14 | 2 | 15 | damage 26, radius 8, heal 10 |
| Marée lunaire ★ | buff | 50 | 2 | 15 | heal 20 |
| Pulsation ★ | aoe | 6 | 1 | 15 | damage 14, radius 8, knockback 1.4 |
| Traînée ★ | dash | 4 | 0 | 15 | damage 20, strength 3.2 |
| Zénith ★ | aoe | 22 | 3 | 17 | damage 38, radius 14, ignite 10 |
| Genèse ★ | buff | 60 | 3 | 17 | heal 60 |
| Jet relativiste ★ | beam | 12 | 2 | 17 | damage 45, range 96 |
| Arrêt du temps ★ | time_stop | 60 | 3 | 17 | radius 32, duration 120 |
| Effondrement ★ | aoe | 25 | 3 | 20 | damage 50, radius 16, pull 2.5 |

### Synergies

| Synergie | Condition | Bonus | Puissance |
|---|---|---|---|
| Maître-lame | 3 × lame | vit. d'attaque +5 %, melee_damage_pct +10 | 4 |
| Fournaise intérieure | 3 × feu | fire_immunity +1, fire_touch +1 | 4 |
| Prédateur nocturne | 2 × ombre + 2 × perception | stealth +20, backstab_pct +30 | 4 |
| Inébranlable | 4 × endurance | rés. recul +0.2, damage_reduction_pct +8 | 5 |
| Meute sauvage | 4 × bete | PV +4, vitesse +5 % | 4 |
| Œil du tireur | 2 × distance + 2 × perception | ranged_damage_pct +15 | 4 |
| Archimage | 3 × arcane | cooldown_reduction_pct +10, magic_power +10 | 5 |
| Légion des morts | 3 × mort | lifesteal_pct +5 | 4 |
| Hiver éternel | 2 × givre | armure +2, frost_touch +1 | 4 |
| Éclair | 3 × vitesse | vitesse +8 %, vit. d'attaque +8 % | 4 |
| Sang noir | 2 × venin | poison_touch +1, regen +0.5 | 3 |
| Lame ardente | 2 × lame + 2 × feu | fire_touch +1, melee_damage_pct +6 | 4 |
| Ombre tranchante | 2 × lame + 2 × ombre | backstab_pct +40 | 4 |
| Étoile montante | 3 × astral | PV +6, all_damage_pct +8 | 8 |
| Tueur de Supernovas | flamme_d_aldric + givre_de_lysmene + ombre_de_corvin + corps_de_hestia | PV +8, all_damage_pct +10 | 10 |
| Sang divin | 1 × divin | PV +4, regen +1 | 6 |
| Panthéon | 3 × divin | damage_reduction_pct +10, all_damage_pct +10 | 14 |
| Théophage | 6 × divin | PV +20, all_damage_pct +20, cooldown_reduction_pct +10 | 30 |
| Les Jumeaux | gemellite | magic_power +15 | 6 |

### Fragilité divine

| Handicap | Effet au départ | Points de soulagement pour l'effacer |
|---|---|---|
| Cœur unique (`coeur`) | max_health -18 | jamais : les traits d'endurance rendent des cœurs |
| Lenteur (`lenteur`) | movement_speed -0.3 | 6 |
| Faiblesse (`faiblesse`) | attack_damage -0.5 | 6 |
| Mains gourdes (`engourdissement`) | block_break_speed -0.5 | 5 |
| Maladresse (`maladresse`) | attack_speed -0.25 | 4 |
| Faim dévorante (`faim`) | faim accélérée | 5 |

### Mobs vanilla

| Créature | Rang | Gains (probabilité) |
|---|---|---|
| allay | 0 | esprit_malin |
| armadillo | 0 | carapace |
| axolotl | 0 | regeneration_lente, nage |
| bat | 0 | echolocation |
| bee | 0 | venin |
| chicken | 0 | chair_rustique, patte_legere (50 %) |
| cod | 0 | nage |
| endermite | 0 | fouisseur, voile_d_ender (30 %) |
| frog | 0 | ressort, bond (40 %) |
| glow_squid | 0 | nage, yeux_de_nuit (50 %) |
| parrot | 0 | oeil_d_aigle |
| pufferfish | 0 | venin, epines (50 %) |
| rabbit | 0 | patte_legere, ressort (50 %), bond (30 %) |
| salmon | 0 | nage |
| silverfish | 0 | fouisseur |
| snow_golem | 0 | givre_errant |
| squid | 0 | nage |
| tadpole | 0 | nage |
| tropical_fish | 0 | nage |
| bogged | 1 | os_secs, venin |
| camel | 1 | galop, cuir_epais |
| cat | 1 | patte_legere, pas_feutre, yeux_de_nuit (50 %) |
| cave_spider | 1 | venin, yeux_de_nuit |
| cow | 1 | chair_rustique |
| creeper | 1 | poudre_vive, detonation (60 %) |
| dolphin | 1 | nage, echolocation |
| donkey | 1 | cuir_epais, galop (50 %) |
| drowned | 1 | vigueur_morte, souffle_marin (50 %) |
| fox | 1 | patte_legere, pas_feutre |
| goat | 1 | ressort, charge (60 %) |
| horse | 1 | galop, cuir_epais (50 %) |
| husk | 1 | vigueur_morte |
| llama | 1 | cuir_epais |
| mooshroom | 1 | chair_rustique, regeneration_lente (30 %) |
| mule | 1 | cuir_epais, galop (50 %) |
| ocelot | 1 | patte_legere, pas_feutre, yeux_de_nuit (50 %) |
| phantom | 1 | plane, oeil_d_aigle |
| pig | 1 | chair_rustique |
| sheep | 1 | chair_rustique |
| skeleton | 1 | os_secs |
| skeleton_horse | 1 | galop, os_secs (50 %) |
| slime | 1 | ressort |
| sniffer | 1 | fouisseur, cuir_epais |
| spider | 1 | yeux_de_nuit, bond (60 %) |
| stray | 1 | os_secs, givre_errant, souffle_de_givre (50 %) |
| strider | 1 | sang_de_braise |
| trader_llama | 1 | cuir_epais |
| turtle | 1 | carapace, nage |
| villager | 1 | labeur_humain, sens_du_troc, savoir_artisan (50 %) |
| wandering_trader | 1 | sens_du_troc, carte_du_havre (60 %) |
| wolf | 1 | meute, croc_et_griffe (50 %) |
| zombie | 1 | vigueur_morte |
| zombie_horse | 1 | galop, vigueur_morte (50 %) |
| zombie_villager | 1 | vigueur_morte, labeur_humain (30 %) |
| zombified_piglin | 1 | vigueur_morte, force_brute (50 %) |
| blaze | 2 | sang_de_braise, salve_de_braise (70 %) |
| breeze | 2 | souffle_vif, rafale (70 %) |
| enderman | 2 | voile_d_ender, clignement (70 %) |
| guardian | 2 | epines, souffle_marin, rayon_gardien (40 %) |
| hoglin | 2 | colosse, charge (60 %) |
| magma_cube | 2 | sang_de_braise, ressort |
| panda | 2 | colosse |
| piglin | 2 | sens_du_troc, force_brute (50 %) |
| pillager | 2 | oeil_d_aigle, tir_tendu (50 %) |
| polar_bear | 2 | colosse, givre_errant, croc_et_griffe |
| shulker | 2 | carapace |
| vex | 2 | esprit_malin |
| vindicator | 2 | force_brute |
| witch | 2 | sang_arcanique, regeneration_lente, breuvage (60 %), alchimie (50 %) |
| zoglin | 2 | colosse, vigueur_morte |
| evoker | 3 | sang_arcanique, crocs |
| ghast | 3 | boule_de_feu, regeneration_lente |
| giant | 3 | colosse, vigueur_morte |
| illusioner | 3 | sang_arcanique, voile_illusoire |
| iron_golem | 3 | masse_de_fer |
| piglin_brute | 3 | force_brute, discipline |
| ravager | 3 | colosse, rugissement |
| wither_skeleton | 3 | fletrissure, force_brute (50 %) |
| elder_guardian | 4 | regard_ancien, rayon_gardien, epines |
| wither | 5 | couronne_fletrie, crane_fletri, fletrissure |
| ender_dragon | 6 | sang_de_dragon, souffle_du_dragon |
| warden | 6 | ossature_abyssale, onde_sonique, echolocation |

### Personnages

| Personnage | Catégorie | Rang | PV | Dégâts | Armure | Faiblesse | Attaques |
|---|---|---|---|---|---|---|---|
| Paysan | peasant | 1 | 20 | 2 | 0 | — | IA vanilla |
| Soldat de Nébulosia | soldier | 2 | 28 | 5 | 6 | — | slash (5 dég., annonce 16 t) |
| Chevalier | knight | 3 | 44 | 8 | 12 | — | slash (8 dég., annonce 14 t), lunge (10 dég., annonce 20 t) |
| Mage royal | mage | 3 | 30 | 7 | 2 | — | shot (0 dég., annonce 16 t), slam (8 dég., annonce 26 t) |
| Aurèle III, Roi de Nébulosia | king | 2 | 40 | 4 | 8 | — | slash (4 dég., annonce 18 t) |
| Aldric Solbrand | supernova | 4 | 140 | 11 | 14 | magic | slash (11 dég., annonce 14 t), lunge (13 dég., annonce 20 t), solar_ring (14 dég., annonce 26 t) |
| Lysmène Givreclair | supernova | 4 | 110 | 12 | 6 | fire | shot (0 dég., annonce 16 t), ice_rain (12 dég., annonce 30 t), ice_prison (10 dég., annonce 28 t) |
| Corvin Brumeval | supernova | 4 | 100 | 12 | 6 | explosion | slash (10 dég., annonce 10 t), ambush (14 dég., annonce 16 t), knives (8 dég., annonce 14 t) |
| Hestia Ferrecœur | supernova | 4 | 180 | 12 | 18 | projectile | hook (12 dég., annonce 14 t), slam (15 dég., annonce 22 t), rush (14 dég., annonce 18 t) |
| Garmond l'Aspirant | supernova | 4 | 110 | 10 | 12 | backstab | slash (10 dég., annonce 14 t), lunge (12 dég., annonce 20 t) |
| Gorgath, le Ravageur ancien | demigod | 5 | 240 | 16 | 10 | fire | IA vanilla |
| La Veuve Blême | demigod | 5 | 200 | 14 | 8 | fire | IA vanilla |
| Citoyen d'Azalée | peasant | 1 | 20 | 2 | 0 | — | IA vanilla |
| Légionnaire | soldier | 4 | 60 | 11 | 14 | — | slash (11 dég., annonce 13 t), lunge (13 dég., annonce 18 t) |
| Tirailleur | soldier | 4 | 44 | 9 | 8 | — | shot (9 dég., annonce 16 t), volley (7 dég., annonce 22 t) |
| Prétorien | knight | 5 | 90 | 15 | 18 | — | slash (15 dég., annonce 12 t), lunge (17 dég., annonce 18 t), slam (18 dég., annonce 22 t) |
| Magistère | mage | 5 | 64 | 13 | 6 | — | shot (0 dég., annonce 14 t), meteors (15 dég., annonce 28 t), fold (12 dég., annonce 18 t) |
| Cledith | hypernova | 6 | 340 | 22 | 20 | projectile | slash (22 dég., annonce 12 t), crescent (26 dég., annonce 18 t), lunge (26 dég., annonce 16 t), flash_step (24 dég., annonce 14 t), thousand_blades (30 dég., annonce 24 t) |
| Suke | hypernova | 6 | 260 | 20 | 10 | melee | rifle (24 dég., annonce 22 t), scatter (18 dég., annonce 30 t), retreat (0 dég., annonce 10 t), mortar (20 dég., annonce 26 t) |
| Ignarelle | hypernova | 6 | 280 | 22 | 10 | melee | shot (0 dég., annonce 14 t), meteors (24 dég., annonce 28 t), flame_cone (22 dég., annonce 20 t), furnace (28 dég., annonce 30 t) |
| Noctalys | hypernova | 6 | 250 | 24 | 8 | explosion | slash (22 dég., annonce 9 t), ambush (28 dég., annonce 14 t), knives (16 dég., annonce 12 t), eclipse (32 dég., annonce 12 t) |
| Bragor | hypernova | 6 | 420 | 20 | 26 | magic | shield_bash (20 dég., annonce 16 t), slam (26 dég., annonce 24 t), bull_rush (24 dég., annonce 22 t), second_wind (0 dég., annonce 30 t) |
| Zéphyrine | hypernova | 6 | 260 | 21 | 10 | backstab | thrust (18 dég., annonce 8 t), lunge (24 dég., annonce 12 t), white_lightning (28 dég., annonce 10 t), storm (22 dég., annonce 20 t) |
| Oriel | hypernova | 6 | 250 | 20 | 8 | melee | ray (20 dég., annonce 22 t), meteors (22 dég., annonce 26 t), fold_away (0 dég., annonce 10 t), lattice (24 dég., annonce 28 t) |
| Maréchal Valdrek | hypernova | 6 | 320 | 20 | 20 | backstab | slash (20 dég., annonce 14 t), slam (24 dég., annonce 24 t), call (0 dég., annonce 30 t), call_guard (0 dég., annonce 30 t), lunge (26 dég., annonce 18 t) |
| Segaroth | emperor | 7 | 520 | 28 | 22 | backstab | slash (28 dég., annonce 13 t), lunge (32 dég., annonce 16 t), meteors (30 dég., annonce 26 t), ambush (34 dég., annonce 14 t), call (0 dég., annonce 30 t), millennium (40 dég., annonce 30 t), undying (0 dég., annonce 40 t) |
| Kael l'Usurpateur | hypernova | 6 | 300 | 22 | 18 | magic | slash (22 dég., annonce 13 t), lunge (25 dég., annonce 18 t), call (0 dég., annonce 30 t) |
| Le Geôlier d'Andromède | demigod | 6 | 420 | 26 | 14 | projectile | IA vanilla |
| Le Colosse de Sombrefer | demigod | 6 | 480 | 26 | 22 | magic | IA vanilla |
| Le Roi Flétri | demigod | 6 | 400 | 26 | 16 | fire | IA vanilla |
| Séléné | god | 8 | 620 | 30 | 20 | fire | moonfall (30 dég., annonce 28 t), new_moon (34 dég., annonce 16 t), moonbeam (32 dég., annonce 26 t), high_tide (40 dég., annonce 32 t), full_moon (0 dég., annonce 40 t) |
| Comète | god | 8 | 560 | 30 | 20 | explosion | streak (32 dég., annonce 12 t), slash (28 dég., annonce 10 t), ice_tail (30 dég., annonce 24 t), perihelion (38 dég., annonce 10 t) |
| Mars | god | 8 | 700 | 34 | 20 | magic | slash (34 dég., annonce 13 t), slam (38 dég., annonce 22 t), lunge (40 dég., annonce 16 t), rust_breath (30 dég., annonce 18 t), blood_tide (46 dég., annonce 26 t) |
| Saturne | god | 8 | 680 | 32 | 20 | backstab | sentence (34 dég., annonce 26 t), rings (32 dég., annonce 28 t), gavel (36 dég., annonce 24 t), verdict (44 dég., annonce 34 t) |
| Pulsar | god | 8 | 600 | 30 | 20 | projectile | beat (26 dég., annonce 14 t), off_beat (32 dég., annonce 20 t), lighthouse (34 dég., annonce 20 t), syncopation (30 dég., annonce 22 t) |
| Sol | god | 9 | 820 | 38 | 20 | magic | shot (0 dég., annonce 18 t), flares (38 dég., annonce 28 t), corona (40 dég., annonce 20 t), noon (46 dég., annonce 30 t), zenith (60 dég., annonce 44 t) |
| Nébula | god | 9 | 900 | 34 | 20 | melee | stellar_nursery (34 dég., annonce 30 t), veil (36 dég., annonce 18 t), cradle (40 dég., annonce 32 t), rebirth (0 dég., annonce 40 t) |
| Quasar | god | 9 | 780 | 40 | 20 | backstab | jet (42 dég., annonce 24 t), wide_jet (34 dég., annonce 34 t), accretion (38 dég., annonce 26 t), maw (44 dég., annonce 28 t) |
| Trou-Blanc | god | 9 | 860 | 38 | 20 | melee | slash (36 dég., annonce 13 t), outpour (38 dég., annonce 24 t), time_stop (0 dég., annonce 40 t), mirror_step (42 dég., annonce 12 t), everything_returns (40 dég., annonce 26 t) |
| Higgs | god | 10 | 1000 | 50 | 20 | backstab | backhand (48 dég., annonce 14 t), weight (54 dég., annonce 22 t), collapse (40 dég., annonce 34 t), mass_drop (50 dég., annonce 28 t), field (56 dég., annonce 30 t), inevitable (60 dég., annonce 18 t), condemnation (70 dég., annonce 50 t) |

### Lieux

| Lieu | Continent | Plan | Distance | Angle | Habitants |
|---|---|---|---|---|---|
| Bourg-Levant | Nébulosia | village | 180 | 40° | paysan × 6, soldat × 1 |
| Clairmoulin | Nébulosia | village | 520 | 200° | paysan × 8, soldat × 2, chevalier × 1, corvin × 1 |
| Château d'Astrelune | Nébulosia | castle | 850 | 120° | roi_aurele × 1, aldric × 1, lysmene × 1, hestia × 1, chevalier × 6, mage_royal × 3, soldat × 6 |
| Havre-Gris | Nébulosia | harbor | 1300 | 300° | soldat × 2, paysan × 4 |
| Lande de Gorgath | Nébulosia | lair | 1100 | 350° | gorgath × 1 |
| Combe de la Veuve | Nébulosia | lair | 800 | 250° | veuve_bleme × 1 |
| Autel de la Lune | Nébulosia | sanctuary | 1000 | 165° | selene × 1 |
| Le Sillage | Nébulosia | sanctuary | 1500 | 20° | comete × 1 |
| Port-Écume | Diviniarth | harbor | 0 | 0° | legionnaire × 4, citoyen × 4 |
| Azalith | Diviniarth | city | 700 | 0° | citoyen × 10, legionnaire × 6, magistere × 2, pretorien × 2 |
| Trône d'Azalée | Diviniarth | palace | 900 | 8° | segaroth × 1, noctalys × 1, pretorien × 6, magistere × 2 |
| L'Aiguille de Veille | Diviniarth | tower | 500 | 60° | suke × 1, tirailleur × 4 |
| Ronceval | Diviniarth | city | 800 | 140° | citoyen × 8, legionnaire × 8, tirailleur × 3, zephyrine × 1 |
| Bastion d'Épinoire | Diviniarth | fortress | 1100 | 120° | cledith × 1, bragor × 1, legionnaire × 8, pretorien × 4 |
| Lunecorolle | Diviniarth | city | 900 | 240° | citoyen × 8, magistere × 5, legionnaire × 4, oriel × 1, ignarelle × 1 |
| Fort-Pollen | Diviniarth | fortress | 1200 | 262° | valdrek × 1, legionnaire × 8, tirailleur × 3, pretorien × 3 |
| Tombeau d'Andromède | Diviniarth | lair | 1500 | 180° | geolier × 1 |
| Forge de Sombrefer | Diviniarth | lair | 1300 | 330° | sombrefer × 1 |
| Trône de cendres | Diviniarth | lair | 1600 | 85° | roi_fletri × 1 |
| Champ de Rouille | Diviniarth | sanctuary | 1700 | 105° | mars × 1 |
| Tribunal des Anneaux | Diviniarth | sanctuary | 1800 | 205° | saturne × 1 |
| Le Phare | Diviniarth | sanctuary | 1500 | 30° | pulsar × 1 |
| Le Zénith | Diviniarth | sanctuary | 2100 | 300° | sol × 1 |
| Le Berceau | Diviniarth | sanctuary | 2000 | 150° | nebula × 1 |
| La Gueule | Diviniarth | sanctuary | 2300 | 240° | quasar × 1 |
| L'Horizon Blanc | Diviniarth | sanctuary | 2500 | 60° | trou_blanc × 1 |
| Le Trône de Masse | Diviniarth | sanctuary | 3000 | 0° | higgs × 1 |

### Budget de puissance (tout absorbé au niveau maximal, hors synergies)

| Palier | Puissance ajoutée |
|---|---|
| Mobs vanilla | 311 |
| Mortels de Nébulosia (PNJ, roi, Supernovas) | 79 |
| Demi-dieux | 104 |
| Empire d'Azalée (armée, Hypernovas, Segaroth) | 257 |
| Dieux | 625 |
