package com.rivet.engine.modules;

import com.rivet.engine.opengl.Shader;
import com.rivet.engine.opengl.ShaderException;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Модуль инициализации OpenGL 3.2+ Core Profile
 */
public class InitializationModule implements GameModule {
    
    private static final Logger logger = LoggerFactory.getLogger(InitializationModule.class);
    private boolean initialized = false;
    private long window;
    private int width;
    private int height;
    private boolean fullscreen;
    
    // OpenGL объекты
    private Shader basicShader;
    
    public InitializationModule(int width, int height, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
    }
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        // 1. Инициализация GLFW
        initializeGLFW();
        
        // 2. Создание окна с современным OpenGL
        createWindow();
        
        // 3. Настройка OpenGL 3.2+ Core Profile
        setupModernOpenGL();
        
        // 4. Загрузка шейдеров
        loadShaders();
        
        initialized = true;
        logger.info("InitializationModule: Инициализация завершена");
        logger.info("OpenGL Version: {}", GL11.glGetString(GL11.GL_VERSION));
        logger.info("OpenGL Vendor: {}", GL11.glGetString(GL11.GL_VENDOR));
        logger.info("OpenGL Renderer: {}", GL11.glGetString(GL11.GL_RENDERER));
    }
    
    private void initializeGLFW() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW for OpenGL 3.2+ Core Profile
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
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
        this.window = GLFW.glfwCreateWindow(this.width, this.height, "Rivet 1.0.0 - Modern 3D Engine", monitor, 0);
        if (this.window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // Make context current
        GLFW.glfwMakeContextCurrent(this.window);
        GL.createCapabilities();
        
        // Debug: Check if context is current
        logger.debug("Context current: {}", GLFW.glfwGetCurrentContext());
        logger.debug("Window: {}", this.window);
    }
    
    private void setupModernOpenGL() {
        // Настройка OpenGL состояния
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        
        // Включаем смешивание для прозрачности
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Настройка viewport
        GL11.glViewport(0, 0, width, height);
        
        // Устанавливаем цвет очистки (небесно-голубой)
        GL11.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        
        // Проверяем профиль OpenGL
        String version = GL11.glGetString(GL11.GL_VERSION);
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        
        logger.info("OpenGL 3.2+ Core Profile настроен");
        logger.info("OpenGL Version: {}", version);
        logger.info("OpenGL Vendor: {}", vendor);
        logger.info("OpenGL Renderer: {}", renderer);
        
        // Проверяем, что мы действительно используем Core Profile
        if (version != null && version.contains("Core")) {
            logger.info("✓ OpenGL Core Profile активен");
        } else {
            logger.warn("⚠ Возможно, используется Compatibility Profile вместо Core Profile");
        }
    }
    
    private void loadShaders() throws ShaderException {
        try {
            // Создаем базовый шейдер
            basicShader = new Shader();
            basicShader.loadVertexShader("/shaders/basic.vert");
            basicShader.loadFragmentShader("/shaders/basic.frag");
            basicShader.link();
            
            logger.info("Шейдеры загружены успешно");
        } catch (ShaderException e) {
            throw new ShaderException("Failed to load shaders: " + e.getMessage(), e);
        }
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
        if (basicShader != null) {
            basicShader.cleanup();
        }
        
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
    public Shader getBasicShader() { return basicShader; }
    
    // Метод для обновления размеров окна
    public void updateWindowSize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
        GL11.glViewport(0, 0, width, height);
    }
}
