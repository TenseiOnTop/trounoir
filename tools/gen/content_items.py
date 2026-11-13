# -*- coding: utf-8 -*-
"""Armes signatures (lâchées par leur porteur) et reliques des Dieux (coffres des lieux).

Les dégâts et la cadence du clic gauche sont fixés dans ModRegistry.ARTIFACTS (ils font partie de l'objet).
Ici : le pouvoir du clic droit (mêmes types que les techniques), l'effet au toucher, les textes, le dessin.
"""


def fx(effect, duration, amplifier=0):
    return {'effect': 'minecraft:' + effect, 'duration': duration, 'amplifier': amplifier}


def I(id, fr, en, owner, shape, colors, type, cooldown, params, left_fr, left_en, right_fr, right_en, on_hit=None):
    return dict(id=id, fr=fr, en=en, owner=owner, shape=shape, colors=colors, type=type, cooldown=cooldown, params=params,
                left_fr=left_fr, left_en=left_en, right_fr=right_fr, right_en=right_en, on_hit=on_hit or {})


WEAPONS = [
    I('solbrand', 'Solbrand', 'Solbrand', 'aldric', 'sword', ('#ffd866', '#d8a03a', '#7a1f2a'), 'aoe', 12,
      {'radius': 5, 'cone_angle': 120, 'damage': 10, 'damage_type': 'physical', 'ignite': 5, 'particle': 'minecraft:flame', 'sound': 'minecraft:item.firecharge.use'},
      'Coup : 8 dégâts, enflamme.', 'Hit: 8 damage, ignites.', 'Spécial : arc de feu devant vous (12 s).', 'Special: arc of fire in front of you (12 s).',
      {'ignite': 3, 'particle': 'minecraft:flame'}),
    I('baton_de_givre', 'Bâton de givre', 'Frost Staff', 'lysmene', 'staff', ('#bfe6ff', '#4a6a9a', '#ffffff'), 'aoe', 15,
      {'radius': 6, 'damage': 6, 'effects': [fx('slowness', 100, 4)], 'particle': 'minecraft:snowflake', 'sound': 'minecraft:block.glass.break'},
      'Coup : 4 dégâts, ralentit.', 'Hit: 4 damage, slows.', 'Spécial : gèle tout autour de vous (15 s).', 'Special: freezes everything around you (15 s).',
      {'effects': [fx('slowness', 60, 1)], 'particle': 'minecraft:snowflake'}),
    I('epee_de_cledith', 'Épée de Cledith', "Cledith's Sword", 'cledith', 'sword', ('#f4f4ff', '#b8bcc8', '#a03a6a'), 'aoe', 25,
      {'radius': 6, 'damage': 20, 'damage_type': 'physical', 'knockback': 0.6, 'particle': 'minecraft:sweep_attack'},
      'Coup : 13 dégâts.', 'Hit: 13 damage.', 'Spécial : tranche tout à 6 blocs (25 s).', 'Special: cuts everything within 6 blocks (25 s).',
      {'particle': 'minecraft:sweep_attack'}),
    I('fusil_de_suke', 'Fusil de Suke', "Suke's Rifle", 'suke', 'rifle', ('#8a8f6a', '#3a4a3a', '#ffd866'), 'beam', 4,
      {'range': 200, 'damage': 30, 'width': 0.4, 'particle': 'minecraft:end_rod', 'sound': 'minecraft:entity.firework_rocket.blast'},
      'Coup : 3 dégâts (crosse).', 'Hit: 3 damage (stock).', 'Tir : 30 dégâts à 200 blocs (4 s).', 'Shot: 30 damage at 200 blocks (4 s).'),
    I('sceptre_d_ignarelle', "Sceptre d'Ignarelle", "Ignarelle's Scepter", 'ignarelle', 'staff', ('#ffb03a', '#c8421f', '#3a1a12'), 'projectile', 5,
      {'projectile': 'large_fireball', 'power': 2, 'speed': 1.4},
      'Coup : 5 dégâts, enflamme.', 'Hit: 5 damage, ignites.', 'Spécial : boule de feu explosive (5 s).', 'Special: explosive fireball (5 s).',
      {'ignite': 4, 'particle': 'minecraft:flame'}),
    I('lame_de_noctalys', 'Lame de Noctalys', "Noctalys's Blade", 'noctalys', 'dagger', ('#3a2a5a', '#14141c', '#8a6fd1'), 'buff', 30,
      {'self_effects': [fx('invisibility', 160), fx('speed', 160, 1)], 'particle': 'minecraft:smoke', 'sound': 'minecraft:entity.illusioner.mirror_move'},
      'Coup : 11 dégâts, rapide.', 'Hit: 11 damage, fast.', 'Spécial : invisible et vif 8 s (30 s).', 'Special: unseen and quick for 8 s (30 s).',
      {'particle': 'minecraft:smoke'}),
    I('pavois_de_bragor', 'Pavois de Bragor', "Bragor's Pavise", 'bragor', 'shield', ('#c8ccd4', '#5a5f6a', '#a03a6a'), 'aoe', 20,
      {'radius': 5, 'damage': 8, 'damage_type': 'physical', 'knockback': 2.0, 'self_effects': [fx('resistance', 100, 1)], 'particle': 'minecraft:poof',
       'sound': 'minecraft:item.shield.block'},
      'Coup : 10 dégâts, très lent.', 'Hit: 10 damage, very slow.', 'Spécial : repousse tout, vous protège 5 s (20 s).', 'Special: pushes all back, shields you 5 s (20 s).',
      {'particle': 'minecraft:crit'}),
    I('trident_de_zephyrine', 'Trident de Zéphyrine', "Zéphyrine's Trident", 'zephyrine', 'trident', ('#f4f8ff', '#6fd6ff', '#ffffff'), 'dash', 6,
      {'strength': 2.4, 'damage': 14, 'ticks': 10, 'sound': 'minecraft:item.trident.riptide_3'},
      'Coup : 10 dégâts, rapide.', 'Hit: 10 damage, fast.', 'Spécial : charge éclair qui traverse (6 s).', 'Special: lightning charge through foes (6 s).',
      {'particle': 'minecraft:electric_spark'}),
    I('compas_d_oriel', "Compas d'Oriel", "Oriel's Compass", 'oriel', 'orb', ('#c792ea', '#3a2a6a', '#ffffff'), 'blink', 5,
      {'range': 24}, 'Coup : 4 dégâts.', 'Hit: 4 damage.', 'Spécial : téléportation de 24 blocs (5 s).', 'Special: 24-block teleport (5 s).',
      {'particle': 'minecraft:portal'}),
    I('hache_de_valdrek', 'Hache de Valdrek', "Valdrek's Axe", 'valdrek', 'axe', ('#c8ccd4', '#7a1f2a', '#e6c14a'), 'buff', 40,
      {'self_effects': [fx('strength', 300, 1), fx('speed', 300)], 'sound': 'minecraft:event.raid.horn', 'particle': 'minecraft:angry_villager'},
      'Coup : 14 dégâts, très lent.', 'Hit: 14 damage, very slow.', 'Spécial : force et vitesse 15 s (40 s).', 'Special: strength and speed for 15 s (40 s).',
      {'particle': 'minecraft:crit'}),
    I('grimoire_de_segaroth', 'Grimoire de Segaroth', "Segaroth's Grimoire", 'segaroth', 'book', ('#2a1030', '#e6c14a', '#f0c8dc'), 'aoe', 45,
      {'radius': 8, 'damage': 16, 'effects': [fx('weakness', 100, 1)], 'self_effects': [fx('regeneration', 160, 1)], 'particle': 'minecraft:soul_fire_flame', 'sound': 'minecraft:item.totem.use'},
      'Coup : 6 dégâts, rend 1 PV.', 'Hit: 6 damage, heals 1 HP.', 'Spécial : déflagration de mille ans à 8 blocs, vous régénère (45 s).', 'Special: a thousand-year blast within 8 blocks, regenerates you (45 s).',
      {'heal': 1, 'particle': 'minecraft:soul_fire_flame'}),
]

