package com.kavinshi.playertitle.client.gui;

import com.kavinshi.playertitle.client.ClientTitleData;
import com.kavinshi.playertitle.network.NetworkHandler;
import com.kavinshi.playertitle.network.TitleUpdatePacket;
import com.kavinshi.playertitle.network.CustomTitleUpdatePacket;
import com.kavinshi.playertitle.title.ChromaType;
import com.kavinshi.playertitle.title.CustomTitleData;
import com.kavinshi.playertitle.title.RainbowColorUtil;
import com.kavinshi.playertitle.title.TitleColorUtil;
import com.kavinshi.playertitle.title.TitleDefinition;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * 标题系统主界面，显示玩家已解锁的称号列表和自定义标题设置。
 * 提供称号装备、自定义标题编辑和视觉预览功能。
 */
public class TitleScreen extends Screen {
    private static final int GUI_WIDTH = 370;
    private static final int GUI_HEIGHT = 260;
    private static final int LIST_WIDTH = 175;
    private static final int ENTRY_HEIGHT = 20;
    private static final int MARGIN = 14;
    private static final int DIVIDER_X = 195;
    private static final int CUSTOM_PANEL_HEIGHT = 55;

    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private int titlesPerPage = 8;
    private int selectedTitleId = -1;
    private int detailScrollOffset = 0;
    private int detailContentHeight = 0;
    private int detailVisibleHeight = 0;
    private boolean editingCustomTitle = false;
    private String customTitleEditText = "";
    private int customTitleEditColor1 = 0xFFFFFF;
    private int customTitleEditColor2 = 0xFFFFFF;
    
    // 颜色选择器相关变量
    private static final int[] COLOR_PALETTE = {
        0xFF0000, // 红
        0x00FF00, // 绿
        0x0000FF, // 蓝
        0xFFFF00, // 黄
        0xFF00FF, // 紫
        0x00FFFF, // 青
        0xFFA500, // 橙
        0x800080, // 紫红
        0x008000, // 深绿
        0x000080, // 深蓝
        0x808080, // 灰
        0xC0C0C0, // 银
        0xFFD700, // 金
        0xFFFFFF, // 白
        0x000000  // 黑
    };
    private static final int COLOR_SQUARE_SIZE = 14;
    private static final int COLOR_SQUARE_MARGIN = 2;
    private static final int COLORS_PER_ROW = 8;
    private boolean selectingColor1 = false;
    private boolean selectingColor2 = false;
    private int colorPickerX = 0;
    private int colorPickerY = 0;
    private int colorPickerWidth = 0;
    private int colorPickerHeight = 0;

    public TitleScreen() {
        super(Component.literal("Titles"));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null && minecraft.player != null) {
            NetworkHandler.getChannel().sendToServer(
                new com.kavinshi.playertitle.network.RequestSyncPacket(minecraft.player.getUUID(), true));
        }
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
        selectedTitleId = ClientTitleData.getEquippedTitleId();
        titlesPerPage = Math.max(4, (GUI_HEIGHT - 58 - CUSTOM_PANEL_HEIGHT) / ENTRY_HEIGHT);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

        int pageY = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 18;
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
        graphics.fill(divX, guiTop + 22, divX + 1, guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 22, 0xFFCCB06E);
        graphics.fill(divX - 1, guiTop + 22, divX + 2, guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 24, 0x40D4A017);
        graphics.fill(divX - 1, guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 24, divX + 2, guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 22, 0x40D4A017);

        int detailX = divX + 10;
        int detailWidth = GUI_WIDTH - DIVIDER_X - MARGIN - 10;
        int detailBottomLimit = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 4;
        renderDetailPanel(graphics, detailX, guiTop + 24, detailWidth, detailBottomLimit, mouseX, mouseY, font, unlocked, equippedId);

        renderCustomPanel(graphics, guiLeft, guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT, GUI_WIDTH, CUSTOM_PANEL_HEIGHT, mouseX, mouseY, font);

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
                    int iconTexW = 85;
                    int iconTexH = 16;
                    float scale = (float)(ENTRY_HEIGHT - 1) / iconTexH;
                    int drawW = Math.round(iconTexW * scale);
                    int drawH = ENTRY_HEIGHT - 1;
                    graphics.blit(iconLoc, textX, y + 1, drawW, drawH, 0, 0, iconTexW, iconTexH, iconTexW, iconTexH);
                    textX += drawW + 2;
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

