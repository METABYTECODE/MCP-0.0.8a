package com.mojang.minecraft;

import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.gui.Font;
import com.mojang.minecraft.level.Chunk;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelRenderer;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.renderer.Frustum;
import com.mojang.minecraft.renderer.Tesselator;
import com.mojang.minecraft.renderer.Textures;
import java.awt.Component;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;

public class Minecraft implements Runnable {
   public static final String VERSION_STRING = "0.0.8a ";
   private boolean fullscreen = false;
   private int width;
   private int height;
   private FloatBuffer fogColor0 = BufferUtils.createFloatBuffer(4);
   private FloatBuffer fogColor1 = BufferUtils.createFloatBuffer(4);
   private Timer timer = new Timer(20.0F);
   private Level level;
   private LevelRenderer levelRenderer;
   private Player player;
   private int paintTexture = 1;
   private ParticleEngine particleEngine;
   private ArrayList<Entity> entities = new ArrayList<Entity>();
   public volatile boolean pause = false;
   private int yMouseAxis = 1;
   public Textures textures;
   private Font font;
   private int editMode = 0;
   private volatile boolean running = false;
   private String fpsString = "";
   private boolean mouseGrabbed = false;
   private IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
   private IntBuffer selectBuffer = BufferUtils.createIntBuffer(2000);
   private HitResult hitResult = null;
   FloatBuffer lb = BufferUtils.createFloatBuffer(16);
   
   // GLFW 3 variables
   private long window;

   public Minecraft(int width, int height, boolean fullscreen) {
      this.width = width;
      this.height = height;
      this.fullscreen = fullscreen;
      this.textures = new Textures();
   }

   public void init() throws IOException {
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
      // Use compatibility profile to support legacy OpenGL functions
      
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
      
      // Setup fog colors
      int col0 = 16710650;
      int col1 = 920330;
      float fr = 0.5F;
      float fg = 0.8F;
      float fb = 1.0F;
      this.fogColor0.put(new float[]{(float)(col0 >> 16 & 255) / 255.0F, (float)(col0 >> 8 & 255) / 255.0F, (float)(col0 & 255) / 255.0F, 1.0F});
      this.fogColor0.flip();
      this.fogColor1.put(new float[]{(float)(col1 >> 16 & 255) / 255.0F, (float)(col1 >> 8 & 255) / 255.0F, (float)(col1 & 255) / 255.0F, 1.0F});
      this.fogColor1.flip();
      
      // Create level first (no OpenGL calls)
      this.level = new Level(256, 256, 64);
      this.player = new Player(this.level);

      for(int i = 0; i < 10; ++i) {
         Zombie zombie = new Zombie(this.level, this.textures, 128.0F, 0.0F, 128.0F);
         zombie.resetPos();
         this.entities.add(zombie);
      }

      // Setup OpenGL after all components are initialized
      // Make sure context is current
      GLFW.glfwMakeContextCurrent(this.window);
      
      this.checkGlError("Pre startup");
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      // GL11.glShadeModel(GL11.GL_SMOOTH); // Deprecated in modern OpenGL
      GL11.glClearColor(fr, fg, fb, 0.0F);
      GL11.glClearDepth(1.0D);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDepthFunc(GL11.GL_LEQUAL);
      GL11.glEnable(GL11.GL_ALPHA_TEST);
      GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      this.checkGlError("Startup");
      
      // Now create OpenGL-dependent objects
      this.levelRenderer = new LevelRenderer(this.level, this.textures);
      this.particleEngine = new ParticleEngine(this.level, this.textures);
      this.font = new Font("/default.gif", this.textures);

      // Show window
      GLFW.glfwShowWindow(this.window);
      
      // Setup input callbacks
      this.setupInputCallbacks();
      
      // Disable VSync for maximum FPS (set to 0)
      // Set to 1 for VSync (60 FPS), 0 for unlimited FPS
      GLFW.glfwSwapInterval(0);
      
      // Auto-grab mouse for FPS controls
      this.grabMouse();
      
      this.checkGlError("Post startup");
   }

