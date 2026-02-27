package com.github.dev.tool.plugin;

import java.util.Locale;

/**
 * Manages application themes (light/dark mode) and applies them to UI components.
 * Integrates with FlatLaf for modern theme support.
 */
public interface ThemeManager {

    /**
     * Apply a theme (light or dark).
     * @param isDark true for dark theme, false for light theme
     */
    void applyTheme(boolean isDark);

    /**
     * Check if dark theme is currently applied.
     */
    boolean isDarkTheme();

    /**
     * Register a listener to be notified when theme changes.
     */
    void addThemeChangeListener(ThemeChangeListener listener);

    /**
     * Remove a theme change listener.
     */
    void removeThemeChangeListener(ThemeChangeListener listener);

    /**
     * Theme change event listener.
     */
    interface ThemeChangeListener {
        void onThemeChanged(boolean isDark);
    }
}

