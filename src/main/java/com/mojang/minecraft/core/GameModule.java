package com.mojang.minecraft.core;

/**
 * Базовый интерфейс для всех игровых модулей
 * Обеспечивает единообразную инициализацию и управление жизненным циклом
 */
public interface GameModule {
    
    /**
     * Инициализация модуля
     * @throws Exception если инициализация не удалась
     */
    void initialize() throws Exception;
    
    /**
     * Обновление модуля (вызывается каждый тик)
     * @param deltaTime время с последнего обновления в секундах
     */
    void update(float deltaTime);
    
    /**
     * Рендеринг модуля
     * @param partialTicks частичные тики для плавной анимации
     */
    void render(float partialTicks);
    
    /**
     * Очистка ресурсов модуля
     */
    void cleanup();
    
    /**
     * Проверка, инициализирован ли модуль
     * @return true если модуль готов к работе
     */
    boolean isInitialized();
    
    /**
     * Получение имени модуля для отладки
     * @return имя модуля
     */
    String getName();
}
