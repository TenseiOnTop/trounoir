#!/usr/bin/env python3
"""Vérifie que chaque identifiant vanilla cité par les données existe bien en 1.21.1.

    python3 tools/check_ids.py <dossier des registres>   (ex. un clone de misode/mcmeta, étiquette 1.21.1-registries)
"""
import glob
import json
import os
import re
import sys

reg_dir = sys.argv[1]


def reg(name):
    with open(os.path.join(reg_dir, name, 'data.json')) as f:
        return set('minecraft:' + x for x in json.load(f))


REG = {n: reg(n) for n in ('item', 'block', 'entity_type', 'attribute', 'mob_effect', 'particle_type', 'sound_event', 'recipe')}
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
data = os.path.join(root, 'src', 'main', 'resources', 'data', 'trounoir', 'trounoir')
errors = []


def need(kind, value, where):
    if isinstance(value, str) and value.startswith('minecraft:') and value not in REG[kind]:
        errors.append('%s : %s inconnu (%s)' % (where, value, kind))


def walk(obj, where):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ('icon', 'held_item'):
                need('item', v, where)
            elif k in ('primary', 'secondary', 'accent'):
                need('block', v, where)
            elif k in ('entity', 'base_entity'):
                need('entity_type', v, where)
            elif k == 'attribute':
                need('attribute', v, where)
            elif k == 'effect':
                need('mob_effect', v, where)
            elif k == 'particle':
                need('particle_type', v, where)
            elif k == 'sound':
                need('sound_event', v, where)
            elif k == 'recipes':
                for r in v:
                    need('recipe', r, where)
            walk(v, where)
    elif isinstance(obj, list):
        for v in obj:
            walk(v, where)


for path in glob.glob(os.path.join(data, '**', '*.json'), recursive=True):
    with open(path, encoding='utf-8') as f:
        walk(json.load(f), os.path.relpath(path, data))

# Identifiants cités directement dans le code Java (Fx.sound / Fx.particle / Fx.effect / blocs des plans).
java = ''
for path in glob.glob(os.path.join(root, 'src', 'main', 'java', '**', '*.java'), recursive=True):
    with open(path, encoding='utf-8') as f:
        java += f.read()
for m in re.finditer(r'Fx\.(?:play|playTo|sound)\([^;]*?"([a-z_.]+\.[a-z_.]+)"', java):
    need('sound_event', 'minecraft:' + m.group(1), 'java son')
for m in re.finditer(r'Fx\.(?:particle|burst)\((?:level, )?"([a-z_]+)"', java):
    need('particle_type', 'minecraft:' + m.group(1), 'java particule')
for m in re.finditer(r'Fx\.effect\(\w+, "([a-z_]+)"', java):
    need('mob_effect', 'minecraft:' + m.group(1), 'java effet')
builders = open(os.path.join(root, 'src', 'main', 'java', 'net', 'trounoir', 'world', 'StructureBuilders.java'), encoding='utf-8').read()
for m in re.finditer(r'\bb\("([a-z_]+)"\)', builders):
    need('block', 'minecraft:' + m.group(1), 'java bloc')
for m in re.finditer(r'"(minecraft:(?:generic|player)\.[a-z_.]+)"', java):
    need('attribute', m.group(1), 'java attribut')

# Tous les mobs vivants de la 1.21.1 ont-ils un profil d'absorption ?
NOT_MOBS = {'area_effect_cloud', 'armor_stand', 'arrow', 'block_display', 'boat', 'breeze_wind_charge', 'chest_boat', 'chest_minecart',
            'command_block_minecart', 'dragon_fireball', 'egg', 'end_crystal', 'ender_pearl', 'evoker_fangs', 'experience_bottle',
            'experience_orb', 'eye_of_ender', 'falling_block', 'fireball', 'firework_rocket', 'fishing_bobber', 'furnace_minecart',
            'glow_item_frame', 'hopper_minecart', 'interaction', 'item', 'item_display', 'item_frame', 'leash_knot', 'lightning_bolt',
            'llama_spit', 'marker', 'minecart', 'ominous_item_spawner', 'painting', 'player', 'potion', 'shulker_bullet', 'small_fireball',
            'snowball', 'spawner_minecart', 'spectral_arrow', 'text_display', 'tnt', 'tnt_minecart', 'trident', 'wind_charge', 'wither_skull'}
profiled = set()
for path in glob.glob(os.path.join(data, 'profiles', '*.json')):
    with open(path) as f:
        profiled.add(json.load(f)['entity'])
missing = sorted(e for e in REG['entity_type'] if e.split(':')[1] not in NOT_MOBS and e not in profiled)
if missing:
    errors.append('mobs sans profil : ' + ', '.join(missing))

print('\n'.join(errors) if errors else 'Tous les identifiants vanilla sont valides ; tous les mobs vivants ont un profil.')
sys.exit(1 if errors else 0)
