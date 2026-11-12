package net.trounoir.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.network.Net;
import net.trounoir.util.JsonUtil;
import net.trounoir.util.Lookup;
import net.trounoir.util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * La Renaissance. Écran noir ; l'épitaphe de la vie qui s'achève ; puis la question du nom —
 * qui surgit pour la première fois à la première mort ; puis le choix de l'Écho à conserver.
 */
public class RebirthScreen extends Screen {
    private enum Stage { DARK, NAME, ECHO, WAITING }

    private record Candidate(String id, int level) {}

    private Stage stage = Stage.DARK;
    private int ticks;
    private int stageTicks;
    private EditBox nameBox;
    private String chosenName = "";
    private String chosenEcho = "";
    private int replaceIndex = -1;

    public RebirthScreen() {
        super(Component.translatable("trounoir.screen.rebirth"));
    }

    private JsonObject data() {
        return ClientState.death;
    }

    private List<Candidate> candidates() {
        List<Candidate> out = new ArrayList<>();
        if (data() == null) return out;
        for (JsonObject c : JsonUtil.objects(data(), "candidates")) {
            out.add(new Candidate(JsonUtil.str(c, "id", ""), JsonUtil.integer(c, "level", 1)));
        }
        return out;
    }

    private List<Candidate> echoes() {
        List<Candidate> out = new ArrayList<>();
        if (data() == null) return out;
        for (JsonObject c : JsonUtil.objects(data(), "echoes")) {
            out.add(new Candidate(JsonUtil.str(c, "id", ""), JsonUtil.integer(c, "level", 1)));
        }
        return out;
    }

    private boolean slotsFull() {
        if (data() == null) return false;
        boolean alreadyKept = false;
        for (Candidate echo : echoes()) alreadyKept |= echo.id().equals(chosenEcho);
        return !alreadyKept && echoes().size() >= JsonUtil.integer(data(), "slots", 1);
    }

    private void go(Stage next) {
        stage = next;
        stageTicks = 0;
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        nameBox = null;
        if (stage == Stage.NAME) {
            nameBox = new EditBox(this.font, this.width / 2 - 90, this.height / 2 + 6, 180, 20, Component.translatable("trounoir.rebirth.name_question"));
            nameBox.setMaxLength(24);
            nameBox.setValue(chosenName);
            this.addRenderableWidget(nameBox);
            this.setInitialFocus(nameBox);
        } else if (stage == Stage.ECHO) {
            this.addRenderableWidget(Button.builder(Component.translatable("trounoir.rebirth.confirm"), b -> confirmEcho())
                    .bounds(this.width / 2 - 102, this.height - 34, 100, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("trounoir.rebirth.keep_nothing"), b -> {
                chosenEcho = "";
                replaceIndex = -1;
                send();
            }).bounds(this.width / 2 + 2, this.height - 34, 100, 20).build());
        }
    }

    private void confirmName() {
        if (nameBox == null) return;
        String value = nameBox.getValue().trim();
        if (value.isEmpty()) return;
        chosenName = value;
        if (candidates().isEmpty()) send();
        else go(Stage.ECHO);
    }

    private void confirmEcho() {
        if (chosenEcho.isEmpty()) return;
        if (slotsFull() && replaceIndex < 0) return;
        send();
    }

    private void send() {
        JsonObject json = new JsonObject();
        json.addProperty("echo", chosenEcho);
        json.addProperty("replace", replaceIndex);
        json.addProperty("name", chosenName);
        Net.toServer("rebirth", json);
        go(Stage.WAITING);
    }

