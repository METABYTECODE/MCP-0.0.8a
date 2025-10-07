package com.mojang.minecraft.core;

import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.LevelRenderer;
import com.mojang.minecraft.level.Chunk;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.Player;
import com.mojang.minecraft.gui.Font;
import com.mojang.minecraft.renderer.Frustum;
import com.mojang.minecraft.renderer.Tesselator;
import com.mojang.minecraft.renderer.Textures;
import com.mojang.minecraft.HitResult;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.phys.AABB;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

/**
 * Модуль рендеринга
 * Отвечает за отрисовку игрового мира, сущностей, частиц и GUI
 */
public class RenderingModule implements GameModule {
    
    private boolean initialized = false;
    private InitializationModule initModule;
    
    // Рендеринг компоненты
    private LevelRenderer levelRenderer;
    private ParticleEngine particleEngine;
    private Player player;
    private Textures textures;
    private Font font;
    private List<Object> entities;
    
    // Рендеринг состояние
    private int width;
    private int height;
    private HitResult hitResult;
    private int editMode = 0;
    private int paintTexture = 1;
    private String fpsString = "";
    
    // OpenGL буферы
    private IntBuffer viewportBuffer;
    private IntBuffer selectBuffer;
    private FloatBuffer fogColor0;
    private FloatBuffer fogColor1;
    private FloatBuffer lb;
    
    public RenderingModule(InitializationModule initModule) {
        this.initModule = initModule;
    }
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        // Получаем объекты из модуля инициализации
        this.levelRenderer = initModule.getLevelRenderer();
        this.particleEngine = initModule.getParticleEngine();
        this.player = initModule.getPlayer();
        this.textures = initModule.getTextures();
        this.font = initModule.getFont();
        this.entities = initModule.getEntities();
        this.fogColor0 = initModule.getFogColor0();
        this.fogColor1 = initModule.getFogColor1();
        
        this.width = initModule.getWidth();
        this.height = initModule.getHeight();
        
        // Инициализация буферов
        this.viewportBuffer = org.lwjgl.BufferUtils.createIntBuffer(16);
        this.selectBuffer = org.lwjgl.BufferUtils.createIntBuffer(2000);
        this.lb = org.lwjgl.BufferUtils.createFloatBuffer(16);
        
