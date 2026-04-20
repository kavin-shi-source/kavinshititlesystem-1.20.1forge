package com.kavinshi.playertitle.icon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IconManagerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void constructorInitializesEmptyMaps() {
        IconManager manager = new IconManager(tempDir);
        assertTrue(manager.getAllIcons().isEmpty());
    }
    
    @Test
    void scanIconsCreatesDirectoryWhenNotExists() throws IOException {
        Path nonExistentDir = tempDir.resolve("icons");
        IconManager manager = new IconManager(nonExistentDir);
        
        manager.scanIcons();
        
        assertTrue(Files.exists(nonExistentDir));
        assertTrue(Files.isDirectory(nonExistentDir));
        assertTrue(manager.getAllIcons().isEmpty());
    }
    
    @Test
    void getAllIconsReturnsUnmodifiableView() throws IOException {
        IconManager manager = new IconManager(tempDir);
        Map<String, IconDefinition> icons = manager.getAllIcons();
        
        assertThrows(UnsupportedOperationException.class, 
            () -> icons.put("test", null));
    }
    
    @Test
    void getIconAndGetIconByCharWorkCorrectly() throws IOException {
        // 创建一个简单的PNG文件
        Path pngFile = tempDir.resolve("my_icon.png");
        createSimplePng(pngFile, 16, 16);
        
        IconManager manager = new IconManager(tempDir);
        manager.scanIcons();
        
        IconDefinition icon = manager.getIcon("my_icon");
        assertNotNull(icon);
        assertEquals("my_icon", icon.getId());
        assertEquals("my icon", icon.getName());
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
        assertEquals(16, icon.getAscent());
        assertEquals(-4, icon.getDescent());
        
        // Unicode字符应该在私有使用区
        char unicodeChar = icon.getUnicodeChar();
        assertTrue(unicodeChar >= 0xE000 && unicodeChar <= 0xF8FF);
        
        // 应该能通过Unicode字符找到同一个图标
        IconDefinition sameIcon = manager.getIconByChar(unicodeChar);
        assertEquals(icon, sameIcon);
        
        // 不存在的图标应该返回null
        assertNull(manager.getIcon("nonexistent"));
        assertNull(manager.getIconByChar('A'));
    }
    
    @Test
    void scanIconsLoadsMultiplePngFiles() throws IOException {
        // 创建多个PNG文件
        createSimplePng(tempDir.resolve("icon1.png"), 16, 16);
        createSimplePng(tempDir.resolve("icon2.png"), 32, 32);
        createSimplePng(tempDir.resolve("icon3.png"), 24, 24);
        
        IconManager manager = new IconManager(tempDir);
        manager.scanIcons();
        
        Map<String, IconDefinition> icons = manager.getAllIcons();
        assertEquals(3, icons.size());
        
        assertNotNull(icons.get("icon1"));
        assertNotNull(icons.get("icon2"));
        assertNotNull(icons.get("icon3"));
        
        // 每个图标应该有唯一的Unicode字符
        IconDefinition icon1 = icons.get("icon1");
        IconDefinition icon2 = icons.get("icon2");
        IconDefinition icon3 = icons.get("icon3");
        
        assertNotEquals(icon1.getUnicodeChar(), icon2.getUnicodeChar());
        assertNotEquals(icon1.getUnicodeChar(), icon3.getUnicodeChar());
        assertNotEquals(icon2.getUnicodeChar(), icon3.getUnicodeChar());
    }
    
    @Test
    void invalidPngFileIsSkipped() throws IOException {
        Path invalidFile = tempDir.resolve("invalid.png");
        Files.write(invalidFile, new byte[] {0, 1, 2, 3, 4, 5});
        
        // 也放一个有效的PNG文件来验证扫描正常进行
        createSimplePng(tempDir.resolve("valid.png"), 16, 16);
        
        IconManager manager = new IconManager(tempDir);
        
        // 扫描不应该抛出异常，无效文件应被跳过
        manager.scanIcons();
        
        // 验证只有有效的PNG文件被加载
        Map<String, IconDefinition> icons = manager.getAllIcons();
        assertEquals(1, icons.size());
        assertNotNull(icons.get("valid"));
        assertNull(icons.get("invalid"));
    }
    
    @Test
    void scanIconsClearsPreviousIcons() throws IOException {
        // 先扫描一个图标
        createSimplePng(tempDir.resolve("icon1.png"), 16, 16);
        IconManager manager = new IconManager(tempDir);
        manager.scanIcons();
        
        assertEquals(1, manager.getAllIcons().size());
        assertNotNull(manager.getIcon("icon1"));
        
        // 删除图标文件，再扫描
        Files.delete(tempDir.resolve("icon1.png"));
        manager.scanIcons();
        
        // 应该没有图标了
        assertTrue(manager.getAllIcons().isEmpty());
        assertNull(manager.getIcon("icon1"));
    }
    
    @Test
    void unicodeCharacterAllocationIncrements() throws IOException {
        createSimplePng(tempDir.resolve("icon1.png"), 16, 16);
        createSimplePng(tempDir.resolve("icon2.png"), 32, 32);
        createSimplePng(tempDir.resolve("icon3.png"), 24, 24);
        
        IconManager manager = new IconManager(tempDir);
        manager.scanIcons();
        
        Map<String, IconDefinition> icons = manager.getAllIcons();
        
        // 按顺序分配的Unicode字符应该递增
        char[] unicodeChars = icons.values().stream()
            .map(IconDefinition::getUnicodeChar)
            .sorted()
            .mapToInt(c -> (int) c)  // 转换为int
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString()
            .toCharArray();
        
        assertTrue(unicodeChars[0] < unicodeChars[1]);
        assertTrue(unicodeChars[1] < unicodeChars[2]);
    }
    
    @Test
    void iconNameConvertsUnderscoresToSpaces() throws IOException {
        createSimplePng(tempDir.resolve("my_special_icon.png"), 16, 16);
        createSimplePng(tempDir.resolve("another-icon.png"), 16, 16);
        
        IconManager manager = new IconManager(tempDir);
        manager.scanIcons();
        
        IconDefinition icon1 = manager.getIcon("my_special_icon");
        assertEquals("my special icon", icon1.getName());
        
        IconDefinition icon2 = manager.getIcon("another-icon");
        assertEquals("another-icon", icon2.getName()); // 连字符不变
    }
    
    private void createSimplePng(Path path, int width, int height) throws IOException {
        // 创建最小PNG文件头（仅用于测试）
        byte[] pngHeader = {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG签名
            0x00, 0x00, 0x00, 0x0D, // IHDR块长度
            0x49, 0x48, 0x44, 0x52, // "IHDR"
            (byte)(width >> 24), (byte)(width >> 16), (byte)(width >> 8), (byte)width,
            (byte)(height >> 24), (byte)(height >> 16), (byte)(height >> 8), (byte)height,
            0x08, 0x02, 0x00, 0x00, 0x00, // 位深度、颜色类型等
            0x00, 0x00, 0x00, 0x00 // CRC（简化）
        };
        Files.write(path, pngHeader);
    }
}