    @Override
    public void tick() {
        ticks++;
        stageTicks++;
        // Le noir dure assez pour peser ; il cède dès que le serveur a transmis la vie achevée.
        if (stage == Stage.DARK && stageTicks > 110 && data() != null) go(Stage.NAME);
        // Renaissance douce : le serveur a fait réapparaître le joueur, la nouvelle vie commence.
        if (stage == Stage.WAITING && stageTicks > 10 && this.minecraft != null && this.minecraft.player != null
                && !this.minecraft.player.isDeadOrDying()) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    private static int fade(int ticks, int start, int length, int rgb) {
        float t = Math.max(0F, Math.min(1F, (ticks - start) / (float) length));
        int alpha = Math.max(4, Math.round(255 * t));
        return (alpha << 24) | rgb;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        switch (stage) {
            case DARK -> {
                if (data() == null || stageTicks < 30) return;
                JsonObject record = JsonUtil.obj(data(), "record");
                String epitaph = JsonUtil.str(record, "epitaph", "");
                if (!epitaph.isEmpty()) {
                    List<FormattedCharSequence> lines = this.font.split(Component.literal(epitaph), Math.min(320, this.width - 40));
                    int y = cy - lines.size() * 6;
                    for (FormattedCharSequence line : lines) {
                        g.drawCenteredString(this.font, line, cx, y, fade(stageTicks, 30, 40, 0x9A90C0));
                        y += 12;
                    }
                }
            }
            case NAME -> {
                // La question s'écrit lettre à lettre.
                String question = I18n.get("trounoir.rebirth.name_question");
                int shown = Math.min(question.length(), stageTicks / 2);
                g.drawCenteredString(this.font, question.substring(0, shown), cx, cy - 18, 0xFFFFFFFF);
                if (shown == question.length()) {
                    g.drawCenteredString(this.font, I18n.get("trounoir.rebirth.name_hint"), cx, cy + 34, fade(stageTicks, question.length() * 2 + 20, 30, 0x5F567A));
                }
            }
            case ECHO -> renderEchoChoice(g, mouseX, mouseY);
            case WAITING -> {
                String dots = ".".repeat(1 + (ticks / 10) % 3);
                g.drawCenteredString(this.font, dots, cx, cy, 0xFF8877AA);
            }
        }
    }

    private int columns() {
        return Math.max(1, Math.min(4, (this.width - 40) / 150));
    }

    private void renderEchoChoice(GuiGraphics g, int mouseX, int mouseY) {
        int cx = this.width / 2;
        g.drawCenteredString(this.font, I18n.get("trounoir.rebirth.echo_question"), cx, 18, 0xFFFFFFFF);
        g.drawCenteredString(this.font, I18n.get("trounoir.rebirth.echo_hint", echoes().size(), JsonUtil.integer(data(), "slots", 1)), cx, 30, 0xFF6F668A);
        List<Candidate> candidates = candidates();
        int columns = columns();
        int x0 = cx - columns * 75;
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            AbsorbableDef def = ClientState.ABSORBABLES.get(c.id());
            if (def == null) continue;
            int x = x0 + (i % columns) * 150;
            int y = 48 + (i / columns) * 22;
            if (y > this.height - 96) break;
            boolean hovered = mouseX >= x && mouseX < x + 146 && mouseY >= y && mouseY < y + 20;
            boolean picked = c.id().equals(chosenEcho);
            g.fill(x, y, x + 146, y + 20, picked ? 0xFF3A2F5C : hovered ? 0xFF1C1630 : 0xFF0E0A1A);
            g.renderItem(Lookup.stack(def.icon), x + 2, y + 2);
            String label = I18n.get(def.nameKey()) + (def.maxLevel > 1 ? " " + Text.roman(c.level()) : "");
            g.drawString(this.font, this.font.plainSubstrByWidth(label, 122), x + 22, y + 6, picked ? 0xFFFFFFFF : 0xFFB8AEDC);
        }
        // Emplacements déjà occupés : il faut en sacrifier un si tout est plein.
        List<Candidate> echoes = echoes();
        if (!echoes.isEmpty()) {
            int y = this.height - 78;
            g.drawCenteredString(this.font, I18n.get(slotsFull() ? "trounoir.rebirth.replace_which" : "trounoir.rebirth.current_echoes"), cx, y - 12, slotsFull() ? 0xFFE6D08A : 0xFF6F668A);
            int ex0 = cx - echoes.size() * 75;
            for (int i = 0; i < echoes.size(); i++) {
                AbsorbableDef def = ClientState.ABSORBABLES.get(echoes.get(i).id());
                int x = ex0 + i * 150;
                g.fill(x, y, x + 146, y + 20, i == replaceIndex ? 0xFF5C2F3A : 0xFF0E0A1A);
                if (def != null) {
                    g.renderItem(Lookup.stack(def.icon), x + 2, y + 2);
                    g.drawString(this.font, this.font.plainSubstrByWidth(I18n.get(def.nameKey()), 122), x + 22, y + 6, 0xFF9A90C0);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (stage == Stage.DARK && data() != null && stageTicks > 40) {
            go(Stage.NAME);
            return true;
        }
        if (stage == Stage.ECHO) {
            List<Candidate> candidates = candidates();
            int columns = columns();
            int x0 = this.width / 2 - columns * 75;
            for (int i = 0; i < candidates.size(); i++) {
                int x = x0 + (i % columns) * 150;
                int y = 48 + (i / columns) * 22;
                if (y > this.height - 96) break;
                if (mouseX >= x && mouseX < x + 146 && mouseY >= y && mouseY < y + 20) {
                    chosenEcho = candidates.get(i).id();
                    replaceIndex = -1;
                    return true;
                }
            }
            List<Candidate> echoes = echoes();
            int y = this.height - 78;
            int ex0 = this.width / 2 - echoes.size() * 75;
            for (int i = 0; i < echoes.size(); i++) {
                int x = ex0 + i * 150;
                if (mouseX >= x && mouseX < x + 146 && mouseY >= y && mouseY < y + 20) {
                    replaceIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (stage == Stage.NAME && (keyCode == 257 || keyCode == 335)) {   // Entrée
            confirmName();
            return true;
        }
        if (stage == Stage.DARK && data() != null && stageTicks > 40) {
            go(Stage.NAME);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
