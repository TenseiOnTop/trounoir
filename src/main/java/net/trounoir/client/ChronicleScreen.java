package net.trounoir.client;

import com.google.gson.JsonObject;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.util.JsonUtil;
import net.trounoir.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La Chronique des Vies : chaque vie achevée est une étoile dans un ciel noir.
 * Vie après vie, les étoiles dessinent une constellation ; survoler une étoile ouvre sa fiche.
 */
public class ChronicleScreen extends Screen {
    private record Star(JsonObject life, float x, float y, int color, int size) {}

    private final List<Star> stars = new ArrayList<>();
    private final List<int[]> dust = new ArrayList<>();
    private int ticks;

    public ChronicleScreen() {
        super(Component.translatable("trounoir.screen.chronicle"));
    }

    @Override
    protected void init() {
        dust.clear();
        Random random = new Random(7919L);
        for (int i = 0; i < 220; i++) {
            dust.add(new int[]{random.nextInt(Math.max(1, this.width)), random.nextInt(Math.max(1, this.height)), 0x18 + random.nextInt(0x40)});
        }
        reload();
    }

    /** Reconstruit la constellation à partir de la Chronique reçue du serveur. */
    public void reload() {
        stars.clear();
        if (ClientState.chronicle == null) return;
        List<JsonObject> lives = JsonUtil.objects(ClientState.chronicle, "lives");
        float cx = this.width / 2F;
        float cy = this.height / 2F + 6;
        float reach = Math.min(this.width, this.height) / 2F - 34;
        float step = lives.size() <= 1 ? 0 : reach / (float) Math.sqrt(lives.size());
        for (int i = 0; i < lives.size(); i++) {
            JsonObject life = lives.get(i);
            // Spirale à angle d'or, légèrement irrégulière : la même vie garde toujours la même place.
            Random jitter = new Random(31L * JsonUtil.integer(life, "number", i + 1) + JsonUtil.str(life, "name", "").hashCode());
            double angle = i * 2.399963 + (jitter.nextDouble() - 0.5) * 0.5;
            double radius = 22 + step * Math.sqrt(i) + (jitter.nextDouble() - 0.5) * 10;
            float x = cx + (float) (Math.cos(angle) * radius * 1.35);
            float y = cy + (float) (Math.sin(angle) * radius);
            String path = JsonUtil.str(life, "path", "");
            int color = JsonUtil.bool(life, "victory", false) ? 0xFFE9A0 : path.isEmpty() ? 0xBFC6D8 : ClientState.tagColor(path);
            int size = 1 + Math.min(3, JsonUtil.integer(life, "rank", 0) / 3);
            stars.add(new Star(life, x, y, color, size));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF020108);
        for (int[] d : dust) {
            int a = d[2];
            g.fill(d[0], d[1], d[0] + 1, d[1] + 1, (a << 24) | 0xFFFFFF);
        }
    }

