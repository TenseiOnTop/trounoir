#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Génère le contenu du mod Trou-Noir à partir des tables content_*.py.

    python3 tools/gen/gen.py

Écrit : JSON du datapack (data/trounoir/...), fichiers de langue, textures de personnages,
ressources du gouvernail et tableaux de docs/EQUILIBRAGE.md.
Les JSON produits sont la vérité en jeu ; on peut aussi les modifier à la main sans relancer ce script.
"""
import json
import os
import random
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import content_absorbables as A  # noqa: E402
import content_items as IT  # noqa: E402
import content_text as X  # noqa: E402
import content_world as W  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data', 'trounoir', 'trounoir')
ASSETS = os.path.join(RES, 'assets', 'trounoir')

LANG_FR = {}
LANG_EN = {}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write('\n')


def lang(key, fr, en):
    LANG_FR[key] = fr
    LANG_EN[key] = en


def bonus(attrs, stats, relief, effects):
    out = {}
    if attrs:
        out['attributes'] = [{'attribute': A.ATTR[k][0], 'amount': v, 'operation': A.ATTR[k][1]} for k, v in attrs.items()]
    if effects:
        out['effects'] = [{'effect': e, 'amplifier': a, 'min_level': m} for e, a, m in effects]
    if stats:
        out['stats'] = stats
    if relief:
        out['relief'] = relief
    return out


LABELS = {
    'hp': ('PV', 'HP', 1), 'armor': ('armure', 'armor', 1), 'atk': ('dégâts', 'damage', 1),
    'spd': ('vitesse', 'speed', 100), 'aspd': ("vitesse d'attaque", 'attack speed', 100), 'kb': ('résistance au recul', 'knockback resistance', 100),
    'melee_damage_pct': ('dégâts au corps-à-corps', 'melee damage', 1), 'ranged_damage_pct': ('dégâts à distance', 'ranged damage', 1),
    'all_damage_pct': ('tous les dégâts', 'all damage', 1), 'backstab_pct': ('dégâts dans le dos', 'backstab damage', 1),
    'magic_power': ('puissance des techniques', 'technique power', 1), 'cooldown_reduction_pct': ('recharge', 'cooldown', -1),
    'damage_reduction_pct': ('dégâts subis', 'damage taken', -1), 'lifesteal_pct': ('vol de vie', 'lifesteal', 1),
    'regen': ('PV régénérés toutes les 5 s', 'HP regenerated every 5 s', 1), 'stealth': ('discrétion', 'stealth', 1),
}
FLAGS = {'fire_immunity': ('immunité au feu', 'fire immunity'), 'fire_touch': ('vos coups enflamment', 'your blows ignite'),
         'frost_touch': ('vos coups ralentissent', 'your blows slow'), 'poison_touch': ('vos coups empoisonnent', 'your blows poison'),
         'wither_touch': ('vos coups flétrissent', 'your blows wither')}
PERCENT = {'spd', 'aspd', 'kb'}


def readable(attrs, stats, lng):
    i = 0 if lng == 'fr' else 1
    parts = []
    for k, v in list(attrs.items()) + list(stats.items()):
        if k in FLAGS:
            parts.append(FLAGS[k][i])
            continue
        label = LABELS[k]
        value = v * abs(label[2]) * (-1 if label[2] < 0 else 1)
        pct = k in PERCENT or k.endswith('_pct') or k in ('magic_power', 'stealth')
        parts.append('%+g%s %s' % (value, ' %' if pct else '', label[i]))
    return ', '.join(parts)


def trait_name(id, lng):
    for t in A.TRAITS + A.TECHNIQUES:
        if t['id'] == id:
            return t['fr'] if lng == 'fr' else t['en']
    return id


STAT_LABELS = {
    'generic.max_health': ('PV max', 'max HP'), 'generic.armor': ('armure', 'armor'), 'generic.attack_damage': ('dégâts', 'damage'),
    'generic.movement_speed': ('vitesse', 'speed'), 'generic.attack_speed': ("vitesse d'attaque", 'attack speed'),
    'generic.knockback_resistance': ('résistance au recul', 'knockback resistance'), 'player.block_break_speed': ('vitesse de minage', 'mining speed'),
    'generic.jump_strength': ('hauteur de saut', 'jump height'), 'generic.safe_fall_distance': ('blocs de chute sans dégâts', 'blocks of safe fall'),
    'generic.step_height': ("hauteur d'enjambée", 'step height'), 'generic.luck': ('chance', 'luck'), 'generic.oxygen_bonus': ('souffle sous l\'eau', 'underwater breath'),
    'generic.water_movement_efficiency': ('aisance dans l\'eau', 'water movement'), 'player.sneaking_speed': ('vitesse accroupi', 'sneaking speed'),
    'generic.burning_time': ('durée de brûlure', 'burning time'), 'player.entity_interaction_range': ("blocs d'allonge", 'blocks of reach'),
    'player.block_interaction_range': ('blocs de portée', 'blocks of block reach'), 'player.sweeping_damage_ratio': ('dégâts de balayage', 'sweeping damage'),
    'melee_damage_pct': ('dégâts au corps-à-corps', 'melee damage'), 'ranged_damage_pct': ('dégâts à distance', 'ranged damage'),
    'all_damage_pct': ('tous les dégâts', 'all damage'), 'backstab_pct': ('dégâts dans le dos', 'backstab damage'),
    'magic_power': ('puissance des techniques', 'technique power'), 'cooldown_reduction_pct': ('recharge plus rapide', 'faster cooldowns'),
    'damage_reduction_pct': ('dégâts subis en moins', 'less damage taken'), 'explosion_reduction_pct': ("dégâts d'explosion en moins", 'less explosion damage'),
    'fall_reduction_pct': ('dégâts de chute en moins', 'less fall damage'), 'lifesteal_pct': ('vol de vie', 'lifesteal'),
    'thorns': ('dégâts renvoyés à qui vous frappe', 'damage returned to attackers'), 'regen': ('PV régénérés toutes les 5 s', 'HP regenerated every 5 s'),
    'stealth': ('discrétion (on vous repère de moins loin)', 'stealth (spotted from closer)'),
    'perception_range': ('blocs de perception (créatures visibles à travers les murs)', 'blocks of perception (creatures seen through walls)'),
    'fire_immunity': ('Immunité au feu', 'Fire immunity'), 'fire_touch': ('Vos coups enflamment', 'Your blows ignite'),
    'frost_touch': ('Vos coups ralentissent', 'Your blows slow'), 'poison_touch': ('Vos coups empoisonnent', 'Your blows poison'),
    'wither_touch': ('Vos coups flétrissent', 'Your blows wither'), 'sniper': ('Flèches bien plus rapides et tendues', 'Much faster, flatter arrows'),
    'p.damage': ('Dégâts : %s', 'Damage: %s'), 'p.radius': ('Rayon : %s blocs', 'Radius: %s blocks'), 'p.range': ('Portée : %s blocs', 'Range: %s blocks'),
    'p.heal': ('Soin : %s PV', 'Heal: %s HP'), 'p.duration': ('Durée : %s s', 'Duration: %s s'), 'p.count': ('Projectiles : %s', 'Projectiles: %s'),
    'p.ignite': ('Enflamme %s s', 'Ignites for %s s'), 'p.hunger': ('Coût : %s de faim', 'Cost: %s hunger'),
    'inactive': ('pas encore actif à ce niveau', 'not yet active at this level'), 'relief': ('Atténue : %s', 'Eases: %s'),
    'effect': ('Effet permanent : %s', 'Permanent effect: %s'), 'effect_later': ('Effet permanent au niveau %s : %s', 'Permanent effect at level %s: %s'),
    'level': ('Niveau %s / %s', 'Level %s / %s'), 'next': ('Prochain niveau : %s / %s absorptions', 'Next level: %s / %s absorptions'),
    'max': ('Niveau maximal', 'Max level'), 'echo': ('Écho : effets à demi-force', 'Echo: effects at half strength'),
    'next_rank': ('Prochain rang à %s', 'Next rank at %s'), 'self': ('Sur vous : %s', 'On you: %s'), 'target': ('Sur la cible : %s', 'On target: %s'),
}


def clean(directory):
    if os.path.isdir(directory):
        shutil.rmtree(directory)


# ------------------------------------------------------------------------------------------------- datapack
def gen_data():
    clean(DATA)
    for id, (fr, en, color) in A.TAGS.items():
        write_json(os.path.join(DATA, 'tags', id + '.json'), {'color': color})
        lang('tntag.trounoir.' + id, fr, en)

    for id, fr, en, attribute, amount, needed, hunger in A.HANDICAPS:
        obj = {'relief_needed': needed}
        if attribute:
            obj.update({'attribute': attribute[0], 'amount': amount, 'operation': attribute[1]})
        if hunger:
            obj['hunger'] = True
        write_json(os.path.join(DATA, 'fragility', id + '.json'), obj)
        lang('handicap.trounoir.' + id, fr, en)

    for t in A.TRAITS:
        obj = {'tags': t['tags'], 'max_level': t['max_level'], 'power': t['power'], 'icon': t['icon'],
               'bonus': bonus(t['attrs'], t['stats'], t['relief'], t['effects'])}
        if t['signature']:
            obj['signature'] = True
        write_json(os.path.join(DATA, 'traits', t['id'] + '.json'), obj)
        lang('trait.trounoir.' + t['id'], t['fr'], t['en'])
        if t['desc_fr']:
            lang('trait.trounoir.' + t['id'] + '.desc', t['desc_fr'], t['desc_en'])

    for q in A.TECHNIQUES:
        obj = {'tags': q['tags'], 'max_level': q['max_level'], 'power': q['power'], 'icon': q['icon'], 'type': q['type'],
               'cooldown': q['cooldown'], 'hunger_cost': q['hunger'], 'params': q['params']}
        if q['signature']:
            obj['signature'] = True
        write_json(os.path.join(DATA, 'techniques', q['id'] + '.json'), obj)
        lang('technique.trounoir.' + q['id'], q['fr'], q['en'])
        if q['desc_fr']:
            lang('technique.trounoir.' + q['id'] + '.desc', q['desc_fr'], q['desc_en'])

    for id, fr, en, icon, ktype, target, recipes, dfr, den in A.KNOWLEDGE:
        obj = {'icon': icon, 'knowledge_type': ktype}
        if target:
            obj['target'] = target
        if recipes:
            obj['recipes'] = recipes
        write_json(os.path.join(DATA, 'knowledge', id + '.json'), obj)
        lang('knowledge.trounoir.' + id, fr, en)
        lang('knowledge.trounoir.' + id + '.desc', dfr, den)

    for id, fr, en, tags, traits, attrs, stats, power, effects in A.SYNERGIES:
        obj = {'power': power, 'bonus': bonus(attrs, stats, {}, effects)}
        if tags:
            obj['requires_tags'] = tags
        if traits:
            obj['requires_all'] = ['trounoir:' + t for t in traits]
        write_json(os.path.join(DATA, 'synergies', id + '.json'), obj)
        lang('synergy.trounoir.' + id, fr, en)
        lang('synergy.trounoir.' + id + '.desc', readable(attrs, stats, 'fr'), readable(attrs, stats, 'en'))
        need_fr = ', '.join('%d × %s' % (n, A.TAGS[t][0]) for t, n in tags.items()) or ' + '.join(trait_name(t, 'fr') for t in traits)
        need_en = ', '.join('%d × %s' % (n, A.TAGS[t][1]) for t, n in tags.items()) or ' + '.join(trait_name(t, 'en') for t in traits)
        lang('synergy.trounoir.' + id + '.need', 'Condition : ' + need_fr, 'Requires: ' + need_en)

    for entity, profile in W.PROFILES.items():
        obj = {'entity': 'minecraft:' + entity, 'rank': profile[0], 'grants': profile[1]}
        if len(profile) > 2:
            obj['faction'] = profile[2]
            obj['kill_reputation'] = profile[3]
        write_json(os.path.join(DATA, 'profiles', entity + '.json'), obj)

    for c in W.CHARACTERS:
        obj = {'unique': c.get('unique', False), 'category': c['category'], 'faction': c['faction'], 'rank': c['rank'],
               'behavior': c.get('behavior', 'civilian'), 'health': c['health'], 'damage': c['damage']}
        for key in ('drops', 'variants', 'fights_back', 'base_entity', 'armor', 'speed', 'scale', 'knockback_resistance', 'follow_range', 'held_item', 'phases',
                    'weakness', 'kill_reputation', 'notoriety', 'language', 'dialogue_lines', 'boss_bar'):
            if key in c:
                obj[key] = c[key]
        if 'skin' in c:
            obj['texture'] = 'trounoir:textures/entity/character/%s.png' % c['id']
        if c.get('weakness'):
            obj['weakness_multiplier'] = 1.5
        if 'title_fr' in c:
            obj['title'] = True
        accent = (c.get('skin') or (None, None, None, '#ff8a3c'))[3]
        for m in c.get('moves', []):
            # Chaque personnage colore ses annonces : son identité se lit au sol avant même le coup.
            m.setdefault('color', accent)
        obj['moves'] = c.get('moves', [])
        obj['grants'] = c.get('grants', [])
        obj['on_death'] = c.get('on_death', [])
        write_json(os.path.join(DATA, 'characters', c['id'] + '.json'), obj)
        lang('character.trounoir.' + c['id'], c['fr'], c['en'])
        if 'title_fr' in c:
            lang('character.trounoir.' + c['id'] + '.title', c['title_fr'], c['title_en'])
        lines = X.DIALOGUES.get(c['id'], [])
        if len(lines) != c.get('dialogue_lines', 0):
            raise SystemExit('dialogues de %s : %d lignes écrites, %d déclarées' % (c['id'], len(lines), c.get('dialogue_lines', 0)))
        for i, (fr, en) in enumerate(lines, 1):
            lang('dialogue.trounoir.%s.%d' % (c['id'], i), fr, en)

    for id, (fr, en, color, squad) in W.FACTIONS.items():
        write_json(os.path.join(DATA, 'factions', id + '.json'),
                   {'color': color, 'hunt_squad': [{'character': 'trounoir:' + ch, 'count': n} for ch, n in squad]})
        lang('faction.trounoir.' + id, fr, en)

    for s in W.SITES:
        obj = {'dimension': s['dimension'], 'blueprint': s['blueprint'], 'distance': s['distance'], 'angle': s['angle'],
               'faction': s['faction'],
               'palette': {'primary': s['palette'][0], 'secondary': s['palette'][1], 'accent': s['palette'][2]},
               'residents': [{'character': 'trounoir:' + ch, 'count': n} for ch, n in s['residents']]}
        if s['blueprint'] in ('castle', 'city', 'fortress', 'palace', 'tower', 'lair', 'ruin'):
            obj['relic_spot'] = True
        if s['crossing']:
            obj['crossing'] = True
        write_json(os.path.join(DATA, 'sites', s['id'] + '.json'), obj)
        lang('site.trounoir.' + s['id'], s['fr'], s['en'])

    for it in IT.WEAPONS + IT.RELICS:
        obj = {'type': it['type'], 'cooldown': it['cooldown'], 'params': it['params']}
        if it['on_hit']:
            obj['on_hit'] = it['on_hit']
        write_json(os.path.join(DATA, 'weapons', it['id'] + '.json'), obj)
        lang('item.trounoir.' + it['id'], it['fr'], it['en'])
        if it['left_fr']:
            lang('item.trounoir.' + it['id'] + '.left', 'Clic gauche — ' + it['left_fr'], 'Left click — ' + it['left_en'])
        lang('item.trounoir.' + it['id'] + '.right', 'Clic droit — ' + it['right_fr'], 'Right click — ' + it['right_en'])
        write_json(os.path.join(ASSETS, 'models', 'item', it['id'] + '.json'),
                   {'parent': 'minecraft:item/handheld' if it['left_fr'] else 'minecraft:item/generated',
                    'textures': {'layer0': 'trounoir:item/' + it['id']}})

    for code, table in (('fr_fr', X.EPITAPHS_FR), ('en_us', X.EPITAPHS_EN)):
        write_json(os.path.join(DATA, 'epitaphs', code + '.json'), dict(language=code, **table))

    # La dimension de Diviniarth : même graine que l'Overworld, relief « grands biomes » (terres plus vastes).
    write_json(os.path.join(RES, 'data', 'trounoir', 'dimension', 'diviniarth.json'), {
        'type': 'minecraft:overworld',
        'generator': {'type': 'minecraft:noise', 'settings': 'minecraft:large_biomes',
                      'biome_source': {'type': 'minecraft:multi_noise', 'preset': 'minecraft:overworld'}}})

    for key, (fr, en) in STAT_LABELS.items():
        lang('tnstat.trounoir.' + key, fr, en)
    attr_names = {'hp': ('PV max', 'max HP'), 'armor': ('armure', 'armor'), 'atk': ('dégâts de mêlée', 'melee damage'),
                  'spd': ('vitesse de déplacement', 'movement speed'), 'aspd': ("vitesse d'attaque", 'attack speed'),
                  'kb': ('résistance au recul', 'knockback resistance'), 'break': ('vitesse de minage', 'mining speed'),
                  'jump': ('force de saut', 'jump strength'), 'fall': ('blocs de chute sans dégâts', 'blocks of safe fall'),
                  'step': ("hauteur d'enjambée", 'step height'), 'luck': ('chance', 'luck'), 'oxy': ('souffle sous l\'eau', 'underwater breath'),
                  'swim': ('aisance dans l\'eau', 'water movement'), 'sneak': ('vitesse accroupi', 'sneaking speed'),
                  'burn': ('durée de brûlure', 'burning time'), 'reach': ("allonge d'attaque (blocs)", 'attack reach (blocks)'),
                  'breach': ('portée sur les blocs', 'block reach'), 'sweep': ('dégâts de balayage', 'sweeping damage')}
    for k, (fr, en) in attr_names.items():
        lang('tnstat.' + A.ATTR[k][0].split(':')[1], fr, en)
    stat_names = {'melee_damage_pct': ('dégâts de mêlée', 'melee damage'), 'ranged_damage_pct': ('dégâts à distance', 'ranged damage'),
                  'all_damage_pct': ('tous les dégâts', 'all damage'), 'backstab_pct': ('dégâts dans le dos', 'backstab damage'),
                  'magic_power': ('puissance des techniques', 'technique power'), 'cooldown_reduction_pct': ('recharge plus rapide', 'faster cooldowns'),
                  'damage_reduction_pct': ('dégâts subis en moins', 'less damage taken'), 'explosion_reduction_pct': ('dégâts d\'explosion en moins', 'less explosion damage'),
                  'fall_reduction_pct': ('dégâts de chute en moins', 'less fall damage'), 'lifesteal_pct': ('des dégâts rendus en vie', 'of damage healed back'),
                  'thorns': ('dégâts renvoyés à qui vous frappe', 'damage returned to attackers'), 'regen': ('PV rendus toutes les 5 s', 'HP restored every 5 s'),
                  'stealth': ('discrétion (on vous repère de moins loin)', 'stealth (spotted from closer)'),
                  'perception_range': ('blocs de perception (les créatures luisent)', 'blocks of perception (creatures glow)'),
                  'fire_immunity': ('Immunité au feu', 'Fire immunity'), 'fire_touch': ('Vos coups enflamment', 'Your blows ignite'),
                  'frost_touch': ('Vos coups ralentissent', 'Your blows slow'), 'poison_touch': ('Vos coups empoisonnent', 'Your blows poison'),
                  'wither_touch': ('Vos coups flétrissent', 'Your blows wither'), 'sniper': ('Flèches bien plus rapides', 'Much faster arrows')}
    for k, (fr, en) in stat_names.items():
        lang('tnstat.' + k, fr, en)
    for k, (fr, en) in {'damage': ('Dégâts : %s', 'Damage: %s'), 'radius': ('Rayon : %s blocs', 'Radius: %s blocks'),
                        'range': ('Portée : %s blocs', 'Range: %s blocks'), 'heal': ('Soin : %s PV', 'Heal: %s HP'),
                        'count': ('Projectiles : %s', 'Projectiles: %s'), 'cone_angle': ('Cône : %s°', 'Cone: %s°')}.items():
        lang('tnparam.' + k, fr, en)
    lang('trounoir.tip.level', 'Niveau %s / %s', 'Level %s / %s')
    lang('trounoir.tip.echo', 'Écho : effets à demi-force', 'Echo: effects at half strength')
    lang('trounoir.tip.next', 'Niveau suivant : encore %s absorption(s)', 'Next level: %s more absorption(s)')
    lang('trounoir.tip.max', 'Niveau maximal atteint', 'Maximum level reached')
    lang('trounoir.tip.later', '%s — au niveau %s', '%s — at level %s')
    lang('trounoir.tip.relief', 'Atténue : %s', 'Eases: %s')
    lang('trounoir.tip.active', 'actif', 'active')
    lang('trounoir.tip.hover', 'Survolez une ligne pour voir exactement ce qu\'elle vous apporte.', 'Hover a line to see exactly what it gives you.')

    for key, (fr, en) in X.UI.items():
        lang(key, fr, en)


# ------------------------------------------------------------------------------------------------- textures
def hex_rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb)


def gen_textures():
    from PIL import Image

    out_dir = os.path.join(ASSETS, 'textures', 'entity', 'character')
    clean(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    def paint(kind, primary, secondary, accent, seed, female=False, look=None):
        rng = random.Random(seed)
        img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        px = img.load()
        p, s, a = hex_rgb(primary), hex_rgb(secondary), hex_rgb(accent)
        skin = {'god': shade(a, 0.95), 'emperor': (214, 190, 170)}.get(kind, rng.choice([(226, 190, 160), (198, 150, 120), (150, 105, 80), (236, 208, 184)]))
        hair = {'god': p, 'peasant': (88, 62, 40), 'king': (200, 200, 205)}.get(kind, shade(s, 0.6))
        godstyle = None
        if look:
            # Les Astres ont un visage : cheveux, yeux et peau choisis un à un, pas une silhouette géante.
            skin = hex_rgb(look['skin'])
            hair = hex_rgb(look['hair'])
            godstyle = look.get('style', 'short')

        def rect(x0, y0, x1, y1, color, noise=0.08):
            for x in range(x0, x1):
                for y in range(y0, y1):
                    f = 1.0 + (rng.random() - 0.5) * 2 * noise
                    px[x, y] = shade(color, f) + (255,)

        # Tête (8x8x8) : dessus, dessous, droite, face, gauche, dos.
        rect(8, 0, 16, 8, hair)
        rect(16, 0, 24, 8, shade(skin, 0.8))
        for x0 in (0, 8, 16, 24):
            rect(x0, 8, x0 + 8, 16, skin)
        rect(0, 8, 8, 11, hair)
        rect(16, 8, 24, 11, hair)
        rect(24, 8, 32, 14, hair)
        rect(8, 8, 16, 10, hair)
        eye = hex_rgb(look['eyes']) if look else (a if kind in ('god', 'hypernova', 'emperor') else (40, 40, 50))
        for ex in (9, 10, 13, 14):
            px[ex, 12] = ((255, 255, 255) if ex in (9, 14) else eye) + (255,)
        px[11, 14] = px[12, 14] = shade(skin, 0.7) + (255,)

        # Corps (8x12x4), bras (4x12x4), jambes (4x12x4) : première couche.
        torso = p
        legs = shade(s, 0.9)
        rect(16, 16, 40, 32, torso)      # corps
        rect(40, 16, 56, 32, torso)      # bras droit
        rect(32, 48, 48, 64, torso)      # bras gauche
        rect(0, 16, 16, 32, legs)        # jambe droite
        rect(16, 48, 32, 64, legs)       # jambe gauche
        # Mains et bottes.
        for x0, y0 in ((40, 16), (32, 48)):
            rect(x0, y0 + 13, x0 + 16, y0 + 16, skin)
        for x0, y0 in ((0, 16), (16, 48)):
            rect(x0, y0 + 13, x0 + 16, y0 + 16, shade(s, 0.45))
        # Ceinture, col et liseré central.
        rect(16, 28, 40, 29, shade(a, 0.9))
        rect(20, 20, 28, 21, a)
        rect(23, 21, 25, 28, shade(a, 0.85))

        if kind in ('soldier', 'knight', 'supernova', 'hypernova', 'emperor'):
            # Épaulières et plastron.
            for x0, y0 in ((40, 16), (32, 48)):
                rect(x0, y0 + 4, x0 + 16, y0 + 7, shade(s, 1.15))
            rect(21, 22, 27, 27, shade(s, 1.1))
        if kind in ('mage', 'god', 'emperor', 'king'):
            # Robe : les jambes prennent la couleur du torse, ourlet d'accent.
            rect(0, 20, 16, 32, shade(p, 0.85))
            rect(16, 52, 32, 64, shade(p, 0.85))
            rect(0, 30, 16, 31, a)
            rect(16, 62, 32, 63, a)
        if (kind in ('king', 'emperor') and not look) or godstyle == 'crown':
            # Couronne sur la seconde couche de la tête (chapeau).
            for x in range(32, 64):
                px[x, 9] = a + (255,)
                if x % 2 == 0:
                    px[x, 8] = a + (255,)
        if godstyle == 'long':
            # Cheveux longs dans le dos.
            rect(0, 8, 8, 16, hair)
            rect(16, 8, 24, 16, hair)
            rect(24, 8, 32, 16, hair)
            rect(32, 20, 40, 26, hair)
        if godstyle == 'boy':
            # Un jeune garçon : mèches courtes en épis, grands yeux clairs, joues plus pleines.
            rect(8, 8, 16, 11, hair, noise=0.2)
            for x in (8, 10, 13, 15):
                px[x, 11] = shade(hair, 0.9) + (255,)
            for ex in (9, 10, 13, 14):
                px[ex, 11] = px[ex, 11] if ex in (8, 15) else px[ex, 11]
            px[9, 13] = px[14, 13] = (233, 160, 150, 255)
        if female:
            # Cheveux longs : ils couvrent les côtés et l'arrière de la tête, et tombent sur le haut du dos.
            rect(0, 8, 8, 16, hair)
            rect(16, 8, 24, 16, hair)
            rect(24, 8, 32, 16, hair)
            rect(8, 8, 16, 10, hair)
            px[8, 10] = px[8, 11] = px[15, 10] = px[15, 11] = hair + (255,)
            rect(32, 20, 40, 25, hair)
            px[11, 14] = px[12, 14] = (190, 90, 90, 255)
        if kind == 'god':
            # Poussière d'étoiles sur tout le corps.
            for _ in range(70):
                x, y = rng.randrange(64), rng.randrange(16, 64)
                if px[x, y][3]:
                    px[x, y] = (255, 255, 255, 255)
        if kind in ('supernova', 'hypernova'):
            # Étoile sur la poitrine : quatre branches pour une Supernova, huit pour une Hypernova.
            cx, cy = 24, 24
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                px[cx + dx, cy + dy] = a + (255,)
            if kind == 'hypernova':
                for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1), (2, 0), (-2, 0)):
                    px[cx + dx, cy + dy] = shade(a, 0.9) + (255,)
        return img

    paint('peasant', '#6a6a72', '#4a4a52', '#9a9aa2', 'default').save(os.path.join(out_dir, 'default.png'))
    for c in W.CHARACTERS:
        if 'skin' in c:
            kind, p, s, a = c['skin']
            paint(kind, p, s, a, c['id'], look=c.get('look')).save(os.path.join(out_dir, c['id'] + '.png'))
            for v in range(1, c.get('variants', 1)):
                # Chaque variante décale un peu les couleurs du vêtement ; une sur deux est une femme.
                rv = random.Random(c['id'] + str(v))
                tint = lambda h: '#%02x%02x%02x' % shade(hex_rgb(h), 0.8 + rv.random() * 0.45)
                paint(kind, tint(p), tint(s), a, c['id'] + str(v), female=(v % 2 == 1)).save(os.path.join(out_dir, '%s_%d.png' % (c['id'], v)))

    # Gouvernail : une roue de bois sombre à huit rayons.
    block_dir = os.path.join(ASSETS, 'textures', 'block')
    os.makedirs(block_dir, exist_ok=True)
    img = Image.new('RGBA', (16, 16), (92, 62, 38, 255))
    px = img.load()
    rng = random.Random('helm')
    for x in range(16):
        for y in range(16):
            base = (92, 62, 38) if (y // 4) % 2 == 0 else (84, 56, 34)
            px[x, y] = shade(base, 1.0 + (rng.random() - 0.5) * 0.12) + (255,)
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.2 <= d <= 6.6:
                px[x, y] = (196, 160, 96, 255)
            elif d < 1.6:
                px[x, y] = (226, 196, 120, 255)
            elif d < 5.2 and (abs(x - 7.5) < 0.8 or abs(y - 7.5) < 0.8 or abs(abs(x - 7.5) - abs(y - 7.5)) < 0.8):
                px[x, y] = (170, 132, 78, 255)
    img.save(os.path.join(block_dir, 'helm.png'))


def gen_item_textures():
    """Icônes 16x16 des armes et reliques : une silhouette par famille, trois couleurs par objet."""
    from PIL import Image
    out_dir = os.path.join(ASSETS, 'textures', 'item')
    clean(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    SHAPES = {
        # '1' couleur claire, '2' couleur moyenne, '3' couleur d'accent, '.' vide. Orientation : poignée en bas à gauche.
        'sword': ["..............11", ".............121", "............121.", "...........121..", "..........121...", ".........121....",
                  "........121.....", ".......121......", "..3...121.......", "..33.121........", "...3321.........", "...333..........",
                  "..23.33.........", ".22...3.........", "22..............", "2..............."],
        'dagger': ["................", "................", "................", "...........11...", "..........121...", ".........121....",
                   "........121.....", ".......121......", "....3.121.......", "....3321........", ".....33.........", "....2333........",
                   "...22...........", "..22............", "................", "................"],
        'staff': ["............333.", "...........31113", "...........31113", "...........3113.", "..........2333..", ".........22.....",
                  "........22......", ".......22.......", "......22........", ".....22.........", "....22..........", "...22...........",
                  "..22............", ".22.............", "22..............", "................"],
        'rifle': ["................", "..............11", ".............12.", "............12..", "...........12...", "......33..12....",
                  ".....3..312.....", "........12......", ".......122......", "......1222......", ".....22.2.......", "....222.........",
                  "...2222.........", "..222...........", ".22.............", "................"],
        'trident': ["..........1.1.1.", "..........1.1.1.", "..........1.111.", "..........1111..", "...........12...", "..........12....",
                    ".........12.....", "........12......", ".......12.......", "......32........", ".....33.........", "....22..........",
                    "...22...........", "..22............", ".22.............", "................"],
        'axe': ["................", ".........1111...", "........111111..", ".......11112211.", ".......1112.221.", "........12..21..",
                ".......32....1..", "......32........", ".....32.........", "....32..........", "...32...........", "..32............",
                ".32.............", "32..............", "................", "................"],
        'gauntlet': ["................", "....11.11.11....", "....12.12.12.1..", "....12.12.12.12.", "....1222222212..", "....12222222....",
                     "....12222222....", ".....2222222....", ".....3333333....", ".....2222222....", ".....2222222....", "......22222.....",
                     "......33333.....", "......22222.....", "................", "................"],
        'shield': ["................", "..111111111111..", "..122222222221..", "..122233322221..", "..122233322221..", "..123333333321..",
                   "..123333333321..", "..122233322221..", "..122233322221..", "...1222332221...", "...1222222221...", "....12222221....",
                   ".....122221.....", "......1221......", ".......11.......", "................"],
        'orb': ["................", ".....222222.....", "...2211111122...", "..221111111122..", "..211133331112..", ".21113333331112.",
                ".21133311333112.", ".21133133133112.", ".21133133133112.", ".21133311333112.", ".21113333331112.", "..211133331112..",
                "..221111111122..", "...2211111122...", ".....222222.....", "................"],
        'ring': ["................", "................", ".....333333.....", "...3311111133...", "..331......133..", "..31........13..",
                 ".31..........13.", ".31..........13.", ".31..........13.", ".31..........13.", "..31........13..", "..331......133..",
                 "...3311111133...", ".....333333.....", "................", "................"],
        'cup': ["................", "..222222222222..", "..211111111112..", "..213333333312..", "..213333333312..", "...2111111112...",
                "....21111112....", ".....222222.....", ".......22.......", ".......22.......", ".......22.......", "......2222......",
                "....22222222....", "....23333332....", "................", "................"],
        'hourglass': ["................", "...3333333333...", "...2111111112...", "....21111112....", "....21111112....", ".....211112.....",
                      "......2112......", ".......22.......", ".......22.......", "......2..2......", ".....2.11.2.....", "....2..11..2....",
                      "....2.1111.2....", "...2111111112...", "...3333333333...", "................"],
        'shard': ["................", ".........1......", "........111.....", "........1121....", ".......11122....", ".......11122....",
                  "......1112223...", "......1112223...", ".....11122223...", ".....1112223....", "......112223....", "......12223.....",
                  ".......2223.....", ".......223......", "........3.......", "................"],
        'book': ["................", "..222222222222..", "..211111111112..", "..213131313112..", "..211111111112..", "..213333333312..",
                 "..211111111112..", "..213131313112..", "..211111111112..", "..213333333312..", "..211111111112..", "..211111111112..",
                 "..222222222222..", "...2222222222...", "................", "................"],
        'drop': ["................", ".......11.......", ".......11.......", "......1111......", "......1121......", ".....111221.....",
                 ".....111222.....", "....11112222....", "....11122223....", "....11122223....", "....11222233....", "....12222233....",
                 ".....222233.....", "......2333......", "................", "................"],
    }
    for it in IT.WEAPONS + IT.RELICS:
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        px = img.load()
        cols = [hex_rgb(c) for c in it['colors']]
        rows = SHAPES[it['shape']]
        for y, row in enumerate(rows):
            for x, ch in enumerate(row.ljust(16, '.')[:16]):
                if ch != '.':
                    px[x, y] = cols[int(ch) - 1] + (255,)
        # Contour sombre : l'icône reste lisible sur n'importe quel fond.
        outline = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        po = outline.load()
        for y in range(16):
            for x in range(16):
                if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    po[x, y] = (24, 18, 36, 255)
        outline.alpha_composite(img)
        outline.save(os.path.join(out_dir, it['id'] + '.png'))


def check_book():
    """Vérifie que chaque page du carnet tient dans un livre (14 lignes de 114 pixels), largeurs de la police vanilla."""
    narrow = {'i': 2, 'l': 3, 't': 4, 'I': 4, 'f': 5, 'k': 5, '.': 2, ',': 2, ':': 2, ';': 2, '!': 2, "'": 2, ' ': 4, '’': 3}
    def width(word):
        return sum(narrow.get(ch, 6) for ch in word)
    for lang_name, table in (('fr', LANG_FR), ('en', LANG_EN)):
        for key, text in table.items():
            if not key.startswith('trounoir.book.page'):
                continue
            lines = 0
            for para in text.split('\n'):
                lines += 1
                x = 0
                for word in para.split(' '):
                    w = width(word)
                    if x and x + 4 + w > 114:
                        lines += 1
                        x = w
                    else:
                        x += (4 if x else 0) + w
            if lines > 14:
                raise SystemExit('%s (%s) : %d lignes, la page déborde' % (key, lang_name, lines))


def gen_block_assets():
    write_json(os.path.join(ASSETS, 'blockstates', 'helm.json'), {'variants': {'': {'model': 'trounoir:block/helm'}}})
    write_json(os.path.join(ASSETS, 'models', 'block', 'helm.json'),
               {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'trounoir:block/helm'}})
    write_json(os.path.join(ASSETS, 'models', 'item', 'helm.json'), {'parent': 'trounoir:block/helm'})
    write_json(os.path.join(ASSETS, 'models', 'item', 'character_egg.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/spawn_egg', 'layer1': 'minecraft:item/spawn_egg_overlay'}})
    write_json(os.path.join(ASSETS, 'models', 'item', 'essence.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/nether_star'}})


def gen_lang():
    write_json(os.path.join(ASSETS, 'lang', 'fr_fr.json'), dict(sorted(LANG_FR.items())))
    write_json(os.path.join(ASSETS, 'lang', 'en_us.json'), dict(sorted(LANG_EN.items())))


# ------------------------------------------------------------------------------------------------- documentation
def scale(level, decay=0.7):
    return sum(decay ** i for i in range(level))


def describe_bonus(attrs, stats, relief, effects):
    parts = []
    names = {'hp': 'PV', 'armor': 'armure', 'atk': 'dégâts', 'spd': 'vitesse', 'aspd': "vit. d'attaque", 'kb': 'rés. recul',
             'break': 'minage', 'jump': 'saut', 'fall': 'chute sûre', 'step': 'enjambée', 'luck': 'chance', 'oxy': 'souffle',
             'swim': 'nage', 'sneak': 'vit. accroupi', 'burn': 'durée de brûlure', 'reach': 'allonge', 'breach': 'portée blocs',
             'sweep': 'balayage'}
    for k, v in attrs.items():
        pct = A.ATTR[k][1] != 'add_value'
        parts.append('%s %+g%s' % (names[k], v * 100 if pct else v, ' %' if pct else ''))
    for k, v in stats.items():
        parts.append('%s %+g' % (k, v))
    for k, v in relief.items():
        parts.append('soulage %s %g' % (k, v))
    for e, a, m in effects:
        parts.append('%s (niv. %d+)' % (e.split(':')[1], m))
    return ', '.join(parts)


def gen_balance_tables():
    lines = []
    w = lines.append
    w('<!-- Tableaux générés par tools/gen/gen.py : ne pas modifier à la main, relancer le script. -->')
    w('')
    w('### Traits')
    w('')
    w('| Trait | Étiquettes | Niv. max | Puissance / niv. | Par niveau (avant rendement décroissant) |')
    w('|---|---|---|---|---|')
    for t in A.TRAITS:
        w('| %s%s | %s | %d | %g | %s |' % (t['fr'], ' ★' if t['signature'] else '', ', '.join(t['tags']), t['max_level'], t['power'],
                                         describe_bonus(t['attrs'], t['stats'], t['relief'], t['effects'])))
    w('')
    w('★ = capacité signature d\'un personnage unique.')
    w('')
    w('### Techniques')
    w('')
    w('| Technique | Type | Recharge (s) | Faim | Puissance | Paramètres principaux |')
    w('|---|---|---|---|---|---|')
    for q in A.TECHNIQUES:
        keys = ('damage', 'radius', 'range', 'cone_angle', 'strength', 'heal', 'duration', 'count', 'knockback', 'pull', 'ignite')
        params = ', '.join('%s %g' % (k, q['params'][k]) for k in keys if k in q['params'])
        w('| %s%s | %s | %g | %d | %g | %s |' % (q['fr'], ' ★' if q['signature'] else '', q['type'], q['cooldown'], q['hunger'], q['power'], params))
    w('')
    w('### Synergies')
    w('')
    w('| Synergie | Condition | Bonus | Puissance |')
    w('|---|---|---|---|')
    for id, fr, en, tags, traits, attrs, stats, power, effects in A.SYNERGIES:
        cond = ' + '.join('%d × %s' % (n, t) for t, n in tags.items()) or ' + '.join(traits)
        w('| %s | %s | %s | %g |' % (fr, cond, describe_bonus(attrs, stats, {}, effects), power))
    w('')
    w('### Fragilité divine')
    w('')
    w('| Handicap | Effet au départ | Points de soulagement pour l\'effacer |')
    w('|---|---|---|')
    for id, fr, en, attribute, amount, needed, hunger in A.HANDICAPS:
        effect = 'faim accélérée' if hunger else '%s %+g' % (attribute[0].split('.')[-1], amount)
        w('| %s (`%s`) | %s | %s |' % (fr, id, effect, needed if needed else 'jamais : les traits d\'endurance rendent des cœurs'))
    w('')
    w('### Mobs vanilla')
    w('')
    w('| Créature | Rang | Gains (probabilité) |')
    w('|---|---|---|')
    for entity, profile in sorted(W.PROFILES.items(), key=lambda kv: (kv[1][0], kv[0])):
        gains = ', '.join((x['id'].split(':')[1] + ' (%d %%)' % round(x['chance'] * 100)) if isinstance(x, dict) else x.split(':')[1] for x in profile[1])
        w('| %s | %d | %s |' % (entity, profile[0], gains))
    w('')
    w('### Personnages')
    w('')
    w('| Personnage | Catégorie | Rang | PV | Dégâts | Armure | Faiblesse | Attaques |')
    w('|---|---|---|---|---|---|---|---|')
    for c in W.CHARACTERS:
        moves = ', '.join('%s (%d dég., annonce %d t)' % (m['id'], m.get('damage', 0), m['telegraph']) for m in c.get('moves', [])) or 'IA vanilla'
        w('| %s | %s | %d | %d | %d | %s | %s | %s |' % (c['fr'], c['category'], c['rank'], c['health'], c['damage'], c.get('armor', 0),
                                                    c.get('weakness', '—') or '—', moves))
    w('')
    w('### Lieux')
    w('')
    w('| Lieu | Continent | Plan | Distance | Angle | Habitants |')
    w('|---|---|---|---|---|---|')
    for s in W.SITES:
        w('| %s | %s | %s | %d | %d° | %s |' % (s['fr'], 'Diviniarth' if s['dimension'].endswith('diviniarth') else 'Nébulosia', s['blueprint'],
                                              s['distance'], s['angle'], ', '.join('%s × %d' % r for r in s['residents'])))
    w('')
    # Budget de puissance : ce qu'une vie peut atteindre, palier par palier.
    def total(items):
        return sum(i['power'] * scale(i['max_level']) for i in items)

    by_id = {i['id']: i for i in A.TRAITS + A.TECHNIQUES}
    vanilla_ids = set()
    for profile in W.PROFILES.values():
        for x in profile[1]:
            vanilla_ids.add((x['id'] if isinstance(x, dict) else x).split(':')[1])
    vanilla = total([by_id[i] for i in vanilla_ids if i in by_id])

    def tier(pred):
        ids = set()
        for c in W.CHARACTERS:
            if pred(c):
                for x in c.get('grants', []):
                    ids.add((x['id'] if isinstance(x, dict) else x).split(':')[1])
        return total([by_id[i] for i in ids if i in by_id and i not in vanilla_ids])

    w('### Budget de puissance (tout absorbé au niveau maximal, hors synergies)')
    w('')
    w('| Palier | Puissance ajoutée |')
    w('|---|---|')
    w('| Mobs vanilla | %d |' % vanilla)
    w('| Mortels de Nébulosia (PNJ, roi, Supernovas) | %d |' % tier(lambda c: c['faction'] == 'nebulosia'))
    w('| Demi-dieux | %d |' % tier(lambda c: c['category'] == 'demigod'))
    w('| Empire d\'Azalée (armée, Hypernovas, Segaroth) | %d |' % tier(lambda c: c['faction'] == 'azalee'))
    w('| Dieux | %d |' % tier(lambda c: c['category'] == 'god'))
    w('')
    return '\n'.join(lines)


def main():
    gen_data()
    gen_textures()
    gen_block_assets()
    gen_item_textures()
    check_book()
    gen_lang()
    # docs/EQUILIBRAGE.md : le texte au-dessus du marqueur est écrit à la main, les tableaux en dessous sont régénérés.
    marker = '<!-- TABLES -->'
    doc = os.path.join(ROOT, 'docs', 'EQUILIBRAGE.md')
    head = ''
    if os.path.exists(doc):
        with open(doc, encoding='utf-8') as f:
            head = f.read().split(marker)[0]
    with open(doc, 'w', encoding='utf-8') as f:
        f.write(head.rstrip('\n') + '\n\n' + marker + '\n' + gen_balance_tables())
    counts = {d: len(os.listdir(os.path.join(DATA, d))) for d in sorted(os.listdir(DATA))}
    print('datapack :', counts)
    print('langue   :', len(LANG_FR), 'clés')


if __name__ == '__main__':
    main()
