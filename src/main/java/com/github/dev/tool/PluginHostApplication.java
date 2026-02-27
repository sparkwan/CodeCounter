package com.github.dev.tool;

import com.github.dev.tool.plugin.*;
import com.github.dev.tool.plugin.impl.DefaultPluginManager;
import com.github.dev.tool.plugin.impl.DefaultThemeManager;
import com.github.dev.tool.plugin.impl.DefaultLocalizationManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Main application frame that hosts all plugins.
 * Provides a tabbed interface for switching between plugins.
 */
public class PluginHostApplication extends JFrame {
    private static final long serialVersionUID = 1L;

    private Preferences appPreferences;
    private PluginManager pluginManager;
    private ThemeManager themeManager;
    private LocalizationManager localizationManager;
    private ResourceBundle bundle;

    private JTabbedPane pluginTabs;
    private Map<String, PluginPanel> pluginPanels = new LinkedHashMap<>();

    // Menu components for dynamic update
    private JMenuBar menuBar;
    private JMenu fileMenu, viewMenu, toolsMenu, helpMenu, languageMenu;
    private JMenuItem exitItem, pluginManagerItem, aboutItem, licenseItem;
    private JCheckBoxMenuItem darkThemeItem;
    private ButtonGroup languageGroup;
    private java.util.List<JRadioButtonMenuItem> languageMenuItems = new ArrayList<>();
    private JLabel statusLabel, pluginCountLabel;
    private Locale[] supportedLocales = {
        Locale.ENGLISH,
        Locale.SIMPLIFIED_CHINESE,
        Locale.TRADITIONAL_CHINESE,
        Locale.JAPANESE,
        new Locale("es"),
        Locale.GERMAN,
        Locale.FRENCH,
        new Locale("pt")
    };

    private static final String PREF_DARK = "app.theme.dark";
    private static final String PREF_LOCALE = "app.locale";

    public PluginHostApplication() {
        // Initialize preferences
        appPreferences = Preferences.userNodeForPackage(this.getClass());

        // Create theme manager
        themeManager = new DefaultThemeManager();

        // Create localization manager
        localizationManager = new DefaultLocalizationManager();

        // Determine initial locale: saved preference > OS default > English
        Locale initialLocale = resolveInitialLocale();
        localizationManager.setLocale(initialLocale);

        // Create plugin manager
        pluginManager = new DefaultPluginManager(themeManager, localizationManager, appPreferences);

        // Apply saved theme
        boolean isDark = appPreferences.getBoolean(PREF_DARK, false);
        themeManager.applyTheme(isDark);

        // Setup UI
        setupUI();
    }

    /**
     * Resolve the initial locale:
     * 1. If user has previously saved a preference, use that.
     * 2. Otherwise match the OS default locale against supported languages.
     * 3. If no match, fall back to English.
     */
    private Locale resolveInitialLocale() {
        String saved = appPreferences.get(PREF_LOCALE, null);
        if (saved != null) {
            return Locale.forLanguageTag(saved);
        }
        // No saved preference — match OS locale
        Locale osLocale = Locale.getDefault();
        for (Locale supported : supportedLocales) {
            if (localeMatches(osLocale, supported)) {
                return supported;
            }
        }
        return Locale.ENGLISH;
    }

