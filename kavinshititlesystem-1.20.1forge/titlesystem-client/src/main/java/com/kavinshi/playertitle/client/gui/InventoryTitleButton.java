package com.kavinshi.playertitle.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("null")
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class InventoryTitleButton {

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (!(screen instanceof InventoryScreen)) return;

        int guiLeft = screen.width / 2 - 88;
        int guiTop = screen.height / 2 - 100;
        int bx = guiLeft + 176 + 4;
        int by = guiTop + 4;

        event.addListener(Button.builder(Component.literal("T"), btn -> openTitleScreen())
            .bounds(bx, by, 18, 18)
            .build());
    }

    @SubscribeEvent
    public static void onInventoryDraw(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int guiLeft = event.getScreen().width / 2 - 88;
        int guiTop = event.getScreen().height / 2 - 100;
        int bx = guiLeft + 176 + 4;
        int by = guiTop + 4;

        boolean hovered = isHovered(event, bx, by, 18, 18);
        int bgColor = hovered ? 0xCCD4A017 : 0xAA8C7246;
        int borderColor = hovered ? 0xFFCCB06E : 0xFF99774A;

        graphics.fill(bx - 1, by - 1, bx + 19, by + 19, borderColor);
        graphics.fill(bx, by, bx + 18, by + 18, bgColor);
        graphics.fill(bx, by, bx + 18, by + 1, 0x30FFFFFF);
        graphics.fill(bx, by + 17, bx + 18, by + 18, 0x30000000);

        String label = "T";
        Minecraft mc = Minecraft.getInstance();
        int labelW = mc.font.width(label);
        graphics.drawString(mc.font, label, bx + (18 - labelW) / 2, by + 5, 0xFFF0E6D5, false);
    }

    private static void openTitleScreen() {
        Minecraft.getInstance().setScreen(new TitleScreen());
    }

    private static boolean isHovered(ScreenEvent.Render.Post event, int x, int y, int w, int h) {
        double mx = event.getMouseX(), my = event.getMouseY();
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
