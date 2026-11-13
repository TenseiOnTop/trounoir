# -*- coding: utf-8 -*-
"""Tables de contenu : profils d'absorption des mobs vanilla, personnages, factions, lieux."""


def g(id, chance=1.0):
    """Un gain d'absorption."""
    return {'id': 'trounoir:' + id, 'chance': chance} if chance < 1.0 else 'trounoir:' + id


# ---------------------------------------------------------------------------------------------
# Profils d'absorption : TOUS les mobs vivants de Minecraft 1.21.1.
# entité -> (rang, gains, faction, réputation perdue en le tuant)
# ---------------------------------------------------------------------------------------------
PROFILES = {
    # --- rang 0 : petites bêtes
    'chicken': (0, [g('chair_rustique'), g('patte_legere', 0.5)]),
    'rabbit': (0, [g('patte_legere'), g('ressort', 0.5), g('bond', 0.3)]),
    'cod': (0, [g('nage')]),
    'salmon': (0, [g('nage')]),
    'tropical_fish': (0, [g('nage')]),
    'pufferfish': (0, [g('venin'), g('epines', 0.5)]),
    'tadpole': (0, [g('nage')]),
    'bat': (0, [g('echolocation')]),
    'parrot': (0, [g('oeil_d_aigle')]),
    'bee': (0, [g('venin')]),
    'frog': (0, [g('ressort'), g('bond', 0.4)]),
    'allay': (0, [g('esprit_malin')]),
    'axolotl': (0, [g('regeneration_lente'), g('nage')]),
    'squid': (0, [g('nage')]),
    'glow_squid': (0, [g('nage'), g('yeux_de_nuit', 0.5)]),
    'armadillo': (0, [g('carapace')]),
    'snow_golem': (0, [g('givre_errant')]),
    'silverfish': (0, [g('fouisseur')]),
    'endermite': (0, [g('fouisseur'), g('voile_d_ender', 0.3)]),
    # --- rang 1 : gros animaux et monstres communs
    'cow': (1, [g('chair_rustique')]),
    'mooshroom': (1, [g('chair_rustique'), g('regeneration_lente', 0.3)]),
    'pig': (1, [g('chair_rustique')]),
    'sheep': (1, [g('chair_rustique')]),
    'horse': (1, [g('galop'), g('cuir_epais', 0.5)]),
    'donkey': (1, [g('cuir_epais'), g('galop', 0.5)]),
    'mule': (1, [g('cuir_epais'), g('galop', 0.5)]),
    'skeleton_horse': (1, [g('galop'), g('os_secs', 0.5)]),
    'zombie_horse': (1, [g('galop'), g('vigueur_morte', 0.5)]),
    'llama': (1, [g('cuir_epais')]),
    'trader_llama': (1, [g('cuir_epais')]),
    'camel': (1, [g('galop'), g('cuir_epais')]),
    'cat': (1, [g('patte_legere'), g('pas_feutre'), g('yeux_de_nuit', 0.5)]),
    'ocelot': (1, [g('patte_legere'), g('pas_feutre'), g('yeux_de_nuit', 0.5)]),
    'fox': (1, [g('patte_legere'), g('pas_feutre')]),
    'wolf': (1, [g('meute'), g('croc_et_griffe', 0.5)]),
    'goat': (1, [g('ressort'), g('charge', 0.6)]),
    'turtle': (1, [g('carapace'), g('nage')]),
    'dolphin': (1, [g('nage'), g('echolocation')]),
    'sniffer': (1, [g('fouisseur'), g('cuir_epais')]),
    'strider': (1, [g('sang_de_braise')]),
    'villager': (1, [g('labeur_humain'), g('sens_du_troc'), g('savoir_artisan', 0.5)], 'nebulosia', -8),
    'wandering_trader': (1, [g('sens_du_troc'), g('carte_du_havre', 0.6)], 'nebulosia', -4),
    'zombie': (1, [g('vigueur_morte')]),
    'husk': (1, [g('vigueur_morte')]),
    'zombie_villager': (1, [g('vigueur_morte'), g('labeur_humain', 0.3)]),
    'drowned': (1, [g('vigueur_morte'), g('souffle_marin', 0.5)]),
    'skeleton': (1, [g('os_secs')]),
    'stray': (1, [g('os_secs'), g('givre_errant'), g('souffle_de_givre', 0.5)]),
    'bogged': (1, [g('os_secs'), g('venin')]),
    'spider': (1, [g('yeux_de_nuit'), g('bond', 0.6)]),
    'cave_spider': (1, [g('venin'), g('yeux_de_nuit')]),
    'creeper': (1, [g('poudre_vive'), g('detonation', 0.6)]),
    'slime': (1, [g('ressort')]),
    'zombified_piglin': (1, [g('vigueur_morte'), g('force_brute', 0.5)]),
    'phantom': (1, [g('plane'), g('oeil_d_aigle')]),
    # --- rang 2 : créatures dangereuses
    'polar_bear': (2, [g('colosse'), g('givre_errant'), g('croc_et_griffe')]),
    'panda': (2, [g('colosse')]),
    'enderman': (2, [g('voile_d_ender'), g('clignement', 0.7)]),
    'witch': (2, [g('sang_arcanique'), g('regeneration_lente'), g('breuvage', 0.6), g('alchimie', 0.5)]),
    'pillager': (2, [g('oeil_d_aigle'), g('tir_tendu', 0.5)]),
    'vindicator': (2, [g('force_brute')]),
    'piglin': (2, [g('sens_du_troc'), g('force_brute', 0.5)]),
    'hoglin': (2, [g('colosse'), g('charge', 0.6)]),
    'zoglin': (2, [g('colosse'), g('vigueur_morte')]),
    'blaze': (2, [g('sang_de_braise'), g('salve_de_braise', 0.7)]),
    'magma_cube': (2, [g('sang_de_braise'), g('ressort')]),
    'guardian': (2, [g('epines'), g('souffle_marin'), g('rayon_gardien', 0.4)]),
    'vex': (2, [g('esprit_malin')]),
    'shulker': (2, [g('carapace')]),
    'breeze': (2, [g('souffle_vif'), g('rafale', 0.7)]),
    # --- rang 3 : élites
    'iron_golem': (3, [g('masse_de_fer')], 'nebulosia', -5),
    'ravager': (3, [g('colosse'), g('rugissement')]),
    'evoker': (3, [g('sang_arcanique'), g('crocs')]),
    'illusioner': (3, [g('sang_arcanique'), g('voile_illusoire')]),
    'piglin_brute': (3, [g('force_brute'), g('discipline')]),
    'wither_skeleton': (3, [g('fletrissure'), g('force_brute', 0.5)]),
    'ghast': (3, [g('boule_de_feu'), g('regeneration_lente')]),
    'giant': (3, [g('colosse'), g('vigueur_morte')]),
    # --- rang 4 à 6 : les grands monstres de Minecraft
    'elder_guardian': (4, [g('regard_ancien'), g('rayon_gardien'), g('epines')]),
    'wither': (5, [g('couronne_fletrie'), g('crane_fletri'), g('fletrissure')]),
    'warden': (6, [g('ossature_abyssale'), g('onde_sonique'), g('echolocation')]),
    'ender_dragon': (6, [g('sang_de_dragon'), g('souffle_du_dragon')]),
}


