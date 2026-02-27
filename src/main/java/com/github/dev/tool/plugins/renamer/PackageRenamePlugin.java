package com.github.dev.tool.plugins.renamer;

import com.github.dev.tool.plugin.Plugin;
import com.github.dev.tool.plugin.PluginContext;
import com.github.dev.tool.plugin.PluginMetadata;
import com.github.dev.tool.plugin.PluginPanel;

/**
 * Package Rename Plugin - renames packages/namespaces in source code.
 * This is a template plugin demonstrating batch refactoring capabilities.
 */
public class PackageRenamePlugin implements Plugin {

    private static final String PLUGIN_ID = "com.github.tools.package-rename";
    private static final String PLUGIN_NAME = "Package Rename";
    private static final String PLUGIN_VERSION = "1.0.0";

    private PluginMetadata metadata;
    private PluginContext context;
    private PackageRenamePluginPanel pluginPanel;
    private boolean initialized = false;

    public PackageRenamePlugin() {
        this.metadata = new PluginMetadata(
            PLUGIN_ID,
            PLUGIN_NAME,
            PLUGIN_VERSION,
            "Batch rename packages/namespaces in source code and file paths",
             "Spark Wan",
            "com.github.dev.tool.plugins.renamer.PackageRenamePlugin",
            "1.0.0"
        );
    }

    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void initialize(PluginContext context) throws Exception {
        this.context = context;
        this.pluginPanel = new PackageRenamePluginPanel(context);
        this.pluginPanel.initializeUI();
        this.initialized = true;
    }

    @Override
    public void shutdown() {
        if (pluginPanel != null) {
            pluginPanel.dispose();
        }
        initialized = false;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public PluginPanel getPluginPanel() {
        return pluginPanel;
    }
}

