package com.rivet.engine.resources;

/**
 * Адрес ресурса в системе ресурсов Rivet
 * Аналогично ResourceLocation из Minecraft
 */
public class ResourceLocation {
    
    private final String namespace;
    private final String path;
    
    /**
     * Создать ResourceLocation
     * @param namespace пространство имен (например, "textures", "fonts", "sounds")
     * @param path путь к ресурсу (например, "blocks/stone.png")
     */
    public ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }
    
    /**
     * Создать ResourceLocation с пространством имен по умолчанию "rivet"
     * @param path путь к ресурсу
     */
    public ResourceLocation(String path) {
        this("rivet", path);
    }
    
    /**
     * Получить полный путь к ресурсу
     * @return полный путь (например, "textures/blocks/stone.png")
     */
    public String getFullPath() {
        if (namespace.isEmpty()) {
            return path;
        }
        return namespace + "/" + path;
    }
    
    /**
     * Получить путь для загрузки из classpath
     * @return путь с префиксом "/" (например, "/textures/blocks/stone.png")
     */
    public String getClasspathPath() {
        return "/" + getFullPath();
    }
    
    /**
     * Получить пространство имен
     * @return пространство имен
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Получить путь к ресурсу
     * @return путь к ресурсу
     */
    public String getPath() {
        return path;
    }
    
    @Override
    public String toString() {
        return getFullPath();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResourceLocation that = (ResourceLocation) obj;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }
    
    @Override
    public int hashCode() {
        return namespace.hashCode() * 31 + path.hashCode();
    }
}