# ---------------------------------------------------------------------------------------------
# Attaques (toutes annoncées). Tailles en blocs, durées en ticks (20 ticks = 1 s).
# ---------------------------------------------------------------------------------------------
def fx(effect, duration, amplifier=0):
    return {'effect': 'minecraft:' + effect, 'duration': duration, 'amplifier': amplifier}


def _mv(defaults, kw):
    """Fusionne les valeurs par défaut d'une attaque et les surcharges données."""
    d = dict(defaults)
    d.update(kw)
    return d


def slash(dmg, size=3.0, angle=100, tele=14, cd=30, **kw):
    """Coup de taille : cône devant soi."""
    return _mv(dict(id='slash', shape='cone', size=size, angle=angle, damage=dmg, telegraph=tele, cooldown=cd,
                    max_range=size, weight=3, particle='minecraft:sweep_attack',
                    sound='minecraft:entity.player.attack.sweep'), kw)


def lunge(dmg, length=9.0, tele=20, cd=90, **kw):
    """Fente : le personnage traverse une ligne."""
    return _mv(dict(id='lunge', shape='line', size=length, width=2.0, damage=dmg, telegraph=tele, cooldown=cd,
                    min_range=3, max_range=length, pre='dash', weight=2, particle='minecraft:crit',
                    sound='minecraft:entity.player.attack.strong'), kw)


def slam(dmg, radius=5.0, tele=24, cd=120, **kw):
    """Frappe au sol : cercle autour de soi."""
    return _mv(dict(id='slam', shape='circle', size=radius, anchor='self', damage=dmg, telegraph=tele, cooldown=cd,
                    max_range=radius, knockback=1.0, weight=2, particle='minecraft:poof',
                    sound='minecraft:entity.generic.explode'), kw)


def shot(kind, dmg, count=1, tele=16, cd=60, lo=5, hi=22, **kw):
    """Tir : projectiles vers la cible, après une visée en ligne. (Les dégâts ne valent que pour les flèches.)"""
    return _mv(dict(id='shot', shape='line', size=hi, width=1.0, damage=dmg if kind == 'arrow' else 0, telegraph=tele,
                    cooldown=cd, min_range=lo, max_range=hi, projectile=kind, projectile_count=count, weight=3,
                    knockback=0, particle='minecraft:crit', sound='minecraft:entity.arrow.shoot'), kw)


def meteors(dmg, radius=3.0, count=4, spread=7.0, tele=30, cd=140, lo=3, hi=26, **kw):
    """Pluie : plusieurs cercles autour de la cible."""
    return _mv(dict(id='meteors', shape='circle', size=radius, anchor='target', count=count, spread=spread,
                    damage=dmg, telegraph=tele, cooldown=cd, min_range=lo, max_range=hi, weight=2,
                    particle='minecraft:flame', sound='minecraft:entity.blaze.shoot'), kw)


def ambush(dmg, radius=3.0, tele=18, cd=110, lo=4, hi=24, **kw):
    """Surgir : le personnage se téléporte sur la zone annoncée et frappe."""
    return _mv(dict(id='ambush', shape='circle', size=radius, anchor='target', damage=dmg, telegraph=tele, cooldown=cd,
                    min_range=lo, max_range=hi, pre='teleport', weight=2, particle='minecraft:portal',
                    sound='minecraft:entity.enderman.teleport'), kw)


def retreat(distance=12.0, tele=10, cd=120, hi=8, **kw):
    """Repli : le personnage se téléporte à distance quand on l'approche de trop près."""
    return _mv(dict(id='retreat', shape='circle', size=1.5, anchor='self', damage=0, telegraph=tele, cooldown=cd,
                    min_range=0, max_range=hi, pre='retreat', spread=distance, weight=3, knockback=0,
                    particle='minecraft:portal', sound='minecraft:entity.enderman.teleport'), kw)


def ray(dmg, length=24.0, width=2.0, tele=26, cd=100, lo=4, **kw):
    """Rayon : une ligne large, figée à l'annonce."""
    return _mv(dict(id='ray', shape='line', size=length, width=width, damage=dmg, telegraph=tele, cooldown=cd,
                    min_range=lo, max_range=length, weight=2, particle='minecraft:end_rod',
                    sound='minecraft:entity.warden.sonic_boom'), kw)


def call(character, count, cd=400, **kw):
    """Appel de renforts."""
    return _mv(dict(id='call', shape='circle', size=2.0, anchor='self', damage=0, telegraph=30, cooldown=cd,
                    max_range=30, summon='trounoir:' + character, summon_count=count, weight=1, knockback=0,
                    particle='minecraft:note', sound='minecraft:event.raid.horn'), kw)


def C(id, fr, en, category, faction, rank, health, damage, **kw):
    d = dict(id=id, fr=fr, en=en, category=category, faction=faction, rank=rank, health=health, damage=damage)
    d.update(kw)
    return d


def death(faction, rep, alert=24000, mourning=0, hunt=0, message=None, vengeance=None, successor=None, replaces=None):
    """Réactions du monde à la mort d'un personnage unique."""
    out = [{'type': 'reputation', 'faction': faction, 'amount': rep}]
    if alert:
        out.append({'type': 'alert', 'faction': faction, 'duration': alert})
    if mourning:
        out.append({'type': 'mourning', 'faction': faction, 'duration': mourning, 'only_if_player': False})
    if hunt:
        out.append({'type': 'hunt', 'faction': faction, 'waves': hunt, 'interval': 3600, 'delay': 1200})
    if vengeance:
        out.append({'type': 'vengeance', 'character': 'trounoir:' + vengeance, 'delay': 2400})
    if successor:
        out.append({'type': 'successor', 'character': 'trounoir:' + successor, 'replaces': 'trounoir:' + replaces,
                    'delay': 24000, 'only_if_player': False})
    if message:
        out.append({'type': 'message', 'key': 'trounoir.reaction.' + message, 'only_if_player': False})
    return out


NEB = 'trounoir:langue_commune'
AZA = 'trounoir:haut_azaleen'

