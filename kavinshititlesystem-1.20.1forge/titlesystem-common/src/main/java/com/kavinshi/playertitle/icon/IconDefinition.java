package com.kavinshi.playertitle.icon;

import java.nio.file.Path;

/**
 * 图标定义，代表一个PNG图标及其元数据。
 * 包含图标ID、名称、PNG文件路径、分配的Unicode字符和渲染参数。
 */
public class IconDefinition {
    private final String id;
    private final String name;
    private final Path pngPath;
    private final char unicodeChar;
    private final int width;
    private final int height;
    private final int ascent;
    private final int descent;
    
    /**
     * 创建图标定义。
     *
     * @param id 图标ID（文件名不带扩展名）
     * @param name 图标显示名称
     * @param pngPath PNG文件路径
     * @param unicodeChar 分配的Unicode字符（私有使用区）
     * @param width 图标宽度（像素）
     * @param height 图标高度（像素）
     * @param ascent 上升高度（正数，用于垂直对齐）
     * @param descent 下降高度（负数，用于垂直对齐）
     */
    public IconDefinition(String id, String name, Path pngPath, char unicodeChar, 
                         int width, int height, int ascent, int descent) {
        this.id = id;
        this.name = name;
        this.pngPath = pngPath;
        this.unicodeChar = unicodeChar;
        this.width = width;
        this.height = height;
        this.ascent = ascent;
        this.descent = descent;
    }
    
    /**
     * 获取图标ID（文件名不带扩展名）。
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取图标显示名称。
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取PNG文件路径。
     */
    public Path getPngPath() {
        return pngPath;
    }
    
    /**
     * 获取分配的Unicode字符（私有使用区）。
     */
    public char getUnicodeChar() {
        return unicodeChar;
    }
    
    /**
     * 获取图标宽度（像素）。
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * 获取图标高度（像素）。
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * 获取上升高度（正数，用于垂直对齐）。
     */
    public int getAscent() {
        return ascent;
    }
    
    /**
     * 获取下降高度（负数，用于垂直对齐）。
     */
    public int getDescent() {
        return descent;
    }
    
    @Override
    public String toString() {
        return "IconDefinition{id='" + id + "', name='" + name + 
               "', unicodeChar=U+" + Integer.toHexString(unicodeChar).toUpperCase() + 
               ", width=" + width + ", height=" + height + "}";
    }
}