        initialized = true;
        System.out.println("RenderingModule: Инициализация завершена");
    }
    
    @Override
    public void update(float deltaTime) {
        if (!initialized) return;
        
        // Обновление размеров окна если изменились
        int newWidth = initModule.getWidth();
        int newHeight = initModule.getHeight();
        
        if (newWidth != this.width || newHeight != this.height) {
            this.width = newWidth;
            this.height = newHeight;
            System.out.println("RenderingModule: Размеры обновлены до " + width + "x" + height);
        }
    }
    
    @Override
    public void render(float partialTicks) {
        if (!initialized) return;
        
        // Установка viewport
        GL11.glViewport(0, 0, width, height);
        
        // Pick объекты
        pick(partialTicks);
        
        // Очистка экрана
        GL11.glClear(16640);
        
        // Настройка камеры
        setupCamera(partialTicks);
        
        // Рендеринг уровня (неосвещенные объекты)
        GL11.glEnable(2884);
        Frustum frustum = Frustum.getFrustum();
        levelRenderer.updateDirtyChunks(player);
        setupFog(0);
        GL11.glEnable(2912);
        levelRenderer.render(player, 0);
        
        // Рендеринг сущностей (освещенные)
        renderEntities(frustum, partialTicks, true);
        
        // Рендеринг частиц (освещенные)
        particleEngine.render(player, partialTicks, 0);
        
        // Рендеринг уровня (освещенные объекты)
        setupFog(1);
        levelRenderer.render(player, 1);
        
        // Рендеринг сущностей (неосвещенные)
        renderEntities(frustum, partialTicks, false);
        
        // Рендеринг частиц (неосвещенные)
        particleEngine.render(player, partialTicks, 1);
        
        // Отключение состояний
        GL11.glDisable(2896);
        GL11.glDisable(3553);
        GL11.glDisable(2912);
        
        // Рендеринг выделения
        if (hitResult != null) {
            GL11.glDisable(3008);
            levelRenderer.renderHit(hitResult, editMode, paintTexture);
            GL11.glEnable(3008);
        }
        
        // Рендеринг GUI
        drawGui(partialTicks);
    }
    
    private void renderEntities(Frustum frustum, float partialTicks, boolean lit) {
        for(int i = 0; i < entities.size(); ++i) {
            Object entity = entities.get(i);
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                if (zombie.isLit() == lit && frustum.isVisible(zombie.bb)) {
                    zombie.render(partialTicks);
                }
            }
        }
    }
    
    private void setupCamera(float partialTicks) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        // Using JOML for perspective projection
        Matrix4f projection = new Matrix4f();
        projection.setPerspective((float)Math.toRadians(70.0f), (float)width / (float)height, 0.05f, 1000.0f);
        GL11.glMultMatrixf(projection.get(new float[16]));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        moveCameraToPlayer(partialTicks);
    }
    
    private void moveCameraToPlayer(float partialTicks) {
        GL11.glTranslatef(0.0F, 0.0F, -0.3F);
        GL11.glRotatef(player.xRot, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(player.yRot, 0.0F, 1.0F, 0.0F);
        float x = player.xo + (player.x - player.xo) * partialTicks;
        float y = player.yo + (player.y - player.yo) * partialTicks;
        float z = player.zo + (player.z - player.zo) * partialTicks;
        GL11.glTranslatef(-x, -y, -z);
    }
    
    private void pick(float partialTicks) {
        selectBuffer.clear();
        GL11.glSelectBuffer(selectBuffer);
        GL11.glRenderMode(7170);
        setupPickCamera(partialTicks, width / 2, height / 2);
        levelRenderer.pick(player, Frustum.getFrustum());
        int hits = GL11.glRenderMode(7168);
        selectBuffer.flip();
        selectBuffer.limit(selectBuffer.capacity());
        long closest = 0L;
        int[] names = new int[10];
        int hitNameCount = 0;

        for(int i = 0; i < hits; ++i) {
            int nameCount = selectBuffer.get();
            long minZ = (long)selectBuffer.get();
            selectBuffer.get();
            int j;
            if (minZ >= closest && i != 0) {
                for(j = 0; j < nameCount; ++j) {
                    selectBuffer.get();
                }
            } else {
                closest = minZ;
                hitNameCount = nameCount;

                for(j = 0; j < nameCount; ++j) {
                    names[j] = selectBuffer.get();
                }
            }
        }

        if (hitNameCount > 0) {
            hitResult = new HitResult(names[0], names[1], names[2], names[3], names[4]);
        } else {
            hitResult = null;
        }
    }
    
    private void setupPickCamera(float partialTicks, int x, int y) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        viewportBuffer.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);
        viewportBuffer.flip();
        viewportBuffer.limit(16);
        // Using JOML for pick matrix and perspective
        Matrix4f pickMatrix = new Matrix4f();
        pickMatrix.translate((float)(width - 2 * x) / (float)width, (float)(height - 2 * y) / (float)height, 0.0F);
        pickMatrix.scale((float)width / 5.0F, (float)height / 5.0F, 1.0F);
        Matrix4f projection = new Matrix4f();
        projection.setPerspective((float)Math.toRadians(70.0f), (float)width / (float)height, 0.05f, 1000.0f);
        pickMatrix.mul(projection);
        GL11.glMultMatrixf(pickMatrix.get(new float[16]));
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        moveCameraToPlayer(partialTicks);
    }
    
    private void drawGui(float partialTicks) {
        int screenWidth = width * 240 / height;
        int screenHeight = height * 240 / height;
        GL11.glClear(256);
        GL11.glMatrixMode(5889);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, (double)screenWidth, (double)screenHeight, 0.0D, 100.0D, 300.0D);
        GL11.glMatrixMode(5888);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -200.0F);
        
        GL11.glPushMatrix();
        GL11.glTranslatef((float)(screenWidth - 16), 16.0F, 0.0F);
        Tesselator t = Tesselator.instance;
        GL11.glScalef(16.0F, 16.0F, 16.0F);
        GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-1.5F, 0.5F, -0.5F);
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        int id = textures.loadTexture("/terrain.png", 9728);
        GL11.glBindTexture(3553, id);
        GL11.glEnable(3553);
        t.init();
        Tile.tiles[paintTexture].render(t, initModule.getLevel(), 0, -2, 0, 0);
        t.flush();
        GL11.glDisable(3553);
        GL11.glPopMatrix();
        
        font.drawShadow("0.0.8a ", 2, 2, 16777215);
        font.drawShadow(fpsString, 2, 12, 16777215);
        
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
    }
    
    private void setupFog(int i) {
        if (i == 0) {
            GL11.glFogi(2917, 2048);
            GL11.glFogf(2914, 0.001F);
            GL11.glFogfv(GL11.GL_FOG_COLOR, fogColor0);
            GL11.glDisable(2896);
        } else if (i == 1) {
            GL11.glFogi(2917, 2048);
            GL11.glFogf(2914, 0.01F);
            GL11.glFogfv(GL11.GL_FOG_COLOR, fogColor1);
            GL11.glEnable(2896);
            GL11.glEnable(2903);
            float br = 0.6F;
            GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, getBuffer(br, br, br, 1.0F));
        }
    }
    
    private FloatBuffer getBuffer(float a, float b, float c, float d) {
        lb.clear();
        lb.put(a).put(b).put(c).put(d);
        lb.flip();
        return lb;
    }
    
    @Override
    public void cleanup() {
        // Очистка ресурсов рендеринга
        initialized = false;
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public String getName() {
        return "RenderingModule";
    }
    
    // Сеттеры для обновления состояния
    public void setFpsString(String fpsString) {
        this.fpsString = fpsString;
    }
    
    public void setEditMode(int editMode) {
        this.editMode = editMode;
    }
    
    public void setPaintTexture(int paintTexture) {
        this.paintTexture = paintTexture;
    }
    
    public HitResult getHitResult() {
        return hitResult;
    }
}