CHARACTERS = [
    # ================================================================== Nébulosia : gens du commun
    C('paysan', 'Paysan', 'Peasant', 'peasant', 'nebulosia', 1, 20, 2, behavior='civilian', speed=0.25,
      grants=[g('labeur_humain'), g('langue_commune'), g('chemin_de_clairmoulin', 0.5)], kill_reputation={'nebulosia': -10},
      language=NEB, dialogue_lines=4, skin=('peasant', '#8a6a4a', '#5a4632', '#c9a27a')),
    C('soldat', 'Soldat de Nébulosia', 'Nebulosian Soldier', 'soldier', 'nebulosia', 2, 28, 5, behavior='guard', armor=6,
      held_item='minecraft:iron_sword', moves=[slash(5, tele=16)],
      grants=[g('discipline'), g('langue_commune', 0.5), g('route_d_astrelune', 0.5), g('rumeur_de_gorgath', 0.25)],
      kill_reputation={'nebulosia': -12}, language=NEB, dialogue_lines=3, skin=('soldier', '#3f5f8a', '#9aa0a8', '#d0d4da')),
    C('chevalier', 'Chevalier', 'Knight', 'knight', 'nebulosia', 3, 44, 8, behavior='guard', armor=12, speed=0.27,
      held_item='minecraft:iron_sword', moves=[slash(8, tele=14), lunge(10, 8, 20, 100)],
      grants=[g('art_de_l_epee'), g('discipline'), g('estoc', 0.5), g('carte_du_havre', 0.5)],
      kill_reputation={'nebulosia': -15}, language=NEB, dialogue_lines=3, skin=('knight', '#c8ccd4', '#3f5f8a', '#e6d08a')),
    C('mage_royal', 'Mage royal', 'Royal Mage', 'mage', 'nebulosia', 3, 30, 7, behavior='guard', armor=2, speed=0.26,
      held_item='minecraft:blaze_rod',
      moves=[shot('small_fireball', 7, 1, 16, 50, 4, 18), slam(8, 4, 26, 160, particle='minecraft:witch', sound='minecraft:entity.illusioner.cast_spell')],
      grants=[g('sang_arcanique'), g('trait_arcanique', 0.6), g('traite_des_etoiles', 0.3), g('cieux_cartographies', 0.5)],
      kill_reputation={'nebulosia': -15}, language=NEB, dialogue_lines=3, skin=('mage', '#4a3a7a', '#c792ea', '#e6d08a')),

    # ================================================================== Nébulosia : uniques
    C('roi_aurele', 'Aurèle III, Roi de Nébulosia', 'Aurèle III, King of Nebulosia', 'king', 'nebulosia', 2, 40, 4,
      unique=True, behavior='guard', armor=8, held_item='minecraft:golden_sword', moves=[slash(4, tele=18)],
      grants=[g('sang_royal'), g('langue_commune'), g('secret_du_trone')], kill_reputation={'nebulosia': -60},
      on_death=death('nebulosia', -40, 48000, 72000, 3, 'king_dead'), language=NEB, dialogue_lines=3, boss_bar=False,
      skin=('king', '#7a1f2a', '#e6c14a', '#f4e3a0'), title_fr='Roi de Nébulosia', title_en='King of Nebulosia'),
    C('aldric', 'Aldric Solbrand', 'Aldric Solbrand', 'supernova', 'nebulosia', 4, 140, 11, unique=True, behavior='guard',
      armor=14, speed=0.29, knockback_resistance=0.3, held_item='minecraft:golden_sword', phases=[0.5],
      moves=[slash(11, 3.5, 110, 14, 30),
             lunge(13, 10, 20, 90),
             slam(14, 5.5, 26, 150, particle='minecraft:flame', min_phase=1, id='solar_ring')],
      grants=[g('flamme_d_aldric'), g('lame_solaire'), g('carte_du_havre')], kill_reputation={'nebulosia': -40},
      on_death=death('nebulosia', -20, 24000, 24000, 2, 'supernova_fallen', vengeance='lysmene'),
      weakness='magic', language=NEB, dialogue_lines=3, skin=('supernova', '#d8a03a', '#7a1f2a', '#ffd866'),
      title_fr='Capitaine des Supernovas, la Lame solaire', title_en='Captain of the Supernovas, the Solar Blade'),
    C('lysmene', 'Lysmène Givreclair', 'Lysmène Givreclair', 'supernova', 'nebulosia', 4, 110, 12, unique=True, behavior='guard',
      armor=6, speed=0.27, held_item='minecraft:amethyst_shard', phases=[0.5],
      moves=[shot('small_fireball', 10, 2, 16, 50, 4, 20, particle='minecraft:snowflake'),
             meteors(12, 3, 3, 6, 30, 120, particle='minecraft:snowflake', effects=[fx('slowness', 80, 2)], id='ice_rain'),
             slam(10, 6, 28, 180, particle='minecraft:snowflake', effects=[fx('slowness', 100, 4)], min_phase=1, id='ice_prison')],
      grants=[g('givre_de_lysmene'), g('prison_de_glace'), g('cieux_cartographies')], kill_reputation={'nebulosia': -40},
      on_death=death('nebulosia', -20, 24000, 24000, 2, 'supernova_fallen', vengeance='aldric'),
      weakness='fire', language=NEB, dialogue_lines=3, skin=('supernova', '#bfe6ff', '#4a6a9a', '#ffffff'),
      title_fr='Supernova, Mage du givre', title_en='Supernova, Frost Mage'),
    C('gorgath', 'Gorgath, le Ravageur ancien', 'Gorgath, the Elder Ravager', 'demigod', 'sauvage', 5, 240, 16, unique=True,
      behavior='hostile', base_entity='minecraft:ravager', armor=10, speed=0.32, scale=1.6, knockback_resistance=0.8,
      follow_range=32, grants=[g('fureur_de_gorgath'), g('rugissement'), g('colosse')], weakness='fire',
      on_death=[{'type': 'reputation', 'faction': 'nebulosia', 'amount': 25},
                {'type': 'message', 'key': 'trounoir.reaction.gorgath_dead'}],
      title_fr='Demi-dieu des landes', title_en='Demigod of the moors'),
    C('veuve_bleme', 'La Veuve Blême', 'The Pale Widow', 'demigod', 'sauvage', 5, 200, 14, unique=True, behavior='hostile',
      base_entity='minecraft:spider', armor=8, speed=0.36, scale=2.4, knockback_resistance=0.5, follow_range=32,
      grants=[g('toile_de_la_veuve'), g('bond'), g('venin')], weakness='fire',
      on_death=[{'type': 'reputation', 'faction': 'nebulosia', 'amount': 25},
                {'type': 'message', 'key': 'trounoir.reaction.widow_dead'}],
      title_fr='Demi-déesse des combes', title_en='Demigoddess of the hollows'),

    # ================================================================== Empire d'Azalée : gens et armée
    C('citoyen', "Citoyen d'Azalée", 'Citizen of Azalea', 'peasant', 'azalee', 1, 20, 4, behavior='civilian', fights_back=True, speed=0.25,
      grants=[g('labeur_humain'), g('haut_azaleen')], kill_reputation={'azalee': -8}, notoriety=4, language=AZA,
      dialogue_lines=4, skin=('peasant', '#a03a6a', '#5a3a4a', '#f0c8dc')),
    C('legionnaire', 'Légionnaire', 'Legionary', 'soldier', 'azalee', 4, 60, 11, behavior='guard', armor=14, speed=0.28,
      held_item='minecraft:diamond_sword', moves=[slash(11, 3, 100, 13, 26), lunge(13, 8, 18, 110)],
      grants=[g('discipline_imperiale'), g('haut_azaleen', 0.5), g('cartes_imperiales', 0.5), g('carreau', 0.3), g('veille_de_suke', 0.25), g('route_du_bastion', 0.3), g('doctrine_hypernova', 0.2)], kill_reputation={'azalee': -10},
      notoriety=6, language=AZA, dialogue_lines=3, skin=('soldier', '#a03a6a', '#3a3a48', '#f0c8dc')),
    C('magistere', 'Magistère', 'Magister', 'mage', 'azalee', 5, 64, 13, behavior='guard', armor=6, speed=0.27,
      held_item='minecraft:end_rod',
      moves=[shot('small_fireball', 13, 2, 14, 45, 4, 22), meteors(15, 3, 3, 6, 28, 130), ambush(12, 3, 18, 160, id='fold')],
      grants=[g('canalisation'), g('sang_arcanique'), g('trait_arcanique', 0.6), g('theorie_du_tir', 0.3), g('astronomie_imperiale', 0.3)],
      kill_reputation={'azalee': -15}, notoriety=10, language=AZA, dialogue_lines=2, skin=('mage', '#5a1f4a', '#f0c8dc', '#e6c14a')),

    # ================================================================== les 8 Hypernovas
    C('cledith', 'Cledith', 'Cledith', 'hypernova', 'azalee', 6, 340, 22, unique=True, behavior='guard', armor=20, speed=0.33,
      knockback_resistance=0.5, held_item='minecraft:netherite_sword', phases=[0.66, 0.33], follow_range=40,
      moves=[slash(22, 3.6, 120, 12, 20), slash(26, 5, 200, 18, 70, id='crescent', weight=2),
             lunge(26, 12, 16, 70), ambush(24, 3, 14, 100, min_phase=1, id='flash_step', particle='minecraft:sweep_attack'),
             slam(30, 7, 24, 160, min_phase=2, id='thousand_blades', style='judgement_cut', color='#f4f4ff', particle='minecraft:sweep_attack')],
      grants=[g('maitrise_de_cledith'), g('mille_lames'), g('art_de_l_epee')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='zephyrine'),
      weakness='projectile', language=AZA, dialogue_lines=3, skin=('hypernova', '#e8e8f0', '#2a2a34', '#a03a6a'),
      title_fr='Hypernova, le plus grand épéiste', title_en='Hypernova, the greatest swordsman'),
    C('suke', 'Suke', 'Suke', 'hypernova', 'azalee', 6, 260, 20, unique=True, behavior='guard', armor=10, speed=0.30,
      held_item='minecraft:spyglass', phases=[0.5], follow_range=64,
      moves=[ray(24, 40, 1.4, 22, 50, 3, id='rifle', particle='minecraft:end_rod', sound='minecraft:entity.firework_rocket.blast', weight=4),
             ray(18, 30, 4.0, 30, 130, 3, id='scatter', particle='minecraft:crit'),
             retreat(14, 10, 120, 7),
             meteors(20, 2.5, 5, 9, 26, 150, 3, 40, min_phase=1, id='mortar', particle='minecraft:end_rod', sound='minecraft:entity.firework_rocket.blast')],
      grants=[g('oeil_de_suke'), g('tir_hypernova'), g('tir_tendu')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'suke_fallen', vengeance='noctalys'),
      weakness='melee', language=AZA, dialogue_lines=3, skin=('hypernova', '#3a4a3a', '#8a8f6a', '#ffd866'),
      title_fr="Hypernova, l'Œil de l'Empire", title_en='Hypernova, the Eye of the Empire'),
    C('ignarelle', 'Ignarelle', 'Ignarelle', 'hypernova', 'azalee', 6, 280, 22, unique=True, behavior='guard', armor=10,
      speed=0.29, held_item='minecraft:blaze_rod', phases=[0.6, 0.3], follow_range=40,
      moves=[shot('small_fireball', 20, 3, 14, 40, 4, 26, particle='minecraft:flame', sound='minecraft:entity.blaze.shoot'),
             meteors(24, 3.5, 4, 8, 28, 110), slash(22, 7, 70, 20, 80, id='flame_cone', particle='minecraft:flame'),
             slam(28, 9, 30, 200, min_phase=2, id='furnace', particle='minecraft:flame')],
      grants=[g('brasier_d_ignarelle'), g('fournaise'), g('sang_de_braise')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='oriel'),
      weakness='melee', language=AZA, dialogue_lines=3, skin=('hypernova', '#c8421f', '#3a1a12', '#ffb03a'),
      title_fr='Hypernova, la Fournaise', title_en='Hypernova, the Furnace'),
    C('noctalys', 'Noctalys', 'Noctalys', 'hypernova', 'azalee', 6, 250, 24, unique=True, behavior='guard', armor=8, speed=0.38,
      held_item='minecraft:netherite_sword', phases=[0.5], follow_range=40,
      moves=[slash(22, 2.8, 90, 9, 18), ambush(28, 2.5, 14, 60, weight=4), shot('arrow', 16, 3, 12, 90, 5, 20, id='knives'),
             ambush(32, 3.5, 12, 45, min_phase=1, id='eclipse', weight=4)],
      grants=[g('linceul_de_noctalys'), g('eclipse'), g('pas_feutre')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='suke'),
      weakness='explosion', language=AZA, dialogue_lines=3, skin=('hypernova', '#14141c', '#3a2a5a', '#8a6fd1'),
      title_fr="Hypernova, l'Ombre de l'Empereur", title_en="Hypernova, the Emperor's Shadow"),
    C('bragor', 'Bragor', 'Bragor', 'hypernova', 'azalee', 6, 420, 20, unique=True, behavior='guard', armor=26, speed=0.25,
      knockback_resistance=1.0, scale=1.25, held_item='minecraft:shield', phases=[0.5], follow_range=40,
      moves=[slash(20, 3.4, 130, 16, 30, id='shield_bash', knockback=1.4), slam(26, 6, 24, 100),
             lunge(24, 9, 22, 120, id='bull_rush'), slam(0, 3, 30, 400, min_phase=1, id='second_wind', heal=60, particle='minecraft:heart')],
      grants=[g('rempart_de_bragor'), g('bastion'), g('discipline_imperiale')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='valdrek'),
      weakness='magic', language=AZA, dialogue_lines=3, skin=('hypernova', '#5a5f6a', '#a03a6a', '#c8ccd4'),
      title_fr='Hypernova, le Rempart', title_en='Hypernova, the Rampart'),
    C('zephyrine', 'Zéphyrine', 'Zéphyrine', 'hypernova', 'azalee', 6, 260, 21, unique=True, behavior='guard', armor=10, speed=0.42,
      held_item='minecraft:trident', phases=[0.6, 0.3], follow_range=40,
      moves=[slash(18, 3.2, 60, 8, 14, id='thrust'), lunge(24, 14, 12, 40, weight=4), lunge(28, 18, 10, 30, min_phase=1, id='white_lightning', style='lightning', color='#fff2a8', weight=4),
             meteors(22, 2.5, 5, 8, 20, 120, min_phase=2, id='storm', style='lightning', color='#fff2a8', particle='minecraft:electric_spark', sound='minecraft:entity.lightning_bolt.thunder')],
      grants=[g('foudre_de_zephyrine'), g('eclair_blanc'), g('souffle_vif')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='cledith'),
      weakness='backstab', language=AZA, dialogue_lines=3, skin=('hypernova', '#f4f8ff', '#6fd6ff', '#ffffff'),
      title_fr='Hypernova, la Foudre blanche', title_en='Hypernova, the White Lightning'),
    C('oriel', 'Oriel', 'Oriel', 'hypernova', 'azalee', 6, 250, 20, unique=True, behavior='guard', armor=8, speed=0.28,
      held_item='minecraft:end_rod', phases=[0.5], follow_range=40,
      moves=[ray(20, 22, 2, 22, 60), meteors(22, 3, 4, 7, 26, 100, particle='minecraft:portal', sound='minecraft:entity.enderman.teleport'),
             retreat(12, 10, 80, 7, id='fold_away'), slam(24, 8, 28, 180, min_phase=1, id='lattice', pull=1.2, particle='minecraft:portal')],
      grants=[g('geometrie_d_oriel'), g('pli_spatial'), g('canalisation')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='ignarelle'),
      weakness='melee', language=AZA, dialogue_lines=3, skin=('hypernova', '#3a2a6a', '#c792ea', '#ffffff'),
      title_fr='Hypernova, le Géomètre', title_en='Hypernova, the Geometer'),
    C('valdrek', 'Maréchal Valdrek', 'Marshal Valdrek', 'hypernova', 'azalee', 6, 320, 20, unique=True, behavior='guard', armor=20,
      speed=0.28, knockback_resistance=0.5, held_item='minecraft:netherite_axe', phases=[0.6, 0.3], follow_range=40,
      moves=[slash(20, 3.4, 110, 14, 26), slam(24, 5.5, 24, 120), call('legionnaire', 3, 500), call('legionnaire', 3, 700, min_phase=1, id='call_guard'),
             lunge(26, 10, 18, 90, min_phase=2)],
      grants=[g('commandement_de_valdrek'), g('ordre_de_charge'), g('discipline_imperiale')], kill_reputation={'azalee': -40}, notoriety=30,
      on_death=death('azalee', -20, 48000, 24000, 3, 'hypernova_fallen', vengeance='bragor'),
      weakness='backstab', language=AZA, dialogue_lines=3, skin=('hypernova', '#7a1f2a', '#2a2a34', '#e6c14a'),
      title_fr='Hypernova, le Stratège', title_en='Hypernova, the Strategist'),

    # ================================================================== l'Empereur et son usurpateur
    # Segaroth ne porte pas d'arme : mille ans lui ont appris toutes les magies.
    C('segaroth', 'Segaroth', 'Segaroth', 'emperor', 'azalee', 7, 520, 28, unique=True, behavior='guard', armor=22, speed=0.31,
      knockback_resistance=0.8, scale=1.15, phases=[0.7, 0.4, 0.15], follow_range=48,
      moves=[ray(26, 22, 2.5, 20, 60, id='arcane_lance', color='#c792ea', particle='minecraft:enchant'),
             slash(24, 7, 80, 16, 70, id='flame_school', color='#ff8a3c', particle='minecraft:flame', sound='minecraft:entity.blaze.shoot'),
             meteors(26, 3.5, 6, 10, 24, 110, id='frost_school', color='#bfe6ff', particle='minecraft:snowflake', effects=[fx('slowness', 80, 2)]),
             slam(30, 8, 26, 130, min_phase=1, id='storm_school', style='lightning', color='#fff2a8', particle='minecraft:electric_spark', sound='minecraft:entity.lightning_bolt.thunder'),
             ambush(32, 3.5, 14, 90, min_phase=1, id='shadow_school', color='#3a2a5a'),
             slam(44, 11, 34, 220, min_phase=2, id='millennium', style='nova', color='#e6c14a', particle='minecraft:soul_fire_flame'),
             slam(0, 3, 40, 900, min_phase=3, id='undying', heal=150, color='#f0c8dc', particle='minecraft:totem_of_undying')],
      grants=[g('immortalite_de_segaroth'), g('mille_ans'), g('secret_des_dieux'), g('haut_azaleen')],
      kill_reputation={'azalee': -100}, notoriety=50,
      on_death=death('azalee', -50, 72000, 144000, 4, 'emperor_dead'),
      weakness='backstab', language=AZA, dialogue_lines=4, skin=('emperor', '#2a1030', '#e6c14a', '#f0c8dc'),
      title_fr="Empereur d'Azalée, l'Immortel", title_en='Emperor of Azalea, the Immortal'),
    # ================================================================== demi-dieux de Diviniarth
    C('geolier', "Le Geôlier d'Andromède", "Andromeda's Jailer", 'demigod', 'sauvage', 6, 420, 26, unique=True, behavior='hostile',
      base_entity='minecraft:warden', armor=14, speed=0.3, scale=1.3, knockback_resistance=1.0, follow_range=32,
      grants=[g('chaines_du_geolier'), g('onde_sonique'), g('echolocation')], weakness='projectile',
      on_death=[{'type': 'message', 'key': 'trounoir.reaction.jailer_dead'}],
      title_fr="Demi-dieu, gardien d'une Déesse morte", title_en='Demigod, keeper of a dead Goddess'),
]


