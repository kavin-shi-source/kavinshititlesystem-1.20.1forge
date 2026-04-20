package com.kavinshi.playertitle.icon;

/**
 * 图标服务，提供称号图标ID到实际图标定义的映射。
 * 作为图标系统与称号系统之间的桥梁。
 */
public final class IconService {
    private final IconManager iconManager;
    
    public IconService(IconManager iconManager) {
        this.iconManager = iconManager;
    }
    
    /**
     * 根据图标ID获取对应的Unicode字符。
     * 如果图标不存在，返回回退字符或默认字符。
     *
     * @param iconId 图标ID（来自TitleDefinition的icon字段）
     * @return Unicode字符，如果找不到图标则返回'\u0000'
     */
    public char getIconChar(String iconId) {
        if (iconId == null || iconId.isBlank()) {
            return '\u0000'; // 空字符表示无图标
        }
        
        IconDefinition icon = iconManager.getIcon(iconId);
        if (icon != null) {
            return icon.getUnicodeChar();
        }
        
        // 如果找不到图标，尝试作为直接Unicode字符解析
        // 这允许配置中使用直接的Unicode字符（如"✦"）
        if (iconId.length() == 1) {
            return iconId.charAt(0);
        }
        
        return '\u0000';
    }
    
    /**
     * 根据图标ID获取图标定义。
     *
     * @param iconId 图标ID
     * @return 图标定义，如果找不到则返回null
     */
    public IconDefinition getIconDefinition(String iconId) {
        if (iconId == null || iconId.isBlank()) {
            return null;
        }
        
        IconDefinition icon = iconManager.getIcon(iconId);
        if (icon != null) {
            return icon;
        }
        
        // 如果不是PNG图标ID，可能是直接的Unicode字符
        // 这种情况下，我们创建一个虚拟的图标定义
        if (iconId.length() == 1) {
            char ch = iconId.charAt(0);
            return new IconDefinition(
                "unicode_" + Integer.toHexString(ch),
                "Unicode: " + iconId,
                null, // 无PNG文件
                ch,
                16, 16, 16, -4
            );
        }
        
        return null;
    }
    
    /**
     * 检查图标是否存在（无论是PNG图标还是Unicode字符）。
     *
     * @param iconId 图标ID
     * @return 如果图标存在返回true
     */
    public boolean hasIcon(String iconId) {
        if (iconId == null || iconId.isBlank()) {
            return false;
        }
        
        if (iconManager.getIcon(iconId) != null) {
            return true;
        }
        
        // 检查是否为单字符Unicode
        if (iconId.length() == 1) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取默认图标字符（用于回退）。
     *
     * @return 默认图标字符
     */
    public char getDefaultIconChar() {
        // 使用一个常见的Unicode符号作为默认图标
        return '✦';
    }
}