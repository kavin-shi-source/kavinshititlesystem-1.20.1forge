package com.kavinshi.playertitle.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.kavinshi.playertitle.title.TitleAnimationProfile;
import com.kavinshi.playertitle.title.TitleCondition;
import com.kavinshi.playertitle.title.TitleConditionType;
import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleStyleMode;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonTitleConfigRepository implements TitleConfigRepository {
    private final Gson gson = new Gson();

    @Override
    public List<TitleDefinition> loadDefinitions(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonTitleDefinition[] rawDefinitions = this.gson.fromJson(reader, JsonTitleDefinition[].class);
            if (rawDefinitions == null) {
                return List.of();
            }

            List<TitleDefinition> definitions = new ArrayList<>();
            for (JsonTitleDefinition raw : rawDefinitions) {
                definitions.add(toDefinition(raw));
            }
            return List.copyOf(definitions);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Failed to load title definitions from " + path, exception);
        }
    }

    private static TitleDefinition toDefinition(JsonTitleDefinition raw) {
        List<TitleCondition> conditions = new ArrayList<>();
        if (raw.conditions != null) {
            for (JsonTitleCondition condition : raw.conditions) {
                conditions.add(new TitleCondition(
                    TitleConditionType.valueOf(condition.type),
                    condition.target,
                    condition.requiredCount
                ));
            }
        }

        return new TitleDefinition(
            raw.id,
            raw.name,
            raw.displayOrder,
            raw.color,
            conditions,
            raw.category,
            raw.icon,
            raw.iconColor,
            raw.styleMode == null ? TitleStyleMode.PLAIN : TitleStyleMode.valueOf(raw.styleMode),
            raw.baseColors == null ? List.of() : List.of(raw.baseColors),
            raw.animationProfile == null ? null : new TitleAnimationProfile(
                raw.animationProfile.cycleMillis,
                raw.animationProfile.stepSize
            )
        );
    }

    private static final class JsonTitleDefinition {
        int id;
        String name;
        int displayOrder;
        int color;
        String category;
        String icon;
        String iconColor;
        String styleMode;
        String[] baseColors;
        JsonAnimationProfile animationProfile;
        JsonTitleCondition[] conditions;
    }

    private static final class JsonTitleCondition {
        String type;
        String target;
        int requiredCount;
    }

    private static final class JsonAnimationProfile {
        int cycleMillis;
        int stepSize;
    }
}