def god(id, fr, en, rank, health, damage, moves, grants, skin, title_fr, title_en, weakness, behavior='boss', scale=1.8,
        vengeance=None, speed=0.31, phases=(0.66, 0.33), lines=3, **kw):
    reactions = [{'type': 'reputation', 'faction': 'pantheon', 'amount': -30},
                 {'type': 'message', 'key': 'trounoir.reaction.god_dead', 'only_if_player': False}]
    if vengeance:
        reactions.append({'type': 'vengeance', 'character': 'trounoir:' + vengeance, 'delay': 6000})
    return C(id, fr, en, 'god', 'pantheon', rank, health, damage, unique=True, behavior=behavior, armor=20, speed=speed,
             scale=scale, knockback_resistance=1.0, follow_range=56, phases=list(phases), moves=moves, grants=grants,
             on_death=reactions, weakness=weakness, dialogue_lines=lines, skin=skin, title_fr=title_fr, title_en=title_en, **kw)


CHARACTERS += [
    # ================================================================== le Panthéon
    god('selene', 'Séléné', 'Selene', 8, 620, 30,
        [meteors(30, 3.5, 5, 9, 28, 90, particle='minecraft:end_rod', sound='minecraft:block.amethyst_block.chime', id='moonfall'),
         ambush(34, 3.5, 16, 100, id='new_moon'), ray(32, 26, 3, 26, 110, id='moonbeam'),
         slam(40, 11, 32, 220, min_phase=1, id='high_tide', pull=1.5, particle='minecraft:end_rod'),
         slam(0, 3, 40, 900, min_phase=2, id='full_moon', heal=120, particle='minecraft:heart')],
        [g('voile_de_selene'), g('maree_lunaire')], ('god', '#dfe8ff', '#8a9ad8', '#ffffff'),
        'Déesse de la Lune, des secrets et des marées', 'Goddess of the Moon, of secrets and tides', 'fire',
        behavior='neutral_boss', look={'hair': '#e7ecf7', 'eyes': '#9ab0e8', 'skin': '#f4ead9', 'style': 'long'}, scale=1.05, vengeance='sol'),
    god('comete', 'Comète', 'Comet', 8, 560, 30,
        [lunge(32, 20, 12, 36, weight=5, id='streak', particle='minecraft:snowflake'), slash(28, 4, 100, 10, 20),
         meteors(30, 3, 6, 10, 24, 110, particle='minecraft:snowflake', id='ice_tail', effects=[fx('slowness', 60, 2)]),
         lunge(38, 26, 10, 28, min_phase=1, weight=5, id='perihelion', particle='minecraft:end_rod')],
        [g('sillage_de_comete'), g('trainee')], ('god', '#bfe6ff', '#3a6a9a', '#ffffff'),
        'Dieu errant, messager des cieux', 'Wandering God, messenger of the heavens', 'explosion', speed=0.46, look={'hair': '#bfe6ff', 'eyes': '#5fc4ff', 'skin': '#eadfd2', 'style': 'short'}, scale=1.0, vengeance='pulsar'),
    god('mars', 'Mars', 'Mars', 8, 700, 34,
        [slash(34, 4.5, 130, 13, 20), slam(38, 7, 22, 100, particle='minecraft:damage_indicator'),
         lunge(40, 14, 16, 70), slash(30, 8, 80, 18, 90, id='rust_breath', particle='minecraft:crimson_spore', effects=[fx('wither', 100, 1), fx('weakness', 100, 1)]),
         slam(46, 10, 26, 160, min_phase=1, id='blood_tide', heal=40, particle='minecraft:damage_indicator')],
        [g('rouille_de_mars'), g('maree_de_sang')], ('god', '#8a2a1a', '#3a1a12', '#d0603a'),
        'Dieu de la rouille et du sang', 'God of rust and blood', 'magic', look={'hair': '#5a1812', 'eyes': '#ff4830', 'skin': '#c9a186', 'style': 'short'}, scale=1.15, vengeance='saturne'),
    god('saturne', 'Saturne', 'Saturn', 8, 680, 32,
        [ray(34, 28, 3, 26, 80, id='sentence'), meteors(32, 3.5, 6, 10, 28, 100, particle='minecraft:enchant', id='rings'),
         slam(36, 8, 24, 120, particle='minecraft:enchant', effects=[fx('slowness', 80, 3)], id='gavel'),
         slam(44, 13, 34, 240, min_phase=1, id='verdict', effects=[fx('weakness', 160, 2), fx('slowness', 100, 3)], particle='minecraft:enchant')],
        [g('balance_de_saturne'), g('verdict')], ('god', '#c8b07a', '#5a3a7a', '#f4e3a0'),
        'Déesse de la justice', 'Goddess of justice', 'backstab', look={'hair': '#caa866', 'eyes': '#8a5fd8', 'skin': '#e8cfae', 'style': 'long'}, scale=1.1, vengeance='higgs'),
    god('pulsar', 'Pulsar', 'Pulsar', 8, 600, 30,
        [slam(26, 6, 14, 40, id='beat', weight=5, particle='minecraft:sonic_boom', sound='minecraft:block.note_block.basedrum'),
         slam(32, 10, 20, 90, id='off_beat', particle='minecraft:sonic_boom', sound='minecraft:block.note_block.basedrum'),
         ray(34, 30, 2.5, 20, 70, id='lighthouse'),
         meteors(30, 3, 8, 12, 22, 120, min_phase=1, id='syncopation', particle='minecraft:note', sound='minecraft:block.note_block.bell')],
        [g('cadence_de_pulsar'), g('pulsation')], ('god', '#3ad8c8', '#12343a', '#ffffff'),
        'Dieu de la cadence, héraut des Dieux', 'God of cadence, herald of the Gods', 'projectile', look={'hair': '#2ee8d2', 'eyes': '#ffffff', 'skin': '#d8c4a8', 'style': 'short'}, scale=1.05, vengeance='quasar'),
    god('sol', 'Sol', 'Sol', 9, 820, 38,
        [shot('large_fireball', 0, 2, 18, 60, 5, 32, particle='minecraft:flame', sound='minecraft:entity.blaze.shoot'),
         meteors(38, 4, 6, 11, 28, 90, id='flares'), slash(40, 9, 90, 20, 70, id='corona', particle='minecraft:flame'),
         slam(46, 12, 30, 150, min_phase=1, id='noon', particle='minecraft:flame'),
         slam(60, 18, 44, 320, min_phase=2, id='zenith', style='nova', color='#ffd23a', particle='minecraft:flame', sound='minecraft:entity.generic.explode')],
        [g('couronne_de_sol'), g('zenith')], ('god', '#ffd23a', '#c8421f', '#fff6c8'),
        'Dieu du Soleil, premier-né de Higgs', 'God of the Sun, firstborn of Higgs', 'magic', look={'hair': '#ffd23a', 'eyes': '#ff9018', 'skin': '#e0b88a', 'style': 'crown'}, scale=1.25, vengeance='higgs'),
    god('nebula', 'Nébula', 'Nebula', 9, 900, 34,
        [meteors(34, 4, 7, 12, 30, 90, particle='minecraft:witch', id='stellar_nursery'),
         ambush(36, 4, 18, 110, id='veil'),
         slam(40, 12, 32, 170, min_phase=1, id='cradle', pull=2.0, particle='minecraft:witch'),
         slam(0, 3, 40, 700, min_phase=1, id='rebirth', heal=180, particle='minecraft:heart')],
        [g('berceau_de_nebula'), g('genese')], ('god', '#d86fd1', '#3a2a6a', '#ffb3e6'),
        'Déesse des naissances, mère des formes', 'Goddess of births, mother of forms', 'melee', look={'hair': '#e07fd8', 'eyes': '#c792ea', 'skin': '#f0dcd0', 'style': 'long'}, scale=1.15, vengeance='sol'),
    god('quasar', 'Quasar', 'Quasar', 9, 780, 40,
        [ray(42, 40, 3, 24, 60, id='jet', weight=4), ray(34, 30, 7, 34, 140, id='wide_jet'),
         meteors(38, 3.5, 6, 12, 26, 110, particle='minecraft:end_rod', id='accretion'),
         slam(44, 11, 28, 180, min_phase=1, id='maw', pull=2.5, heal=40, particle='minecraft:reverse_portal')],
        [g('voracite_de_quasar'), g('jet_relativiste')], ('god', '#f4f4ff', '#6a1fd8', '#ffd866'),
        'Dieu de la voracité et de l\'éclat', 'God of voracity and radiance', 'backstab', look={'hair': '#f4f4ff', 'eyes': '#ffd866', 'skin': '#d9c6b5', 'style': 'short'}, scale=1.1, vengeance='trou_blanc'),
    god('trou_blanc', 'Trou-Blanc', 'White-Hole', 9, 860, 38,
        [slash(36, 4.5, 120, 13, 22), ray(38, 30, 3, 24, 80, particle='minecraft:white_ash', id='outpour'),
         slam(0, 14, 40, 260, id='time_stop', time_stop=70, style='za_warudo', color='#ffffff', particle='minecraft:end_rod', sound='minecraft:block.beacon.deactivate'),
         ambush(42, 4, 12, 70, min_phase=1, id='mirror_step'),
         meteors(40, 4, 8, 13, 26, 120, min_phase=2, id='everything_returns', particle='minecraft:white_ash')],
        [g('gemellite'), g('arret_du_temps')], ('god', '#ffffff', '#c8ccd4', '#dfe8ff'),
        'Votre jumeau. Ce que vous engloutissez, il le rend.', 'Your twin. What you swallow, he gives back.', 'melee',
        look={'hair': '#f6f6f6', 'eyes': '#e6b422', 'skin': '#f2e3d5', 'style': 'boy'}, scale=0.95, vengeance='higgs', lines=4),
    god('higgs', 'Higgs', 'Higgs', 10, 1000, 50,
        [slash(48, 6, 140, 14, 22, id='backhand'), slam(54, 9, 22, 80, id='weight'),
         slam(40, 16, 34, 200, id='collapse', pull=3.0, effects=[fx('slowness', 100, 4)], particle='minecraft:reverse_portal'),
         meteors(50, 4.5, 8, 14, 28, 100, id='mass_drop', particle='minecraft:falling_obsidian_tear', sound='minecraft:entity.generic.explode'),
         ray(56, 44, 5, 30, 110, min_phase=1, id='field'),
         ambush(60, 6, 18, 90, min_phase=1, id='inevitable'),
         slam(70, 20, 50, 300, min_phase=2, id='condemnation', style='nova', pull=3.5, color='#6a1fd8', particle='minecraft:reverse_portal', sound='minecraft:entity.wither.spawn')],
        [g('masse_primordiale'), g('effondrement')], ('god', '#0c0c14', '#6a1fd8', '#ffd866'),
        'Père des Dieux. Celui qui donne sa masse à toute chose.', 'Father of the Gods. He who gives all things their mass.', 'backstab',
        look={'hair': '#14101c', 'eyes': '#ffd866', 'skin': '#cdb9a6', 'style': 'crown'}, scale=1.35, phases=(0.75, 0.5, 0.25), lines=4),
]

