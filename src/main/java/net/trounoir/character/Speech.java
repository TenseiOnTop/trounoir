package net.trounoir.character;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.trounoir.data.CharacterDef;
import net.trounoir.faction.Reputation;
import net.trounoir.rebirth.PersistentStore;

/**
 * Paroles des personnages. Le Trou-Noir ne comprend une langue qu'après l'avoir absorbée :
 * avant cela, il n'entend que des sons. (Les Savoirs étant des souvenirs, il la comprend dans toutes ses vies suivantes.)
 */
public final class Speech {
    private Speech() {}

    private static final String[] SYLLABLES = {"ka", "ro", "en", "ti", "ul", "sha", "mo", "ve", "dra", "is", "ol", "né", "gar", "thi", "ba", "lu"};

    public static void speak(ServerPlayer player, CharacterEntity speaker) {
        CharacterDef def = speaker.def();
        if (def == null || def.dialogueLines <= 0) return;
        String key;
        if (!def.unique && Reputation.inMourning(player.server, def.faction)) {
            key = "dialogue.trounoir.mourning." + def.faction;
        } else {
            key = "dialogue." + def.id.getNamespace() + "." + def.id.getPath() + "." + (1 + speaker.getRandom().nextInt(def.dialogueLines));
        }
        PersistentStore.Memory memory = PersistentStore.get(player.server).memory(player.getUUID());
        boolean understood = def.language.isEmpty() || memory.knowledge.contains(def.language);
        Component line = understood
                ? Component.translatable(key)
                : Component.literal(garble(key)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
        player.displayClientMessage(Component.translatable("trounoir.speech", speaker.getDisplayName(), line), false);
    }

    /** Charabia stable : la même phrase incomprise sonne toujours pareil. */
    private static String garble(String key) {
        int h = key.hashCode();
        StringBuilder out = new StringBuilder();
        int words = 3 + Math.floorMod(h, 4);
        for (int w = 0; w < words; w++) {
            int syllables = 1 + Math.floorMod(h >> (w * 3), 3);
            for (int s = 0; s < syllables; s++) {
                h = h * 31 + 17;
                out.append(SYLLABLES[Math.floorMod(h >> 8, SYLLABLES.length)]);
            }
            out.append(w == words - 1 ? "…" : " ");
        }
        return out.toString();
    }
}
