package com.mojang.minecraft.core;

import com.mojang.minecraft.renderer.Textures;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelRenderer;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.Player;
import com.mojang.minecraft.gui.Font;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Модуль инициализации игры
 * Отвечает за настройку GLFW, OpenGL, создание игровых объектов
 */
public class InitializationModule implements GameModule {
    
    private boolean initialized = false;
    private long window;
    private int width;
    private int height;
    private boolean fullscreen;
    
    // Игровые объекты
    private Level level;
    private LevelRenderer levelRenderer;
    private Player player;
    private ParticleEngine particleEngine;
    private Textures textures;
    private Font font;
    private List<Object> entities;
    
    // OpenGL настройки
    private FloatBuffer fogColor0;
    private FloatBuffer fogColor1;
    
    public InitializationModule(int width, int height, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        this.entities = new ArrayList<>();
    }
    
    @Override
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }
        
        // 1. Инициализация GLFW
        initializeGLFW();
        
        // 2. Создание окна
        createWindow();
        
        // 3. Настройка OpenGL
        setupOpenGL();
        
        // 4. Создание игровых объектов
        createGameObjects();
        
        // 5. Настройка текстур и шрифтов
        setupTexturesAndFonts();
        
        initialized = true;
        System.out.println("InitializationModule: Инициализация завершена");
    }
    
    private void initializeGLFW() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
    }
    
    private void createWindow() {
        // Get primary monitor for fullscreen
        long monitor = 0;
        if (this.fullscreen) {
            monitor = GLFW.glfwGetPrimaryMonitor();
            GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitor);
            this.width = vidmode.width();
            this.height = vidmode.height();
        }
        
        // Create window
        this.window = GLFW.glfwCreateWindow(this.width, this.height, "Minecraft 0.0.8a", monitor, 0);
        if (this.window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // Make context current
        GLFW.glfwMakeContextCurrent(this.window);
        GL.createCapabilities();
        
        // Debug: Check if context is current
        System.out.println("Context current: " + GLFW.glfwGetCurrentContext());
        System.out.println("Window: " + this.window);
    }
    
    private void setupOpenGL() {
        // Setup fog colors
        int col0 = 16710650;
        int col1 = 920330;
        float fr = 0.5F;
        float fg = 0.8F;
        float fb = 1.0F;
        
        this.fogColor0 = org.lwjgl.BufferUtils.createFloatBuffer(4);
        this.fogColor1 = org.lwjgl.BufferUtils.createFloatBuffer(4);
        
        this.fogColor0.put(new float[]{(float)(col0 >> 16 & 255) / 255.0F, (float)(col0 >> 8 & 255) / 255.0F, (float)(col0 & 255) / 255.0F, 1.0F});
        this.fogColor0.flip();
        this.fogColor1.put(new float[]{(float)(col1 >> 16 & 255) / 255.0F, (float)(col1 >> 8 & 255) / 255.0F, (float)(col1 & 255) / 255.0F, 1.0F});
        this.fogColor1.flip();
        
        // Setup OpenGL state
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glClearColor(fr, fg, fb, 0.0F);
        GL11.glClearDepth(1.0D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    
    private void createGameObjects() {
        // Initialize textures first
        this.textures = new Textures();
        
        // Create level first (no OpenGL calls)
        this.level = new Level(256, 256, 64);
        this.player = new Player(this.level);
        
      // Create entities
      for(int i = 0; i < 10; ++i) {
         Zombie zombie = new Zombie(this.level, this.textures, 128.0F, 0.0F, 128.0F);
         // zombie.resetPos(); // Этот метод может быть недоступен
         this.entities.add(zombie);
      }
    }
    
    private void setupTexturesAndFonts() {
        // Create OpenGL-dependent objects
        this.levelRenderer = new LevelRenderer(this.level, this.textures);
        this.particleEngine = new ParticleEngine(this.level, this.textures);
        this.font = new Font("/default.gif", this.textures);
    }
    
    @Override
    public void update(float deltaTime) {
        // Инициализация не требует обновления
    }
    
    @Override
    public void render(float partialTicks) {
        // Инициализация не требует рендеринга
    }
    
    @Override
    public void cleanup() {
        if (window != 0) {
            GLFW.glfwDestroyWindow(window);
        }
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
        initialized = false;
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String getName() {
        return "InitializationModule";
    }
    
    // Геттеры для доступа к созданным объектам
    public long getWindow() { return window; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Level getLevel() { return level; }
    public LevelRenderer getLevelRenderer() { return levelRenderer; }
    public Player getPlayer() { return player; }
    public ParticleEngine getParticleEngine() { return particleEngine; }
    public Textures getTextures() { return textures; }
    public Font getFont() { return font; }
    public List<Object> getEntities() { return entities; }
    public FloatBuffer getFogColor0() { return fogColor0; }
    public FloatBuffer getFogColor1() { return fogColor1; }
    
    // Метод для обновления размеров окна
    public void updateWindowSize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
    }
}
