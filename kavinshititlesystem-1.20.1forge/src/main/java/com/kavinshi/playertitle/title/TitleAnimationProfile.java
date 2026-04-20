package com.kavinshi.playertitle.title;

public record TitleAnimationProfile(int cycleMillis, int stepSize) {
    public TitleAnimationProfile {
        if (cycleMillis <= 0) {
            throw new IllegalArgumentException("cycleMillis must be positive");
        }
        if (stepSize <= 0) {
            throw new IllegalArgumentException("stepSize must be positive");
        }
    }
}
