package com.github.dev.tool.plugin.impl;

import com.github.dev.tool.plugin.ThemeManager;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of ThemeManager.
 * Manages light/dark themes using FlatLaf.
 */
public class DefaultThemeManager implements ThemeManager {

    private boolean isDarkTheme = false;
    private List<ThemeChangeListener> listeners = new ArrayList<>();

    @Override
    public void applyTheme(boolean isDark) {
        try {
            if (isDark) {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }

            this.isDarkTheme = isDark;
            notifyListeners(isDark);
        } catch (Exception e) {
            System.err.println("Failed to apply theme: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean isDarkTheme() {
        return isDarkTheme;
    }

    @Override
    public void addThemeChangeListener(ThemeChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeThemeChangeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(boolean isDark) {
        for (ThemeChangeListener listener : listeners) {
            try {
                listener.onThemeChanged(isDark);
            } catch (Exception e) {
                System.err.println("Error in theme change listener: " + e.getMessage());
            }
        }
    }
}
