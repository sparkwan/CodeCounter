package com.github.dev.tool.plugin;

import javax.swing.JPanel;

/**
 * Abstract base class for plugin UI panels.
 * Provides common functionality and interface for all plugin user interfaces.
 */
public abstract class PluginPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    protected PluginContext context;

    public PluginPanel(PluginContext context) {
        this.context = context;
        setLayout(new java.awt.BorderLayout());
    }

    /**
     * Initialize the UI components.
     * Called after the panel is created.
     */
    public abstract void initializeUI();

    /**
     * Get localized string from plugin's resource bundle.
     */
    protected String getString(String key) {
        return context.getString(key);
    }

    /**
     * Update all UI texts when locale changes.
     * Subclasses should override this to update their UI components.
     */
    public abstract void updateLocale(java.util.Locale newLocale);

    /**
     * Refresh the UI when theme changes.
     * Subclasses should override this if they need special handling for theme changes.
     */
    public void refreshTheme() {
        // Default: just repaint; subclasses can override for more complex handling
        revalidate();
        repaint();
    }

    /**
     * Clean up resources when the panel is being removed.
     */
    public void dispose() {
        // Override if needed to clean up resources
    }
}

