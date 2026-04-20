package com.kavinshi.playertitle.title;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TitleRegistry {
    private final Map<Integer, TitleDefinition> titles = new ConcurrentHashMap<>();

    public void loadAll(List<TitleDefinition> definitions) {
        this.titles.clear();
        for (TitleDefinition definition : definitions) {
            this.titles.put(definition.getId(), definition);
        }
    }

    public TitleDefinition getTitle(int id) {
        return this.titles.get(id);
    }

    public List<TitleDefinition> getAllTitlesSorted() {
        List<TitleDefinition> sorted = new ArrayList<>(this.titles.values());
        sorted.sort(Comparator
            .comparingInt(TitleDefinition::getDisplayOrder)
            .thenComparingInt(TitleDefinition::getId));
        return sorted;
    }

    public int size() {
        return this.titles.size();
    }
}
