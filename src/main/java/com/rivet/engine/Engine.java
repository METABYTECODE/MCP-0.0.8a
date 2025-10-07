package com.rivet.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Базовый класс движка Rivet
 * Предоставляет основную функциональность для 3D игр
 */
public class Engine {
    
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);
    private boolean initialized = false;
    
    public Engine() {
        // Инициализация движка
    }
    
    /**
     * Инициализация движка
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        logger.info("Rivet Engine: Инициализация движка");
        initialized = true;
    }
    
    /**
     * Очистка движка
     */
    public void cleanup() {
        if (initialized) {
            logger.info("Rivet Engine: Очистка движка");
            initialized = false;
        }
    }
    
    /**
     * Проверить, инициализирован ли движок
     * @return true если движок готов
     */
    public boolean isInitialized() {
        return initialized;
    }
}
