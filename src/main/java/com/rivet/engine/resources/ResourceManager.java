package com.rivet.engine.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер ресурсов для движка Rivet
 * Управляет загрузкой, кэшированием и доступом к ресурсам
 */
public class ResourceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    
    private final Map<String, ResourceLocation> resourceMap = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Resource> loadedResources = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Инициализация менеджера ресурсов
     * Загружает конфигурацию ресурсов из JSON
     */
    public void initialize() throws ResourceLoadException {
        logger.info("Инициализация ResourceManager");
        
        try {
            loadResourceConfig();
            logger.info("Конфигурация ресурсов загружена: {} записей", resourceMap.size());
        } catch (Exception e) {
            throw new ResourceLoadException("Failed to initialize ResourceManager", e);
        }
    }
    
    /**
     * Загрузить конфигурацию ресурсов из JSON
     */
    private void loadResourceConfig() throws IOException {
        try (InputStream configStream = getClass().getResourceAsStream("/resources.json")) {
            if (configStream == null) {
                logger.warn("Файл конфигурации ресурсов не найден, создаем базовую конфигурацию");
                createDefaultResourceConfig();
                return;
            }
            
            JsonNode root = objectMapper.readTree(configStream);
            
            // Загружаем текстуры
            if (root.has("textures")) {
                JsonNode textures = root.get("textures");
                textures.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    String path = entry.getValue().asText();
                    // Путь уже содержит "textures/", создаем ResourceLocation с пустым namespace
                    resourceMap.put(key, new ResourceLocation("", path));
                });
            }
            
            
            // Загружаем звуки
            if (root.has("sounds")) {
                JsonNode sounds = root.get("sounds");
                sounds.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    String path = entry.getValue().asText();
                    // Путь уже содержит "sounds/", создаем ResourceLocation с пустым namespace
                    resourceMap.put(key, new ResourceLocation("", path));
                });
            }
        }
    }
    
    /**
     * Создать базовую конфигурацию ресурсов
     */
    private void createDefaultResourceConfig() {
        
        // Базовые текстуры
        resourceMap.put("MISSING_TEXTURE", new ResourceLocation("textures", "missing.png"));
        
        logger.info("Создана базовая конфигурация ресурсов");
    }
    
    /**
     * Получить ResourceLocation по ключу
     * @param key ключ ресурса
     * @return адрес ресурса
     * @throws ResourceLoadException если ресурс не найден
     */
    public ResourceLocation getLocation(String key) throws ResourceLoadException {
        ResourceLocation location = resourceMap.get(key);
        if (location == null) {
            throw new ResourceLoadException("Resource not found: " + key);
        }
        return location;
    }
    
    /**
     * Загрузить ресурс по ключу
     * @param key ключ ресурса
     * @return загруженный ресурс
     * @throws ResourceLoadException если загрузка не удалась
     */
    public Resource loadResource(String key) throws ResourceLoadException {
        ResourceLocation location = getLocation(key);
        return loadResource(location);
    }
    
    /**
     * Загрузить ресурс по ResourceLocation
     * @param location адрес ресурса
     * @return загруженный ресурс
     * @throws ResourceLoadException если загрузка не удалась
     */
    public Resource loadResource(ResourceLocation location) throws ResourceLoadException {
        // Проверяем кэш
        Resource cached = loadedResources.get(location);
        if (cached != null && cached.isLoaded()) {
            return cached;
        }
        
        // Определяем тип ресурса и создаем соответствующий загрузчик
        Resource resource = createResource(location);
        resource.load();
        
        // Кэшируем
        loadedResources.put(location, resource);
        
        logger.debug("Ресурс загружен: {}", location);
        return resource;
    }
    
    /**
     * Создать ресурс по типу
     * @param location адрес ресурса
     * @return созданный ресурс
     * @throws ResourceLoadException если тип ресурса не поддерживается
     */
    private Resource createResource(ResourceLocation location) throws ResourceLoadException {
        String path = location.getPath();
        
        if (path.startsWith("textures/")) {
            return new TextureResource(location);
        } else if (path.startsWith("sounds/")) {
            return new SoundResource(location);
        } else {
            throw new ResourceLoadException(location, "Unsupported resource type for path: " + path);
        }
    }
    
    /**
     * Проверить, загружен ли ресурс
     * @param key ключ ресурса
     * @return true если ресурс загружен
     */
    public boolean isResourceLoaded(String key) {
        try {
            ResourceLocation location = getLocation(key);
            Resource resource = loadedResources.get(location);
            return resource != null && resource.isLoaded();
        } catch (ResourceLoadException e) {
            return false;
        }
    }
    
    /**
     * Выгрузить ресурс
     * @param key ключ ресурса
     */
    public void unloadResource(String key) {
        try {
            ResourceLocation location = getLocation(key);
            Resource resource = loadedResources.get(location);
            if (resource != null) {
                resource.unload();
                loadedResources.remove(location);
                logger.debug("Ресурс выгружен: {}", location);
            }
        } catch (ResourceLoadException e) {
            logger.warn("Не удалось выгрузить ресурс: {}", key, e);
        }
    }
    
    /**
     * Очистить все ресурсы
     */
    public void cleanup() {
        logger.info("Очистка ResourceManager");
        
        for (Resource resource : loadedResources.values()) {
            try {
                resource.unload();
            } catch (Exception e) {
                logger.warn("Ошибка при выгрузке ресурса: {}", resource.getLocation(), e);
            }
        }
        
        loadedResources.clear();
        resourceMap.clear();
    }
    
    /**
     * Получить количество загруженных ресурсов
     * @return количество ресурсов
     */
    public int getLoadedResourceCount() {
        return loadedResources.size();
    }
    
    /**
     * Получить общий размер загруженных ресурсов
     * @return размер в байтах
     */
    public long getTotalResourceSize() {
        return loadedResources.values().stream()
                .mapToLong(Resource::getSize)
                .sum();
    }
}
