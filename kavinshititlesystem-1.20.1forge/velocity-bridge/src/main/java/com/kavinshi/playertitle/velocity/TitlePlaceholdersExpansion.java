package com.kavinshi.playertitle.velocity;

import com.velocitypowered.api.proxy.Player;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;

import java.util.Optional;
import java.util.UUID;

public final class TitlePlaceholdersExpansion {

    private final TitleCache cache;
    private Expansion expansion;

    public TitlePlaceholdersExpansion(TitleCache cache) {
        this.cache = cache;
    }

    public void register() {
        expansion = Expansion.builder("playertitle")
            .filter(Player.class)
            .audiencePlaceholder("title", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(Component.text(getTitle(player.getUniqueId())));
            })
            .audiencePlaceholder("titleName", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(Component.text(getTitleName(player.getUniqueId())));
            })
            .audiencePlaceholder("playerName", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(Component.text(getPlayerName(player.getUniqueId())));
            })
            .audiencePlaceholder("serverName", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(Component.text(getServerName(player.getUniqueId())));
            })
            .audiencePlaceholder("hasTitle", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(Component.text(String.valueOf(hasTitle(player.getUniqueId()))));
            })
            .audiencePlaceholder("heading", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(Component.text(getHeading(player.getUniqueId())));
            })
            .audiencePlaceholder("display", (aud, queue, ctx) -> {
                Player player = (Player) aud;
                return Tag.inserting(createFullDisplayComponent(player));
            })
            .build();
            
        expansion.register();
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
