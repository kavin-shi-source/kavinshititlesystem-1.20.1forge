package com.kavinshi.playertitle.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kavinshi.playertitle.title.TitleDefinition;
import com.kavinshi.playertitle.title.TitleRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonTitleConfigRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsTitleDefinitionsFromJsonFile() throws Exception {
        Path configFile = tempDir.resolve("titles.json");
        Files.writeString(configFile, """
            [
              {
                "id": 4002,
                "name": "Ancient Warden",
                "displayOrder": 20,
                "color": 16777215,
                "category": "combat",
                "icon": "⚔",
                "iconColor": "#ffaa00",
                "styleMode": "STATIC_GRADIENT",
                "baseColors": ["#ff0000", "#0000ff"],
                "conditions": [
                  {
                    "type": "KILL_MOB",
                    "target": "minecraft:warden",
                    "requiredCount": 2
                  }
                ]
              },
              {
                "id": 4001,
                "name": "Thirty Minutes",
                "displayOrder": 10,
                "color": 16776960,
                "category": "survival",
                "conditions": [
                  {
                    "type": "SURVIVAL_TIME",
                    "target": "",
                    "requiredCount": 30
                  }
                ]
              }
            ]
            """);

        TitleConfigRepository repository = new JsonTitleConfigRepository();
        List<TitleDefinition> definitions = repository.loadDefinitions(configFile);

        assertEquals(2, definitions.size());
        assertEquals(4002, definitions.get(0).getId());
        assertEquals("Ancient Warden", definitions.get(0).getName());
        assertEquals("combat", definitions.get(0).getCategory());
        assertEquals("⚔", definitions.get(0).getIcon());
        assertEquals(2, definitions.get(0).getBaseColors().size());
        assertEquals(1, definitions.get(0).getConditions().size());
    }

    @Test
    void loadsAnimatedStyleMetadataFromJsonFile() throws Exception {
        Path configFile = tempDir.resolve("animated.json");
        Files.writeString(configFile, """
            [
              {
                "id": 4010,
                "name": "Rainbow Legend",
                "displayOrder": 50,
                "color": 16777215,
                "category": "special",
                "icon": "☄",
                "iconColor": "#ffffff",
                "styleMode": "ANIMATED_CHROMA",
                "baseColors": ["#ff0000", "#00ff00", "#0000ff"],
                "animationProfile": {
                  "cycleMillis": 1800,
                  "stepSize": 3
                },
                "conditions": [
                  {
                    "type": "SURVIVAL_TIME",
                    "target": "",
                    "requiredCount": 60
                  }
                ]
              }
            ]
            """);

        TitleConfigRepository repository = new JsonTitleConfigRepository();
        TitleDefinition definition = repository.loadDefinitions(configFile).get(0);

        assertEquals("☄", definition.getIcon());
        assertEquals("#ffffff", definition.getIconColor());
        assertEquals(3, definition.getBaseColors().size());
        assertEquals(1800, definition.getAnimationProfile().cycleMillis());
        assertEquals(3, definition.getAnimationProfile().stepSize());
    }

    @Test
    void registrySortRemainsStableAfterLoadingFromRepository() throws Exception {
        Path configFile = tempDir.resolve("titles.json");
        Files.writeString(configFile, """
            [
              {
                "id": 4003,
                "name": "Later",
                "displayOrder": 30,
                "color": 16777215,
                "category": "default",
                "conditions": [
                  {
                    "type": "SURVIVAL_TIME",
                    "target": "",
                    "requiredCount": 1
                  }
                ]
              },
              {
                "id": 4001,
                "name": "First",
                "displayOrder": 10,
                "color": 16777215,
                "category": "default",
                "conditions": [
                  {
                    "type": "SURVIVAL_TIME",
                    "target": "",
                    "requiredCount": 1
                  }
                ]
              },
              {
                "id": 4002,
                "name": "Second",
                "displayOrder": 10,
                "color": 16777215,
                "category": "default",
                "conditions": [
                  {
                    "type": "SURVIVAL_TIME",
                    "target": "",
                    "requiredCount": 1
                  }
                ]
              }
            ]
            """);

        TitleConfigRepository repository = new JsonTitleConfigRepository();
        TitleRegistry registry = new TitleRegistry();
        registry.loadAll(repository.loadDefinitions(configFile));

        assertEquals(List.of(4001, 4002, 4003), registry.getAllTitlesSorted().stream().map(TitleDefinition::getId).toList());
    }

    @Test
    void throwsClearExceptionForInvalidJson() throws Exception {
        Path configFile = tempDir.resolve("broken.json");
        Files.writeString(configFile, """
            [
              {
                "id": 5001,
                "name": "Broken"
            """);

        TitleConfigRepository repository = new JsonTitleConfigRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.loadDefinitions(configFile));
    }
}
