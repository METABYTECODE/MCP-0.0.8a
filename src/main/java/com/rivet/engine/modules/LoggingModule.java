package com.rivet.engine.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Модуль логирования для движка Rivet
 * Предоставляет централизованный доступ к логгерам
 */
public class LoggingModule implements GameModule {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingModule.class);
    private boolean initialized = false;
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        logger.info("Инициализация модуля логирования");
        logger.info("SLF4J + Logback настроены для Rivet Engine");
        initialized = true;
    }
    
    @Override
    public void update(float deltaTime) {
        // Логирование не требует обновления
    }
    
    @Override
    public void render(float partialTicks) {
        // Логирование не требует рендеринга
    }
    
    @Override
    public void cleanup() {
        if (initialized) {
            logger.info("Очистка модуля логирования");
            initialized = false;
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String getName() {
        return "LoggingModule";
    }
    
    /**
     * Получить логгер для класса
     * @param clazz класс для которого нужен логгер
     * @return логгер
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Получить логгер по имени
     * @param name имя логгера
     * @return логгер
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * Получить корневой логгер
     * @return корневой логгер
     */
    public static Logger getRootLogger() {
        return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
