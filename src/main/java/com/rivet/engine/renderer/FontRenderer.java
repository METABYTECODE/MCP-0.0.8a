package com.rivet.engine.renderer;

import com.rivet.engine.resources.FontResource;
import com.rivet.engine.resources.ResourceLocation;
import com.rivet.engine.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBTruetype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Рендерер текста с использованием STB TTF
 * Поддерживает загрузку TTF шрифтов и рендеринг текста
 */
public class FontRenderer {
    
    private static final Logger logger = LoggerFactory.getLogger(FontRenderer.class);
    
    private final ResourceManager resourceManager;
    private FontResource defaultFont;
    private int vao;
    private int vbo;
    private boolean initialized = false;
    
    public FontRenderer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    /**
     * Инициализация рендерера шрифтов
     */
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        logger.info("Инициализация FontRenderer");
        
        // Загружаем шрифт по умолчанию
        try {
            defaultFont = (FontResource) resourceManager.loadResource("DEFAULT");
            logger.info("Шрифт по умолчанию загружен: {}", defaultFont.getLocation());
        } catch (Exception e) {
            logger.warn("Не удалось загрузить шрифт по умолчанию, создаем заглушку");
            // TODO: Создать заглушку шрифта
        }
        
        // Создаем VAO и VBO для рендеринга
        setupBuffers();
        
        initialized = true;
        logger.info("FontRenderer инициализирован");
    }
    
    /**
     * Настройка буферов для рендеринга
     */
    private void setupBuffers() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        
        // Настройка атрибутов вершин
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // position
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4); // texCoords
        GL30.glEnableVertexAttribArray(0);
        GL30.glEnableVertexAttribArray(1);
        
        GL30.glBindVertexArray(0);
    }
    
    /**
     * Рендеринг текста
     * @param text текст для рендеринга
     * @param x позиция X
     * @param y позиция Y
     * @param fontSize размер шрифта
     * @param color цвет текста (RGBA)
     */
    public void renderText(String text, float x, float y, float fontSize, float[] color) {
        if (!initialized || defaultFont == null) {
            return;
        }
        
        // TODO: Реализовать рендеринг текста через STB TTF
        // Пока что заглушка
        logger.debug("Рендеринг текста: '{}' в позиции ({}, {}) размером {}", text, x, y, fontSize);
    }
    
    /**
     * Рендеринг текста с шрифтом по умолчанию
     * @param text текст для рендеринга
     * @param x позиция X
     * @param y позиция Y
     * @param fontSize размер шрифта
     */
    public void renderText(String text, float x, float y, float fontSize) {
        renderText(text, x, y, fontSize, new float[]{1.0f, 1.0f, 1.0f, 1.0f});
    }
    
    /**
     * Получить ширину текста
     * @param text текст для измерения
     * @param fontSize размер шрифта
     * @return ширина в пикселях
     */
    public float getTextWidth(String text, float fontSize) {
        if (defaultFont == null) {
            return 0;
        }
        return defaultFont.getTextWidth(text, fontSize);
    }
    
    /**
     * Получить высоту шрифта
     * @param fontSize размер шрифта
     * @return высота в пикселях
     */
    public float getFontHeight(float fontSize) {
        if (defaultFont == null) {
            return fontSize;
        }
        
        int[] metrics = defaultFont.getFontMetrics();
        return (metrics[0] - metrics[1]) * STBTruetype.stbtt_ScaleForPixelHeight(defaultFont.getFontInfo(), fontSize);
    }
    
    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        if (initialized) {
            if (vao != 0) {
                GL30.glDeleteVertexArrays(vao);
            }
            if (vbo != 0) {
                GL15.glDeleteBuffers(vbo);
            }
            initialized = false;
            logger.info("FontRenderer очищен");
        }
    }
    
    /**
     * Проверить, инициализирован ли рендерер
     * @return true если готов к работе
     */
    public boolean isInitialized() {
        return initialized;
    }
}
