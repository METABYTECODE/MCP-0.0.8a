package com.rivet.engine.renderer;

import com.rivet.engine.resources.Resource;
import com.rivet.engine.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class SimpleTextureResource implements Resource {
    private final int textureId;
    private final int width;
    private final int height;
    private final ResourceLocation location;
    
    public SimpleTextureResource(int textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.location = new ResourceLocation("simple_texture");
    }
    
    @Override
    public ResourceLocation getLocation() {
        return location;
    }
    
    @Override
    public boolean isLoaded() {
        return true;
    }
    
    @Override
    public void load() {
        // Уже загружена
    }
    
    @Override
    public void unload() {
        GL11.glDeleteTextures(textureId);
    }
    
    @Override
    public long getSize() {
        return width * height * 4; // RGBA
    }
    
    public int getTextureId() {
        return textureId;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }
    
    public void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
