package com.kavinshi.playertitle.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kavinshi.playertitle.icon.IconDefinition;
import com.kavinshi.playertitle.icon.IconManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.nio.file.StandardCopyOption;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * 客户端图标管理器，扩展IconManager以支持字体资源包生成。
 * 负责将PNG图标转换为Minecraft字体定义和纹理图集。
 */
public class ClientIconManager extends IconManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    
    public ClientIconManager(Path configDir) {
        super(configDir);
        this.configDir = configDir;
    }
    
    public Path getConfigDir() {
        return configDir;
    }
    
    /**
     * 生成字体资源包，包含所有图标的字体定义和纹理。
     * 将资源包写入指定目录。
     *
     * @param outputDir 资源包输出目录（例如：config/playertitle/resourcepack）
     * @throws IOException 如果生成失败
     */
    public void generateFontResourcePack(Path outputDir) throws IOException {
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        
        // 创建资源包结构
        Path assetsDir = outputDir.resolve("assets").resolve("playertitle");
        Path texturesDir = assetsDir.resolve("textures");
        Path fontsDir = assetsDir.resolve("font");
        
        Files.createDirectories(texturesDir);
        Files.createDirectories(fontsDir);
        
        // 生成纹理图集
        Path atlasPath = generateTextureAtlas(texturesDir);
        
        // 生成字体定义
        generateFontDefinition(fontsDir);
        
        // 生成资源包meta文件
        generatePackMeta(outputDir);
    }
    
    /**
     * 生成纹理图集，将所有PNG图标合并到一个大图中（简单实现：分别存储）。
     * 为简化，我们为每个图标生成单独的纹理文件。
     *
     * @param texturesDir 纹理目录
     * @return 图集文件路径（实际上返回第一个图标的路径）
     * @throws IOException 如果生成失败
     */
    private Path generateTextureAtlas(Path texturesDir) throws IOException {
        Map<String, IconDefinition> icons = getAllIcons();
        Path firstTexture = null;
        
        for (IconDefinition icon : icons.values()) {
            if (icon.getPngPath() == null || !Files.exists(icon.getPngPath())) {
                continue;
            }
            
            // 复制PNG文件到纹理目录
            String textureName = icon.getId() + ".png";
            Path texturePath = texturesDir.resolve(textureName);
            Files.copy(icon.getPngPath(), texturePath, StandardCopyOption.REPLACE_EXISTING);
            
            if (firstTexture == null) {
                firstTexture = texturePath;
            }
        }
        
        // 如果没有图标，创建一个占位符纹理
        if (firstTexture == null) {
            Path placeholderPath = texturesDir.resolve("placeholder.png");
            createPlaceholderTexture(placeholderPath, 16, 16);
            firstTexture = placeholderPath;
        }
        
        return firstTexture;
    }
    
    /**
     * 创建占位符纹理（16x16透明像素）。
     */
    private void createPlaceholderTexture(Path path, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // 完全透明
        ImageIO.write(image, "PNG", path.toFile());
    }
    
    /**
     * 生成字体定义JSON文件。
     * 为每个图标创建一个字体提供程序（bitmap类型）。
     *
     * @param fontsDir 字体目录
     * @param atlasPath 图集文件路径
     * @throws IOException 如果生成失败
     */
    private void generateFontDefinition(Path fontsDir) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray providers = new JsonArray();
        
        Map<String, IconDefinition> icons = getAllIcons();
        
        for (IconDefinition icon : icons.values()) {
            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", "playertitle:" + icon.getId() + ".png");
            provider.addProperty("ascent", icon.getAscent());
            provider.addProperty("height", icon.getHeight());
            
            // Unicode字符
            JsonArray chars = new JsonArray();
            chars.add(String.valueOf(icon.getUnicodeChar()));
            provider.add("chars", chars);
            
            providers.add(provider);
        }
        
        // 如果没有图标，添加一个占位符提供程序
        if (icons.isEmpty()) {
            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", "playertitle:placeholder.png");
            provider.addProperty("ascent", 16);
            provider.addProperty("height", 16);
            
            JsonArray chars = new JsonArray();
            chars.add("\uE000"); // 私有使用区字符
            provider.add("chars", chars);
            
            providers.add(provider);
        }
        
        root.add("providers", providers);
        
        // 写入字体定义文件
        Path fontFile = fontsDir.resolve("playertitle.json");
        Files.writeString(fontFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }
    
    /**
     * 生成资源包meta文件（pack.mcmeta）。
     *
     * @param outputDir 资源包输出目录
     * @throws IOException 如果生成失败
     */
    private void generatePackMeta(Path outputDir) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 15); // Minecraft 1.20.1使用pack_format 15
        pack.addProperty("description", "PlayerTitle图标字体资源包");
        root.add("pack", pack);
        
        Path metaFile = outputDir.resolve("pack.mcmeta");
        Files.writeString(metaFile, GSON.toJson(root), StandardCharsets.UTF_8);
    }
    
    /**
     * 获取字体资源包目录（相对于游戏目录）。
     * 默认位置：config/playertitle/resourcepack
     *
     * @return 资源包目录
     */
    public Path getDefaultResourcePackDir() {
        return getConfigDir().getParent().resolve("resourcepack");
    }
}