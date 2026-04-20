package com.kavinshi.playertitle.client;

import com.kavinshi.playertitle.icon.IconDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试客户端图标管理器的字体资源包生成功能。
 */
class ClientIconManagerTest {
    @TempDir
    Path tempDir;
    
    private ClientIconManager clientIconManager;
    private Path iconsDir;
    
    @BeforeEach
    void setUp() throws IOException {
        iconsDir = tempDir.resolve("icons");
        Files.createDirectories(iconsDir);
        clientIconManager = new ClientIconManager(iconsDir);
    }
    
    /**
     * 测试在没有PNG图标时扫描图标。
     */
    @Test
    void scanIcons_withNoPngFiles() throws IOException {
        clientIconManager.scanIcons();
        assertEquals(0, clientIconManager.getIconCount());
    }
    
    /**
     * 测试扫描有效的PNG图标文件（16x16）。
     */
    @Test
    void scanIcons_withValidPng() throws IOException {
        // 创建一个16x16的PNG文件
        Path pngFile = iconsDir.resolve("crown.png");
        createTestPng(pngFile, 16, 16);
        
        clientIconManager.scanIcons();
        
        assertEquals(1, clientIconManager.getIconCount());
        IconDefinition icon = clientIconManager.getIcon("crown");
        assertNotNull(icon);
        assertEquals("crown", icon.getId());
        assertEquals("crown", icon.getName());
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
        assertTrue(icon.getUnicodeChar() >= 0xE000 && icon.getUnicodeChar() <= 0xF8FF);
    }
    
    /**
     * 测试扫描多个PNG图标文件。
     */
    @Test
    void scanIcons_withMultiplePngs() throws IOException {
        createTestPng(iconsDir.resolve("crown.png"), 16, 16);
        createTestPng(iconsDir.resolve("star.png"), 32, 32);
        
        clientIconManager.scanIcons();
        
        assertEquals(2, clientIconManager.getIconCount());
        assertNotNull(clientIconManager.getIcon("crown"));
        assertNotNull(clientIconManager.getIcon("star"));
        
        // 确保分配的Unicode字符唯一
        IconDefinition crown = clientIconManager.getIcon("crown");
        IconDefinition star = clientIconManager.getIcon("star");
        assertNotEquals(crown.getUnicodeChar(), star.getUnicodeChar());
    }
    
    /**
     * 测试生成字体资源包（无图标时生成占位符）。
     */
    @Test
    void generateFontResourcePack_withNoIcons() throws IOException {
        clientIconManager.scanIcons();
        Path outputDir = tempDir.resolve("resourcepack");
        
        clientIconManager.generateFontResourcePack(outputDir);
        
        // 检查资源包文件是否存在
        assertTrue(Files.exists(outputDir.resolve("pack.mcmeta")));
        assertTrue(Files.exists(outputDir.resolve("assets").resolve("playertitle").resolve("font").resolve("playertitle.json")));
        assertTrue(Files.exists(outputDir.resolve("assets").resolve("playertitle").resolve("textures").resolve("placeholder.png")));
    }
    
    /**
     * 测试生成字体资源包（有图标时）。
     */
    @Test
    void generateFontResourcePack_withIcons() throws IOException {
        createTestPng(iconsDir.resolve("crown.png"), 16, 16);
        clientIconManager.scanIcons();
        Path outputDir = tempDir.resolve("resourcepack");
        
        clientIconManager.generateFontResourcePack(outputDir);
        
        // 检查资源包文件
        assertTrue(Files.exists(outputDir.resolve("pack.mcmeta")));
        assertTrue(Files.exists(outputDir.resolve("assets").resolve("playertitle").resolve("font").resolve("playertitle.json")));
        assertTrue(Files.exists(outputDir.resolve("assets").resolve("playertitle").resolve("textures").resolve("crown.png")));
        
        // 检查字体定义JSON是否包含图标提供程序
        Path fontJson = outputDir.resolve("assets").resolve("playertitle").resolve("font").resolve("playertitle.json");
        String jsonContent = Files.readString(fontJson);
        assertTrue(jsonContent.contains("\"type\": \"bitmap\""));
        assertTrue(jsonContent.contains("\"file\": \"playertitle:crown.png\""));
    }
    
    /**
     * 测试获取所有图标。
     */
    @Test
    void getAllIcons() throws IOException {
        createTestPng(iconsDir.resolve("crown.png"), 16, 16);
        createTestPng(iconsDir.resolve("star.png"), 32, 32);
        clientIconManager.scanIcons();
        
        Map<String, IconDefinition> icons = clientIconManager.getAllIcons();
        assertEquals(2, icons.size());
        assertTrue(icons.containsKey("crown"));
        assertTrue(icons.containsKey("star"));
    }
    
    /**
     * 创建测试PNG文件。
     */
    private void createTestPng(Path path, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // 填充一些颜色
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = (x * 255 / width) << 16 | (y * 255 / height) << 8 | 128;
                image.setRGB(x, y, rgb);
            }
        }
        ImageIO.write(image, "PNG", path.toFile());
    }
}