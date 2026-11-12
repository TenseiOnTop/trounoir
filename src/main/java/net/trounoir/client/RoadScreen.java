package net.trounoir.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Choix d'une route : un lieu déjà découvert, marqué ensuite sur la boussole. */
public class RoadScreen extends Screen {
    private static final int W = 240;
    private static final int ROW = 16;
    private static final int VISIBLE = 10;
    private final List<ClientState.PlaceInfo> places = new ArrayList<>();
    private int scroll;

    public RoadScreen() {
        super(Component.translatable("trounoir.road.title"));
    }

    @Override
    protected void init() {
        places.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        String dim = mc.level.dimension().location().toString();
        for (ClientState.PlaceInfo p : ClientState.PLACES) if (ClientState.testMode || p.dimension().equals(dim)) places.add(p);
        places.sort(Comparator.comparing((ClientState.PlaceInfo p) -> !p.dimension().equals(dim)).thenComparingDouble(RoadScreen::distance));
    }

    private static boolean here(ClientState.PlaceInfo p) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && p.dimension().equals(mc.level.dimension().location().toString());
    }

    /** Mode test : rangée des jeux de pouvoirs (0 à 10), au-dessus de la liste. */
    private int kitTop() { return top() + 20; }
    private int listTop() { return top() + (ClientState.testMode ? 40 : 22); }
    private int height() { return VISIBLE * ROW + (ClientState.testMode ? 62 : 44); }

    private static double distance(ClientState.PlaceInfo p) {
        if (!here(p)) return Double.MAX_VALUE;
        Minecraft mc = Minecraft.getInstance();
        double dx = p.x() + 0.5 - mc.player.getX();
        double dz = p.z() + 0.5 - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private int left() { return (this.width - W) / 2; }
    private int top() { return (this.height - height()) / 2; }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) return text;
        return this.font.plainSubstrByWidth(text, Math.max(8, width - this.font.width("…"))) + "…";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        int h = height();
        g.fill(left, top, left + W, top + h, 0xE0100C1C);
        g.fill(left, top, left + W, top + 1, 0x80C9B8FF);
        g.drawCenteredString(this.font, this.title, left + W / 2, top + 7, 0xC9B8FF);
        int hovered = -1;
        if (ClientState.testMode) {
            for (int r = 0; r <= 10; r++) {
                int x = left + 10 + r * 20;
                int y = kitTop();
                boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 14;
                boolean current = r == ClientState.rank;
                g.fill(x, y, x + 18, y + 14, current ? 0x90E6C14A : hover ? 0x60C9B8FF : 0x40FFFFFF);
                String label = r == 0 ? "0" : net.trounoir.util.Text.roman(r);
                g.pose().pushPose();
                g.pose().translate(x + 9, y + 4, 0F);
                g.pose().scale(0.75F, 0.75F, 1F);
                g.drawString(this.font, label, -this.font.width(label) / 2, 0, 0xFFFFFFFF, false);
                g.pose().popPose();
                if (hover) hovered = r;
            }
        }
        if (places.isEmpty()) {
            g.drawCenteredString(this.font, fit(I18n.get("trounoir.road.none"), W - 16), left + W / 2, top + 40, 0x8F86A8);
        }
        for (int i = 0; i < VISIBLE && i + scroll < places.size(); i++) {
            ClientState.PlaceInfo p = places.get(i + scroll);
            int y = listTop() + i * ROW;
            boolean hover = mouseX >= left + 4 && mouseX < left + W - 4 && mouseY >= y && mouseY < y + ROW;
            boolean chosen = p.id().equals(ClientState.road);
            if (chosen) g.fill(left + 4, y, left + W - 4, y + ROW, 0x50E6C14A);
            else if (hover) g.fill(left + 4, y, left + W - 4, y + ROW, 0x30FFFFFF);
            String dist = here(p) ? Math.round(distance(p)) + " m" : I18n.get("trounoir.road.elsewhere");
            int dw = this.font.width(dist);
            g.drawString(this.font, fit(I18n.get(ClientState.siteName(p.id())), W - 24 - dw), left + 10, y + 4, chosen ? 0xFFE6C14A : 0xFFE8E0FF);
            g.drawString(this.font, dist, left + W - 10 - dw, y + 4, 0x8F86A8);
        }
        String hint = I18n.get(ClientState.testMode ? "trounoir.road.hint_test" : ClientState.road == null ? "trounoir.road.hint" : "trounoir.road.hint_clear");
        g.drawCenteredString(this.font, fit(hint, W - 12), left + W / 2, top + h - 14, 0x6F6888);
        if (hovered >= 0) g.renderTooltip(this.font, Component.translatable("trounoir.test.kit_hover", hovered == 0 ? "0" : net.trounoir.util.Text.roman(hovered)), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = left();
        int top = top();
        int h = height();
        for (int i = 0; i < VISIBLE && i + scroll < places.size(); i++) {
            int y = listTop() + i * ROW;
            if (mouseX >= left + 4 && mouseX < left + W - 4 && mouseY >= y && mouseY < y + ROW) {
                ClientState.PlaceInfo p = places.get(i + scroll);
                String id = p.id();
                if (ClientState.testMode && (button == 1 || !here(p))) {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("id", id);
                    net.trounoir.network.Net.toServer("test_tp", json);
                    if (!here(p) && ClientState.road != null && ClientState.road.equals(id)) ClientState.road = null;
                    this.onClose();
                    return true;
                }
                ClientState.road = id.equals(ClientState.road) ? null : id;
                this.onClose();
                return true;
            }
        }
        if (ClientState.testMode) {
            for (int r = 0; r <= 10; r++) {
                int x = left + 10 + r * 20;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= kitTop() && mouseY < kitTop() + 14) {
                    com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                    json.addProperty("rank", r);
                    net.trounoir.network.Net.toServer("test_kit", json);
                    return true;
                }
            }
        }
        if (mouseX >= left && mouseX < left + W && mouseY >= top + h - 18 && mouseY < top + h) {
            ClientState.road = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(Math.max(0, places.size() - VISIBLE), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
