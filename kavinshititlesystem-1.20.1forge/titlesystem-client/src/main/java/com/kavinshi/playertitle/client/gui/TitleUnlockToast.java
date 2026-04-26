package com.kavinshi.playertitle.client.gui;

import com.kavinshi.playertitle.title.ChromaType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

@SuppressWarnings("null")
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE,
    value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public final class TitleUnlockToast {
    private static volatile String currentTitleName = null;
    private static volatile String currentRarityName = null;
    private static volatile int currentRarityTier = 0;
    private static volatile long showTime = 0;
    private static final long SLIDE_IN = 400L;
    private static final long DISPLAY = 4000L;
    private static final long SLIDE_OUT = 400L;
    private static final long TOTAL = SLIDE_IN + DISPLAY + SLIDE_OUT;

    public static void show(String titleName, String rarityName, int rarityTier) {
        currentRarityTier = rarityTier;
        currentRarityName = rarityName != null ? rarityName : "Unknown";
        currentTitleName = titleName;
        showTime = System.currentTimeMillis();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isSameThread()) {
                mc.getSoundManager()
                    .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f));
            } else {
                mc.execute(() -> mc.getSoundManager()
                    .play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f)));
            }
        } catch (Exception ignored) {}
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onRenderGui(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        if (currentTitleName == null) return;

        long elapsed = System.currentTimeMillis() - showTime;
        if (elapsed > TOTAL) { currentTitleName = null; return; }

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();

        float offset;
        if (elapsed < SLIDE_IN) offset = 1.0f - (float)elapsed / SLIDE_IN;
        else if (elapsed > TOTAL - SLIDE_OUT) offset = (float)(elapsed - (TOTAL - SLIDE_OUT)) / SLIDE_OUT;
        else offset = 0.0f;

        int tw = 180, th = 40;
        int x = (int)(sw - tw + offset * (tw + 12));
        int y = 10;

        ChromaType[] types = ChromaType.values();
        ChromaType chroma = ChromaType.NONE;
        for (ChromaType ct : types) {
            if (ct.getRarityTier() == currentRarityTier) {
                chroma = ct;
                break;
            }
        }
        if (chroma == ChromaType.NONE && currentRarityTier > 0) {
            for (ChromaType ct : types) {
                if (ct.getRarityTier() <= currentRarityTier && ct.getRarityTier() > chroma.getRarityTier()) {
                    chroma = ct;
                }
            }
        }
        int accent = chroma.getAccentColor();

        g.fill(x, y, x + tw, y + th, 0xD0000000);
        g.fill(x, y, x + 3, y + th, accent | 0xFF000000);
        g.fill(x, y, x + tw, y + 2, 0x30FFFFFF);
        g.fill(x, y + th - 2, x + tw, y + th, 0x20000000);

        g.drawString(mc.font, "\u2605", x + 8, y + 8, accent | 0xFF000000, false);

        int txtX = x + 26;
        g.drawString(mc.font, "★ Title Unlocked!", txtX, y + 5, 0xFFFFD700, false);
        g.drawString(mc.font, currentTitleName, txtX, y + 17, 0xFFFFFFFF, false);

        String rarityStr = "[" + currentRarityName + "]";
        g.drawString(mc.font, rarityStr, txtX, y + 29, accent | 0xFF000000, false);
    }
}
