package com.mojang.minecraft.core;

import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.Player;
import com.mojang.minecraft.Entity;

import java.util.List;

/**
 * Модуль игровой логики
 * Отвечает за обновление игровых объектов (игрок, сущности, уровень, частицы)
 */
public class GameLogicModule implements GameModule {
    
    private boolean initialized = false;
    private InitializationModule initModule;
    
    // Игровые объекты
    private Level level;
    private Player player;
    private ParticleEngine particleEngine;
    private List<Object> entities;
    
    public GameLogicModule(InitializationModule initModule) {
        this.initModule = initModule;
    }
    
    @Override
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        // Получаем объекты из модуля инициализации
        this.level = initModule.getLevel();
        this.player = initModule.getPlayer();
        this.particleEngine = initModule.getParticleEngine();
        this.entities = initModule.getEntities();
        
        initialized = true;
        System.out.println("GameLogicModule: Инициализация завершена");
    }
    
    @Override
    public void update(float deltaTime) {
        if (!initialized) return;
        
        // Обновление уровня
        level.tick();
        
        // Обновление частиц
        particleEngine.tick();
        
        // Обновление сущностей
        for(int i = 0; i < entities.size(); ++i) {
            Object entity = entities.get(i);
            if (entity instanceof Entity) {
                Entity gameEntity = (Entity) entity;
                gameEntity.tick();
                if (gameEntity.removed) {
                    entities.remove(i--);
                }
            }
        }
        
        // Обновление игрока
        player.tick();
    }
    
    @Override
    public void render(float partialTicks) {
        // Модуль логики не рендерит ничего
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
        return "GameLogicModule";
    }
}