DROPS = {'aldric': 'solbrand', 'lysmene': 'baton_de_givre', 'cledith': 'epee_de_cledith', 'suke': 'fusil_de_suke', 'ignarelle': 'sceptre_d_ignarelle', 'noctalys': 'lame_de_noctalys', 'bragor': 'pavois_de_bragor', 'zephyrine': 'trident_de_zephyrine', 'oriel': 'compas_d_oriel', 'valdrek': 'hache_de_valdrek', 'segaroth': 'grimoire_de_segaroth'}
for _c in CHARACTERS:
    if _c['id'] in DROPS:
        _c['drops'] = ['trounoir:' + DROPS[_c['id']]]
    if not _c.get('unique') and 'skin' in _c:
        _c['variants'] = 6      # apparences : 3 hommes, 3 femmes

FACTIONS = {
    'nebulosia': ('Royaume de Nébulosia', 'Kingdom of Nebulosia', '#3f5f8a', [('soldat', 3), ('chevalier', 1)]),
    'azalee': ("Empire d'Azalée", 'Empire of Azalea', '#a03a6a', [('legionnaire', 3), ('legionnaire', 1), ('legionnaire', 1)]),
    'pantheon': ('Panthéon', 'Pantheon', '#fff2b0', []),
    'sauvage': ('Terres sauvages', 'The Wilds', '#6fbf73', []),
}

