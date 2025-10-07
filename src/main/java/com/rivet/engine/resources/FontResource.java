package com.rivet.engine.resources;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Ресурс шрифта TTF для системы рендеринга текста
 */
public class FontResource implements Resource {
    
    private static final Logger logger = LoggerFactory.getLogger(FontResource.class);
    
    private final ResourceLocation location;
    private STBTTFontinfo fontInfo;
    private ByteBuffer fontData;
    private boolean loaded = false;
    private long size = 0;
    
    public FontResource(ResourceLocation location) {
        this.location = location;
    }
    
    @Override
    public ResourceLocation getLocation() {
        return location;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded && fontInfo != null;
    }
    
    @Override
    public void load() throws ResourceLoadException {
        if (loaded) {
            return;
        }
        
        try (InputStream inputStream = getClass().getResourceAsStream(location.getClasspathPath())) {
            if (inputStream == null) {
                throw new ResourceLoadException(location, "Font file not found");
            }
            
            // Читаем данные шрифта
            byte[] fontBytes = inputStream.readAllBytes();
            size = fontBytes.length;
            
            // Создаем ByteBuffer для STB
            fontData = ByteBuffer.allocateDirect(fontBytes.length);
            fontData.put(fontBytes);
            fontData.flip();
            
            // Инициализируем STB TTF
            fontInfo = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(fontInfo, fontData)) {
                throw new ResourceLoadException(location, "Failed to initialize STB TTF font");
            }
            
            loaded = true;
            logger.debug("Шрифт загружен: {} ({} bytes)", location, size);
            
        } catch (IOException e) {
            throw new ResourceLoadException(location, "Failed to load font file", e);
        }
    }
    
    @Override
    public void unload() {
        if (fontInfo != null) {
            fontInfo.free();
            fontInfo = null;
        }
        
        if (fontData != null) {
            fontData = null;
        }
        
        loaded = false;
        size = 0;
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    /**
     * Получить STB TTF font info
     * @return font info для рендеринга
     */
    public STBTTFontinfo getFontInfo() {
        if (!isLoaded()) {
            throw new IllegalStateException("Font not loaded: " + location);
        }
        return fontInfo;
    }
    
    /**
     * Получить данные шрифта
     * @return ByteBuffer с данными шрифта
     */
    public ByteBuffer getFontData() {
        if (!isLoaded()) {
            throw new IllegalStateException("Font not loaded: " + location);
        }
        return fontData;
    }
    
    /**
     * Получить метрики шрифта
     * @return массив с метриками [ascent, descent, lineGap]
     */
    public int[] getFontMetrics() {
        if (!isLoaded()) {
            throw new IllegalStateException("Font not loaded: " + location);
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);
            
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);
            
            return new int[]{ascent.get(0), descent.get(0), lineGap.get(0)};
        }
    }
    
    /**
     * Получить ширину текста в пикселях
     * @param text текст для измерения
     * @param fontSize размер шрифта
     * @return ширина в пикселях
     */
    public float getTextWidth(String text, float fontSize) {
        if (!isLoaded()) {
            throw new IllegalStateException("Font not loaded: " + location);
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize);
            float width = 0;
            
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);
            
            for (char c : text.toCharArray()) {
                STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, c, advanceWidth, leftSideBearing);
                width += advanceWidth.get(0) * scale;
            }
            
            return width;
        }
    }
}
