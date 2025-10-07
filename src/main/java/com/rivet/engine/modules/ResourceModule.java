package com.rivet.engine.modules;

import com.rivet.engine.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Модуль управления ресурсами для движка Rivet
 */
public class ResourceModule implements GameModule {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceModule.class);
    private boolean initialized = false;
    private ResourceManager resourceManager;
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        logger.info("Инициализация модуля ресурсов");
        
        resourceManager = new ResourceManager();
        resourceManager.initialize();
        
        initialized = true;
        logger.info("Модуль ресурсов инициализирован");
    }
    
    @Override
    public void update(float deltaTime) {
        // Ресурсы не требуют обновления
    }
    
    @Override
    public void render(float partialTicks) {
        // Ресурсы не требуют рендеринга
    }
    
    @Override
    public void cleanup() {
        if (initialized && resourceManager != null) {
            resourceManager.cleanup();
            resourceManager = null;
            initialized = false;
            logger.info("Модуль ресурсов очищен");
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String getName() {
        return "ResourceModule";
    }
    
    /**
     * Получить менеджер ресурсов
     * @return ResourceManager
     */
    public ResourceManager getResourceManager() {
        if (!initialized) {
            throw new IllegalStateException("ResourceModule not initialized");
        }
        return resourceManager;
    }
}
