package net.trounoir.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Écran noir avec une seule ligne : le temps qui se rembobine. */
public class BlackScreen extends Screen {
    public BlackScreen(Component message) {
        super(message);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 4, 0x8877AA);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
