package com.kavinshi.playertitle.service;

import java.util.List;

public record ProgressUpdateResult(List<Integer> unlockedTitleIds) {
    public static ProgressUpdateResult empty() {
        return new ProgressUpdateResult(List.of());
    }
}
