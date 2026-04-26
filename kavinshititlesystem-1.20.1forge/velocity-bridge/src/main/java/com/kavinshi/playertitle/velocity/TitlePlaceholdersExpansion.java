package com.kavinshi.playertitle.velocity;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Optional;
import java.util.UUID;

public final class TitlePlaceholdersExpansion {

    private final TitleCache cache;

    public TitlePlaceholdersExpansion(TitleCache cache) {
        this.cache = cache;
    }

    public String getTitle(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::getEffectiveTitle)
            .orElse("");
    }

    public String getPlayerName(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::getPlayerName)
            .orElse("");
    }

    public String getTitleName(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::getTitleName)
            .orElse("");
    }

    public String getServerName(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::getServerName)
            .orElse("");
    }

    public boolean hasTitle(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::isHasTitle)
            .orElse(false);
    }

    public int getTitleId(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::getEquippedTitleId)
            .orElse(-1);
    }

    public String getHeading(UUID playerId) {
        return cache.get(playerId)
            .map(TitleCache.TitleEntry::getHeading)
            .orElse("");
    }

    public Component createTitleComponent(Player player) {
        Optional<TitleCache.TitleEntry> entry = cache.get(player.getUniqueId());
        if (entry.isEmpty() || !entry.get().isHasTitle()) {
            return Component.empty();
        }
        TitleCache.TitleEntry e = entry.get();
        return Component.text(e.getTitleName()).color(TextColor.color(e.getTitleColor()));
    }

    public Component createHeadingComponent(Player player) {
        Optional<TitleCache.TitleEntry> entry = cache.get(player.getUniqueId());
        if (entry.isEmpty() || entry.get().getHeading().isEmpty()) {
            return Component.empty();
        }
        return Component.text(entry.get().getHeading());
    }

    public Component createFullDisplayComponent(Player player) {
        Optional<TitleCache.TitleEntry> entry = cache.get(player.getUniqueId());
        Component nameComp = Component.text(player.getUsername());
        if (entry.isEmpty()) {
            return nameComp;
        }
        
        Component headingComp = createHeadingComponent(player);
        Component titleComp = createTitleComponent(player);
        
        Component result = Component.empty();
        if (headingComp != Component.empty()) {
            result = result.append(Component.text("[")).append(headingComp).append(Component.text("]"));
        }
        if (titleComp != Component.empty()) {
            result = result.append(Component.text("[")).append(titleComp).append(Component.text("]"));
        }
        
        if (result != Component.empty()) {
            result = result.append(Component.text(" "));
        }
        return result.append(nameComp);
    }
}
