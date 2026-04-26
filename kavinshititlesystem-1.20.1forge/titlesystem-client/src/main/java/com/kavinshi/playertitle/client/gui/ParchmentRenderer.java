package com.kavinshi.playertitle.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public final class ParchmentRenderer {
    private static final int SHADOW = 0x33000000;
    private static final int BORDER_GOLD = 0xFFD4A017;
    private static final int BORDER_DARK = 0xFF8C7246;
    private static final int FILL = 0xF0E6D5AC;
    private static final int LINE_SUBTLE = 0x14FFFFFF;
    private static final int DIVIDER_GOLD = 0xFFCCB06E;

    private ParchmentRenderer() {}

    public static void render(PoseStack poseStack, int x, int y, int width, int height, int dividerX) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            Matrix4f matrix = poseStack.last().pose();

            fillRect(matrix, x + 3, y + 3, x + width, y + height, SHADOW);
            fillRect(matrix, x, y, x + width - 3, y + height - 3, BORDER_GOLD);
            fillRect(matrix, x + 2, y + 2, x + width - 5, y + height - 5, BORDER_DARK);
            fillRect(matrix, x + 3, y + 3, x + width - 6, y + height - 6, 0xFFF0E0C0);
            fillRect(matrix, x + 4, y + 4, x + width - 7, y + height - 7, FILL);

            for (int ly = y + 8; ly < y + height - 8; ly += 4) {
                fillRect(matrix, x + 6, ly, x + width - 9, ly + 1, LINE_SUBTLE);
            }

            int cs = 14;
            int ct = 2;
            fillRect(matrix, x + 4, y + 4, x + 4 + cs, y + 4 + ct, BORDER_GOLD);
            fillRect(matrix, x + 4, y + 4, x + 4 + ct, y + 4 + cs, BORDER_GOLD);
            fillRect(matrix, x + width - 7 - cs, y + 4, x + width - 7, y + 4 + ct, BORDER_GOLD);
            fillRect(matrix, x + width - 7 - ct, y + 4, x + width - 7, y + 4 + cs, BORDER_GOLD);
            fillRect(matrix, x + 4, y + height - 7 - ct, x + 4 + cs, y + height - 7, BORDER_GOLD);
            fillRect(matrix, x + 4, y + height - 7 - cs, x + 4 + ct, y + height - 7, BORDER_GOLD);
            fillRect(matrix, x + width - 7 - cs, y + height - 7 - ct, x + width - 7, y + height - 7, BORDER_GOLD);
            fillRect(matrix, x + width - 7 - ct, y + height - 7 - cs, x + width - 7, y + height - 7, BORDER_GOLD);

            if (dividerX > 0) {
                fillRect(matrix, dividerX, y + 20, dividerX + 1, y + height - 20, DIVIDER_GOLD);
                fillRect(matrix, dividerX + 1, y + 20, dividerX + 2, y + height - 20, 0x20000000);
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    @SuppressWarnings("null")
    private static void fillRect(Matrix4f matrix, int x1, int y1, int x2, int y2, int colorARGB) {
        float a = (float)(colorARGB >> 24 & 0xFF) / 255.0f;
        float r = (float)(colorARGB >> 16 & 0xFF) / 255.0f;
        float g = (float)(colorARGB >> 8 & 0xFF) / 255.0f;
        float b = (float)(colorARGB & 0xFF) / 255.0f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x1, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        tesselator.end();
    }
}
