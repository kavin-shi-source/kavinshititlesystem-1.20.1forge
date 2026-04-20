package com.kavinshi.playertitle.config;

import com.kavinshi.playertitle.title.TitleDefinition;
import java.nio.file.Path;
import java.util.List;

public interface TitleConfigRepository {
    List<TitleDefinition> loadDefinitions(Path path);
}
