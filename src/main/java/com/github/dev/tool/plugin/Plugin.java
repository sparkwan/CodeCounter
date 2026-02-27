package com.github.dev.tool.plugin;

/**
 * Base interface for all plugins.
 * Each plugin must implement this interface to be discovered and loaded by the framework.
 */
public interface Plugin {

    /**
     * Returns metadata about this plugin (name, version, description, etc.)
     */
    PluginMetadata getMetadata();

    /**
     * Called when the plugin is being initialized.
     * This is where plugins should set up their resources and register listeners.
     *
     * @param context The plugin context providing access to shared services
     * @throws Exception if initialization fails
     */
    void initialize(PluginContext context) throws Exception;

    /**
     * Called when the plugin is about to be unloaded.
     * This is where plugins should clean up resources.
     */
    void shutdown();

    /**
     * Returns true if the plugin is initialized and ready to use.
     */
    boolean isInitialized();

    /**
     * Get the UI panel for this plugin.
     * The panel should extend PluginPanel for consistent look and feel.
     *
     * @return the UI component representing this plugin's interface
     */
    PluginPanel getPluginPanel();
}

