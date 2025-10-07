package com.mojang.minecraft;

import com.mojang.minecraft.core.*;
import com.mojang.minecraft.level.Chunk;
import java.awt.Component;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.lwjgl.glfw.GLFW;

/**
 * Главный класс игры Minecraft
 * Теперь использует модульную архитектуру для лучшей организации кода
 */
public class Minecraft implements Runnable {
   public static final String VERSION_STRING = "0.0.8a ";
   
   // Модульная архитектура
   private ModuleManager moduleManager;
   private InitializationModule initModule;
   private RenderingModule renderingModule;
   private InputModule inputModule;
   private GameLogicModule gameLogicModule;
   
   // Основные параметры
   private boolean fullscreen = false;
   private int width;
   private int height;
   
   // Игровое состояние
   private Timer timer = new Timer(20.0F);
   public volatile boolean pause = false;
   private volatile boolean running = false;
   private String fpsString = "";

   public Minecraft(int width, int height, boolean fullscreen) {
      this.width = width;
      this.height = height;
      this.fullscreen = fullscreen;
      
      // Инициализация модульной архитектуры
      this.moduleManager = new ModuleManager();
      this.initModule = new InitializationModule(width, height, fullscreen);
      this.gameLogicModule = new GameLogicModule(initModule);
      this.renderingModule = new RenderingModule(initModule);
      this.inputModule = new InputModule(initModule, renderingModule);
      
      // Добавление модулей в менеджер
      moduleManager.addModule(initModule);
      moduleManager.addModule(gameLogicModule);
      moduleManager.addModule(renderingModule);
      moduleManager.addModule(inputModule);
   }

   public void init() throws IOException {
      try {
         // Инициализация всех модулей
         moduleManager.initializeAll();
         
         // Показать окно после инициализации
         GLFW.glfwShowWindow(initModule.getWindow());
         
         // Отключить VSync для максимального FPS
         GLFW.glfwSwapInterval(0);
         
         System.out.println("Minecraft: Инициализация завершена успешно");
      } catch (Exception e) {
         System.err.println("Minecraft: Ошибка инициализации: " + e.getMessage());
         throw new IOException("Failed to initialize game modules", e);
      }
   }

   public void destroy() {
      try {
         // Сохранение уровня
         if (initModule != null && initModule.getLevel() != null) {
            initModule.getLevel().save();
         }
      } catch (Exception e) {
         System.err.println("Ошибка сохранения уровня: " + e.getMessage());
      }

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
         JOptionPane.showMessageDialog((Component)null, e.toString(), "Failed to start Minecraft", 0);
         return;
      }

      long lastTime = System.currentTimeMillis();
      long lastFrameTime = System.nanoTime();
      int frames = 0;
      final long NANOS_PER_FRAME = 1000000000L / 120L; // 120 FPS limit

      try {
         while(this.running && !GLFW.glfwWindowShouldClose(initModule.getWindow())) {
            if (this.pause) {
               Thread.sleep(100L);
            } else {
               // Poll events
               GLFW.glfwPollEvents();
               
               this.timer.advanceTime();

               // Обновление игровой логики
               for(int i = 0; i < this.timer.ticks; ++i) {
                  gameLogicModule.update(1.0f / 20.0f); // 20 TPS
               }
               
               // Обновление модуля рендеринга (для ресайза)
               renderingModule.update(this.timer.a);

               // Рендеринг
               renderingModule.render(this.timer.a);
               
               // Swap buffers
               GLFW.glfwSwapBuffers(initModule.getWindow());
               ++frames;
               
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
                  renderingModule.setFpsString(frames + " fps, " + Chunk.updates + " chunk updates");
                  Chunk.updates = 0;
                  lastTime += 1000L;
                  frames = 0;
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         this.destroy();
      }
   }

   public void stop() {
      this.running = false;
   }

   public static void main(String[] args) {
      Minecraft minecraft = new Minecraft(854, 480, false);
      (new Thread(minecraft)).start();
   }
}