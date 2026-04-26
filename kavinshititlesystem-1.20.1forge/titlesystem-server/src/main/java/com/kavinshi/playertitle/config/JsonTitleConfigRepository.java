package com.kavinshi.playertitle.config;

import com.google.gson.*;
import com.kavinshi.playertitle.title.*;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JsonTitleConfigRepository implements TitleConfigRepository {
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(int.class, new ColorDeserializer())
        .registerTypeAdapter(Integer.class, new ColorDeserializer())
        .create();

    @Override
    public List<TitleDefinition> loadDefinitions(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonTitleDefinition[] rawDefinitions = GSON.fromJson(reader, JsonTitleDefinition[].class);
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
                TitleConditionType type = TitleParseUtils.safeConditionType(condition.type);
                if (type != null) {
                    conditions.add(new TitleCondition(type, condition.target, condition.requiredCount));
                }
            }
        }

        List<TitleBuff> buffs = new ArrayList<>();
        if (raw.buffs != null) {
            for (JsonTitleBuff buff : raw.buffs) {
                TitleBuff.BuffType type = TitleParseUtils.safeBuffType(buff.type);
                if (type != null) {
                    buffs.add(new TitleBuff(type, buff.value, buff.target));
                }
            }
        }

        return new TitleDefinition(
            raw.id,
            raw.name,
            raw.displayOrder,
            raw.color,
            raw.chromaType,
            conditions,
            buffs,
            raw.description,
            raw.category,
            raw.icon,
            raw.iconColor,
            raw.styleMode == null ? TitleStyleMode.PLAIN : TitleStyleMode.valueOf(raw.styleMode),
            raw.baseColors == null ? List.of() : List.of(raw.baseColors),
            raw.animationProfile == null ? null : new TitleAnimationProfile(
                raw.animationProfile.cycleMillis,
                raw.animationProfile.stepSize
            ),
            raw.auraEffect
        );
    }

    private static final class JsonTitleDefinition {
        int id;
        String name;
        int displayOrder;
        int color;
        String chromaType;
        String description;
        String category;
        String icon;
        String iconColor;
        String styleMode;
        String[] baseColors;
        JsonAnimationProfile animationProfile;
        String auraEffect;
        JsonTitleCondition[] conditions;
        JsonTitleBuff[] buffs;
    }

    private static final class JsonTitleCondition {
        String type;
        String target;
        int requiredCount;
    }

    private static final class JsonTitleBuff {
        String type;
        double value;
        String target;
    }

    private static final class JsonAnimationProfile {
        int cycleMillis;
        int stepSize;
    }

    private static final class ColorDeserializer implements JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                JsonPrimitive prim = json.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    return prim.getAsInt();
                }
                if (prim.isString()) {
                    String hex = prim.getAsString().trim();
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    if (hex.length() == 3) {
                        hex = "" + hex.charAt(0) + hex.charAt(0)
                            + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2);
                    }
                    try {
                        return Integer.parseInt(hex, 16);
                    } catch (NumberFormatException e) {
                        throw new JsonParseException("Invalid hex color: " + prim.getAsString());
                    }
                }
            }
            return 0xFFFFFF;
        }
    }
}
