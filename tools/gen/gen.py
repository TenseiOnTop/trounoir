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

    for entity, profile in W.PROFILES.items():
        obj = {'entity': 'minecraft:' + entity, 'rank': profile[0], 'grants': profile[1]}
        if len(profile) > 2:
            obj['faction'] = profile[2]
            obj['kill_reputation'] = profile[3]
        write_json(os.path.join(DATA, 'profiles', entity + '.json'), obj)

    for c in W.CHARACTERS:
        obj = {'unique': c.get('unique', False), 'category': c['category'], 'faction': c['faction'], 'rank': c['rank'],
               'behavior': c.get('behavior', 'civilian'), 'health': c['health'], 'damage': c['damage']}
        for key in ('base_entity', 'armor', 'speed', 'scale', 'knockback_resistance', 'follow_range', 'held_item', 'phases',
                    'weakness', 'kill_reputation', 'notoriety', 'language', 'dialogue_lines', 'boss_bar'):
            if key in c:
                obj[key] = c[key]
        if 'skin' in c:
            obj['texture'] = 'trounoir:textures/entity/character/%s.png' % c['id']
        if c.get('weakness'):
            obj['weakness_multiplier'] = 1.5
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
        if s['crossing']:
            obj['crossing'] = True
        write_json(os.path.join(DATA, 'sites', s['id'] + '.json'), obj)
        lang('site.trounoir.' + s['id'], s['fr'], s['en'])

    for code, table in (('fr_fr', X.EPITAPHS_FR), ('en_us', X.EPITAPHS_EN)):
        write_json(os.path.join(DATA, 'epitaphs', code + '.json'), dict(language=code, **table))

    # La dimension de Diviniarth : même graine que l'Overworld, relief « grands biomes » (terres plus vastes).
    write_json(os.path.join(RES, 'data', 'trounoir', 'dimension', 'diviniarth.json'), {
        'type': 'minecraft:overworld',
        'generator': {'type': 'minecraft:noise', 'settings': 'minecraft:large_biomes',
                      'biome_source': {'type': 'minecraft:multi_noise', 'preset': 'minecraft:overworld'}}})

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

    def paint(kind, primary, secondary, accent, seed):
        rng = random.Random(seed)
        img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        px = img.load()
        p, s, a = hex_rgb(primary), hex_rgb(secondary), hex_rgb(accent)
        skin = {'god': shade(a, 0.95), 'emperor': (214, 190, 170)}.get(kind, rng.choice([(226, 190, 160), (198, 150, 120), (150, 105, 80), (236, 208, 184)]))
        hair = {'god': p, 'peasant': (88, 62, 40), 'king': (200, 200, 205)}.get(kind, shade(s, 0.6))

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
        eye = a if kind in ('god', 'hypernova', 'emperor') else (40, 40, 50)
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
        if kind in ('king', 'emperor', 'god'):
            # Couronne sur la seconde couche de la tête (chapeau).
            for x in range(32, 64):
                px[x, 9] = a + (255,)
                if x % 2 == 0:
                    px[x, 8] = a + (255,)
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
            paint(kind, p, s, a, c['id']).save(os.path.join(out_dir, c['id'] + '.png'))

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


def gen_block_assets():
    write_json(os.path.join(ASSETS, 'blockstates', 'helm.json'), {'variants': {'': {'model': 'trounoir:block/helm'}}})
    write_json(os.path.join(ASSETS, 'models', 'block', 'helm.json'),
               {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'trounoir:block/helm'}})
    write_json(os.path.join(ASSETS, 'models', 'item', 'helm.json'), {'parent': 'trounoir:block/helm'})


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
