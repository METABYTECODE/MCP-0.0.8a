package com.rivet.engine.opengl;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Класс для работы с шейдерами OpenGL 3.2+
 * Поддерживает вертексные и фрагментные шейдеры
 */
public class Shader {
    
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private boolean linked = false;
    
    /**
     * Создать новый шейдер
     */
    public Shader() {
        programId = GL20.glCreateProgram();
    }
    
    /**
     * Загрузить вертексный шейдер из ресурсов
     * @param resourcePath путь к ресурсу
     * @throws ShaderException если загрузка не удалась
     */
    public void loadVertexShader(String resourcePath) throws ShaderException {
        String source = loadShaderSource(resourcePath);
        vertexShaderId = compileShader(source, GL20.GL_VERTEX_SHADER);
        GL20.glAttachShader(programId, vertexShaderId);
    }
    
    /**
     * Загрузить фрагментный шейдер из ресурсов
     * @param resourcePath путь к ресурсу
     * @throws ShaderException если загрузка не удалась
     */
    public void loadFragmentShader(String resourcePath) throws ShaderException {
        String source = loadShaderSource(resourcePath);
        fragmentShaderId = compileShader(source, GL20.GL_FRAGMENT_SHADER);
        GL20.glAttachShader(programId, fragmentShaderId);
    }
    
    /**
     * Связать шейдерную программу
     * @throws ShaderException если связывание не удалось
     */
    public void link() throws ShaderException {
        GL20.glLinkProgram(programId);
        
        int status = GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS);
        if (status == 0) {
            String log = GL20.glGetProgramInfoLog(programId);
            throw new ShaderException("Failed to link shader program: " + log);
        }
        
        linked = true;
    }
    
    /**
     * Использовать шейдерную программу
     */
    public void use() {
        if (!linked) {
            throw new IllegalStateException("Shader program not linked!");
        }
        GL20.glUseProgram(programId);
    }
    
    /**
     * Перестать использовать шейдерную программу
     */
    public void stop() {
        GL20.glUseProgram(0);
    }
    
    /**
     * Получить ID программы
     * @return ID программы
     */
    public int getProgramId() {
        return programId;
    }
    
    /**
     * Получить расположение uniform переменной
     * @param name имя переменной
     * @return расположение переменной
     */
    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(programId, name);
    }
    
    /**
     * Получить расположение атрибута
     * @param name имя атрибута
     * @return расположение атрибута
     */
    public int getAttributeLocation(String name) {
        return GL20.glGetAttribLocation(programId, name);
    }
    
    /**
     * Установить uniform матрицу 4x4
     * @param location расположение uniform
     * @param matrix матрица
     */
    public void setUniformMatrix4f(int location, org.joml.Matrix4f matrix) {
        float[] matrixArray = new float[16];
        matrix.get(matrixArray);
        GL20.glUniformMatrix4fv(location, false, matrixArray);
    }
    
    /**
     * Установить uniform вектор 3
     * @param location расположение uniform
     * @param vector вектор
     */
    public void setUniformVector3f(int location, org.joml.Vector3f vector) {
        GL20.glUniform3f(location, vector.x, vector.y, vector.z);
    }
    
    /**
     * Установить uniform float
     * @param location расположение uniform
     * @param value значение
     */
    public void setUniformFloat(int location, float value) {
        GL20.glUniform1f(location, value);
    }
    
    /**
     * Установить uniform int
     * @param location расположение uniform
     * @param value значение
     */
    public void setUniformInt(int location, int value) {
        GL20.glUniform1i(location, value);
    }
    
    /**
     * Загрузить исходный код шейдера из ресурсов
     * @param resourcePath путь к ресурсу
     * @return исходный код шейдера
     * @throws ShaderException если загрузка не удалась
     */
    private String loadShaderSource(String resourcePath) throws ShaderException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ShaderException("Shader resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ShaderException("Failed to load shader: " + resourcePath, e);
        }
    }
    
    /**
     * Скомпилировать шейдер
     * @param source исходный код шейдера
     * @param type тип шейдера
     * @return ID скомпилированного шейдера
     * @throws ShaderException если компиляция не удалась
     */
    private int compileShader(String source, int type) throws ShaderException {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        
        int status = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (status == 0) {
            String log = GL20.glGetShaderInfoLog(shaderId);
            GL20.glDeleteShader(shaderId);
            throw new ShaderException("Failed to compile shader: " + log);
        }
        
        return shaderId;
    }
    
    /**
     * Очистить ресурсы шейдера
     */
    public void cleanup() {
        if (vertexShaderId != 0) {
            GL20.glDeleteShader(vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL20.glDeleteShader(fragmentShaderId);
        }
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }
}
