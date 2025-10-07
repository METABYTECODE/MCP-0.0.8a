package com.rivet.engine.opengl;

/**
 * Исключение для ошибок работы с шейдерами
 */
public class ShaderException extends Exception {
    
    public ShaderException(String message) {
        super(message);
    }
    
    public ShaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