   private void setupInputCallbacks() {
      // Setup key callback
      GLFW.glfwSetKeyCallback(this.window, (window, key, scancode, action, mods) -> {
         boolean pressed = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
         InputManager.setKeyState(key, pressed);
         
         if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            this.releaseMouse();
         }
         if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
            this.level.save();
         }
         // Handle texture selection keys
         if (key == GLFW.GLFW_KEY_1 && action == GLFW.GLFW_PRESS) {
            this.paintTexture = 1;
         }
         if (key == GLFW.GLFW_KEY_2 && action == GLFW.GLFW_PRESS) {
            this.paintTexture = 3;
         }
         if (key == GLFW.GLFW_KEY_3 && action == GLFW.GLFW_PRESS) {
            this.paintTexture = 4;
         }
         if (key == GLFW.GLFW_KEY_4 && action == GLFW.GLFW_PRESS) {
            this.paintTexture = 5;
         }
         if (key == GLFW.GLFW_KEY_6 && action == GLFW.GLFW_PRESS) {
            this.paintTexture = 6;
         }
         if (key == GLFW.GLFW_KEY_U && action == GLFW.GLFW_PRESS) {
            this.yMouseAxis *= -1;
         }
         if (key == GLFW.GLFW_KEY_G && action == GLFW.GLFW_PRESS) {
            this.entities.add(new Zombie(this.level, this.textures, this.player.x, this.player.y, this.player.z));
         }
      });
      
      // Setup mouse callback
      GLFW.glfwSetMouseButtonCallback(this.window, (window, button, action, mods) -> {
         boolean pressed = action == GLFW.GLFW_PRESS;
         InputManager.setMouseButtonState(button, pressed);
         
         if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            if (!this.mouseGrabbed) {
               this.grabMouse();
            } else {
               this.handleMouseClick();
            }
         }
         if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
            this.editMode = (this.editMode + 1) % 2;
         }
      });
      
      // Setup cursor position callback
      GLFW.glfwSetCursorPosCallback(this.window, (window, xpos, ypos) -> {
         if (this.mouseGrabbed) {
            // Calculate relative mouse movement
            float centerX = this.width / 2.0f;
            float centerY = this.height / 2.0f;
            float deltaX = (float)xpos - centerX;
            float deltaY = (float)ypos - centerY;
            
            if (Math.abs(deltaX) > 1.0f || Math.abs(deltaY) > 1.0f) {
               this.player.turn(deltaX, -deltaY * (float)this.yMouseAxis);
               // Reset cursor to center
               GLFW.glfwSetCursorPos(this.window, centerX, centerY);
            }
         }
      });
      
      // Setup window resize callback
      GLFW.glfwSetFramebufferSizeCallback(this.window, (window, width, height) -> {
         this.width = width;
         this.height = height;
         GL11.glViewport(0, 0, width, height);
         System.out.println("Window resized to: " + width + "x" + height);
      });
   }

   private void checkGlError(String string) {
      int errorCode = GL11.glGetError();
      if (errorCode != 0) {
         System.out.println("########## GL ERROR ##########");
         System.out.println("@ " + string);
         System.out.println("Error code: " + errorCode);
         System.exit(0);
      }
   }

   public void destroy() {
      try {
         this.level.save();
      } catch (Exception var2) {
         ;
      }

      // Cleanup GLFW
      if (this.window != 0) {
         GLFW.glfwDestroyWindow(this.window);
      }
      GLFW.glfwTerminate();
      GLFW.glfwSetErrorCallback(null).free();
   }

   public void run() {
      this.running = true;

      try {
         this.init();
      } catch (Exception var9) {
         JOptionPane.showMessageDialog((Component)null, var9.toString(), "Failed to start Minecraft", 0);
         return;
      }

      long lastTime = System.currentTimeMillis();
      long lastFrameTime = System.nanoTime();
      int frames = 0;
      final long NANOS_PER_FRAME = 1000000000L / 120L; // 120 FPS limit (set to 60 for 60 FPS)

      try {
         while(this.running && !GLFW.glfwWindowShouldClose(this.window)) {
            if (this.pause) {
               Thread.sleep(100L);
            } else {
               // Poll events
               GLFW.glfwPollEvents();
               
               this.timer.advanceTime();

               for(int i = 0; i < this.timer.ticks; ++i) {
                  this.tick();
               }

               this.checkGlError("Pre render");
               this.render(this.timer.a);
               this.checkGlError("Post render");
               
               // Swap buffers
               GLFW.glfwSwapBuffers(this.window);
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

               while(System.currentTimeMillis() >= lastTime + 1000L) {
                  this.fpsString = frames + " fps, " + Chunk.updates + " chunk updates";
                  Chunk.updates = 0;
                  lastTime += 1000L;
                  frames = 0;
               }
            }
         }
      } catch (Exception var10) {
         var10.printStackTrace();
      } finally {
         this.destroy();
      }

   }

   public void stop() {
      this.running = false;
   }

   public void grabMouse() {
      if (!this.mouseGrabbed) {
         this.mouseGrabbed = true;
         GLFW.glfwSetInputMode(this.window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
         GLFW.glfwSetCursorPos(this.window, this.width / 2.0, this.height / 2.0);
      }
   }

   public void releaseMouse() {
      if (this.mouseGrabbed) {
         this.mouseGrabbed = false;
         GLFW.glfwSetInputMode(this.window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
      }
   }

   private void handleMouseClick() {
      if (this.editMode == 0) {
         if (this.hitResult != null) {
            Tile oldTile = Tile.tiles[this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z)];
            boolean changed = this.level.setTile(this.hitResult.x, this.hitResult.y, this.hitResult.z, 0);
            if (oldTile != null && changed) {
               oldTile.destroy(this.level, this.hitResult.x, this.hitResult.y, this.hitResult.z, this.particleEngine);
            }
         }
      } else if (this.hitResult != null) {
         int x = this.hitResult.x;
         int y = this.hitResult.y;
         int z = this.hitResult.z;
         if (this.hitResult.f == 0) {
            --y;
         }

         if (this.hitResult.f == 1) {
            ++y;
         }

         if (this.hitResult.f == 2) {
            --z;
         }

         if (this.hitResult.f == 3) {
            ++z;
         }

         if (this.hitResult.f == 4) {
            --x;
         }

         if (this.hitResult.f == 5) {
            ++x;
         }

         AABB aabb = Tile.tiles[this.paintTexture].getAABB(x, y, z);
         if (aabb == null || this.isFree(aabb)) {
            this.level.setTile(x, y, z, this.paintTexture);
         }
      }

   }

   public void tick() {
      // Input handling is now done in callbacks, so we just update game logic
      this.level.tick();
      this.particleEngine.tick();

      for(int i = 0; i < this.entities.size(); ++i) {
         ((Entity)this.entities.get(i)).tick();
         if (((Entity)this.entities.get(i)).removed) {
            this.entities.remove(i--);
         }
      }

      this.player.tick();
   }

   private boolean isFree(AABB aabb) {
      if (this.player.bb.intersects(aabb)) {
         return false;
      } else {
         for(int i = 0; i < this.entities.size(); ++i) {
            if (((Entity)this.entities.get(i)).bb.intersects(aabb)) {
               return false;
            }
         }

         return true;
      }
   }

   private void moveCameraToPlayer(float a) {
      GL11.glTranslatef(0.0F, 0.0F, -0.3F);
      GL11.glRotatef(this.player.xRot, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(this.player.yRot, 0.0F, 1.0F, 0.0F);
      float x = this.player.xo + (this.player.x - this.player.xo) * a;
      float y = this.player.yo + (this.player.y - this.player.yo) * a;
      float z = this.player.zo + (this.player.z - this.player.zo) * a;
      GL11.glTranslatef(-x, -y, -z);
   }

   private void setupCamera(float a) {
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      // Using JOML for perspective projection
      Matrix4f projection = new Matrix4f();
      projection.setPerspective((float)Math.toRadians(70.0f), (float)this.width / (float)this.height, 0.05f, 1000.0f);
      GL11.glMultMatrixf(projection.get(new float[16]));
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      this.moveCameraToPlayer(a);
   }

   private void setupPickCamera(float a, int x, int y) {
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      this.viewportBuffer.clear();
      GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewportBuffer);
      this.viewportBuffer.flip();
      this.viewportBuffer.limit(16);
      // Using JOML for pick matrix and perspective
      Matrix4f pickMatrix = new Matrix4f();
      pickMatrix.translate((float)(this.width - 2 * x) / (float)this.width, (float)(this.height - 2 * y) / (float)this.height, 0.0F);
      pickMatrix.scale((float)this.width / 5.0F, (float)this.height / 5.0F, 1.0F);
      Matrix4f projection = new Matrix4f();
      projection.setPerspective((float)Math.toRadians(70.0f), (float)this.width / (float)this.height, 0.05f, 1000.0f);
      pickMatrix.mul(projection);
      GL11.glMultMatrixf(pickMatrix.get(new float[16]));
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      this.moveCameraToPlayer(a);
   }

   private void pick(float a) {
      this.selectBuffer.clear();
      GL11.glSelectBuffer(this.selectBuffer);
      GL11.glRenderMode(7170);
      this.setupPickCamera(a, this.width / 2, this.height / 2);
      this.levelRenderer.pick(this.player, Frustum.getFrustum());
      int hits = GL11.glRenderMode(7168);
      this.selectBuffer.flip();
      this.selectBuffer.limit(this.selectBuffer.capacity());
      long closest = 0L;
      int[] names = new int[10];
      int hitNameCount = 0;

      for(int i = 0; i < hits; ++i) {
         int nameCount = this.selectBuffer.get();
         long minZ = (long)this.selectBuffer.get();
         this.selectBuffer.get();
         int j;
         if (minZ >= closest && i != 0) {
            for(j = 0; j < nameCount; ++j) {
               this.selectBuffer.get();
            }
         } else {
            closest = minZ;
            hitNameCount = nameCount;

            for(j = 0; j < nameCount; ++j) {
               names[j] = this.selectBuffer.get();
            }
         }
      }

      if (hitNameCount > 0) {
         this.hitResult = new HitResult(names[0], names[1], names[2], names[3], names[4]);
      } else {
         this.hitResult = null;
      }

   }

   public void render(float a) {
      // Mouse handling is now done in callbacks
      GL11.glViewport(0, 0, this.width, this.height);

      this.checkGlError("Set viewport");
      this.pick(a);
      this.checkGlError("Picked");
      GL11.glClear(16640);
      this.setupCamera(a);
      this.checkGlError("Set up camera");
      GL11.glEnable(2884);
      Frustum frustum = Frustum.getFrustum();
      this.levelRenderer.updateDirtyChunks(this.player);
      this.checkGlError("Update chunks");
      this.setupFog(0);
      GL11.glEnable(2912);
      this.levelRenderer.render(this.player, 0);
      this.checkGlError("Rendered level");

      Entity zombie;
      int i;
      for(i = 0; i < this.entities.size(); ++i) {
         zombie = (Entity)this.entities.get(i);
         if (zombie.isLit() && frustum.isVisible(zombie.bb)) {
            ((Entity)this.entities.get(i)).render(a);
         }
      }

      this.checkGlError("Rendered entities");
      this.particleEngine.render(this.player, a, 0);
      this.checkGlError("Rendered particles");
      this.setupFog(1);
      this.levelRenderer.render(this.player, 1);

      for(i = 0; i < this.entities.size(); ++i) {
         zombie = (Entity)this.entities.get(i);
         if (!zombie.isLit() && frustum.isVisible(zombie.bb)) {
            ((Entity)this.entities.get(i)).render(a);
         }
      }

      this.particleEngine.render(this.player, a, 1);
      GL11.glDisable(2896);
      GL11.glDisable(3553);
      GL11.glDisable(2912);
      this.checkGlError("Rendered rest");
      if (this.hitResult != null) {
         GL11.glDisable(3008);
         this.levelRenderer.renderHit(this.hitResult, this.editMode, this.paintTexture);
         GL11.glEnable(3008);
      }

      this.checkGlError("Rendered hit");
      this.drawGui(a);
      this.checkGlError("Rendered gui");
      // Display.update() is now handled in the main loop with glfwSwapBuffers()
   }

   private void drawGui(float a) {
      int screenWidth = this.width * 240 / this.height;
      int screenHeight = this.height * 240 / this.height;
      GL11.glClear(256);
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      GL11.glOrtho(0.0D, (double)screenWidth, (double)screenHeight, 0.0D, 100.0D, 300.0D);
      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
      GL11.glTranslatef(0.0F, 0.0F, -200.0F);
      this.checkGlError("GUI: Init");
      GL11.glPushMatrix();
      GL11.glTranslatef((float)(screenWidth - 16), 16.0F, 0.0F);
      Tesselator t = Tesselator.instance;
      GL11.glScalef(16.0F, 16.0F, 16.0F);
      GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
      GL11.glTranslatef(-1.5F, 0.5F, -0.5F);
      GL11.glScalef(-1.0F, -1.0F, 1.0F);
      int id = this.textures.loadTexture("/terrain.png", 9728);
      GL11.glBindTexture(3553, id);
      GL11.glEnable(3553);
      t.init();
      Tile.tiles[this.paintTexture].render(t, this.level, 0, -2, 0, 0);
      t.flush();
      GL11.glDisable(3553);
      GL11.glPopMatrix();
      this.checkGlError("GUI: Draw selected");
      this.font.drawShadow("0.0.8a ", 2, 2, 16777215);
      this.font.drawShadow(this.fpsString, 2, 12, 16777215);
      this.checkGlError("GUI: Draw text");
      int wc = screenWidth / 2;
      int hc = screenHeight / 2;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      t.init();
      t.vertex((float)(wc + 1), (float)(hc - 4), 0.0F);
      t.vertex((float)(wc - 0), (float)(hc - 4), 0.0F);
      t.vertex((float)(wc - 0), (float)(hc + 5), 0.0F);
      t.vertex((float)(wc + 1), (float)(hc + 5), 0.0F);
      t.vertex((float)(wc + 5), (float)(hc - 0), 0.0F);
      t.vertex((float)(wc - 4), (float)(hc - 0), 0.0F);
      t.vertex((float)(wc - 4), (float)(hc + 1), 0.0F);
      t.vertex((float)(wc + 5), (float)(hc + 1), 0.0F);
      t.flush();
      this.checkGlError("GUI: Draw crosshair");
   }

   private void setupFog(int i) {
      if (i == 0) {
         GL11.glFogi(2917, 2048);
         GL11.glFogf(2914, 0.001F);
         GL11.glFogfv(GL11.GL_FOG_COLOR, this.fogColor0);
         GL11.glDisable(2896);
      } else if (i == 1) {
         GL11.glFogi(2917, 2048);
         GL11.glFogf(2914, 0.01F);
         GL11.glFogfv(GL11.GL_FOG_COLOR, this.fogColor1);
         GL11.glEnable(2896);
         GL11.glEnable(2903);
         float br = 0.6F;
         GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, this.getBuffer(br, br, br, 1.0F));
      }

   }

   private FloatBuffer getBuffer(float a, float b, float c, float d) {
      this.lb.clear();
      this.lb.put(a).put(b).put(c).put(d);
      this.lb.flip();
      return this.lb;
   }

   public static void checkError() {
      int e = GL11.glGetError();
      if (e != 0) {
         throw new IllegalStateException("OpenGL Error: " + e);
      }
   }

   public static void main(String[] args) {
      Minecraft minecraft = new Minecraft(854, 480, false);
      (new Thread(minecraft)).start();
   }
}
