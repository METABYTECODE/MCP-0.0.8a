package com.rivet.engine.resources;

/**
 * Базовый интерфейс для всех ресурсов в системе Rivet
 */
public interface Resource {
    
    /**
     * Получить ResourceLocation этого ресурса
     * @return адрес ресурса
     */
    ResourceLocation getLocation();
    
    /**
     * Проверить, загружен ли ресурс
     * @return true если ресурс готов к использованию
     */
    boolean isLoaded();
    
    /**
     * Загрузить ресурс
     * @throws ResourceLoadException если загрузка не удалась
     */
    void load() throws ResourceLoadException;
    
    /**
     * Выгрузить ресурс из памяти
     */
    void unload();
    
    /**
     * Получить размер ресурса в байтах
     * @return размер в байтах
     */
    long getSize();
}

