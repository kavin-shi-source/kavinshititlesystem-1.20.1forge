package com.kavinshi.playertitle.title;

import com.kavinshi.playertitle.icon.IconService;

/**
 * 称号图标解析器，负责将称号定义中的图标ID解析为实际可用的图标字符。
 * 作为称号系统与图标系统之间的桥梁。
 */
public final class TitleIconResolver {
    private final IconService iconService;
    
    public TitleIconResolver(IconService iconService) {
        this.iconService = iconService;
    }
    
    /**
     * 解析称号定义的图标字符。
     * 如果称号没有图标或图标不可用，返回默认图标字符。
     *
     * @param definition 称号定义
     * @return 图标字符，如果无图标则返回默认图标字符
     */
    public char resolveIconChar(TitleDefinition definition) {
        if (definition == null) {
            return iconService.getDefaultIconChar();
        }
        
        String iconId = definition.getIcon();
        char iconChar = iconService.getIconChar(iconId);
        
        if (iconChar == '\u0000') {
            // 无有效图标，返回默认图标
            return iconService.getDefaultIconChar();
        }
        
        return iconChar;
    }
    
    /**
     * 检查称号是否有有效的图标。
     *
     * @param definition 称号定义
     * @return 如果有有效图标返回true
     */
    public boolean hasValidIcon(TitleDefinition definition) {
        if (definition == null) {
            return false;
        }
        
        String iconId = definition.getIcon();
        return iconService.hasIcon(iconId);
    }
    
    /**
     * 获取称号的图标显示文本（包含图标字符和称号名称）。
     * 格式："图标 称号名称"
     *
     * @param definition 称号定义
     * @return 包含图标的称号显示文本
     */
    public String getTitleDisplayWithIcon(TitleDefinition definition) {
        if (definition == null) {
            return "";
        }
        
        char iconChar = resolveIconChar(definition);
        if (iconChar == '\u0000') {
            return definition.getName();
        }
        
        return iconChar + " " + definition.getName();
    }
    
    /**
     * 获取仅图标的显示文本。
     *
     * @param definition 称号定义
     * @return 图标字符，如果没有图标则返回空字符串
     */
    public String getIconDisplay(TitleDefinition definition) {
        if (definition == null) {
            return "";
        }
        
        char iconChar = resolveIconChar(definition);
        if (iconChar == '\u0000') {
            return "";
        }
        
        return String.valueOf(iconChar);
    }
}