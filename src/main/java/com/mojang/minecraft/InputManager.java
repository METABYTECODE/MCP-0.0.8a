package com.mojang.minecraft;

import org.lwjgl.glfw.GLFW;

public class InputManager {
    private static boolean[] keys = new boolean[1024];
    private static boolean[] mouseButtons = new boolean[8];
    
    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) {
            keys[key] = pressed;
        }
    }
    
    public static void setMouseButtonState(int button, boolean pressed) {
        if (button >= 0 && button < mouseButtons.length) {
            mouseButtons[button] = pressed;
        }
    }
    
    public static boolean isKeyDown(int key) {
        if (key >= 0 && key < keys.length) {
            return keys[key];
        }
        return false;
    }
    
    public static boolean isMouseButtonDown(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }
    
    // Key mappings from old LWJGL 2 to GLFW 3
    public static final int KEY_R = GLFW.GLFW_KEY_R;
    public static final int KEY_W = GLFW.GLFW_KEY_W;
    public static final int KEY_S = GLFW.GLFW_KEY_S;
    public static final int KEY_A = GLFW.GLFW_KEY_A;
    public static final int KEY_D = GLFW.GLFW_KEY_D;
    public static final int KEY_SPACE = GLFW.GLFW_KEY_SPACE;
    public static final int KEY_LEFT_SHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
    
    // Mouse buttons
    public static final int MOUSE_LEFT = GLFW.GLFW_MOUSE_BUTTON_LEFT;
    public static final int MOUSE_RIGHT = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
}
