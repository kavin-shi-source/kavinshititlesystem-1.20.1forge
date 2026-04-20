package com.kavinshi.playertitle.title;

public final class CustomTitleData {
    public static final int PERMISSION_NONE = 0;
    public static final int PERMISSION_SOLID = 1;
    public static final int PERMISSION_GRADIENT = 2;
    public static final int PERMISSION_RAINBOW = 3;

    private String text = "";
    private int permission = PERMISSION_NONE;
    private int color1 = 0xFFFFFF;
    private int color2 = 0xFFFFFF;
    private boolean usingCustomTitle = false;
    private long lastModifiedTime = 0;

    public CustomTitleData() {}

    public CustomTitleData(String text, int permission, int color1, int color2, boolean usingCustomTitle, long lastModifiedTime) {
        this.text = text != null ? text : "";
        this.permission = permission;
        this.color1 = color1;
        this.color2 = color2;
        this.usingCustomTitle = usingCustomTitle;
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text != null ? text : ""; }

    public int getPermission() { return permission; }
    public void setPermission(int permission) { this.permission = Math.max(0, Math.min(3, permission)); }

    public int getColor1() { return color1; }
    public void setColor1(int color1) { this.color1 = color1 & 0xFFFFFF; }

    public int getColor2() { return color2; }
    public void setColor2(int color2) { this.color2 = color2 & 0xFFFFFF; }

    public boolean isUsingCustomTitle() { return usingCustomTitle; }
    public void setUsingCustomTitle(boolean usingCustomTitle) { this.usingCustomTitle = usingCustomTitle; }

    public long getLastModifiedTime() { return lastModifiedTime; }
    public void setLastModifiedTime(long lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }

    public boolean hasPermission() { return permission > PERMISSION_NONE; }

    public String getPermissionName() {
        return switch (permission) {
            case PERMISSION_SOLID -> "Solid";
            case PERMISSION_GRADIENT -> "Gradient";
            case PERMISSION_RAINBOW -> "Rainbow";
            default -> "None";
        };
    }

    public ChromaType getEffectiveChromaType() {
        if (!usingCustomTitle || permission == PERMISSION_NONE) return ChromaType.NONE;
        return switch (permission) {
            case PERMISSION_SOLID -> ChromaType.NONE;
            case PERMISSION_GRADIENT -> ChromaType.CUSTOM_GRADIENT;
            case PERMISSION_RAINBOW -> ChromaType.RAINBOW;
            default -> ChromaType.NONE;
        };
    }

    public int getEffectiveColor() {
        return usingCustomTitle ? color1 : 0xFFFFFF;
    }

    public boolean canModify(long cooldownMs) {
        if (cooldownMs <= 0) return true;
        return System.currentTimeMillis() - lastModifiedTime >= cooldownMs;
    }

    public long getRemainingCooldown(long cooldownMs) {
        if (cooldownMs <= 0) return 0;
        long elapsed = System.currentTimeMillis() - lastModifiedTime;
        return Math.max(0, cooldownMs - elapsed);
    }
}
