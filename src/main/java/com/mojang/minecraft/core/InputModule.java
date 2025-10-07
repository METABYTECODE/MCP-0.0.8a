package com.mojang.minecraft.core;

import com.mojang.minecraft.InputManager;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.Player;
import com.mojang.minecraft.renderer.Textures;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.level.tile.Tile;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Модуль обработки ввода
 * Отвечает за обработку клавиатуры, мыши и игровых действий
 */
public class InputModule implements GameModule {
    
    private boolean initialized = false;
    private InitializationModule initModule;
    private RenderingModule renderingModule;
    
    // Игровые объекты
    private Player player;
    private Level level;
    private ParticleEngine particleEngine;
    private Textures textures;
    private List<Object> entities;
    
    // Состояние ввода
    private long window;
    private int width;
    private int height;
    private boolean mouseGrabbed = false;
    private int yMouseAxis = 1;
    private int editMode = 0;
    private int paintTexture = 1;
    
    public InputModule(InitializationModule initModule, RenderingModule renderingModule) {
        this.initModule = initModule;
        this.renderingModule = renderingModule;
    }
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        // Получаем объекты из других модулей
        this.window = initModule.getWindow();
        this.width = initModule.getWidth();
        this.height = initModule.getHeight();
        this.player = initModule.getPlayer();
        this.level = initModule.getLevel();
        this.particleEngine = initModule.getParticleEngine();
        this.textures = initModule.getTextures();
        this.entities = initModule.getEntities();
        
        // Настройка коллбэков ввода
        setupInputCallbacks();
        
        // Автозахват мыши для FPS контролов
        grabMouse();
        
        initialized = true;
        System.out.println("InputModule: Инициализация завершена");
    }
    
    private void setupInputCallbacks() {
        // Setup key callback
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            boolean pressed = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
            InputManager.setKeyState(key, pressed);
            
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                releaseMouse();
            }
            if (key == GLFW.GLFW_KEY_ENTER && action == GLFW.GLFW_PRESS) {
                level.save();
            }
            // Handle texture selection keys
            if (key == GLFW.GLFW_KEY_1 && action == GLFW.GLFW_PRESS) {
                paintTexture = 1;
                renderingModule.setPaintTexture(paintTexture);
            }
            if (key == GLFW.GLFW_KEY_2 && action == GLFW.GLFW_PRESS) {
                paintTexture = 3;
                renderingModule.setPaintTexture(paintTexture);
            }
            if (key == GLFW.GLFW_KEY_3 && action == GLFW.GLFW_PRESS) {
                paintTexture = 4;
                renderingModule.setPaintTexture(paintTexture);
            }
            if (key == GLFW.GLFW_KEY_4 && action == GLFW.GLFW_PRESS) {
                paintTexture = 5;
                renderingModule.setPaintTexture(paintTexture);
            }
            if (key == GLFW.GLFW_KEY_6 && action == GLFW.GLFW_PRESS) {
                paintTexture = 6;
                renderingModule.setPaintTexture(paintTexture);
            }
            if (key == GLFW.GLFW_KEY_U && action == GLFW.GLFW_PRESS) {
                yMouseAxis *= -1;
            }
            if (key == GLFW.GLFW_KEY_G && action == GLFW.GLFW_PRESS) {
                entities.add(new Zombie(level, textures, player.x, player.y, player.z));
            }
        });
        
        // Setup mouse callback
        GLFW.glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            boolean pressed = action == GLFW.GLFW_PRESS;
            InputManager.setMouseButtonState(button, pressed);
            
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
                if (!mouseGrabbed) {
                    grabMouse();
                } else {
                    handleMouseClick();
                }
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
                editMode = (editMode + 1) % 2;
                renderingModule.setEditMode(editMode);
            }
        });
        
        // Setup cursor position callback
        GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (mouseGrabbed) {
                // Calculate relative mouse movement
                float centerX = width / 2.0f;
                float centerY = height / 2.0f;
                float deltaX = (float)xpos - centerX;
                float deltaY = (float)ypos - centerY;
                
                if (Math.abs(deltaX) > 1.0f || Math.abs(deltaY) > 1.0f) {
                    player.turn(deltaX, -deltaY * (float)yMouseAxis);
                    // Reset cursor to center
                    GLFW.glfwSetCursorPos(window, centerX, centerY);
                }
            }
        });
        
        // Setup window resize callback
        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            this.width = width;
            this.height = height;
            org.lwjgl.opengl.GL11.glViewport(0, 0, width, height);
            
            // Обновляем размеры в модуле инициализации
            initModule.updateWindowSize(width, height);
            
            System.out.println("Window resized to: " + width + "x" + height);
        });
    }
    
    private void handleMouseClick() {
        var hitResult = renderingModule.getHitResult();
        
        if (editMode == 0) {
            if (hitResult != null) {
                Tile oldTile = Tile.tiles[level.getTile(hitResult.x, hitResult.y, hitResult.z)];
                boolean changed = level.setTile(hitResult.x, hitResult.y, hitResult.z, 0);
                if (oldTile != null && changed) {
                    oldTile.destroy(level, hitResult.x, hitResult.y, hitResult.z, particleEngine);
                }
            }
        } else if (hitResult != null) {
            int x = hitResult.x;
            int y = hitResult.y;
            int z = hitResult.z;
            if (hitResult.f == 0) {
                --y;
            }

            if (hitResult.f == 1) {
                ++y;
            }

            if (hitResult.f == 2) {
                --z;
            }

            if (hitResult.f == 3) {
                ++z;
            }

            if (hitResult.f == 4) {
                --x;
            }

            if (hitResult.f == 5) {
                ++x;
            }

            AABB aabb = Tile.tiles[paintTexture].getAABB(x, y, z);
            if (aabb == null || isFree(aabb)) {
                level.setTile(x, y, z, paintTexture);
            }
        }
    }
    
    private boolean isFree(AABB aabb) {
        if (player.bb.intersects(aabb)) {
            return false;
        } else {
            for(int i = 0; i < entities.size(); ++i) {
                Object entity = entities.get(i);
                if (entity instanceof Zombie) {
                    Zombie zombie = (Zombie) entity;
                    if (zombie.bb.intersects(aabb)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
    
    public void grabMouse() {
        if (!mouseGrabbed) {
            mouseGrabbed = true;
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            GLFW.glfwSetCursorPos(window, width / 2.0, height / 2.0);
        }
    }

    public void releaseMouse() {
        if (mouseGrabbed) {
            mouseGrabbed = false;
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }
    
    @Override
    public void update(float deltaTime) {
        // Обработка ввода происходит в коллбэках
        // Здесь можно добавить дополнительную логику обработки ввода
    }
    
    @Override
    public void render(float partialTicks) {
        // Модуль ввода не рендерит ничего
    }
    
    @Override
    public void cleanup() {
        initialized = false;
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String getName() {
        return "InputModule";
    }
}
