package com.rivet.game;

import com.rivet.engine.ModuleManager;
import com.rivet.engine.modules.InitializationModule;
import com.rivet.engine.modules.LoggingModule;
import com.rivet.engine.modules.ResourceModule;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 * Главный класс игры Rivet
 * Современный 3D движок с OpenGL 3.2+
 */
public class Rivet implements Runnable {
    public static final String VERSION_STRING = "Rivet 1.0.0";
    
    // Логгер
    private static final Logger logger = LoggerFactory.getLogger(Rivet.class);
    
    // Движок
    private ModuleManager moduleManager;
    private InitializationModule initModule;
    private LoggingModule loggingModule;
    private ResourceModule resourceModule;
    
    // Основные параметры
    private boolean fullscreen = false;
    private int width;
    private int height;
    
    // Игровое состояние
    public volatile boolean pause = false;
    private volatile boolean running = false;
    private int frames = 0;

    public Rivet(int width, int height, boolean fullscreen) {
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        
        // Инициализация движка
        this.moduleManager = new ModuleManager();
        this.loggingModule = new LoggingModule();
        this.resourceModule = new ResourceModule();
        this.initModule = new InitializationModule(width, height, fullscreen);
        
        // Добавление модулей в менеджер (порядок важен!)
        moduleManager.addModule(loggingModule);    // Сначала логирование
        moduleManager.addModule(resourceModule);   // Потом ресурсы
        moduleManager.addModule(initModule);       // Потом OpenGL
    }

    public void init() throws IOException {
        try {
            // Инициализация всех модулей
            moduleManager.initializeAll();
            
            // Показать окно после инициализации
            GLFW.glfwShowWindow(initModule.getWindow());
            
            // Отключить VSync для максимального FPS
            GLFW.glfwSwapInterval(0);
            
            
            logger.info("Rivet: Инициализация завершена успешно");
        } catch (Exception e) {
            logger.error("Rivet: Ошибка инициализации: " + e.getMessage(), e);
            throw new IOException("Failed to initialize game engine", e);
        }
    }

    public void destroy() {
        
        // Очистка всех модулей
        if (moduleManager != null) {
            moduleManager.cleanupAll();
        }
    }

    public void run() {
        this.running = true;

        try {
            this.init();
        } catch (Exception e) {
            JOptionPane.showMessageDialog((Component)null, e.toString(), "Failed to start Rivet", 0);
            return;
        }

        long lastTime = System.currentTimeMillis();
        long lastFrameTime = System.nanoTime();
        final long NANOS_PER_FRAME = 1000000000L / 120L; // 120 FPS limit

        try {
            while(this.running && !GLFW.glfwWindowShouldClose(initModule.getWindow())) {
                if (this.pause) {
                    Thread.sleep(100L);
                } else {
                    // Poll events
                    GLFW.glfwPollEvents();

                    // Простой рендеринг с современным OpenGL
                    renderSky();

                    // Swap buffers
                    GLFW.glfwSwapBuffers(initModule.getWindow());
                    ++this.frames;

                    // FPS limiting
                    long currentTime = System.nanoTime();
                    long deltaTime = currentTime - lastFrameTime;
                    if (deltaTime < NANOS_PER_FRAME) {
                        try {
                            Thread.sleep((NANOS_PER_FRAME - deltaTime) / 1000000L);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                    lastFrameTime = System.nanoTime();

                    // Обновление FPS строки каждую секунду
                    while(System.currentTimeMillis() >= lastTime + 1000L) {
                        logger.debug("FPS: {}", this.frames);
                        lastTime += 1000L;
                        this.frames = 0;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка в игровом цикле", e);
        } finally {
            this.destroy();
        }
    }

    public void stop() {
        this.running = false;
    }
    
           /**
            * Рендеринг неба и UI
            */
           private void renderSky() {
               // Очистка экрана небесно-голубым цветом
               org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
               
           }

    public static void main(String[] args) {
        Rivet rivet = new Rivet(854, 480, false);
        (new Thread(rivet)).start();
    }
}