    private void renderDetailPanel(GuiGraphics graphics, int x, int y, int width, int bottomLimit,
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

        int btnY = bottomLimit - 18;
        int contentTopY = sepY + 6;
        int contentBottomY = btnY - 4;
        detailVisibleHeight = contentBottomY - contentTopY;
        double guiScale = minecraft != null ? minecraft.getWindow().getGuiScale() : 1.0;
        int sx = (int)(x * guiScale);
        int sy = (int)(((minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : height) - contentBottomY) * guiScale);
        int sw = (int)((width + 2) * guiScale);
        int sh = (int)(Math.max(1, detailVisibleHeight) * guiScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);

        int infoY = contentTopY - detailScrollOffset;

        if (!title.getIcon().isEmpty()) {
            try {
                ResourceLocation iconLoc = ResourceLocation.tryParse(title.getIcon());
                if (iconLoc != null) {
                    int detailIconW = 85;
                    int detailIconH = 16;
                    graphics.blit(iconLoc, x, infoY, detailIconW, detailIconH, 0, 0, detailIconW, detailIconH, detailIconW, detailIconH);
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

        if (minecraft != null && minecraft.player != null) {
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

    private void renderCustomPanel(GuiGraphics graphics, int panelX, int panelY, int panelWidth, int panelHeight,
                                      int mouseX, int mouseY, Font font) {
        CustomTitleData ct = ClientTitleData.getCustomTitle();

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0302010);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFFCC8844);
        graphics.fill(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, 0xFF996633);

        int innerX = panelX + 12;
        int innerY = panelY + 8;
        int innerWidth = panelWidth - 24;
        int btnAreaWidth = 52;

        if (!ct.hasPermission()) {
            String noPermText = "Custom Title: No Permission";
            graphics.drawString(font, noPermText, innerX + (innerWidth - font.width(noPermText)) / 2, innerY + 12, 0xFF888888, false);
            return;
        }

        if (!editingCustomTitle) {
            String customLabel = "\u2728 Custom Title \u2728";
            graphics.drawString(font, customLabel, innerX, innerY, 0xFFCC88FF, false);
            innerY += 12;

            String permStr = "Permission: " + ct.getPermissionName();
            graphics.drawString(font, permStr, innerX, innerY, 0xFF8B7B6B, false);
            innerY += 11;

            if (!ct.getText().isEmpty()) {
                String ctText = "[" + ct.getText() + "]";
                int textCol = 0xFF000000 | ct.getColor1();
                graphics.drawString(font, ctText, innerX, innerY, textCol, false);
                if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                    String arrow = " \u2192 ";
                    graphics.drawString(font, arrow, innerX + font.width(ctText), innerY, 0xFF888888, false);
                    String c2Hex = "#" + Integer.toHexString(ct.getColor2()).toUpperCase();
                    graphics.drawString(font, c2Hex, innerX + font.width(ctText) + font.width(arrow), innerY, 0xFF000000 | ct.getColor2(), false);
                }
                innerY += 11;
            } else {
                graphics.drawString(font, "[Not Set]", innerX, innerY, 0xFFAAAAAA, false);
                innerY += 11;
            }

            String statusStr = ct.isUsingCustomTitle() ? "\u2714 Active" : "\u2718 Inactive";
            int statusColor = ct.isUsingCustomTitle() ? 0xFF55AA33 : 0xFFA58E68;
            graphics.drawString(font, statusStr, innerX, innerY, statusColor, false);

            int btnW = 46;
            int btnH = 14;
            int btnX = panelX + panelWidth - 12 - btnW;
            int editBtnY = panelY + 10;
            boolean editHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= editBtnY && mouseY < editBtnY + btnH;
            int editColor = editHovered ? 0xFFDD8833 : 0xFFCC7722;
            graphics.fill(btnX - 1, editBtnY - 1, btnX + btnW + 1, editBtnY + btnH + 1, 0xFF994422);
            graphics.fill(btnX, editBtnY, btnX + btnW, editBtnY + btnH, editColor);
            graphics.fill(btnX, editBtnY, btnX + btnW, editBtnY + 1, 0x30FFFFFF);
            graphics.fill(btnX, editBtnY + btnH - 1, btnX + btnW, editBtnY + btnH, 0x40000000);
            String editText = "\u270E Edit";
            graphics.drawString(font, editText, btnX + (btnW - font.width(editText)) / 2, editBtnY + 3, 0xFFFFFFFF, false);

            int toggleBtnY = editBtnY + btnH + 4;
            boolean toggleHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= toggleBtnY && mouseY < toggleBtnY + btnH;
            int toggleColor = toggleHovered ? (ct.isUsingCustomTitle() ? 0xFFCC6633 : 0xFFDD8833) : (ct.isUsingCustomTitle() ? 0xFFAA5522 : 0xFFCC7722);
            graphics.fill(btnX - 1, toggleBtnY - 1, btnX + btnW + 1, toggleBtnY + btnH + 1, 0xFF994422);
            graphics.fill(btnX, toggleBtnY, btnX + btnW, toggleBtnY + btnH, toggleColor);
            graphics.fill(btnX, toggleBtnY, btnX + btnW, toggleBtnY + 1, 0x30FFFFFF);
            graphics.fill(btnX, toggleBtnY + btnH - 1, btnX + btnW, toggleBtnY + btnH, 0x40000000);
            String toggleText = ct.isUsingCustomTitle() ? "\u2718 Off" : "\u2714 On";
            graphics.drawString(font, toggleText, btnX + (btnW - font.width(toggleText)) / 2, toggleBtnY + 3, 0xFFFFFFFF, false);
        } else {
            if (customTitleEditText.isEmpty()) {
                customTitleEditText = ct.getText();
            }
            if (customTitleEditColor1 == 0xFFFFFF) {
                customTitleEditColor1 = ct.getColor1();
            }
            if (customTitleEditColor2 == 0xFFFFFF) {
                customTitleEditColor2 = ct.getColor2();
            }

            String editLabel = "\u270E Editing Custom Title";
            graphics.drawString(font, editLabel, innerX, innerY, 0xFFD4A017, false);
            innerY += 13;

            int tfW = innerWidth - 80;
            int tfH = 13;
            int tfX = innerX;
            int tfY = innerY;
            graphics.fill(tfX - 1, tfY - 1, tfX + tfW + 1, tfY + tfH + 1, 0xFF505050);
            graphics.fill(tfX, tfY, tfX + tfW, tfY + tfH, 0xFF202020);
            String displayText = customTitleEditText + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
            graphics.drawString(font, displayText, tfX + 2, tfY + 2, 0xFFFFFFFF, false);

            int colorX = tfX + tfW + 10;
            if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                graphics.drawString(font, "C1:", colorX, innerY + 1, 0xFFD4A017, false);
                renderColorSquare(graphics, colorX + 16, innerY, customTitleEditColor1, mouseX, mouseY);
                graphics.drawString(font, "C2:", colorX, innerY + 12, 0xFFD4A017, false);
                renderColorSquare(graphics, colorX + 16, innerY + 11, customTitleEditColor2, mouseX, mouseY);
                if (selectingColor1 || selectingColor2) {
                    renderColorPickerPanel(graphics, font, mouseX, mouseY, innerX, innerY + 25);
                }
            } else {
                graphics.drawString(font, "Color:", colorX, innerY + 1, 0xFFD4A017, false);
                renderColorSquare(graphics, colorX + 36, innerY, customTitleEditColor1, mouseX, mouseY);
                if (selectingColor1) {
                    renderColorPickerPanel(graphics, font, mouseX, mouseY, innerX, innerY + 25);
                }
            }

            innerY += tfH + 6;

            int saveBtnW = 50;
            int cancelBtnW = 50;
            int totalBtnW = saveBtnW + cancelBtnW + 6;
            int startX = innerX + (innerWidth - totalBtnW) / 2;

            int saveBtnX = startX;
            int saveBtnY = innerY;
            int saveBtnH = 14;
            boolean saveHovered = mouseX >= saveBtnX && mouseX < saveBtnX + saveBtnW && mouseY >= saveBtnY && mouseY < saveBtnY + saveBtnH;
            int saveColor = saveHovered ? 0xFF55AA33 : 0xFF44AA22;
            graphics.fill(saveBtnX - 1, saveBtnY - 1, saveBtnX + saveBtnW + 1, saveBtnY + saveBtnH + 1, 0xFF994422);
            graphics.fill(saveBtnX, saveBtnY, saveBtnX + saveBtnW, saveBtnY + saveBtnH, saveColor);
            graphics.fill(saveBtnX, saveBtnY, saveBtnX + saveBtnW, saveBtnY + 1, 0x30FFFFFF);
            graphics.fill(saveBtnX, saveBtnY + saveBtnH - 1, saveBtnX + saveBtnW, saveBtnY + saveBtnH, 0x40000000);
            graphics.drawString(font, "Save", saveBtnX + (saveBtnW - font.width("Save")) / 2, saveBtnY + 3, 0xFFFFFFFF, false);

            int cancelBtnX = startX + saveBtnW + 6;
            int cancelBtnY = innerY;
            int cancelBtnH = 14;
            boolean cancelHovered = mouseX >= cancelBtnX && mouseX < cancelBtnX + cancelBtnW && mouseY >= cancelBtnY && mouseY < cancelBtnY + cancelBtnH;
            int cancelColor = cancelHovered ? 0xFFCC6633 : 0xFFAA5522;
            graphics.fill(cancelBtnX - 1, cancelBtnY - 1, cancelBtnX + cancelBtnW + 1, cancelBtnY + cancelBtnH + 1, 0xFF994422);
            graphics.fill(cancelBtnX, cancelBtnY, cancelBtnX + cancelBtnW, cancelBtnY + cancelBtnH, cancelColor);
            graphics.fill(cancelBtnX, cancelBtnY, cancelBtnX + cancelBtnW, cancelBtnY + 1, 0x30FFFFFF);
            graphics.fill(cancelBtnX, cancelBtnY + cancelBtnH - 1, cancelBtnX + cancelBtnW, cancelBtnY + cancelBtnH, 0x40000000);
            graphics.drawString(font, "Cancel", cancelBtnX + (cancelBtnW - font.width("Cancel")) / 2, cancelBtnY + 3, 0xFFFFFFFF, false);
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

        int pageY = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 16;
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
            int btnY = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT - 28;
            int btnHeight = 16;
            if (mx >= btnX && mx < btnX + btnWidth && my >= btnY && my < btnY + btnHeight) {
                if (minecraft != null && minecraft.player != null) {
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
                }
                return true;
            }
        }

        CustomTitleData ct = ClientTitleData.getCustomTitle();
        int customPanelY = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT;
        int innerX = guiLeft + 12;
        int innerWidth = GUI_WIDTH - 24;

        if (ct.hasPermission()) {
            if (!editingCustomTitle) {
                int btnW = 46;
                int btnH = 14;
                int btnX = guiLeft + GUI_WIDTH - 12 - btnW;
                int editBtnY = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT + 10;
                if (mx >= btnX && mx < btnX + btnW && my >= editBtnY && my < editBtnY + btnH) {
                    editingCustomTitle = true;
                    customTitleEditText = ct.getText();
                    customTitleEditColor1 = ct.getColor1();
                    customTitleEditColor2 = ct.getColor2();
                    return true;
                }

                int toggleBtnY = editBtnY + btnH + 4;
                if (mx >= btnX && mx < btnX + btnW && my >= toggleBtnY && my < toggleBtnY + btnH) {
                    System.out.println("[TitleSystem] Toggle button clicked: currentUsing=" + ct.isUsingCustomTitle() + ", sending useCustom=" + !ct.isUsingCustomTitle());
                    NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                        CustomTitleUpdatePacket.UpdateType.TOGGLE_USE, !ct.isUsingCustomTitle()));
                    return true;
                }
            } else {
                int saveBtnW = 50;
                int cancelBtnW = 50;
                int totalBtnW = saveBtnW + cancelBtnW + 6;
                int startX = guiLeft + 12 + (innerWidth - totalBtnW) / 2;
                int saveBtnH = 14;
                int innerY = customPanelY + 8 + 13 + 13 + 6;
                int saveBtnY = innerY;

                int saveBtnX = startX;
                if (mx >= saveBtnX && mx < saveBtnX + saveBtnW && my >= saveBtnY && my < saveBtnY + saveBtnH) {
                    if (!customTitleEditText.isEmpty()) {
                        NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                            CustomTitleUpdatePacket.UpdateType.SET_TEXT, customTitleEditText));
                        if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                            NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                                CustomTitleUpdatePacket.UpdateType.SET_COLOR2, customTitleEditColor1, customTitleEditColor2));
                        } else {
                            NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                                CustomTitleUpdatePacket.UpdateType.SET_COLOR1, customTitleEditColor1));
                        }
                    }
                    editingCustomTitle = false;
                    return true;
                }

                int cancelBtnX = startX + saveBtnW + 6;
                if (mx >= cancelBtnX && mx < cancelBtnX + cancelBtnW && my >= saveBtnY && my < saveBtnY + saveBtnH) {
                    editingCustomTitle = false;
                    return true;
                }
            }
            
            // 颜色选择器交互逻辑（编辑模式下）
            if (editingCustomTitle) {
                int innerY = customPanelY + 8;
                
                // 计算颜色方块位置
                int tfW = innerWidth - 80;
                int tfX = innerX;
                int tfY = innerY + 13; // 编辑标签后
                int colorX = tfX + tfW + 10;
                
                // 检查颜色选择器面板点击
                if (selectingColor1 || selectingColor2) {
                    if (mx >= colorPickerX && mx < colorPickerX + colorPickerWidth && 
                        my >= colorPickerY && my < colorPickerY + colorPickerHeight) {
                        // 计算点击的颜色索引
                        int squareSize = COLOR_SQUARE_SIZE;
                        int margin = COLOR_SQUARE_MARGIN;
                        int colorsPerRow = COLORS_PER_ROW;
                        
                        int relativeX = mx - (colorPickerX + 4);
                        int relativeY = my - (colorPickerY + 4);
                        
                        int col = relativeX / (squareSize + margin);
                        int row = relativeY / (squareSize + margin);
                        
                        if (col >= 0 && col < colorsPerRow && row >= 0 && row < Math.ceil((double)COLOR_PALETTE.length / colorsPerRow)) {
                            int index = row * colorsPerRow + col;
                            if (index >= 0 && index < COLOR_PALETTE.length) {
                                int selectedColor = COLOR_PALETTE[index];
                                if (selectingColor1) {
                                    customTitleEditColor1 = selectedColor;
                                    selectingColor1 = false;
                                } else if (selectingColor2) {
                                    customTitleEditColor2 = selectedColor;
                                    selectingColor2 = false;
                                }
                                return true;
                            }
                        }
                    }
                    // 如果点击了颜色选择器面板外部，关闭颜色选择器
                    selectingColor1 = false;
                    selectingColor2 = false;
                    return true;
                }
                
                // 检查颜色方块点击
                if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                    // 颜色1方块
                    int color1SquareX = colorX + 16;
                    int color1SquareY = tfY; // 文本字段的Y坐标，与renderCustomPanel中的innerY相同
                    
                    if (mx >= color1SquareX && mx < color1SquareX + COLOR_SQUARE_SIZE &&
                        my >= color1SquareY && my < color1SquareY + COLOR_SQUARE_SIZE) {
                        selectingColor1 = true;
                        selectingColor2 = false;
                        return true;
                    }
                    
                    // 颜色2方块
                    int color2SquareX = colorX + 16;
                    int color2SquareY = tfY + 11; // 第505行使用innerY + 11
                    
                    if (mx >= color2SquareX && mx < color2SquareX + COLOR_SQUARE_SIZE &&
                        my >= color2SquareY && my < color2SquareY + COLOR_SQUARE_SIZE) {
                        selectingColor1 = false;
                        selectingColor2 = true;
                        return true;
                    }
                } else {
                    // 单色方块
                    int colorSquareX = colorX + 36;
                    int colorSquareY = tfY; // 第514行使用innerY
                    
                    if (mx >= colorSquareX && mx < colorSquareX + COLOR_SQUARE_SIZE &&
                        my >= colorSquareY && my < colorSquareY + COLOR_SQUARE_SIZE) {
                        selectingColor1 = true;
                        selectingColor2 = false;
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<TitleDefinition> allTitles = ClientTitleData.getTitleRegistry();
        int totalPages = Math.max(1, (int)Math.ceil((double)allTitles.size() / titlesPerPage));
        int divX = guiLeft + DIVIDER_X;
        int customPanelTop = guiTop + GUI_HEIGHT - CUSTOM_PANEL_HEIGHT;
        if (mouseX >= divX && mouseX <= guiLeft + GUI_WIDTH && mouseY >= guiTop + 24 && mouseY < customPanelTop && detailContentHeight > detailVisibleHeight) {
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingCustomTitle) {
            if (keyCode == 256) {
                editingCustomTitle = false;
                return true;
            }
            if (keyCode == 257) {
                CustomTitleData ct = ClientTitleData.getCustomTitle();
                if (!customTitleEditText.isEmpty()) {
                    NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                        CustomTitleUpdatePacket.UpdateType.SET_TEXT, customTitleEditText));
                    if (ct.getPermission() >= CustomTitleData.PERMISSION_GRADIENT) {
                        NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                            CustomTitleUpdatePacket.UpdateType.SET_COLOR2, customTitleEditColor1, customTitleEditColor2));
                    } else {
                        NetworkHandler.getChannel().sendToServer(new CustomTitleUpdatePacket(
                            CustomTitleUpdatePacket.UpdateType.SET_COLOR1, customTitleEditColor1));
                    }
                }
                editingCustomTitle = false;
                return true;
            }
            if (keyCode == 259) {
                if (!customTitleEditText.isEmpty()) {
                    customTitleEditText = customTitleEditText.substring(0, customTitleEditText.length() - 1);
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editingCustomTitle) {
            // 允许ASCII字符和汉字等Unicode字母数字字符
            if ((codePoint >= 32 && codePoint <= 126) || Character.isLetterOrDigit(codePoint)) {
                int maxLen = 16;
                if (customTitleEditText.length() < maxLen) {
                    customTitleEditText += codePoint;
                }
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
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

    private void renderColorSquare(GuiGraphics graphics, int x, int y, int color, int mouseX, int mouseY) {
        int size = COLOR_SQUARE_SIZE;
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        
        // 绘制边框
        int borderColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, borderColor);
        
        // 绘制颜色方块
        graphics.fill(x, y, x + size, y + size, 0xFF000000 | color);
        
        // 绘制高光效果
        graphics.fill(x, y, x + size, y + 1, 0x30FFFFFF);
        graphics.fill(x, y + size - 1, x + size, y + size, 0x40000000);
    }

    private void renderColorPickerPanel(GuiGraphics graphics, Font font, int mouseX, int mouseY, int panelX, int panelY) {
        int colorsPerRow = COLORS_PER_ROW;
        int squareSize = COLOR_SQUARE_SIZE;
        int margin = COLOR_SQUARE_MARGIN;
        int totalColors = COLOR_PALETTE.length;
        int rows = (int) Math.ceil((double) totalColors / colorsPerRow);
        
        // 计算面板尺寸
        colorPickerWidth = colorsPerRow * (squareSize + margin) - margin + 8;
        colorPickerHeight = rows * (squareSize + margin) - margin + 8;
        colorPickerX = panelX;
        colorPickerY = panelY;
        
        // 绘制面板背景
        graphics.fill(colorPickerX, colorPickerY, colorPickerX + colorPickerWidth, colorPickerY + colorPickerHeight, 0xFF202020);
        graphics.fill(colorPickerX, colorPickerY, colorPickerX + colorPickerWidth, colorPickerY + 1, 0xFFCC8844);
        graphics.fill(colorPickerX, colorPickerY + colorPickerHeight - 1, colorPickerX + colorPickerWidth, colorPickerY + colorPickerHeight, 0xFF996633);
        
        // 绘制颜色方块
        int currentX = colorPickerX + 4;
        int currentY = colorPickerY + 4;
        
        for (int i = 0; i < totalColors; i++) {
            int color = COLOR_PALETTE[i];
            int squareX = currentX + (i % colorsPerRow) * (squareSize + margin);
            int squareY = currentY + (i / colorsPerRow) * (squareSize + margin);
            
            boolean hovered = mouseX >= squareX && mouseX < squareX + squareSize && mouseY >= squareY && mouseY < squareY + squareSize;
            
            // 绘制边框
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF666666;
            graphics.fill(squareX - 1, squareY - 1, squareX + squareSize + 1, squareY + squareSize + 1, borderColor);
            
            // 绘制颜色方块
            graphics.fill(squareX, squareY, squareX + squareSize, squareY + squareSize, 0xFF000000 | color);
            
            // 绘制高光效果
            graphics.fill(squareX, squareY, squareX + squareSize, squareY + 1, 0x30FFFFFF);
            graphics.fill(squareX, squareY + squareSize - 1, squareX + squareSize, squareY + squareSize, 0x40000000);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
