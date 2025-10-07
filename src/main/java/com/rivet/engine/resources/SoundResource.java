package com.rivet.engine.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Ресурс звука для системы аудио
 * Пока что заглушка, будет реализована позже с OpenAL
 */
public class SoundResource implements Resource {
    
    private static final Logger logger = LoggerFactory.getLogger(SoundResource.class);
    
    private final ResourceLocation location;
    private byte[] soundData;
    private boolean loaded = false;
    private long size = 0;
    
    public SoundResource(ResourceLocation location) {
        this.location = location;
    }
    
    @Override
    public ResourceLocation getLocation() {
        return location;
    }
    
    @Override
    public boolean isLoaded() {
        return loaded && soundData != null;
    }
    
    @Override
    public void load() throws ResourceLoadException {
        if (loaded) {
            return;
        }
        
        try (InputStream inputStream = getClass().getResourceAsStream(location.getClasspathPath())) {
            if (inputStream == null) {
                throw new ResourceLoadException(location, "Sound file not found");
            }
            
            soundData = inputStream.readAllBytes();
            size = soundData.length;
            loaded = true;
            
            logger.debug("Звук загружен: {} ({} bytes)", location, size);
            
        } catch (IOException e) {
            throw new ResourceLoadException(location, "Failed to load sound file", e);
        }
    }
    
    @Override
    public void unload() {
        soundData = null;
        loaded = false;
        size = 0;
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    /**
     * Получить данные звука
     * @return массив байтов с данными звука
     */
    public byte[] getSoundData() {
        if (!isLoaded()) {
            throw new IllegalStateException("Sound not loaded: " + location);
        }
        return soundData;
    }
}
