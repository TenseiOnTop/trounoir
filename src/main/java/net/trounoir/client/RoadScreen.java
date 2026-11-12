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
        for (ClientState.PlaceInfo p : ClientState.PLACES) if (p.dimension().equals(dim)) places.add(p);
        places.sort(Comparator.comparingDouble(p -> distance(p)));
    }

    private static double distance(ClientState.PlaceInfo p) {
        Minecraft mc = Minecraft.getInstance();
        double dx = p.x() + 0.5 - mc.player.getX();
        double dz = p.z() + 0.5 - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private int left() { return (this.width - W) / 2; }
    private int top() { return (this.height - (VISIBLE * ROW + 44)) / 2; }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) return text;
        return this.font.plainSubstrByWidth(text, Math.max(8, width - this.font.width("…"))) + "…";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        int h = VISIBLE * ROW + 44;
        g.fill(left, top, left + W, top + h, 0xE0100C1C);
        g.fill(left, top, left + W, top + 1, 0x80C9B8FF);
        g.drawCenteredString(this.font, this.title, left + W / 2, top + 7, 0xC9B8FF);
        if (places.isEmpty()) {
            g.drawCenteredString(this.font, fit(I18n.get("trounoir.road.none"), W - 16), left + W / 2, top + 40, 0x8F86A8);
        }
        for (int i = 0; i < VISIBLE && i + scroll < places.size(); i++) {
            ClientState.PlaceInfo p = places.get(i + scroll);
            int y = top + 22 + i * ROW;
            boolean hover = mouseX >= left + 4 && mouseX < left + W - 4 && mouseY >= y && mouseY < y + ROW;
            boolean chosen = p.id().equals(ClientState.road);
            if (chosen) g.fill(left + 4, y, left + W - 4, y + ROW, 0x50E6C14A);
            else if (hover) g.fill(left + 4, y, left + W - 4, y + ROW, 0x30FFFFFF);
            String dist = Math.round(distance(p)) + " m";
            int dw = this.font.width(dist);
            g.drawString(this.font, fit(I18n.get(ClientState.siteName(p.id())), W - 24 - dw), left + 10, y + 4, chosen ? 0xFFE6C14A : 0xFFE8E0FF);
            g.drawString(this.font, dist, left + W - 10 - dw, y + 4, 0x8F86A8);
        }
        String hint = I18n.get(ClientState.road == null ? "trounoir.road.hint" : "trounoir.road.hint_clear");
        g.drawCenteredString(this.font, fit(hint, W - 12), left + W / 2, top + h - 14, 0x6F6888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = left();
        int top = top();
        int h = VISIBLE * ROW + 44;
        for (int i = 0; i < VISIBLE && i + scroll < places.size(); i++) {
            int y = top + 22 + i * ROW;
            if (mouseX >= left + 4 && mouseX < left + W - 4 && mouseY >= y && mouseY < y + ROW) {
                String id = places.get(i + scroll).id();
                ClientState.road = id.equals(ClientState.road) ? null : id;
                this.onClose();
                return true;
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