RELICS = [
    I('noyau_de_masse', 'Noyau de masse', 'Mass Core', 'higgs', 'orb', ('#6a1fd8', '#0c0c14', '#ffd866'), 'aoe', 60,
      {'radius': 10, 'damage': 12, 'pull': 2.5, 'effects': [fx('slowness', 80, 3)], 'particle': 'minecraft:reverse_portal', 'sound': 'minecraft:entity.wither.break_block'},
      '', '', 'Attire et écrase tout à 10 blocs (60 s).', 'Pulls in and crushes everything within 10 blocks (60 s).'),
    I('anneau_de_saturne', 'Anneau de Saturne', "Saturn's Ring", 'saturne', 'ring', ('#f4e3a0', '#c8b07a', '#5a3a7a'), 'buff', 60,
      {'self_effects': [fx('resistance', 200, 1)], 'particle': 'minecraft:enchant'},
      '', '', 'Résistance aux dégâts pendant 10 s (60 s).', 'Damage resistance for 10 s (60 s).'),
    I('graal_de_rouille', 'Graal de rouille', 'Rust Grail', 'mars', 'cup', ('#d0603a', '#8a2a1a', '#3a1a12'), 'buff', 45,
      {'heal': 12, 'particle': 'minecraft:damage_indicator', 'sound': 'minecraft:entity.generic.drink'},
      '', '', 'Rend 12 PV (45 s).', 'Heals 12 HP (45 s).'),
    I('sablier_du_trou_blanc', 'Sablier du Trou-Blanc', "White-Hole's Hourglass", 'trou_blanc', 'hourglass', ('#ffffff', '#c8ccd4', '#e6c14a'), 'time_stop', 120,
      {'radius': 16, 'duration': 80, 'resist_rank': 7}, '', '', 'Arrête le temps 4 s à 16 blocs (120 s).', 'Stops time for 4 s within 16 blocks (120 s).'),
    I('eclat_de_sol', 'Éclat de Sol', 'Shard of Sol', 'sol', 'shard', ('#fff6c8', '#ffd23a', '#c8421f'), 'aoe', 45,
      {'radius': 8, 'damage': 10, 'ignite': 6, 'particle': 'minecraft:flame', 'sound': 'minecraft:entity.blaze.shoot'},
      '', '', 'Embrase tout à 8 blocs (45 s).', 'Sets everything ablaze within 8 blocks (45 s).'),
    I('larme_de_selene', 'Larme de Séléné', "Selene's Tear", 'selene', 'drop', ('#ffffff', '#dfe8ff', '#8a9ad8'), 'buff', 90,
      {'self_effects': [fx('invisibility', 600), fx('night_vision', 600)], 'particle': 'minecraft:end_rod', 'sound': 'minecraft:block.amethyst_block.chime'},
      '', '', 'Invisible et nyctalope 30 s (90 s).', 'Unseen with night sight for 30 s (90 s).'),
    I('metronome_de_pulsar', 'Métronome de Pulsar', "Pulsar's Metronome", 'pulsar', 'hourglass', ('#3ad8c8', '#12343a', '#ffffff'), 'buff', 60,
      {'self_effects': [fx('haste', 400, 1), fx('speed', 400, 1)], 'particle': 'minecraft:note', 'sound': 'minecraft:block.note_block.bell'},
      '', '', 'Célérité et vitesse 20 s (60 s).', 'Haste and speed for 20 s (60 s).'),
    I('fragment_de_comete', 'Fragment de Comète', 'Comet Fragment', 'comete', 'shard', ('#ffffff', '#bfe6ff', '#3a6a9a'), 'dash', 10,
      {'strength': 3.0, 'damage': 6, 'ticks': 12, 'sound': 'minecraft:item.trident.riptide_3'},
      '', '', 'Vous projette loin devant (10 s).', 'Hurls you far ahead (10 s).'),
    I('graine_de_nebula', 'Graine de Nébula', "Nebula's Seed", 'nebula', 'drop', ('#ffb3e6', '#d86fd1', '#3a2a6a'), 'buff', 90,
      {'self_effects': [fx('absorption', 600, 2), fx('regeneration', 200)], 'particle': 'minecraft:heart'},
      '', '', '6 cœurs de protection et régénération (90 s).', '6 protective hearts and regeneration (90 s).'),
    I('oeil_de_quasar', 'Œil de Quasar', "Quasar's Eye", 'quasar', 'orb', ('#ffd866', '#f4f4ff', '#6a1fd8'), 'beam', 30,
      {'range': 64, 'damage': 20, 'pierce': True, 'width': 1.2, 'particle': 'minecraft:end_rod', 'sound': 'minecraft:entity.warden.sonic_boom'},
      '', '', 'Rayon perçant : 20 dégâts à 64 blocs (30 s).', 'Piercing beam: 20 damage at 64 blocks (30 s).'),
]