    private void setupUI() {
        // Load resource bundle for application
        bundle = loadApplicationResourceBundle();

        setTitle(getString("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Load and set icon
        try {
            BufferedImage icon = loadIcon();
            if (icon != null) {
                setIconImage(icon);
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + e.getMessage());
        }

        // Create menu bar
        menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Create tabbed pane for plugins
        pluginTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

        // Load and initialize plugins
        try {
            pluginManager.loadPlugins();

            for (Plugin plugin : pluginManager.getLoadedPlugins()) {
                if (pluginManager.isPluginEnabled(plugin.getMetadata().getId())) {
                    try {
                        pluginManager.initializePlugin(plugin);

                        PluginPanel panel = plugin.getPluginPanel();
                        if (panel != null) {
                            pluginTabs.addTab(plugin.getMetadata().getName(), panel);
                            pluginPanels.put(plugin.getMetadata().getId(), panel);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to initialize plugin " + plugin.getMetadata().getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load plugins: " + e.getMessage());
            e.printStackTrace();
        }

        // Add tabs to frame
        getContentPane().add(pluginTabs, BorderLayout.CENTER);

        // Add status bar
        JPanel statusBar = createStatusBar();
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private ResourceBundle loadApplicationResourceBundle() {
        try {
            Locale currentLocale = localizationManager.getCurrentLocale();
            // Clear cache to ensure locale switch takes effect
            ResourceBundle.clearCache();
            return ResourceBundle.getBundle("i18n.strings", currentLocale,
                new DefaultPluginManager.UTF8Control());
        } catch (Exception e) {
            System.err.println("Failed to load resource bundle: " + e.getMessage());
            // Return fallback bundle
            return new ResourceBundle() {
                @Override
                protected Object handleGetObject(String key) {
                    return key;
                }

                @Override
                public Enumeration<String> getKeys() {
                    return Collections.emptyEnumeration();
                }
            };
        }
    }

    private String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    private void updateMenuTexts() {
        fileMenu.setText(getString("menu.file"));
        exitItem.setText(getString("menu.exit"));
        viewMenu.setText(getString("menu.view"));
        darkThemeItem.setText(getString("menu.darkTheme"));
        languageMenu.setText(getString("menu.language"));
        toolsMenu.setText(getString("menu.tools"));
        pluginManagerItem.setText(getString("menu.pluginManager"));
        helpMenu.setText(getString("menu.help"));
        aboutItem.setText(getString("menu.about"));
        licenseItem.setText(getString("menu.license"));
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        // File menu
        fileMenu = new JMenu(getString("menu.file"));
        exitItem = new JMenuItem(getString("menu.exit"));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        bar.add(fileMenu);

        // View menu
        viewMenu = new JMenu(getString("menu.view"));

        // Theme submenu
        darkThemeItem = new JCheckBoxMenuItem(getString("menu.darkTheme"));
        darkThemeItem.setSelected(appPreferences.getBoolean(PREF_DARK, false));
        darkThemeItem.addActionListener(e -> {
            boolean isDark = darkThemeItem.isSelected();
            appPreferences.putBoolean(PREF_DARK, isDark);
            themeManager.applyTheme(isDark);
            SwingUtilities.updateComponentTreeUI(this);
        });
        viewMenu.add(darkThemeItem);
        viewMenu.addSeparator();

        // Language submenu
        languageMenu = new JMenu(getString("menu.language"));
        languageGroup = new ButtonGroup();
        languageMenuItems.clear();

        String[] languages = {
            "English",
            "\u7B80\u4F53\u4E2D\u6587",
            "\u7E41\u9AD4\u4E2D\u6587",
            "\u65E5\u672C\u8A9E",
            "Espa\u00F1ol",
            "Deutsch",
            "Fran\u00E7ais",
            "Portugu\u00EAs"
        };

        // Get current locale
        Locale currentLocale = localizationManager.getCurrentLocale();

        for (int i = 0; i < languages.length; i++) {
            final int index = i;
            JRadioButtonMenuItem langItem = new JRadioButtonMenuItem(languages[i]);

            // Set selected based on current locale
            if (localeMatches(currentLocale, supportedLocales[i])) {
                langItem.setSelected(true);
            }

            langItem.addActionListener(e -> switchLanguage(supportedLocales[index]));
            languageGroup.add(langItem);
            languageMenu.add(langItem);
            languageMenuItems.add(langItem);  // Store for later update
        }

        viewMenu.add(languageMenu);
        bar.add(viewMenu);

        // Tools menu
        toolsMenu = new JMenu(getString("menu.tools"));
        pluginManagerItem = new JMenuItem(getString("menu.pluginManager"));
        pluginManagerItem.addActionListener(e -> openPluginManager());
        toolsMenu.add(pluginManagerItem);
        bar.add(toolsMenu);

        // Help menu
        helpMenu = new JMenu(getString("menu.help"));
        aboutItem = new JMenuItem(getString("menu.about"));
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        licenseItem = new JMenuItem(getString("menu.license"));
        licenseItem.addActionListener(e -> showLicense());
        helpMenu.add(licenseItem);
        bar.add(helpMenu);

        return bar;
    }

    private void switchLanguage(Locale locale) {
        // Save locale preference
        appPreferences.put(PREF_LOCALE, locale.toLanguageTag());

        // Notify localization manager first so getString() uses new locale
        localizationManager.setLocale(locale);

        // Reload resource bundle with new locale
        bundle = loadApplicationResourceBundle();

        // Update menu texts immediately
        updateMenuTexts();

        // Update language selection in menu
        updateLanguageSelection(locale);

        // Update all plugin panels and tab titles
        int tabIndex = 0;
        for (Plugin plugin : pluginManager.getLoadedPlugins()) {
            String pluginId = plugin.getMetadata().getId();
            PluginPanel panel = pluginPanels.get(pluginId);
            if (panel != null) {
                panel.updateLocale(locale);
                pluginTabs.setTitleAt(tabIndex, plugin.getMetadata().getName());
                tabIndex++;
            }
        }

        // Update window title
        setTitle(getString("app.title"));

        // Update status bar
        statusLabel.setText(getString("status.ready"));
        pluginCountLabel.setText(MessageFormat.format(getString("status.plugins"), pluginPanels.size()));

        // Refresh layout without disrupting window chrome
        getContentPane().revalidate();
        getContentPane().repaint();
        menuBar.revalidate();
        menuBar.repaint();
    }

    private void updateLanguageSelection(Locale locale) {
        for (int i = 0; i < languageMenuItems.size(); i++) {
            JRadioButtonMenuItem item = languageMenuItems.get(i);
            item.setSelected(localeMatches(locale, supportedLocales[i]));
        }
    }

    /**
     * Check if two locales match. For locales with the same language (e.g. zh_CN vs zh_TW),
     * also compare the country code.
     */
    private boolean localeMatches(Locale a, Locale b) {
        if (!a.getLanguage().equals(b.getLanguage())) return false;
        // If supported locale has a country (e.g. zh_CN, zh_TW), compare country too
        if (!b.getCountry().isEmpty()) {
            return a.getCountry().equals(b.getCountry());
        }
        return true;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel(getString("status.ready"));
        statusBar.add(statusLabel, BorderLayout.WEST);

        pluginCountLabel = new JLabel(MessageFormat.format(getString("status.plugins"), pluginPanels.size()));
        statusBar.add(pluginCountLabel, BorderLayout.EAST);

        return statusBar;
    }

    private void openPluginManager() {
        JDialog dialog = new JDialog(this, "Plugin Manager", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // List of plugins
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Plugin plugin : pluginManager.getLoadedPlugins()) {
            listModel.addElement(plugin.getMetadata().toString());
        }

        JList<String> pluginList = new JList<>(listModel);
        panel.add(new JScrollPane(pluginList), BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(panel);
        dialog.setVisible(true);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            getString("dialog.about.content"),
            getString("dialog.about.title"),
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLicense() {
        String licenseText = getString("dialog.license.content");
        JTextArea textArea = new JTextArea(licenseText);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(550, 400));
        JOptionPane.showMessageDialog(this,
            scrollPane,
            getString("dialog.license.title"),
            JOptionPane.INFORMATION_MESSAGE);
    }

    private BufferedImage loadIcon() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/icons/app_icon.png")) {
            if (is != null) {
                return javax.imageio.ImageIO.read(is);
            }
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PluginHostApplication app = new PluginHostApplication();
            app.setVisible(true);
        });
    }
}

