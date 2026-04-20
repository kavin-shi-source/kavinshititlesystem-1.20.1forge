package com.kavinshi.playertitle.icon;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 图标定义，表示一个可嵌入称号的PNG图标。
 * 包含字体映射信息和渲染参数。
 */
public final class IconDefinition {
    private final String id;
    private final String name;
    private final Path pngPath;
    private final char unicodeChar;
    private final int width;
    private final int height;
    private final int ascent;
    private final int descent;

    public IconDefinition(
        String id,
        String name,
        Path pngPath,
        char unicodeChar,
        int width,
        int height,
        int ascent,
        int descent
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.pngPath = Objects.requireNonNull(pngPath, "pngPath cannot be null");
        this.unicodeChar = unicodeChar;
        this.width = width;
        this.height = height;
        this.ascent = ascent;
        this.descent = descent;
        
        if (unicodeChar < 0xE000 || unicodeChar > 0xF8FF) {
            throw new IllegalArgumentException("unicodeChar must be in Private Use Area (U+E000-U+F8FF)");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (ascent < 0) {
            throw new IllegalArgumentException("ascent cannot be negative");
        }
        if (descent > 0) {
            throw new IllegalArgumentException("descent must be <= 0");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Path getPngPath() {
        return pngPath;
    }

    public char getUnicodeChar() {
        return unicodeChar;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getAscent() {
        return ascent;
    }

    public int getDescent() {
        return descent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IconDefinition that = (IconDefinition) o;
        return unicodeChar == that.unicodeChar &&
               width == that.width &&
               height == that.height &&
               ascent == that.ascent &&
               descent == that.descent &&
               id.equals(that.id) &&
               name.equals(that.name) &&
               pngPath.equals(that.pngPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, pngPath, unicodeChar, width, height, ascent, descent);
    }

    @Override
    public String toString() {
        return "IconDefinition{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", pngPath=" + pngPath +
               ", unicodeChar=U+" + Integer.toHexString(unicodeChar).toUpperCase() +
               ", width=" + width +
               ", height=" + height +
               ", ascent=" + ascent +
               ", descent=" + descent +
               '}';
    }
}