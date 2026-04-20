package com.kavinshi.playertitle.bootstrap;

/**
 * Central composition root for rewrite services.
 * The first task only establishes a stable place for future wiring.
 */
public final class RewriteBootstrap {
    private static RewriteBootstrap instance;

    private RewriteBootstrap() {
    }

    public static RewriteBootstrap initialize() {
        if (instance == null) {
            instance = new RewriteBootstrap();
        }
        return instance;
    }

    public static RewriteBootstrap getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RewriteBootstrap has not been initialized");
        }
        return instance;
    }
}
