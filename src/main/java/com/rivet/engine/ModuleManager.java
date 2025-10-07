package com.rivet.engine;

import com.rivet.engine.modules.GameModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер модулей движка Rivet
 * Управляет жизненным циклом всех модулей
 */
public class ModuleManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);
    private List<GameModule> modules;
    private boolean initialized = false;
    
    public ModuleManager() {
        this.modules = new ArrayList<>();
    }
    
    /**
     * Добавить модуль в менеджер
     * @param module модуль для добавления
     */
    public void addModule(GameModule module) {
        modules.add(module);
        logger.debug("Добавлен модуль: {}", module.getName());
    }
    
    /**
     * Инициализация всех модулей
     * @throws Exception если инициализация не удалась
     */
    public void initializeAll() throws Exception {
        if (initialized) {
            return;
        }
        
        logger.info("Инициализация модулей движка...");
        
        for (GameModule module : modules) {
            try {
                module.initialize();
                logger.info("✓ {} инициализирован", module.getName());
            } catch (Exception e) {
                logger.error("✗ Ошибка инициализации {}: {}", module.getName(), e.getMessage(), e);
                throw e;
            }
        }
        
        initialized = true;
        logger.info("Все модули движка успешно инициализированы");
    }
    
    /**
     * Обновление всех модулей
     * @param deltaTime время с последнего обновления
     */
    public void updateAll(float deltaTime) {
        for (GameModule module : modules) {
            if (module.isInitialized()) {
                module.update(deltaTime);
            }
        }
    }
    
    /**
     * Рендеринг всех модулей
     * @param partialTicks частичные тики для плавной анимации
     */
    public void renderAll(float partialTicks) {
        for (GameModule module : modules) {
            if (module.isInitialized()) {
                module.render(partialTicks);
            }
        }
    }
    
    /**
     * Очистка всех модулей
     */
    public void cleanupAll() {
        logger.info("Очистка модулей движка...");
        
        // Очищаем в обратном порядке
        for (int i = modules.size() - 1; i >= 0; i--) {
            GameModule module = modules.get(i);
            try {
                module.cleanup();
                logger.info("✓ {} очищен", module.getName());
            } catch (Exception e) {
                logger.error("✗ Ошибка очистки {}: {}", module.getName(), e.getMessage(), e);
            }
        }
        
        modules.clear();
        initialized = false;
        logger.info("Все модули движка очищены");
    }
    
    /**
     * Получить модуль по типу
     * @param moduleClass класс модуля
     * @return модуль или null если не найден
     */
    @SuppressWarnings("unchecked")
    public <T extends GameModule> T getModule(Class<T> moduleClass) {
        for (GameModule module : modules) {
            if (moduleClass.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }
    
    /**
     * Проверить, инициализированы ли все модули
     * @return true если все модули готовы
     */
    public boolean areAllInitialized() {
        for (GameModule module : modules) {
            if (!module.isInitialized()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Получить количество модулей
     * @return количество модулей
     */
    public int getModuleCount() {
        return modules.size();
    }
}
