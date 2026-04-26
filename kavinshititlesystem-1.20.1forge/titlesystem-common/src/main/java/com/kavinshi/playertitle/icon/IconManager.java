package com.kavinshi.playertitle.icon;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 图标管理器，负责扫描配置目录中的PNG图标并管理图标定义。
 * 支持动态加载和Unicode字符分配。
 */
public class IconManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IconManager.class);
    private final Path configDirectory;
    private volatile IconSnapshot iconSnapshot = IconSnapshot.EMPTY;
    private char nextUnicodeChar = 0xE000;

    public IconManager(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    /**
     * 扫描配置目录中的PNG文件并创建图标定义。
     * 每个PNG文件分配一个唯一的私有使用区Unicode字符。
     */
    public void scanIcons() throws IOException {
        if (!Files.exists(configDirectory)) {
            Files.createDirectories(configDirectory);
            // 目录刚创建，没有图标文件
            clearIconMaps();
            return;
        }

        Map<String, IconDefinition> newIcons = new HashMap<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDirectory)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString().toLowerCase();
                if (!fileName.endsWith(".png")) {
                    continue;
                }
                
                try {
                    String iconId = fileName.substring(0, fileName.length() - 4);
                    String iconName = iconId.replace('_', ' ');
                    
                    char unicodeChar = allocateUnicodeChar();
                    
                    ImageDimensions dimensions = readImageDimensions(file);
                    
                    IconDefinition icon = new IconDefinition(
                        iconId,
                        iconName,
                        file,
                        unicodeChar,
                        dimensions.width(),
                        dimensions.height(),
                        dimensions.height(),
                        -dimensions.height() / 4
                    );
                    
                    newIcons.put(iconId, icon);
                } catch (IOException e) {
                    LOGGER.warn("Failed to parse PNG file: {}, error: {}", file, e.getMessage());
                }
            }
        }
        
        Map<String, IconDefinition> newById = Map.copyOf(newIcons);
        Map<Character, IconDefinition> newByChar = new HashMap<>();
        for (IconDefinition icon : newIcons.values()) {
            newByChar.put(icon.getUnicodeChar(), icon);
        }
        iconSnapshot = new IconSnapshot(newById, Map.copyOf(newByChar));
    }

    /**
     * 清空图标映射。
     */
    private void clearIconMaps() {
        iconSnapshot = IconSnapshot.EMPTY;
    }

    public Map<String, IconDefinition> getAllIcons() {
        return Collections.unmodifiableMap(iconSnapshot.iconsById);
    }

    public int getIconCount() {
        return iconSnapshot.iconsById.size();
    }

    public IconDefinition getIcon(String id) {
        return iconSnapshot.iconsById.get(id);
    }

    public IconDefinition getIconByChar(char unicodeChar) {
        return iconSnapshot.iconsByChar.get(unicodeChar);
    }

    /**
     * 分配一个唯一的私有使用区Unicode字符。
     */
    private synchronized char allocateUnicodeChar() {
        if (nextUnicodeChar > 0xF8FF) {
            throw new IllegalStateException("Exhausted Private Use Area Unicode characters");
        }
        return nextUnicodeChar++;
    }

    /**
     * 读取PNG图像尺寸。
     */
    private ImageDimensions readImageDimensions(Path pngFile) throws IOException {
        try (var input = Files.newInputStream(pngFile)) {
            // 简单PNG头部解析
            byte[] header = new byte[24];
            int bytesRead = input.read(header);
            if (bytesRead < 24) {
                throw new IOException("Invalid PNG file: too short");
            }
            
            // PNG签名
            if (header[0] != (byte)0x89 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G') {
                throw new IOException("Not a valid PNG file");
            }
            
            // 读取IHDR块中的宽度和高度
            int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16) |
                       ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
            int height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16) |
                        ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
            
            // 验证尺寸：必须为16x16或32x32
            validateImageDimensions(width, height);
            
            return new ImageDimensions(width, height);
        }
    }
    
    /**
     * 验证图像尺寸是否符合要求（16x16或32x32）。
     */
    private void validateImageDimensions(int width, int height) throws IOException {
        boolean valid = (width == 16 && height == 16) || (width == 32 && height == 32);
        if (!valid) {
            throw new IOException(
                String.format("PNG image dimensions must be 16x16 or 32x32, got %dx%d", width, height)
            );
        }
    }

    /**
     * 图像尺寸记录。
     */
    private record ImageDimensions(int width, int height) {}

    private static final class IconSnapshot {
        static final IconSnapshot EMPTY = new IconSnapshot(Map.of(), Map.of());
        final Map<String, IconDefinition> iconsById;
        final Map<Character, IconDefinition> iconsByChar;

        IconSnapshot(Map<String, IconDefinition> iconsById, Map<Character, IconDefinition> iconsByChar) {
            this.iconsById = iconsById;
            this.iconsByChar = iconsByChar;
        }
    }
}