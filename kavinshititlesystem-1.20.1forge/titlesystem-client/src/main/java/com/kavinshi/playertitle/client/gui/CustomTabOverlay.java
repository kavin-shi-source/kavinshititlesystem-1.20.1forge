package com.kavinshi.playertitle.client.gui;

import com.kavinshi.playertitle.client.ClientTitleData;
import com.kavinshi.playertitle.title.ChromaType;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.util.TabModDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
@SuppressWarnings("null")
public final class CustomTabOverlay {

    private static boolean hasTabMod() {
        return TabModDetector.hasTabMod();
    }

    private static int getRarityTier(UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        boolean isSelf = playerId.equals(mc.player.getUUID());
        if (isSelf && ClientTitleData.isUsingCustomTitle()) {
            return ClientTitleData.getCustomTitle().getEffectiveChromaType().getRarityTier();
        }
        ClientTitleData.EquippedTitleInfo info = ClientTitleData.getEquippedTitleForPlayer(playerId);
        if (info != null) {
            return ChromaType.fromString(info.chromaType).getRarityTier();
        }
        return 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderTablistPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        if (hasTabMod()) return;
        String overlayId = event.getOverlay().id().toString();
        if (overlayId.contains("player_list") || overlayId.contains("PLAYER_LIST")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(RenderGuiEvent.Post event) {
        if (hasTabMod()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyPlayerList.isDown()) return;
        renderCustomTabList(event.getGuiGraphics());
    }

    private static void renderCustomTabList(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        Collection<PlayerInfo> rawPlayerList = mc.getConnection().getOnlinePlayers();
        List<PlayerInfo> playerList = new java.util.ArrayList<>(rawPlayerList);
        playerList.sort((a, b) -> {
            UUID uuidA = a.getProfile().getId();
            UUID uuidB = b.getProfile().getId();
            boolean selfA = uuidA.equals(mc.player.getUUID());
            boolean selfB = uuidB.equals(mc.player.getUUID());
            if (selfA != selfB) return selfA ? -1 : 1;
            int tierA = getRarityTier(uuidA);
            int tierB = getRarityTier(uuidB);
            if (tierA != tierB) return Integer.compare(tierB, tierA);
            return a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName());
        });

        int colWidth = 140;
        int maxListWidth = Math.min(320, sw * 2 / 3);
        int colCount = Math.max(1, maxListWidth / colWidth);
        int actualListWidth = colCount * colWidth;
        int rowsPerCol = 15;
        int entryH = 10;
        int startX = (sw - actualListWidth) / 2;
        int startY = 4;

        int maxRows = Math.min(playerList.size(), rowsPerCol * colCount);
        int bgH = maxRows * entryH + 8;
        graphics.fill(startX - 3, startY - 3, startX + actualListWidth + 3, startY + bgH, 0xC0000000);

        for (int i = 0; i < playerList.size(); i++) {
            PlayerInfo info = playerList.get(i);
            int col = i / rowsPerCol;
            int row = i % rowsPerCol;
            if (col >= colCount) break;

            int x = startX + col * colWidth;
            int y = startY + row * entryH;

            UUID playerUuid = info.getProfile().getId();
            boolean isSelf = playerUuid.equals(mc.player.getUUID());
            String titleName = null;
            ChromaType chroma = ChromaType.NONE;
            int titleColor = 0xFFFFFF;

            if (isSelf && ClientTitleData.isUsingCustomTitle()) {
                CustomTitleData ct = ClientTitleData.getCustomTitle();
                titleName = ct.getText();
                chroma = ct.getEffectiveChromaType();
                titleColor = ct.getColor1();
            } else {
                ClientTitleData.EquippedTitleInfo titleInfo = ClientTitleData.getEquippedTitleForPlayer(playerUuid);
                if (titleInfo != null && titleInfo.titleId >= 0) {
                    titleName = titleInfo.titleName;
                    chroma = ChromaType.fromString(titleInfo.chromaType);
                    titleColor = titleInfo.titleColor;
                }
            }

            try {
                graphics.blit(info.getSkinLocation(), x, y, 8, 8, 8f, 8f, 8, 8, 64, 64);
                graphics.blit(info.getSkinLocation(), x, y, 8, 8, 40f, 8f, 8, 8, 64, 64);
            } catch (Exception ignored) {}

            int nameX = x + 11;
            if (titleName != null && !titleName.isEmpty()) {
                int badgeBg = chroma.getBadgeBgColor();
                int badgeBorder = chroma.getBadgeBorderColor();
                String shortName = titleName.length() > 6 ? titleName.substring(0, 6) : titleName;
                int badgeW = font.width(shortName) + 4;
                graphics.fill(nameX - 1, y - 1, nameX + badgeW + 1, y + 9, badgeBorder);
                graphics.fill(nameX, y, nameX + badgeW, y + 8, badgeBg);

                if (chroma == ChromaType.CUSTOM_GRADIENT && isSelf) {
                    CustomTitleData ct = ClientTitleData.getCustomTitle();
                    for (int ci = 0; ci < shortName.length(); ci++) {
                        int cColor = com.kavinshi.playertitle.title.RainbowColorUtil
                            .getGradientColorForChar(ct.getColor1(), ct.getColor2(), ci, shortName.length());
                        graphics.drawString(font, String.valueOf(shortName.charAt(ci)),
                            nameX + 2 + ci * font.width("W"), y, cColor | 0xFF000000, false);
                    }
                } else if (chroma.hasChroma()) {
                    for (int ci = 0; ci < shortName.length(); ci++) {
                        int cColor = com.kavinshi.playertitle.title.RainbowColorUtil
                            .getChromaColorForChar(chroma, ci, shortName.length());
                        graphics.drawString(font, String.valueOf(shortName.charAt(ci)),
                            nameX + 2 + ci * font.width("W"), y, cColor | 0xFF000000, false);
                    }
                } else {
                    graphics.drawString(font, shortName, nameX + 2, y,
                        titleColor != 0 ? titleColor | 0xFF000000 : 0xFFFFFFFF, false);
                }

                nameX += badgeW + 3;
            }

            int nameColor = chroma.getNameColor();
            graphics.drawString(font, info.getProfile().getName(), nameX, y, nameColor, false);
        }

        int days = (int)(mc.level.getDayTime() / 24000L);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        String timeStr = sdf.format(new java.util.Date());
        String footerStr = "Day " + days + " | " + timeStr;
        int footerY = sh - 12;
        graphics.drawString(font, footerStr, (sw - font.width(footerStr)) / 2, footerY, 0xFF84847A, false);
    }
}
