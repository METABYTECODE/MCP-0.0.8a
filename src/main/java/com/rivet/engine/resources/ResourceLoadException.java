package com.rivet.engine.resources;

/**
 * Исключение для ошибок загрузки ресурсов
 */
public class ResourceLoadException extends Exception {
    
    public ResourceLoadException(String message) {
        super(message);
    }
    
    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ResourceLoadException(ResourceLocation location, String message) {
        super("Failed to load resource " + location + ": " + message);
    }
    
    public ResourceLoadException(ResourceLocation location, String message, Throwable cause) {
        super("Failed to load resource " + location + ": " + message, cause);
    }
}

