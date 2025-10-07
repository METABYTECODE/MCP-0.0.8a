package com.rivet.engine.resources;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Ресурс текстуры для системы рендеринга
 */
public class TextureResource implements Resource {
    
    private static final Logger logger = LoggerFactory.getLogger(TextureResource.class);
    
    private final ResourceLocation location;
    private int textureId = 0;
    private int width = 0;
    private int height = 0;
    private int channels = 0;
    private boolean loaded = false;
    private long size = 0;
    
    public TextureResource(ResourceLocation location) {
        this.location = location;
    }
    
    @Override
    public ResourceLocation getLocation() {
        return location;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded && textureId != 0;
    }
    
    @Override
    public void load() throws ResourceLoadException {
        if (loaded) {
            return;
        }
        
        try (InputStream inputStream = getClass().getResourceAsStream(location.getClasspathPath())) {
            if (inputStream == null) {
                throw new ResourceLoadException(location, "Texture file not found");
            }
            
            // Читаем данные изображения
            byte[] imageBytes = inputStream.readAllBytes();
            size = imageBytes.length;
            
            // Создаем ByteBuffer для STB
            ByteBuffer imageBuffer = ByteBuffer.allocateDirect(imageBytes.length);
            imageBuffer.put(imageBytes);
            imageBuffer.flip();
            
            // Загружаем изображение через STB
            IntBuffer widthBuffer = IntBuffer.allocate(1);
            IntBuffer heightBuffer = IntBuffer.allocate(1);
            IntBuffer channelsBuffer = IntBuffer.allocate(1);
            
            ByteBuffer imageData = STBImage.stbi_load_from_memory(
                imageBuffer, 
                widthBuffer, 
                heightBuffer, 
                channelsBuffer, 
                0
            );
            
            if (imageData == null) {
                throw new ResourceLoadException(location, "Failed to load image: " + STBImage.stbi_failure_reason());
            }
            
            this.width = widthBuffer.get(0);
            this.height = heightBuffer.get(0);
            this.channels = channelsBuffer.get(0);
            
            // Создаем OpenGL текстуру
            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            
            // Определяем формат
            int format = channels == 4 ? GL11.GL_RGBA : 
                        channels == 3 ? GL11.GL_RGB : 
                        channels == 1 ? GL11.GL_RED : GL11.GL_RGBA;
            
            // Загружаем данные в OpenGL
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, width, height, 0, format, GL11.GL_UNSIGNED_BYTE, imageData);
            
            // Настройки фильтрации
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            
            // Генерируем мипмапы
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            
            // Отвязываем текстуру
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            
            // Освобождаем память STB
            STBImage.stbi_image_free(imageData);
            
            loaded = true;
            logger.debug("Текстура загружена: {} ({}x{}, {} channels, {} bytes)", 
                location, width, height, channels, size);
            
        } catch (IOException e) {
            throw new ResourceLoadException(location, "Failed to load texture file", e);
        }
    }
    
    @Override
    public void unload() {
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
        
        loaded = false;
        size = 0;
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    /**
     * Получить ID текстуры OpenGL
     * @return ID текстуры
     */
    public int getTextureId() {
        if (!isLoaded()) {
            throw new IllegalStateException("Texture not loaded: " + location);
        }
        return textureId;
    }
    
    /**
     * Получить ширину текстуры
     * @return ширина в пикселях
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Получить высоту текстуры
     * @return высота в пикселях
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Получить количество каналов
     * @return количество каналов (1=R, 3=RGB, 4=RGBA)
     */
    public int getChannels() {
        return channels;
    }
    
    /**
     * Привязать текстуру для рендеринга
     */
    public void bind() {
        if (!isLoaded()) {
            throw new IllegalStateException("Texture not loaded: " + location);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Отвязать текстуру
     */
    public void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
