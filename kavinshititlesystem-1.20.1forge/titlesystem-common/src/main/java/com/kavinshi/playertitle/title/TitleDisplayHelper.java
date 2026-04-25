package com.kavinshi.playertitle.title;

import com.kavinshi.playertitle.player.PlayerTitleState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class TitleDisplayHelper {

    private TitleDisplayHelper() {}

    public static MutableComponent buildTitleComponent(PlayerTitleState state, TitleRegistry registry) {
        CustomTitleData ct = state.getCustomTitle();
        if (ct.isUsingCustomTitle() && ct.hasPermission()) {
            return createCustomTitleComponent(ct);
        }

        int titleId = state.getEquippedTitleId();
        if (titleId < 0) return null;

        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return null;

        return createTitleComponent(title);
    }

    public static MutableComponent createTitleComponent(TitleDefinition title) {
        String display = "[" + title.getName() + "]";
        MutableComponent component = Component.literal(display);
        int color = title.getColor();
        if (color != 0xFFFFFF) {
            component.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return component;
    }

    public static MutableComponent createCustomTitleComponent(CustomTitleData ct) {
        String display = "[" + ct.getText() + "]";
        MutableComponent component = Component.literal(display);
        int color = ct.getColor1();
        if (color != 0xFFFFFF) {
            component.withStyle(style -> style.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return component;
    }

    public static MutableComponent createTabPrefix(PlayerTitleState state, TitleRegistry registry) {
        CustomTitleData ct = state.getCustomTitle();
        if (ct.isUsingCustomTitle() && ct.hasPermission()) {
            String display = "[" + ct.getText() + "] ";
            return Component.literal(display)
                .withStyle(s -> s.withColor(TextColor.fromRgb(ct.getColor1() & 0xFFFFFF)));
        }

        int titleId = state.getEquippedTitleId();
        if (titleId < 0) return null;

        TitleDefinition title = registry.getTitle(titleId);
        if (title == null) return null;

        String display = "[" + title.getName() + "] ";
        MutableComponent titleComp = Component.literal(display);
        int color = title.getColor();
        if (color != 0xFFFFFF) {
            titleComp.withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
        }
        return titleComp;
    }
}