OW = 'minecraft:overworld'
DV = 'trounoir:diviniarth'
VT = 'trounoir:voute'


def S(id, fr, en, dimension, blueprint, distance, angle, faction, palette, residents, crossing=False):
    return dict(id=id, fr=fr, en=en, dimension=dimension, blueprint=blueprint, distance=distance, angle=angle,
                faction=faction, palette=palette, residents=residents, crossing=crossing)


SITES = [
    # ================================================================== Nébulosia (Overworld), autour du point de renaissance
    S('bourg_levant', 'Bourg-Levant', 'Eastmere', OW, 'village', 180, 40, 'nebulosia',
      ('minecraft:spruce_planks', 'minecraft:oak_planks', 'minecraft:lantern'), [('paysan', 6), ('soldat', 1)]),
    S('clairmoulin', 'Clairmoulin', 'Clairmoulin', OW, 'village', 520, 200, 'nebulosia',
      ('minecraft:dark_oak_planks', 'minecraft:birch_planks', 'minecraft:lantern'), [('paysan', 8), ('soldat', 2), ('chevalier', 1)]),
    S('chateau_d_astrelune', "Château d'Astrelune", 'Astrelune Castle', OW, 'castle', 850, 120, 'nebulosia',
      ('minecraft:stone_bricks', 'minecraft:polished_andesite', 'minecraft:lantern'),
      [('roi_aurele', 1), ('aldric', 1), ('lysmene', 1), ('chevalier', 6), ('mage_royal', 3), ('soldat', 6)]),
    S('pont_aux_saules', 'Pont-aux-Saules', 'Willowbridge', OW, 'village', 380, 300, 'nebulosia',
      ('minecraft:oak_planks', 'minecraft:spruce_planks', 'minecraft:lantern'), [('paysan', 7), ('soldat', 1)]),
    S('vestige_du_guet', 'Vestige du Guet', 'Ruined Watch', OW, 'ruin', 650, 80, 'sauvage',
      ('minecraft:stone_bricks', 'minecraft:mossy_cobblestone', 'minecraft:chiseled_stone_bricks'), []),
    S('cercle_des_anciens', 'Cercle des Anciens', "Elders' Circle", OW, 'ruin', 1250, 210, 'sauvage',
      ('minecraft:mossy_stone_bricks', 'minecraft:stone', 'minecraft:chiseled_stone_bricks'), []),
    S('havre_gris', 'Havre-Gris', 'Greyhaven', OW, 'harbor', 1300, 300, 'nebulosia',
      ('minecraft:cobblestone', 'minecraft:spruce_planks', 'minecraft:lantern'), [('soldat', 2), ('paysan', 4)], crossing=True),
    S('lande_de_gorgath', 'Lande de Gorgath', "Gorgath's Moor", OW, 'lair', 1100, 350, 'sauvage',
      ('minecraft:mossy_cobblestone', 'minecraft:coarse_dirt', 'minecraft:bone_block'), [('gorgath', 1)]),
    S('combe_de_la_veuve', 'Combe de la Veuve', "Widow's Hollow", OW, 'lair', 800, 250, 'sauvage',
      ('minecraft:cobbled_deepslate', 'minecraft:podzol', 'minecraft:white_wool'), [('veuve_bleme', 1)]),
    S('autel_de_la_lune', 'Autel de la Lune', 'Altar of the Moon', VT, 'island', 300, 150, 'pantheon',
      ('minecraft:quartz_block', 'minecraft:smooth_quartz', 'minecraft:sea_lantern'), [('selene', 1)]),
    S('sillage', 'Le Sillage', 'The Wake', VT, 'island', 330, 30, 'pantheon',
      ('minecraft:packed_ice', 'minecraft:blue_ice', 'minecraft:sea_lantern'), [('comete', 1)]),

    # ================================================================== Diviniarth : l'Empire d'Azalée et ses trois royaumes
    S('port_ecume', 'Port-Écume', 'Foamport', DV, 'harbor', 0, 0, 'azalee',
      ('minecraft:deepslate_bricks', 'minecraft:mangrove_planks', 'minecraft:soul_lantern'), [('legionnaire', 4), ('citoyen', 4)], crossing=True),
    # Royaume de Corollis (cœur de l'Empire)
    S('azalith', 'Azalith', 'Azalith', DV, 'city', 700, 0, 'azalee',
      ('minecraft:polished_deepslate', 'minecraft:pink_terracotta', 'minecraft:gold_block'),
      [('citoyen', 10), ('legionnaire', 6), ('magistere', 2), ('legionnaire', 2)]),
    S('trone_d_azalee', "Trône d'Azalée", 'Throne of Azalea', DV, 'palace', 900, 8, 'azalee',
      ('minecraft:polished_blackstone', 'minecraft:magenta_terracotta', 'minecraft:gold_block'),
      [('segaroth', 1), ('noctalys', 1), ('legionnaire', 6), ('magistere', 2)]),
    S('aiguille_de_veille', "L'Aiguille de Veille", 'The Watching Needle', DV, 'tower', 500, 60, 'azalee',
      ('minecraft:calcite', 'minecraft:smooth_quartz', 'minecraft:sea_lantern'), [('suke', 1), ('legionnaire', 4)]),
    # Royaume d'Épinoire (marches militaires)
    S('ronceval', 'Ronceval', 'Ronceval', DV, 'city', 800, 140, 'azalee',
      ('minecraft:deepslate_bricks', 'minecraft:red_terracotta', 'minecraft:iron_block'),
      [('citoyen', 8), ('legionnaire', 8), ('legionnaire', 3), ('zephyrine', 1)]),
    S('bastion_d_epinoire', "Bastion d'Épinoire", 'Bastion of Épinoire', DV, 'fortress', 1100, 120, 'azalee',
      ('minecraft:deepslate_tiles', 'minecraft:polished_deepslate', 'minecraft:iron_block'),
      [('cledith', 1), ('bragor', 1), ('legionnaire', 8), ('legionnaire', 4)]),
    # Royaume de Pétalune (académies)
    S('lunecorolle', 'Lunecorolle', 'Lunecorolle', DV, 'city', 900, 240, 'azalee',
      ('minecraft:end_stone_bricks', 'minecraft:purple_terracotta', 'minecraft:amethyst_block'),
      [('citoyen', 8), ('magistere', 5), ('legionnaire', 4), ('oriel', 1), ('ignarelle', 1)]),
    S('fort_pollen', 'Fort-Pollen', 'Fort Pollen', DV, 'fortress', 1200, 262, 'azalee',
      ('minecraft:polished_blackstone_bricks', 'minecraft:yellow_terracotta', 'minecraft:gold_block'),
      [('valdrek', 1), ('legionnaire', 8), ('legionnaire', 3), ('legionnaire', 3)]),
    # Repaires de demi-dieux
    S('tombeau_d_andromede', "Tombeau d'Andromède", "Andromeda's Tomb", DV, 'lair', 1500, 180, 'sauvage',
      ('minecraft:sculk', 'minecraft:deepslate', 'minecraft:chain'), [('geolier', 1)]),
    S('forge_de_sombrefer', 'Forge de Sombrefer', 'Darkiron Forge', DV, 'ruin', 1300, 330, 'sauvage',
      ('minecraft:blackstone', 'minecraft:smooth_basalt', 'minecraft:gilded_blackstone'), []),
    S('trone_de_cendres', 'Trône de cendres', 'Throne of Ashes', DV, 'ruin', 1650, 280, 'sauvage',
      ('minecraft:soul_soil', 'minecraft:polished_blackstone_bricks', 'minecraft:bone_block'), []),
    # Vestiges
    S('vestige_d_ivoire', "Vestige d'Ivoire", 'Ivory Ruin', DV, 'ruin', 450, 300, 'sauvage',
      ('minecraft:quartz_bricks', 'minecraft:calcite', 'minecraft:chiseled_quartz_block'), []),
    S('vestige_des_marches', 'Vestige des Marches', 'Ruin of the Marches', DV, 'ruin', 1400, 150, 'sauvage',
      ('minecraft:deepslate_bricks', 'minecraft:cobbled_deepslate', 'minecraft:chiseled_deepslate'), []),
    S('cloitre_englouti', 'Cloître englouti', 'Sunken Cloister', DV, 'ruin', 1000, 200, 'sauvage',
      ('minecraft:prismarine_bricks', 'minecraft:prismarine', 'minecraft:sea_lantern'), []),
    S('autel_brise', 'Autel brisé', 'Broken Altar', DV, 'ruin', 1900, 30, 'sauvage',
      ('minecraft:polished_blackstone_bricks', 'minecraft:blackstone', 'minecraft:gilded_blackstone'), []),
    # Lieux des Dieux : la Voûte, au-delà du Seuil
    S('porte_des_astres', 'Porte des Astres', 'Gate of the Stars', DV, 'gate', 2400, 345, 'pantheon',
      ('minecraft:obsidian', 'minecraft:polished_blackstone', 'minecraft:crying_obsidian'), []),
    S('seuil_de_la_voute', 'Seuil de la Voûte', 'Threshold of the Vault', VT, 'gate', 120, 180, 'pantheon',
      ('minecraft:obsidian', 'minecraft:polished_blackstone', 'minecraft:crying_obsidian'), []),
    S('champ_de_rouille', 'Champ de Rouille', 'Field of Rust', VT, 'island', 380, 200, 'pantheon',
      ('minecraft:red_sandstone', 'minecraft:exposed_copper', 'minecraft:oxidized_copper'), [('mars', 1)]),
    S('tribunal_des_anneaux', 'Tribunal des Anneaux', 'Tribunal of Rings', VT, 'island', 420, 255, 'pantheon',
      ('minecraft:smooth_sandstone', 'minecraft:purpur_block', 'minecraft:amethyst_block'), [('saturne', 1)]),
    S('le_phare', 'Le Phare', 'The Lighthouse', VT, 'island', 360, 100, 'pantheon',
      ('minecraft:prismarine_bricks', 'minecraft:dark_prismarine', 'minecraft:sea_lantern'), [('pulsar', 1)]),
    S('le_zenith', 'Le Zénith', 'The Zenith', VT, 'island', 520, 320, 'pantheon',
      ('minecraft:gold_block', 'minecraft:smooth_sandstone', 'minecraft:glowstone'), [('sol', 1)]),
    S('le_berceau', 'Le Berceau', 'The Cradle', VT, 'island', 480, 60, 'pantheon',
      ('minecraft:magenta_concrete', 'minecraft:pink_wool', 'minecraft:amethyst_block'), [('nebula', 1)]),
    S('la_gueule', 'La Gueule', 'The Maw', VT, 'island', 560, 10, 'pantheon',
      ('minecraft:end_stone', 'minecraft:purpur_block', 'minecraft:end_rod'), [('quasar', 1)]),
    S('horizon_blanc', "L'Horizon Blanc", 'The White Horizon', VT, 'island', 650, 180, 'pantheon',
      ('minecraft:white_concrete', 'minecraft:quartz_block', 'minecraft:sea_lantern'), [('trou_blanc', 1)]),
    S('trone_de_masse', 'Le Trône de Masse', 'The Throne of Mass', VT, 'island', 0, 0, 'pantheon',
      ('minecraft:obsidian', 'minecraft:crying_obsidian', 'minecraft:gilded_blackstone'), [('higgs', 1)]),
]
