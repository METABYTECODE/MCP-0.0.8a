package com.mojang.minecraft.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер модулей
 * Управляет жизненным циклом всех игровых модулей
 */
public class ModuleManager {
    
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
        System.out.println("Добавлен модуль: " + module.getName());
    }
    
    /**
     * Инициализация всех модулей
     * @throws Exception если инициализация не удалась
     */
    public void initializeAll() throws Exception {
        if (initialized) {
            return;
        }
        
        System.out.println("Инициализация модулей...");
        
        for (GameModule module : modules) {
            try {
                module.initialize();
                System.out.println("✓ " + module.getName() + " инициализирован");
            } catch (Exception e) {
                System.err.println("✗ Ошибка инициализации " + module.getName() + ": " + e.getMessage());
                throw e;
            }
        }
        
        initialized = true;
        System.out.println("Все модули успешно инициализированы");
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
        System.out.println("Очистка модулей...");
        
        // Очищаем в обратном порядке
        for (int i = modules.size() - 1; i >= 0; i--) {
            GameModule module = modules.get(i);
            try {
                module.cleanup();
                System.out.println("✓ " + module.getName() + " очищен");
            } catch (Exception e) {
                System.err.println("✗ Ошибка очистки " + module.getName() + ": " + e.getMessage());
            }
        }
        
        modules.clear();
        initialized = false;
        System.out.println("Все модули очищены");
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
    
    /**
     * Получить список всех модулей
     * @return список модулей
     */
    public List<GameModule> getAllModules() {
        return new ArrayList<>(modules);
    }
}
