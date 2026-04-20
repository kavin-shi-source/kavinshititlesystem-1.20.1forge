package com.kavinshi.playertitle.client.gui;

import com.kavinshi.playertitle.client.ClientTitleData;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.TitleUpdatePacket;
import com.kavinshi.playertitle.title.ChromaType;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.RainbowColorUtil;
import com.kavinshi.playertitle.title.TitleColorUtil;
import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TitleScreen extends Screen {
    private static final int GUI_WIDTH = 370;
    private static final int GUI_HEIGHT = 240;
    private static final int LIST_WIDTH = 175;
    private static final int ENTRY_HEIGHT = 20;
    private static final int MARGIN = 14;
    private static final int DIVIDER_X = 195;

    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private int titlesPerPage = 8;
    private int selectedTitleId = -1;
    private int detailScrollOffset = 0;
    private int detailContentHeight = 0;
    private int detailVisibleHeight = 0;

    public TitleScreen() {
        super(Component.literal("Titles"));
    }

    @Override
    protected void init() {
        super.init();
        NetworkHandler.getChannel().sendToServer(
            new com.kavinshi.playertitle.network.RequestSyncPacket(minecraft.player.getUUID(), true));
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        selectedTitleId = ClientTitleData.getEquippedTitleId();
        titlesPerPage = Math.max(4, (GUI_HEIGHT - 58) / ENTRY_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        ParchmentRenderer.render(graphics.pose(), guiLeft - 8, guiTop - 8, GUI_WIDTH + 16, GUI_HEIGHT + 16, DIVIDER_X);

        Font font = this.font;

        String header = "\u2694 Title Collection \u2694";
        graphics.drawString(font, header, guiLeft + (GUI_WIDTH - font.width(header)) / 2, guiTop + 7, 0xFF8B4513, false);

        int ornL = guiLeft + MARGIN + 10;
        int ornR = guiLeft + GUI_WIDTH - MARGIN - 10;
        int ornY = guiTop + 18;
        graphics.fill(ornL, ornY, ornR, ornY + 1, 0x50D4A017);
        int dX = guiLeft + GUI_WIDTH / 2;
        graphics.fill(dX - 1, ornY - 1, dX + 2, ornY + 2, 0xFFD4A017);

        List<TitleDefinition> allTitles = ClientTitleData.getTitleRegistry();
        Set<Integer> unlocked = ClientTitleData.getUnlockedTitles();
        int equippedId = ClientTitleData.getEquippedTitleId();
        int totalPages = Math.max(1, (int)Math.ceil((double)allTitles.size() / titlesPerPage));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int startIndex = currentPage * titlesPerPage;
        int endIndex = Math.min(startIndex + titlesPerPage, allTitles.size());
        int listX = guiLeft + MARGIN;
        int listY = guiTop + 24;

        String listLabel = "\u25c8 Titles";
        graphics.drawString(font, listLabel, listX, listY, 0xFF6B5B3B, false);
        listY += 12;

        for (int i = startIndex; i < endIndex; i++) {
            TitleDefinition title = allTitles.get(i);
            int entryY = listY + (i - startIndex) * ENTRY_HEIGHT;
            boolean isUnlocked = unlocked.contains(title.getId());
            boolean isSelected = title.getId() == selectedTitleId;
            boolean isEquipped = title.getId() == equippedId;
            boolean hovered = mouseX >= listX && mouseX < listX + LIST_WIDTH && mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT;
            renderTitleEntry(graphics, title, i + 1, listX, entryY, LIST_WIDTH, ENTRY_HEIGHT,
                isUnlocked, isSelected, isEquipped, hovered, font);
        }

        int pageY = guiTop + GUI_HEIGHT - 18;
        String pageText = "\u25c0 " + (currentPage + 1) + "/" + totalPages + " \u25b6";
        graphics.drawString(font, pageText, listX + (LIST_WIDTH - font.width(pageText)) / 2, pageY, 0xFF84847A, false);
        if (currentPage > 0) {
            boolean prevHovered = mouseX >= listX && mouseX < listX + 20 && mouseY >= pageY - 2 && mouseY < pageY + 10;
            graphics.drawString(font, "\u25c0", listX + 2, pageY, prevHovered ? 0xFFD4A017 : 0xFFA58E68, false);
        }
        if (currentPage < totalPages - 1) {
            boolean nextHovered = mouseX >= listX + LIST_WIDTH - 20 && mouseX < listX + LIST_WIDTH && mouseY >= pageY - 2 && mouseY < pageY + 10;
            graphics.drawString(font, "\u25b6", listX + LIST_WIDTH - 10, pageY, nextHovered ? 0xFFD4A017 : 0xFFA58E68, false);
        }

        int divX = guiLeft + DIVIDER_X;
        graphics.fill(divX, guiTop + 22, divX + 1, guiTop + GUI_HEIGHT - 22, 0xFFCCB06E);
        graphics.fill(divX - 1, guiTop + 22, divX + 2, guiTop + GUI_HEIGHT - 24, 0x40D4A017);
        graphics.fill(divX - 1, guiTop + GUI_HEIGHT - 24, divX + 2, guiTop + GUI_HEIGHT - 22, 0x40D4A017);

        int detailX = divX + 10;
        int detailWidth = GUI_WIDTH - DIVIDER_X - MARGIN - 10;
        renderDetailPanel(graphics, detailX, guiTop + 24, detailWidth, mouseX, mouseY, font, unlocked, equippedId);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTitleEntry(GuiGraphics graphics, TitleDefinition title, int displayNumber,
                                     int x, int y, int width, int height,
                                     boolean isUnlocked, boolean isSelected, boolean isEquipped,
                                     boolean hovered, Font font) {
        int bgColor = isSelected
            ? (isEquipped ? 0xC05F7A32 : 0xC050A017)
            : (hovered ? 0xA0302010 : (displayNumber % 2 == 0 ? 0x70FFFFFF : 0x38FFFFFF));
        graphics.fill(x, y, x + width, y + height - 1, bgColor);

        if (isSelected) {
            graphics.fill(x, y, x + width, y + 1, 0xFF4090C0);
            graphics.fill(x, y + height - 2, x + width, y + height - 1, 0xFF4090C0);
        }

        ChromaType chroma = title.getChromaTypeEnum();
        if (isUnlocked) {
            int barColor = 0xFF000000 | (title.hasChroma()
                ? RainbowColorUtil.getChromaColorForChar(chroma, 0, 1)
                : title.getColor());
            graphics.fill(x, y, x + 3, y + height - 1, barColor);
        } else {
            graphics.fill(x, y, x + 2, y + height - 1, 0x30555555);
        }

        String prefix = "#" + displayNumber + " ";
        int textX = x + 7;

        if (!title.getIcon().isEmpty()) {
            try {
                ResourceLocation iconLoc = ResourceLocation.tryParse(title.getIcon());
                if (iconLoc != null) {
                    int iconW = 9;
                    int iconH = 20;
                    graphics.blit(iconLoc, textX - 1, y, iconW, iconH, 0, 0, 16, 35, 16, 35);
                    textX += iconW + 2;
                }
            } catch (Exception ignored) {}
        }

        if (isUnlocked) {
            graphics.drawString(font, prefix, textX, y + 6, 0xFF84847A, false);
            int prefixWidth = font.width(prefix);
            if (title.hasChroma()) {
                MutableComponent coloredName = TitleColorUtil.buildChromaText(title.getName(), chroma);
                graphics.drawString(font, coloredName, textX + prefixWidth, y + 6, 0xFFFFFF, false);
            } else {
                graphics.drawString(font, title.getName(), textX + prefixWidth, y + 6, 0xFF000000 | title.getColor(), false);
            }
        } else {
            graphics.drawString(font, prefix, textX, y + 6, 0xFF84847A, false);
            int prefixWidth = font.width(prefix);
            if (title.hasChroma()) {
                MutableComponent coloredName = TitleColorUtil.buildChromaText(title.getName(), chroma);
                graphics.drawString(font, coloredName, textX + prefixWidth, y + 6, 0xFFFFFF, false);
            } else {
                int dimColor = 0xFF000000 | (title.getColor() & 0xCCCCCC);
                graphics.drawString(font, title.getName(), textX + prefixWidth, y + 6, dimColor, false);
            }
            graphics.drawString(font, "\uD83D\uDD12", x + width - 12, y + 5, 0xFFAA6644, false);
        }

        if (isEquipped) {
            graphics.drawString(font, "\u2605", x + width - 12, y + 6, 0xFFFF7000, false);
        }
    }

    private void renderDetailPanel(GuiGraphics graphics, int x, int y, int width,
                                      int mouseX, int mouseY, Font font,
                                      Set<Integer> unlocked, int equippedId) {
        if (selectedTitleId < 0) {
            String line1 = "\u25c8 Select a title";
            String line2 = "from the list";
            graphics.drawString(font, line1, x + (width - font.width(line1)) / 2, y + 30, 0xFF6B5B3B, false);
            graphics.drawString(font, line2, x + (width - font.width(line2)) / 2, y + 42, 0xFFA58E68, false);
            return;
        }

        TitleDefinition title = findTitle(selectedTitleId);
        if (title == null) {
            graphics.drawString(font, "Title not found.", x, y + 10, 0xFF906060, false);
            return;
        }

        boolean isUnlocked = unlocked.contains(selectedTitleId);
        boolean isEquipped = selectedTitleId == equippedId;
        ChromaType chroma = title.getChromaTypeEnum();
        int headerY = y;

        if (isUnlocked) {
            MutableComponent titleDisplay = title.hasChroma()
                ? TitleColorUtil.buildChromaText(title.getName(), chroma)
                : TitleColorUtil.buildStaticColorText(title.getName(), title.getColor());
            graphics.drawString(font, titleDisplay, x, headerY, 0xFFFFFF, false);
        } else {
            MutableComponent titleDisplay = title.hasChroma()
                ? TitleColorUtil.buildChromaText(title.getName(), chroma)
                : TitleColorUtil.buildStaticColorText(title.getName(), title.getColor() & 0xCCCCCC);
            graphics.drawString(font, titleDisplay, x, headerY, 0xFFFFFF, false);
            graphics.drawString(font, " \uD83D\uDD12", x + font.width(title.getName()) + 4, headerY, 0xFFAA6644, false);
        }

        String rarityName = chroma.getRarityName();
        int rarityColor = chroma.getRarityDisplayColor();
        graphics.drawString(font, "\u25c6 " + rarityName, x, headerY += 12, rarityColor, false);

        int sepY = headerY += 14;
        graphics.fill(x, sepY, x + width, sepY + 1, 0xFF308888);
        int sepMidX = x + width / 2;
        graphics.fill(sepMidX - 1, sepY - 1, sepMidX + 2, sepY + 2, 0xFF60B080);

        if (!isUnlocked) {
            renderLockedDetail(graphics, x, sepY + 6, width, font, title);
            return;
        }

        int btnY = guiTop + GUI_HEIGHT - 28;
        int contentTopY = sepY + 6;
        int contentBottomY = btnY - 4;
        detailVisibleHeight = contentBottomY - contentTopY;
        double guiScale = minecraft.getWindow().getGuiScale();
        int sx = (int)(x * guiScale);
        int sy = (int)((minecraft.getWindow().getGuiScaledHeight() - contentBottomY) * guiScale);
        int sw = (int)((width + 2) * guiScale);
        int sh = (int)(Math.max(1, detailVisibleHeight) * guiScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);

        int infoY = contentTopY - detailScrollOffset;

        if (!title.getIcon().isEmpty()) {
            try {
                ResourceLocation iconLoc = ResourceLocation.tryParse(title.getIcon());
                if (iconLoc != null) {
                    int detailIconW = 16;
                    int detailIconH = 35;
                    graphics.blit(iconLoc, x, infoY, detailIconW, detailIconH, 0, 0, 16, 35, 16, 35);
                    infoY += detailIconH + 4;
                }
            } catch (Exception ignored) {}
        }

        if (title.getDescription() != null && !title.getDescription().isEmpty()) {
            graphics.drawString(font, "\u25b8 Description:", x, infoY, 0xFFD4A017, false);
            infoY += 11;
            List<String> descLines = wrapText(font, title.getDescription(), width);
            for (String line : descLines) {
                graphics.drawString(font, line, x, infoY, 0xFF6B5533, false);
                infoY += 10;
            }
            infoY += 4;
        }

        graphics.drawString(font, "\u25b8 Title Perks:", x, infoY, 0xFFD4A017, false);
        infoY += 11;
        if (!title.getBuffs().isEmpty()) {
            for (var buff : title.getBuffs()) {
                String buffText = buff.getType().name() + " +" + buff.getValue();
                List<String> buffLines = wrapText(font, buffText, width);
                for (String line : buffLines) {
                    graphics.drawString(font, line, x, infoY, 0xFF55AA33, false);
                    infoY += 10;
                }
            }
        } else {
            graphics.drawString(font, "No buffs.", x, infoY, 0xFFA58E68, false);
            infoY += 10;
        }

        graphics.fill(x, infoY + 6, x + width, infoY + 7, 0xFF504030);
        graphics.drawString(font, "\u25b8 Currently Equipped:", x, infoY + 12, 0xFF6B5B3B, false);
        infoY += 24;

        if (minecraft.player != null) {
            ResourceLocation skinLoc = minecraft.player.getSkinTextureLocation();
            graphics.blit(skinLoc, x, infoY, 8, 8, 8.0f, 8.0f, 8, 8, 64, 64);
            graphics.blit(skinLoc, x, infoY, 8, 8, 40.0f, 8.0f, 8, 8, 64, 64);
            String playerName = minecraft.player.getGameProfile().getName();
            graphics.drawString(font, playerName, x + 11, infoY, 0xFF6B5533, false);
            infoY += 10;
            if (ClientTitleData.isUsingCustomTitle()) {
                CustomTitleData ct = ClientTitleData.getCustomTitle();
                String ctDisplay = "[" + ct.getText() + "]";
                graphics.drawString(font, ctDisplay, x, infoY, 0xFF000000 | ct.getColor1(), false);
                graphics.drawString(font, " (Custom)", x + font.width(ctDisplay) + 2, infoY, 0xFFCC88FF, false);
            } else if (equippedId >= 0) {
                TitleDefinition equipped = findTitle(equippedId);
                if (equipped != null) {
                    MutableComponent eqText = equipped.hasChroma()
                        ? TitleColorUtil.buildChromaText(equipped.getName(), equipped.getChromaTypeEnum())
                        : TitleColorUtil.buildStaticColorText(equipped.getName(), equipped.getColor());
                    graphics.drawString(font, eqText, x, infoY, 0xFFFFFF, false);
                } else {
                    graphics.drawString(font, "None", x, infoY, 0xFFA58E68, false);
                }
            } else {
                graphics.drawString(font, "None", x, infoY, 0xFFA58E68, false);
            }
            infoY += 14;
        }

        CustomTitleData ct = ClientTitleData.getCustomTitle();
        if (ct.hasPermission()) {
            graphics.fill(x, infoY, x + width, infoY + 1, 0xFF504030);
            infoY += 6;
            graphics.drawString(font, "\u25b8 Custom Title:", x, infoY, 0xFFCC88FF, false);
            infoY += 12;
            graphics.drawString(font, "Permission: " + ct.getPermissionName(), x, infoY, 0xFF8B7B6B, false);
            infoY += 11;
            if (!ct.getText().isEmpty()) {
                String ctText = "[" + ct.getText() + "]";
                graphics.drawString(font, ctText, x, infoY, 0xFF000000 | ct.getColor1(), false);
                infoY += 11;
            }
            if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                String c2Str = "Color2: #" + Integer.toHexString(ct.getColor2());
                graphics.drawString(font, c2Str, x, infoY, 0xFF000000 | ct.getColor2(), false);
                infoY += 11;
            }
            String statusStr = ct.isUsingCustomTitle() ? "\u2714 Active" : "\u2718 Inactive";
            graphics.drawString(font, statusStr, x, infoY, ct.isUsingCustomTitle() ? 0xFF55AA33 : 0xFFA58E68, false);
            infoY += 11;
            graphics.drawString(font, "Use /playertitle commands", x, infoY, 0xFF706050, false);
            infoY += 10;
            graphics.drawString(font, "to configure", x, infoY, 0xFF706050, false);
        }

        detailContentHeight = infoY + detailScrollOffset - contentTopY + 12;
        int maxScroll = Math.max(0, detailContentHeight - detailVisibleHeight);
        if (detailScrollOffset > maxScroll) detailScrollOffset = maxScroll;
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (detailContentHeight > detailVisibleHeight) {
            if (detailScrollOffset > 0)
                graphics.drawString(font, "\u25b2", x + width - 8, contentTopY, 0xFFD4A017, false);
            if (detailScrollOffset < maxScroll)
                graphics.drawString(font, "\u25bc", x + width - 8, contentBottomY - 10, 0xFFD4A017, false);
        }

        int btnWidth = Math.min(width, 110);
        int btnHeight = 16;
        int btnX = x + (width - btnWidth) / 2;
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;

        if (isEquipped) {
            int btnColor = btnHovered ? 0xFFCC6633 : 0xFFAA5522;
            graphics.fill(btnX - 1, btnY - 1, btnX + btnWidth + 1, btnY + btnHeight + 1, 0xFF994422);
            graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
            graphics.fill(btnX, btnY, btnX + btnWidth, btnY + 1, 0x30FFFFFF);
            graphics.fill(btnX, btnY + btnHeight - 1, btnX + btnWidth, btnY + btnHeight, 0x40000000);
            String btnText = "\u2716 Remove Title";
            graphics.drawString(font, btnText, btnX + (btnWidth - font.width(btnText)) / 2, btnY + 4, 0xFFFFFFFF, false);
        } else if (isUnlocked) {
            int btnColor = btnHovered ? 0xFFDD8833 : 0xFFCC7722;
            graphics.fill(btnX - 1, btnY - 1, btnX + btnWidth + 1, btnY + btnHeight + 1, 0xFF994422);
            graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
            graphics.fill(btnX, btnY, btnX + btnWidth, btnY + 1, 0x30FFFFFF);
            graphics.fill(btnX, btnY + btnHeight - 1, btnX + btnWidth, btnY + btnHeight, 0x40000000);
            String btnText = "\u2694 Equip Title";
            graphics.drawString(font, btnText, btnX + (btnWidth - font.width(btnText)) / 2, btnY + 4, 0xFFFFFFFF, false);
        }
    }

    private void renderLockedDetail(GuiGraphics graphics, int x, int y, int width, Font font, TitleDefinition title) {
        graphics.drawString(font, "\u2716 Title Locked", x, y, 0xFFAA4422, false);
        graphics.drawString(font, "Complete conditions", x, y + 14, 0xFFA58E68, false);
        graphics.drawString(font, "to unlock this title.", x, y + 26, 0xFFA58E68, false);
        y += 42;

        Map<String, Integer> killCounts = ClientTitleData.getKillCounts();
        int aliveMinutes = ClientTitleData.getAliveMinutes();

        for (var condition : title.getConditions()) {
            int current = getCurrentProgress(condition, killCounts, aliveMinutes);
            int required = condition.getRequiredCount();
            boolean condMet = current >= required;
            float progress = required > 0 ? Math.min(1.0f, (float)current / (float)required) : 0.0f;
            String condText = condition.getType().name() + ": " + current + "/" + required;
            List<String> lines = wrapText(font, condText, width);
            for (String line : lines) {
                graphics.drawString(font, line, x, y, condMet ? 0xFF55AA33 : 0xFF886644, false);
                y += 10;
            }
            int barW = Math.min(width - 4, 130);
            int barH = 7;
            graphics.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xFF994422);
            graphics.fill(x, y, x + barW, y + barH, 0xFF906030);
            int fillW = (int)((float)barW * progress);
            if (fillW > 0) {
                int barColor = condMet ? 0xFF44AA22 : 0xFFEE8844;
                graphics.fill(x, y, x + fillW, y + barH, barColor);
                graphics.fill(x, y, x + fillW, y + 1, 0x30FFFFFF);
            }
            y += barH + 6;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int mx = (int)mouseX;
        int my = (int)mouseY;
        List<TitleDefinition> allTitles = ClientTitleData.getTitleRegistry();
        Set<Integer> unlocked = ClientTitleData.getUnlockedTitles();
        int equippedId = ClientTitleData.getEquippedTitleId();
        int totalPages = Math.max(1, (int)Math.ceil((double)allTitles.size() / titlesPerPage));

        int listX = guiLeft + MARGIN;
        int listY = guiTop + 36;
        int startIndex = currentPage * titlesPerPage;
        int endIndex = Math.min(startIndex + titlesPerPage, allTitles.size());
        for (int i = startIndex; i < endIndex; i++) {
            int entryY = listY + (i - startIndex) * ENTRY_HEIGHT;
            if (mx >= listX && mx < listX + LIST_WIDTH && my >= entryY && my < entryY + ENTRY_HEIGHT) {
                selectedTitleId = allTitles.get(i).getId();
                detailScrollOffset = 0;
                return true;
            }
        }

        int pageY = guiTop + GUI_HEIGHT - 16;
        if (currentPage > 0 && mx >= listX && mx < listX + 20 && my >= pageY - 2 && my < pageY + 10) {
            currentPage--;
            return true;
        }
        if (currentPage < totalPages - 1 && mx >= listX + LIST_WIDTH - 20 && mx < listX + LIST_WIDTH && my >= pageY - 2 && my < pageY + 10) {
            currentPage++;
            return true;
        }

        if (selectedTitleId >= 0) {
            int detailX = guiLeft + DIVIDER_X + 8;
            int detailWidth = GUI_WIDTH - DIVIDER_X - MARGIN - 8;
            int btnWidth = Math.min(detailWidth, 110);
            int btnX = detailX + (detailWidth - btnWidth) / 2;
            int btnY = guiTop + GUI_HEIGHT - 28;
            int btnHeight = 16;
            if (mx >= btnX && mx < btnX + btnWidth && my >= btnY && my < btnY + btnHeight) {
                boolean isEquipped = selectedTitleId == equippedId;
                boolean isUnlocked = unlocked.contains(selectedTitleId);
                if (isEquipped) {
                    NetworkHandler.getChannel().sendToServer(new TitleUpdatePacket(
                        minecraft.player.getUUID(),
                        TitleUpdatePacket.UpdateType.TITLE_EQUIPPED, -1));
                } else if (isUnlocked) {
                    NetworkHandler.getChannel().sendToServer(new TitleUpdatePacket(
                        minecraft.player.getUUID(),
                        TitleUpdatePacket.UpdateType.TITLE_EQUIPPED, selectedTitleId));
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<TitleDefinition> allTitles = ClientTitleData.getTitleRegistry();
        int totalPages = Math.max(1, (int)Math.ceil((double)allTitles.size() / titlesPerPage));
        int divX = guiLeft + DIVIDER_X;
        if (mouseX >= divX && mouseX <= guiLeft + GUI_WIDTH && detailContentHeight > detailVisibleHeight) {
            int maxScroll = Math.max(0, detailContentHeight - detailVisibleHeight);
            if (delta > 0) detailScrollOffset = Math.max(0, detailScrollOffset - 10);
            else if (delta < 0) detailScrollOffset = Math.min(maxScroll, detailScrollOffset + 10);
            return true;
        }
        if (mouseX >= guiLeft + MARGIN && mouseX < guiLeft + MARGIN + LIST_WIDTH) {
            if (delta > 0 && currentPage > 0) { currentPage--; return true; }
            if (delta < 0 && currentPage < totalPages - 1) { currentPage++; return true; }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private TitleDefinition findTitle(int id) {
        for (TitleDefinition def : ClientTitleData.getTitleRegistry())
            if (def.getId() == id) return def;
        return null;
    }

    private int getCurrentProgress(com.kavinshi.playertitle.title.TitleCondition condition,
                                   Map<String, Integer> killCounts, int aliveMinutes) {
        return switch (condition.getType()) {
            case KILL_ANY_HOSTILE -> killCounts.values().stream().mapToInt(Integer::intValue).sum();
            case KILL_MOB -> killCounts.getOrDefault(condition.getTarget(), 0);
            case SURVIVAL_TIME -> aliveMinutes;
            default -> 0;
        };
    }

    private List<String> wrapText(Font font, String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        String remaining = text;
        while (font.width(remaining) > maxWidth && remaining.contains(" ")) {
            int lastSpace = 0;
            for (int i = 0; i < remaining.length(); i++) {
                if (remaining.charAt(i) != ' ') continue;
                if (font.width(remaining.substring(0, i)) > maxWidth) break;
                lastSpace = i;
            }
            if (lastSpace == 0) break;
            lines.add(remaining.substring(0, lastSpace));
            remaining = remaining.substring(lastSpace + 1);
        }
        lines.add(remaining);
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