    private void line(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5F) return;
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0F);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        g.fill(0, 0, Math.round(length), 1, color);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xC9B8FF);
        if (ClientState.chronicle != null) {
            String sub = I18n.get("trounoir.chronicle.count", stars.size());
            int victories = JsonUtil.integer(ClientState.chronicle, "victories", 0);
            if (victories > 0) sub += "  ✦ " + I18n.get("trounoir.chronicle.victories", victories);
            g.drawCenteredString(this.font, sub, this.width / 2, 24, 0x6F668A);
        }

        // Les traits de la constellation relient les vies dans l'ordre où elles furent vécues.
        for (int i = 1; i < stars.size(); i++) {
            Star a = stars.get(i - 1);
            Star b = stars.get(i);
            line(g, a.x(), a.y(), b.x(), b.y(), 0x38A9A0D8);
        }

        Star hovered = null;
        for (int i = 0; i < stars.size(); i++) {
            Star star = stars.get(i);
            float twinkle = 0.75F + 0.25F * (float) Math.sin((ticks + partialTick) * 0.08 + i * 1.7);
            int alpha = Math.round(255 * twinkle);
            int color = (alpha << 24) | star.color();
            int x = Math.round(star.x());
            int y = Math.round(star.y());
            int s = star.size();
            g.fill(x - s, y, x + s + 1, y + 1, color);
            g.fill(x, y - s, x + 1, y + s + 1, color);
            g.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
            if (Math.abs(mouseX - x) <= s + 4 && Math.abs(mouseY - y) <= s + 4) hovered = star;
        }

        // La vie en cours brûle encore : un anneau, au bout du dernier trait.
        if (!stars.isEmpty()) {
            Star last = stars.get(stars.size() - 1);
            int pulse = 3 + (int) ((ticks / 6) % 3);
            int x = Math.round(last.x()) + 14;
            int y = Math.round(last.y()) - 10;
            line(g, last.x(), last.y(), x, y, 0x30FFFFFF);
            g.fill(x - pulse, y - pulse, x + pulse, y - pulse + 1, 0x80C9B8FF);
            g.fill(x - pulse, y + pulse, x + pulse + 1, y + pulse + 1, 0x80C9B8FF);
            g.fill(x - pulse, y - pulse, x - pulse + 1, y + pulse, 0x80C9B8FF);
            g.fill(x + pulse, y - pulse, x + pulse + 1, y + pulse, 0x80C9B8FF);
        }

        if (hovered != null) g.renderTooltip(this.font, card(hovered.life()), mouseX, mouseY);
        else if (stars.isEmpty()) g.drawCenteredString(this.font, I18n.get("trounoir.chronicle.empty"), this.width / 2, this.height / 2, 0x5F567A);
    }

    private static String resolve(String text) {
        return text.startsWith("@") ? I18n.get(text.substring(1)) : text;
    }

    private void add(List<FormattedCharSequence> out, Component line) {
        out.addAll(this.font.split(line, 230));
    }

    /** Fiche d'une vie. */
    private List<FormattedCharSequence> card(JsonObject life) {
        List<FormattedCharSequence> out = new ArrayList<>();
        String name = JsonUtil.str(life, "name", "");
        add(out, Component.literal(name.isEmpty() ? I18n.get("trounoir.nameless") : name).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        add(out, Component.literal(I18n.get("trounoir.card.header", JsonUtil.integer(life, "number", 1),
                Text.duration((long) JsonUtil.num(life, "durationTicks", 0), I18n.get("trounoir.unit.day"), I18n.get("trounoir.unit.hour"), I18n.get("trounoir.unit.minute")),
                Text.roman(JsonUtil.integer(life, "rank", 0)), JsonUtil.integer(life, "power", 0))).withStyle(ChatFormatting.GRAY));
        String path = JsonUtil.str(life, "path", "");
        if (!path.isEmpty()) {
            add(out, Component.literal(I18n.get("trounoir.card.path", I18n.get("tntag.trounoir." + path))).withColor(ClientState.tagColor(path)));
        }
        List<String> notable = new ArrayList<>();
        for (String id : JsonUtil.strings(life, "notable")) {
            AbsorbableDef def = ClientState.ABSORBABLES.get(id);
            notable.add(def == null ? id : I18n.get(def.nameKey()));
        }
        if (!notable.isEmpty()) add(out, Component.literal(I18n.get("trounoir.card.absorbed", String.join(", ", notable))).withStyle(ChatFormatting.LIGHT_PURPLE));
        List<String> allies = new ArrayList<>();
        for (String faction : JsonUtil.strings(life, "allies")) allies.add(I18n.get("faction.trounoir." + faction));
        if (!allies.isEmpty()) add(out, Component.literal(I18n.get("trounoir.card.allies", String.join(", ", allies))).withStyle(ChatFormatting.GREEN));
        int shown = 0;
        for (JsonObject feat : JsonUtil.objects(life, "feats")) {
            if (shown++ >= 6) break;
            List<String> args = new ArrayList<>();
            for (String arg : JsonUtil.strings(feat, "args")) args.add(resolve(arg));
            add(out, Component.literal("✦ " + I18n.get(JsonUtil.str(feat, "key", ""), args.toArray())).withStyle(ChatFormatting.YELLOW));
        }
        String killer = JsonUtil.str(life, "killer", "");
        String cause = JsonUtil.str(life, "cause", "");
        if (!killer.isEmpty()) add(out, Component.literal(I18n.get("trounoir.card.killer", resolve(killer), resolve(JsonUtil.str(life, "place", "")))).withStyle(ChatFormatting.RED));
        else if (!cause.isEmpty()) add(out, Component.literal(cause).withStyle(ChatFormatting.RED));
        String epitaph = JsonUtil.str(life, "epitaph", "");
        if (!epitaph.isEmpty()) {
            out.add(FormattedCharSequence.EMPTY);
            add(out, Component.literal("« " + epitaph + " »").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        return out;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientEvents.OPEN_CHRONICLE.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
