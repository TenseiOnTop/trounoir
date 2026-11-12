package net.trounoir.client;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.SynergyDef;
import net.trounoir.network.Net;
import net.trounoir.util.Lookup;
import net.trounoir.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * L'écran d'Absorption : traits, niveaux, étiquettes, synergies actives, techniques équipées, savoirs.
 * Les onglets n'apparaissent qu'une fois leur contenu découvert.
 */
public class AbsorptionScreen extends Screen {
    private enum Tab { TRAITS, TECHNIQUES, KNOWLEDGE }

    private record Row(String id, String title, String detail, int color, ItemStack icon, List<Component> tooltip) {}

    private static final int W = 400;
    private static final int H = 226;
    private static final int ROW = 22;
    private static final int VISIBLE = 7;

    private Tab tab = Tab.TRAITS;
    private int scroll;
    private String selected;
    private int left;
    private int top;

    public AbsorptionScreen() {
        super(Component.translatable("trounoir.screen.absorption"));
    }

    @Override
    protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<Tab> tabs() {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(Tab.TRAITS);
        if (ClientState.hasAny(AbsorbableDef.Kind.TECHNIQUE)) tabs.add(Tab.TECHNIQUES);
        if (ClientState.hasAny(AbsorbableDef.Kind.KNOWLEDGE)) tabs.add(Tab.KNOWLEDGE);
        return tabs;
    }

    private String tabName(Tab t) {
        return I18n.get("trounoir.tab." + t.name().toLowerCase(java.util.Locale.ROOT));
    }

