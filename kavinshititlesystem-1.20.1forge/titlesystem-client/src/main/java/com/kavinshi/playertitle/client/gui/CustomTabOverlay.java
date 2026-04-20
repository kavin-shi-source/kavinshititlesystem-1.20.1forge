package com.kavinshi.playertitle.client.gui;

import com.kavinshi.playertitle.client.ClientTitleData;
import com.kavinshi.playertitle.title.ChromaType;
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
public final class CustomTabOverlay {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderTablistPre(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        String overlayId = event.getOverlay().id().toString();
        if (overlayId.contains("player_list") || overlayId.contains("PLAYER_LIST")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(RenderGuiEvent.Post event) {
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
        int colCount = Math.max(1, (sw - 20) / 140);
        int rowsPerCol = 15;
        int entryH = 10;
        int startX = 5;
        int startY = 4;

        int maxRows = Math.min(playerList.size(), rowsPerCol * colCount);
        int bgH = maxRows * entryH + 8;
        graphics.fill(startX - 3, startY - 3, sw - startX + 3, startY + bgH, 0xC0000000);

        for (int i = 0; i < playerList.size(); i++) {
            PlayerInfo info = playerList.get(i);
            int col = i / rowsPerCol;
            int row = i % rowsPerCol;
            if (col >= colCount) break;

            int x = startX + col * 140;
            int y = startY + row * entryH;

            UUID playerUuid = info.getProfile().getId();
            ClientTitleData.EquippedTitleInfo titleInfo = ClientTitleData.getEquippedTitleForPlayer(playerUuid);

            try {
                graphics.blit(info.getSkinLocation(), x, y, 8, 8,
                    8f, 8f, 8, 8, 64, 64);
                graphics.blit(info.getSkinLocation(), x, y, 8, 8,
                    40f, 8f, 8, 8, 64, 64);
            } catch (Exception ignored) {}

            int nameX = x + 11;
            if (titleInfo != null && titleInfo.titleId >= 0) {
                ChromaType chroma = ChromaType.fromString(titleInfo.chromaType);
                int badgeBg = chroma.getBadgeBgColor();
                int badgeBorder = chroma.getBadgeBorderColor();
                String shortName = titleInfo.titleName != null
                    ? (titleInfo.titleName.length() > 6 ? titleInfo.titleName.substring(0, 6) : titleInfo.titleName)
                    : "?";
                int badgeW = font.width(shortName) + 4;
                graphics.fill(nameX - 1, y - 1, nameX + badgeW + 1, y + 9, badgeBorder);
                graphics.fill(nameX, y, nameX + badgeW, y + 8, badgeBg);

                if (chroma.hasChroma()) {
                    for (int ci = 0; ci < shortName.length(); ci++) {
                        int cColor = com.kavinshi.playertitle.title.RainbowColorUtil
                            .getChromaColorForChar(chroma, ci, shortName.length());
                        graphics.drawString(font, String.valueOf(shortName.charAt(ci)),
                            nameX + 2 + ci * font.width("W"), y, cColor | 0xFF000000, false);
                    }
                } else {
                    graphics.drawString(font, shortName, nameX + 2, y,
                        titleInfo.titleColor != 0 ? titleInfo.titleColor | 0xFF000000 : 0xFFFFFFFF, false);
                }

                nameX += badgeW + 3;
            }

            int nameColor = chromaFromInfo(titleInfo).getNameColor();
            graphics.drawString(font, info.getProfile().getName(), nameX, y, nameColor, false);
        }

        int days = (int)(mc.level.getDayTime() / 24000L);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        String timeStr = sdf.format(new java.util.Date());
        String footerStr = "Day " + days + " | " + timeStr;
        int footerY = sh - 12;
        graphics.drawString(font, footerStr, (sw - font.width(footerStr)) / 2, footerY, 0xFF84847A, false);
    }

    private static ChromaType chromaFromInfo(ClientTitleData.EquippedTitleInfo info) {
        return info != null ? ChromaType.fromString(info.chromaType) : ChromaType.NONE;
    }
}
