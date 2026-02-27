package com.github.dev.tool.plugin;

/**
 * Metadata describing a plugin.
 * Contains information about the plugin's identity, version, and requirements.
 */
public class PluginMetadata {
    private final String id;                    // Unique identifier (e.g., "com.github.tools.code-counter")
    private final String name;                  // Display name (e.g., "Code Counter")
    private final String version;               // Version (e.g., "1.0.0")
    private final String description;           // Description
    private final String author;                // Author name
    private final String implementationClass;   // Full class name implementing Plugin interface
    private final String minFrameworkVersion;   // Minimum framework version required

    public PluginMetadata(String id, String name, String version, String description,
                         String author, String implementationClass, String minFrameworkVersion) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
        this.implementationClass = implementationClass;
        this.minFrameworkVersion = minFrameworkVersion;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getImplementationClass() { return implementationClass; }
    public String getMinFrameworkVersion() { return minFrameworkVersion; }

    @Override
    public String toString() {
        return String.format("%s v%s (%s)", name, version, id);
    }
}