    // ------------------------------------------------------------------ contenu

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        if (tab == Tab.KNOWLEDGE) {
            for (String id : ClientState.KNOWLEDGE) {
                AbsorbableDef def = ClientState.ABSORBABLES.get(id);
                if (def == null) continue;
                rows.add(new Row(id, I18n.get(def.nameKey()), I18n.get("trounoir.knowledge." + def.knowledgeType), 0x9AD8FF,
                        Lookup.stack(def.icon), describe(def)));
            }
            for (ClientState.PlaceInfo place : ClientState.PLACES) {
                String key = "site." + place.id().replace(':', '.');
                String where = place.x() + ", " + place.z() + "  (" + I18n.get(place.dimension().endsWith("diviniarth") ? "trounoir.place.diviniarth" : "trounoir.place.nebulosia") + ")";
                rows.add(new Row(place.id(), I18n.get(key), where, 0xE6D08A, ItemStack.EMPTY, List.of()));
            }
            for (String id : ClientState.WEAKNESSES) {
                ClientState.CharacterInfo info = ClientState.CHARACTERS.get(id);
                if (info == null || info.weakness().isEmpty()) continue;
                String name = I18n.get("character." + id.replace(':', '.'));
                rows.add(new Row(id, I18n.get("trounoir.weakness.of", name), I18n.get("trounoir.weakness." + info.weakness()), 0xFF9A8A, ItemStack.EMPTY, List.of()));
            }
            return rows;
        }
        AbsorbableDef.Kind kind = tab == Tab.TRAITS ? AbsorbableDef.Kind.TRAIT : AbsorbableDef.Kind.TECHNIQUE;
        List<String> ids = new ArrayList<>(ClientState.XP.keySet());
        for (ClientState.EchoInfo echo : ClientState.ECHOES) if (!ids.contains(echo.id())) ids.add(echo.id());
        for (String id : ids) {
            AbsorbableDef def = ClientState.ABSORBABLES.get(id);
            if (def == null || def.kind != kind) continue;
            int level = ClientState.level(id);
            ClientState.EchoInfo echo = ClientState.echo(id);
            if (level <= 0 && echo == null) continue;
            StringBuilder detail = new StringBuilder();
            if (level > 0) {
                if (def.maxLevel > 1) detail.append(Text.roman(level)).append(" / ").append(Text.roman(def.maxLevel));
            } else {
                detail.append(I18n.get("trounoir.echo.marker"));
            }
            for (String tag : def.tags) detail.append(detail.length() > 0 ? "  " : "").append(I18n.get("tntag.trounoir." + tag));
            int color = def.tags.isEmpty() ? 0xE8E0FF : ClientState.tagColor(def.tags.get(0));
            rows.add(new Row(id, I18n.get(def.nameKey()), detail.toString(), level > 0 ? color : 0x8F86A8, Lookup.stack(def.icon), describe(def)));
        }
        return rows;
    }

    private List<Component> describe(AbsorbableDef def) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(def.nameKey()).withStyle(ChatFormatting.WHITE));
        if (I18n.exists(def.descriptionKey())) lines.add(Component.translatable(def.descriptionKey()).withStyle(ChatFormatting.GRAY));
        if (def.kind == AbsorbableDef.Kind.TECHNIQUE) {
            lines.add(Component.translatable("trounoir.technique.cooldown", def.cooldownTicks / 20).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (def.signature) lines.add(Component.translatable("trounoir.signature").withStyle(ChatFormatting.LIGHT_PURPLE));
        return lines;
    }

    // ------------------------------------------------------------------ rendu

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF3A2F5C);
        g.fill(left, top, left + W, top + H, 0xF00B0714);
        g.fill(left + 244, top + 8, left + 245, top + H - 8, 0xFF2A2242);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<Component> tooltip = null;

        // Onglets.
        int tx = left + 8;
        for (Tab t : tabs()) {
            String name = tabName(t);
            int w = this.font.width(name);
            g.drawString(this.font, name, tx, top + 8, t == tab ? 0xFFFFFF : 0x7F769A);
            if (t == tab) g.fill(tx, top + 19, tx + w, top + 20, 0xFFC9B8FF);
            tx += w + 14;
        }

        // Liste.
        List<Row> rows = rows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - VISIBLE)));
        int listTop = top + 26;
        for (int i = 0; i < VISIBLE && i + scroll < rows.size(); i++) {
            Row row = rows.get(i + scroll);
            int y = listTop + i * ROW;
            boolean hovered = mouseX >= left + 6 && mouseX < left + 240 && mouseY >= y && mouseY < y + ROW;
            if (row.id().equals(selected)) g.fill(left + 6, y, left + 240, y + ROW - 1, 0x503A2F5C);
            else if (hovered) g.fill(left + 6, y, left + 240, y + ROW - 1, 0x30FFFFFF);
            if (!row.icon().isEmpty()) g.renderItem(row.icon(), left + 9, y + 2);
            g.drawString(this.font, row.title(), left + 30, y + 2, row.color());
            g.drawString(this.font, row.detail(), left + 30, y + 12, 0x8F86A8);
            if (hovered && !row.tooltip().isEmpty()) tooltip = row.tooltip();
        }
        if (rows.size() > VISIBLE) {
            g.drawString(this.font, (scroll + 1) + "-" + Math.min(rows.size(), scroll + VISIBLE) + " / " + rows.size(), left + 180, top + 8, 0x5F567A);
        }

        // Emplacements de techniques.
        if (tab == Tab.TECHNIQUES) {
            int sy = top + H - 30;
            g.drawString(this.font, I18n.get(selected == null ? "trounoir.slots.hint_select" : "trounoir.slots.hint_assign"), left + 8, sy - 11, 0x7F769A);
            for (int i = 0; i < ClientState.SLOTS.length; i++) {
                int sx = left + 8 + i * 58;
                g.fill(sx, sy, sx + 54, sy + 22, 0x70201838);
                g.drawString(this.font, ClientEvents.TECHNIQUE_KEYS[i].getTranslatedKeyMessage().getString(), sx + 3, sy + 7, 0xA0A0B0);
                String id = ClientState.SLOTS[i];
                AbsorbableDef def = id == null ? null : ClientState.ABSORBABLES.get(id);
                if (def != null) {
                    g.renderItem(Lookup.stack(def.icon), sx + 34, sy + 3);
                    if (mouseX >= sx && mouseX < sx + 54 && mouseY >= sy && mouseY < sy + 22) tooltip = describe(def);
                }
            }
        }

        sideTooltip = null;
        renderSidePanel(g, mouseX, mouseY);
        if (sideTooltip != null) tooltip = sideTooltip;
        if (tooltip != null) g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private List<Component> sideTooltip;

    private void renderSidePanel(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 252;
        int y = top + 8;
        g.drawString(this.font, I18n.get("trounoir.hud.rank", Text.roman(ClientState.rank), (int) Math.round(ClientState.power)), x, y, 0xC9B8FF);
        y += 12;
        String name = ClientState.lifeName.isEmpty() ? I18n.get("trounoir.nameless") : ClientState.lifeName;
        g.drawString(this.font, I18n.get("trounoir.life.line", ClientState.lifeNumber, name), x, y, 0x8F86A8);
        y += 16;

        // Fragilité : ce qu'il reste de la misère du départ.
        boolean anyHandicap = false;
        for (double v : ClientState.HANDICAPS.values()) anyHandicap |= v > 0.01;
        if (anyHandicap) {
            g.drawString(this.font, I18n.get("trounoir.fragility"), x, y, 0xFFFFFF);
            y += 11;
            for (Map.Entry<String, Double> e : ClientState.HANDICAPS.entrySet()) {
                if (e.getValue() <= 0.01) continue;
                g.drawString(this.font, I18n.get("handicap.trounoir." + e.getKey()), x, y, 0xB08A8A);
                g.fill(x + 84, y + 2, x + 138, y + 6, 0x60000000);
                g.fill(x + 84, y + 2, x + 84 + (int) Math.round(54 * e.getValue()), y + 6, 0xFFB04A4A);
                y += 10;
            }
            y += 6;
        }

        // Synergies actives.
        if (!ClientState.ACTIVE_SYNERGIES.isEmpty()) {
            g.drawString(this.font, I18n.get("trounoir.synergies"), x, y, 0xFFFFFF);
            y += 11;
            for (String id : ClientState.ACTIVE_SYNERGIES) {
                SynergyDef def = ClientState.SYNERGIES.get(id);
                if (def == null || y > top + H - 40) continue;
                g.drawString(this.font, "✦ " + I18n.get(def.nameKey()), x, y, 0xE6D08A);
                if (mouseX >= x && mouseX < left + W - 4 && mouseY >= y - 1 && mouseY < y + 9) {
                    sideTooltip = List.of(Component.translatable(def.nameKey()).withStyle(ChatFormatting.GOLD),
                            Component.translatable(def.nameKey() + ".desc").withStyle(ChatFormatting.WHITE),
                            Component.translatable(def.nameKey() + ".need").withStyle(ChatFormatting.GRAY));
                }
                y += 10;
            }
            y += 6;
        }

        // Échos.
        if (ClientState.deaths > 0 && y < top + H - 24) {
            g.drawString(this.font, I18n.get("trounoir.echoes", ClientState.ECHOES.size(), ClientState.echoSlots), x, y, 0xFFFFFF);
            y += 11;
            for (ClientState.EchoInfo echo : ClientState.ECHOES) {
                AbsorbableDef def = ClientState.ABSORBABLES.get(echo.id());
                if (def == null || y > top + H - 12) continue;
                g.drawString(this.font, "◌ " + I18n.get(def.nameKey()), x, y, 0x9A90C0);
                y += 10;
            }
        }
    }

    // ------------------------------------------------------------------ entrées

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tx = left + 8;
        for (Tab t : tabs()) {
            int w = this.font.width(tabName(t));
            if (mouseX >= tx && mouseX < tx + w && mouseY >= top + 6 && mouseY < top + 20) {
                tab = t;
                scroll = 0;
                selected = null;
                return true;
            }
            tx += w + 14;
        }
        List<Row> rows = rows();
        int listTop = top + 26;
        for (int i = 0; i < VISIBLE && i + scroll < rows.size(); i++) {
            int y = listTop + i * ROW;
            if (mouseX >= left + 6 && mouseX < left + 240 && mouseY >= y && mouseY < y + ROW) {
                selected = rows.get(i + scroll).id();
                return true;
            }
        }
        if (tab == Tab.TECHNIQUES) {
            int sy = top + H - 30;
            for (int i = 0; i < ClientState.SLOTS.length; i++) {
                int sx = left + 8 + i * 58;
                if (mouseX >= sx && mouseX < sx + 54 && mouseY >= sy && mouseY < sy + 22) {
                    JsonObject json = new JsonObject();
                    json.addProperty("slot", i);
                    json.addProperty("id", button == 1 || selected == null ? "" : selected);
                    Net.toServer("assign", json);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientEvents.OPEN_ABSORPTION.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
