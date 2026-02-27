package com.github.dev.tool.plugins.counter;

import com.github.dev.tool.plugin.PluginContext;
import com.github.dev.tool.plugin.PluginPanel;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler.LegendPosition;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Predefined file type templates for quick selection
 */
class FileTypeTemplate {
    public String name;
    public List<String> extensions;
    /** Build/project-specific directories to exclude (VCS and IDE dirs are handled separately) */
    public List<String> buildExcludeDirs;

    /** Common VCS dirs, shared across all templates */
    static final List<String> COMMON_VCS_DIRS = Arrays.asList(".git", ".svn", ".hg", ".bzr", ".cvs");

    /** Common IDE/editor dirs and files, shared across all templates */
    static final List<String> COMMON_IDE_DIRS = Arrays.asList(
            ".idea", ".settings", ".classpath", ".project",".metadata",
            ".vscode", "*.iml",  "nbproject", ".vs");

    public FileTypeTemplate(String name, List<String> buildExcludeDirs, String... exts) {
        this.name = name;
        this.extensions = Arrays.asList(exts);
        this.buildExcludeDirs = buildExcludeDirs;
    }

    // Predefined templates
    static final Map<String, FileTypeTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("Java Web", new FileTypeTemplate("Java Web",
                Arrays.asList("target", "build", ".gradle", "bin", "node_modules"),
                ".java", ".jsp", ".jspx", ".ftl", ".vm", ".html", ".htm", ".css", ".js", ".xml", ".properties", ".yml", ".yaml"));
        TEMPLATES.put("Java Swing/JavaFX", new FileTypeTemplate("Java Swing/JavaFX",
                Arrays.asList("target", "build", ".gradle", "bin", "dist","native-lib"),
                ".java", ".fxml",  ".css",  ".xml", ".properties", ".yml", ".yaml" ));
        TEMPLATES.put("Java Backend", new FileTypeTemplate("Java Backend",
                Arrays.asList("target", "build", ".gradle", "bin"),
                ".java", ".xml", ".properties", ".yml", ".yaml", ".sql"));
        TEMPLATES.put("Frontend", new FileTypeTemplate("Frontend",
                Arrays.asList("node_modules", "dist", "build", ".next", ".nuxt", "coverage", "bower_components"),
                ".html", ".htm", ".css", ".js", ".jsx", ".ts", ".tsx", ".vue", ".scss", ".less"));
        TEMPLATES.put("C++", new FileTypeTemplate("C++",
                Arrays.asList("build", "cmake-build-debug", "cmake-build-release", "out", "Debug", "Release", "x64", "x86"),
                ".cpp", ".cc", ".cxx", ".c", ".h", ".hpp", ".hxx"));
        TEMPLATES.put("PHP", new FileTypeTemplate("PHP",
                Arrays.asList("vendor", "node_modules", "cache", "storage"),
                ".php", ".php3", ".html", ".htm", ".css", ".js", ".json", ".lock", ".env", ".xml", ".twig", ".phtml", ".latte"));
        TEMPLATES.put("Python", new FileTypeTemplate("Python",
                Arrays.asList("__pycache__", ".venv", "venv", "env", ".tox", "dist", "build", ".eggs", "*.egg-info"),
                ".py", ".txt", ".cfg", ".toml", ".ini", ".json", ".yaml", ".yml"));
        TEMPLATES.put("Custom", new FileTypeTemplate("Custom",
                Collections.emptyList(),
                ""));
    }
}

/**
 * UI Panel for the Code Counter Plugin.
 * Provides the interface for selecting folders, file types, and displaying analysis results.
 */
public class CodeCounterPluginPanel extends PluginPanel {
    private static final long serialVersionUID = 1L;

    // UI Components
    private JComboBox<String> folderCombo;
    private JButton browseBtn;

    // Recent folder history
    private static final String PREF_RECENT_FOLDERS = "recent.folders";
    private static final int MAX_RECENT_FOLDERS = 10;
    private static final String HISTORY_SEPARATOR = "\n";
    private JComboBox<String> templateCombo;
    private JLabel templateLabel;
    private JPanel typePanel, optionPanel;
    private JPanel typeListPanel;
    private JPanel customInputPanel;
    private JTextField customExtField;
    private JButton addExtBtn, removeExtBtn;
    private List<JCheckBox> fileTypeCBs;
    private JCheckBox includeBlankCB, includeHeaderCB;
    private JPanel excludeDirPanel;
    private JLabel vcsDirLabel, ideDirLabel, buildDirLabel;
    private JPanel vcsDirListPanel;
    private JPanel ideDirListPanel;
    private JPanel buildDirListPanel;
    private List<JCheckBox> vcsDirCBs;
    private List<JCheckBox> ideDirCBs;
    private List<JCheckBox> buildDirCBs;
    private JButton countBtn, exportBtn, chartBtn;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextArea resultArea;

    // Pagination
    private List<Object[]> allTableRows = new ArrayList<>();
    private int currentPage = 0;
    private int pageSize = 50;
    private JPanel paginationPanel;
    private JButton firstPageBtn, prevPageBtn, nextPageBtn, lastPageBtn;
    private JLabel pageInfoLabel;
    private JComboBox<String> pageSizeCombo;
    private JLabel pageSizeLabel;

    // Core logic
    private CodeCounterCore core;

    // Cached result totals for locale refresh
    private long cachedTotalCode = 0, cachedTotalComment = 0, cachedTotalBlank = 0, cachedTotalTodo = 0;
    private boolean hasResults = false;

    public CodeCounterPluginPanel(PluginContext context) {
        super(context);
        this.core = new CodeCounterCore();
        this.fileTypeCBs = new ArrayList<>();
        this.vcsDirCBs = new ArrayList<>();
        this.ideDirCBs = new ArrayList<>();
        this.buildDirCBs = new ArrayList<>();
    }

    @Override
    public void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: folder selection
        JPanel topPanel = createTopPanel();

        // Middle: file types + options
        JPanel centerPanel = createCenterPanel();

        // Combine top + center into a fixed-height north area
        JPanel northArea = new JPanel(new BorderLayout(0, 6));
        northArea.add(topPanel, BorderLayout.NORTH);
        northArea.add(centerPanel, BorderLayout.CENTER);
        // Constrain the config area height so the result panel gets more space
        centerPanel.setPreferredSize(new Dimension(0, 160));
        centerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Bottom: buttons + results (gets all remaining space)
        JPanel bottomPanel = createBottomPanel();

        // Use a vertical split pane so users can also drag to resize
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northArea, bottomPanel);
        splitPane.setResizeWeight(0.25); // 25% extra space to config, 75% to results
        splitPane.setDividerSize(5);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));

        folderCombo = new JComboBox<>();
        folderCombo.setEditable(true);
        loadRecentFolders();

        browseBtn = new JButton(getString("button.browse"));
        browseBtn.addActionListener(e -> chooseFolder());

        panel.add(folderCombo, BorderLayout.CENTER);
        panel.add(browseBtn, BorderLayout.EAST);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));

        // File type panel
        typePanel = new JPanel(new BorderLayout(0, 8));
        typePanel.setBorder(BorderFactory.createTitledBorder(getString("title.fileTypes")));

        // Template selector dropdown
        JPanel templateRow = new JPanel(new BorderLayout(6, 0));
        templateLabel = new JLabel(getString("dialog.template.label") + ":");
        templateCombo = new JComboBox<>(FileTypeTemplate.TEMPLATES.keySet().toArray(new String[0]));
        templateCombo.setSelectedItem("Java Web");
        templateCombo.addActionListener(e -> {
            String selectedTemplate = (String) templateCombo.getSelectedItem();
            if (selectedTemplate != null && FileTypeTemplate.TEMPLATES.containsKey(selectedTemplate)) {
                applyTemplate(FileTypeTemplate.TEMPLATES.get(selectedTemplate));
            }
        });
        templateRow.add(templateLabel, BorderLayout.WEST);
        templateRow.add(templateCombo, BorderLayout.CENTER);
        typePanel.add(templateRow, BorderLayout.NORTH);

        typeListPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 0));
        JScrollPane typeScroll = new JScrollPane(typeListPanel);
        typeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        typeScroll.setPreferredSize(new Dimension(200, 120));
        typeScroll.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                typeListPanel.revalidate();
            }
        });
        typePanel.add(typeScroll, BorderLayout.CENTER);

        // Custom input panel for adding/removing extensions (shown only for Custom template)
        customInputPanel = new JPanel(new BorderLayout(4, 4));
        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        customExtField = new JTextField();
        customExtField.setToolTipText(getString("dialog.add.prompt"));
        addExtBtn = new JButton(getString("dialog.add"));
        addExtBtn.addActionListener(e -> addCustomExtension());
        // Allow pressing Enter to add
        customExtField.addActionListener(e -> addCustomExtension());
        inputRow.add(customExtField, BorderLayout.CENTER);
        inputRow.add(addExtBtn, BorderLayout.EAST);
        customInputPanel.add(inputRow, BorderLayout.CENTER);
        removeExtBtn = new JButton(getString("dialog.remove"));
        removeExtBtn.addActionListener(e -> removeSelectedExtensions());
        customInputPanel.add(removeExtBtn, BorderLayout.EAST);
        customInputPanel.setVisible(false); // Hidden by default
        typePanel.add(customInputPanel, BorderLayout.SOUTH);

        // Exclude dirs panel — split into VCS (common) and Build (per-template)
        excludeDirPanel = new JPanel(new BorderLayout(0, 4));
        excludeDirPanel.setBorder(BorderFactory.createTitledBorder(getString("title.excludeDirs")));

        JPanel excludeContent = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 0);

        // VCS sub-section (common, always the same)
        vcsDirLabel = new JLabel(getString("title.excludeDirs.vcs"));
        excludeContent.add(vcsDirLabel, gbc);

        gbc.gridy++;
        vcsDirListPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 0));
        // Populate VCS checkboxes (common, never changes with template)
        for (String dir : FileTypeTemplate.COMMON_VCS_DIRS) {
            JCheckBox cb = new JCheckBox(dir, true);
            vcsDirCBs.add(cb);
            vcsDirListPanel.add(cb);
        }
        excludeContent.add(vcsDirListPanel, gbc);

        // IDE sub-section (common, always the same)
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 0, 0);
        ideDirLabel = new JLabel(getString("title.excludeDirs.ide"));
        excludeContent.add(ideDirLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        ideDirListPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 0));
        // Populate IDE checkboxes (common, never changes with template)
        for (String dir : FileTypeTemplate.COMMON_IDE_DIRS) {
            JCheckBox cb = new JCheckBox(dir, true);
            ideDirCBs.add(cb);
            ideDirListPanel.add(cb);
        }
        excludeContent.add(ideDirListPanel, gbc);

        // Build sub-section (template-specific)
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 0, 0);
        buildDirLabel = new JLabel(getString("title.excludeDirs.build"));
        excludeContent.add(buildDirLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 1.0; // push remaining space below
        buildDirListPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 0));
        excludeContent.add(buildDirListPanel, gbc);

        JScrollPane excludeScroll = new JScrollPane(excludeContent);
        excludeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        excludeScroll.setPreferredSize(new Dimension(200, 120));
        // Revalidate content when scroll pane is resized so WrapLayout recalculates wrapping
        excludeScroll.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                excludeContent.revalidate();
            }
        });
        excludeDirPanel.add(excludeScroll, BorderLayout.CENTER);

        // Options panel
        optionPanel = new JPanel(new BorderLayout());
        optionPanel.setBorder(BorderFactory.createTitledBorder(getString("title.options")));

        JPanel optionList = new JPanel();
        optionList.setLayout(new BoxLayout(optionList, BoxLayout.Y_AXIS));
        includeBlankCB = new JCheckBox(getString("option.includeBlank"), false);
        includeHeaderCB = new JCheckBox(getString("option.includeHeader"), true);

        optionList.add(includeBlankCB);
        optionList.add(includeHeaderCB);
        optionList.add(Box.createVerticalGlue());

        JScrollPane optionScroll = new JScrollPane(optionList);
        optionScroll.setPreferredSize(new Dimension(200, 120));
        optionPanel.add(optionScroll, BorderLayout.CENTER);

        panel.add(typePanel);
        panel.add(excludeDirPanel);
        panel.add(optionPanel);

        // Apply default template
        applyTemplate(FileTypeTemplate.TEMPLATES.get("Java Web"));

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        countBtn = new JButton(getString("button.count"));
        countBtn.addActionListener(e -> doCount());

        // Single export button with dropdown menu for format selection
        exportBtn = new JButton(getString("button.exportAs") + " \u25BE");
        exportBtn.addActionListener(e -> {
            JPopupMenu exportMenu = new JPopupMenu();
            JMenuItem csvItem = new JMenuItem(getString("button.export"));
            csvItem.addActionListener(ev -> exportCSV());
            JMenuItem xlsxItem = new JMenuItem(getString("button.exportXlsx"));
            xlsxItem.addActionListener(ev -> exportXLSX());
            JMenuItem pdfItem = new JMenuItem(getString("button.exportPdf"));
            pdfItem.addActionListener(ev -> exportPDF());
            JMenuItem wordItem = new JMenuItem(getString("button.exportWord"));
            wordItem.addActionListener(ev -> exportWord());
            exportMenu.add(csvItem);
            exportMenu.add(xlsxItem);
            exportMenu.add(pdfItem);
            exportMenu.add(wordItem);
            exportMenu.show(exportBtn, 0, exportBtn.getHeight());
        });

        chartBtn = new JButton(getString("button.charts"));
        chartBtn.addActionListener(e -> showCharts());

        buttonPanel.add(countBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(chartBtn);

        // Result table
        tableModel = new DefaultTableModel(new String[] {
            getString("table.header.index"),
            getString("table.header.path"),
            getString("table.header.type"),
            getString("table.header.code"),
            getString("table.header.comment"),
            getString("table.header.blank"),
            getString("table.header.todo")
        }, 0);
        fileTable = new JTable(tableModel);
        applyTableColumnWidths();
        JScrollPane tableScroll = new JScrollPane(fileTable);
        tableScroll.setPreferredSize(new Dimension(800, 200));

        // Pagination controls
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        firstPageBtn = new JButton("\u23EE");
        firstPageBtn.setToolTipText(getString("pagination.first"));
        firstPageBtn.setMargin(new Insets(2, 6, 2, 6));
        firstPageBtn.addActionListener(e -> goToPage(0));

        prevPageBtn = new JButton("\u25C0");
        prevPageBtn.setToolTipText(getString("pagination.prev"));
        prevPageBtn.setMargin(new Insets(2, 6, 2, 6));
        prevPageBtn.addActionListener(e -> goToPage(currentPage - 1));

        nextPageBtn = new JButton("\u25B6");
        nextPageBtn.setToolTipText(getString("pagination.next"));
        nextPageBtn.setMargin(new Insets(2, 6, 2, 6));
        nextPageBtn.addActionListener(e -> goToPage(currentPage + 1));

        lastPageBtn = new JButton("\u23ED");
        lastPageBtn.setToolTipText(getString("pagination.last"));
        lastPageBtn.setMargin(new Insets(2, 6, 2, 6));
        lastPageBtn.addActionListener(e -> goToPage(getTotalPages() - 1));

        pageInfoLabel = new JLabel(getString("pagination.info.empty"));

        pageSizeLabel = new JLabel(getString("pagination.pageSize") + ":");
        pageSizeCombo = new JComboBox<>(new String[]{"20", "50", "100", "200", "500"});
        pageSizeCombo.setSelectedItem(String.valueOf(pageSize));
        pageSizeCombo.addActionListener(e -> {
            pageSize = Integer.parseInt((String) pageSizeCombo.getSelectedItem());
            currentPage = 0;
            showCurrentPage();
        });

        paginationPanel.add(firstPageBtn);
        paginationPanel.add(prevPageBtn);
        paginationPanel.add(pageInfoLabel);
        paginationPanel.add(nextPageBtn);
        paginationPanel.add(lastPageBtn);
        paginationPanel.add(Box.createHorizontalStrut(16));
        paginationPanel.add(pageSizeLabel);
        paginationPanel.add(pageSizeCombo);

        updatePaginationButtons();

        // Table + pagination wrapper
        JPanel tableWithPagination = new JPanel(new BorderLayout());
        tableWithPagination.add(tableScroll, BorderLayout.CENTER);
        tableWithPagination.add(paginationPanel, BorderLayout.SOUTH);

        // Result text
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane textScroll = new JScrollPane(resultArea);
        textScroll.setPreferredSize(new Dimension(800, 120));

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(tableWithPagination, BorderLayout.CENTER);
        panel.add(textScroll, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Apply a file type template by completely rebuilding the type list with all extensions from the template.
     * All extensions are checked by default.
     */
    private void applyTemplate(FileTypeTemplate template) {
        // Clear previous file type checkboxes
        typeListPanel.removeAll();
        fileTypeCBs.clear();

        // Create a new checkbox for each extension in the template
        for (String ext : template.extensions) {
            if (!ext.isEmpty()) {  // Skip empty extensions
                JCheckBox cb = new JCheckBox(ext, true);  // All checked by default
                fileTypeCBs.add(cb);
                typeListPanel.add(cb);
            }
        }

        // Show custom input controls only for the "Custom" template
        boolean isCustom = "Custom".equals(template.name);
        customInputPanel.setVisible(isCustom);

        typeListPanel.revalidate();
        typeListPanel.repaint();

        // Clear previous build exclude dir checkboxes (VCS dirs are not affected)
        buildDirListPanel.removeAll();
        buildDirCBs.clear();

        // Create a new checkbox for each build exclude dir in the template
        for (String dir : template.buildExcludeDirs) {
            if (!dir.isEmpty()) {
                JCheckBox cb = new JCheckBox(dir, true);  // All checked by default
                buildDirCBs.add(cb);
                buildDirListPanel.add(cb);
            }
        }

        buildDirListPanel.revalidate();
        buildDirListPanel.repaint();
    }

    /**
     * Add one or more custom file extensions from the input field.
     * Supports comma/space/semicolon separated input, e.g. ".java, .xml .html;.css"
     * Also accepts glob-style input like "*.java" or just "java"
     */
    private void addCustomExtension() {
        String raw = customExtField.getText().trim();
        if (raw.isEmpty()) return;

        // Split by comma, space, or semicolon
        String[] parts = raw.split("[,;\\s]+");
        for (String part : parts) {
            String ext = part.trim();
            if (ext.isEmpty()) continue;
            // Strip leading wildcard: *.java → .java, * .java → .java
            if (ext.startsWith("*.")) {
                ext = ext.substring(1); // "*.java" → ".java"
            } else if (ext.startsWith("*")) {
                ext = ext.substring(1); // "*java" → "java"
            }
            // Auto-prepend dot if missing
            if (!ext.startsWith(".")) {
                ext = "." + ext;
            }
            // Skip if already exists
            boolean exists = false;
            for (JCheckBox cb : fileTypeCBs) {
                if (cb.getText().equalsIgnoreCase(ext)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                JCheckBox cb = new JCheckBox(ext, true);
                fileTypeCBs.add(cb);
                typeListPanel.add(cb);
            }
        }

        customExtField.setText("");
        typeListPanel.revalidate();
        typeListPanel.repaint();
    }

    /**
     * Remove all checked (selected) extensions from the custom list.
     */
    private void removeSelectedExtensions() {
        List<JCheckBox> toRemove = new ArrayList<>();
        for (JCheckBox cb : fileTypeCBs) {
            if (cb.isSelected()) {
                toRemove.add(cb);
            }
        }
        for (JCheckBox cb : toRemove) {
            fileTypeCBs.remove(cb);
            typeListPanel.remove(cb);
        }
        typeListPanel.revalidate();
        typeListPanel.repaint();
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Start from the currently selected path if available
        String current = getSelectedFolder();
        if (!current.isEmpty()) {
            chooser.setCurrentDirectory(new java.io.File(current));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            addToRecentFolders(path);
        }
    }

    /**
     * Get the currently selected/entered folder path from the combo box.
     */
    private String getSelectedFolder() {
        Object item = folderCombo.getEditor().getItem();
        return item != null ? item.toString().trim() : "";
    }

    /**
     * Load recent folder history from Preferences into the combo box.
     */
    private void loadRecentFolders() {
        java.util.prefs.Preferences prefs = context.getPreferences();
        String saved = prefs.get(PREF_RECENT_FOLDERS, "");
        folderCombo.removeAllItems();
        if (!saved.isEmpty()) {
            String[] folders = saved.split(HISTORY_SEPARATOR);
            for (String folder : folders) {
                String trimmed = folder.trim();
                if (!trimmed.isEmpty()) {
                    folderCombo.addItem(trimmed);
                }
            }
        }
    }

    /**
     * Add a folder path to the recent history (most recent first),
     * update the combo box, and persist to Preferences.
     */
    private void addToRecentFolders(String path) {
        if (path == null || path.trim().isEmpty()) return;
        path = path.trim();

        // Collect current items, remove duplicate if exists
        List<String> folders = new ArrayList<>();
        folders.add(path);
        for (int i = 0; i < folderCombo.getItemCount(); i++) {
            String item = folderCombo.getItemAt(i);
            if (!item.equalsIgnoreCase(path)) {
                folders.add(item);
            }
        }
        // Limit to MAX_RECENT_FOLDERS
        if (folders.size() > MAX_RECENT_FOLDERS) {
            folders = folders.subList(0, MAX_RECENT_FOLDERS);
        }

        // Update combo box
        folderCombo.removeAllItems();
        for (String f : folders) {
            folderCombo.addItem(f);
        }
        folderCombo.setSelectedIndex(0);

        // Persist to Preferences
        java.util.prefs.Preferences prefs = context.getPreferences();
        prefs.put(PREF_RECENT_FOLDERS, String.join(HISTORY_SEPARATOR, folders));
    }

    private void doCount() {
        String rootPath = getSelectedFolder();
        if (rootPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("message.selectFolder"));
            return;
        }

        // Save to recent history
        addToRecentFolders(rootPath);

        // Collect selected file types
        List<String> extensions = new ArrayList<>();
        for (JCheckBox cb : fileTypeCBs) {
            if (cb.isSelected()) {
                String ext = cb.getText().trim();
                if (!ext.isEmpty()) {
                    // Normalize: strip leading wildcard (*.java → .java)
                    if (ext.startsWith("*.")) {
                        ext = ext.substring(1);
                    } else if (ext.startsWith("*")) {
                        ext = ext.substring(1);
                    }
                    if (!ext.startsWith(".")) {
                        ext = "." + ext;
                    }
                    extensions.add(ext);
                }
            }
        }

        // Build exclude list from selected VCS + IDE + build dir checkboxes
        List<String> excludeDirs = new ArrayList<>();
        for (JCheckBox cb : vcsDirCBs) {
            if (cb.isSelected()) {
                String dir = cb.getText().trim();
                if (!dir.isEmpty()) {
                    excludeDirs.add(dir);
                }
            }
        }
        for (JCheckBox cb : ideDirCBs) {
            if (cb.isSelected()) {
                String dir = cb.getText().trim();
                if (!dir.isEmpty()) {
                    excludeDirs.add(dir);
                }
            }
        }
        for (JCheckBox cb : buildDirCBs) {
            if (cb.isSelected()) {
                String dir = cb.getText().trim();
                if (!dir.isEmpty()) {
                    excludeDirs.add(dir);
                }
            }
        }

        // Run the counting logic
        Path root = Paths.get(rootPath);
        List<CodeCounterCore.FileStat> fileStats = core.countLinesWithDetail(root, extensions,
            includeBlankCB.isSelected(), includeHeaderCB.isSelected(), excludeDirs);

        // Update table
        allTableRows.clear();
        tableModel.setRowCount(0);
        long totalCode = 0, totalComment = 0, totalBlank = 0, totalTodo = 0;
        int idx = 1;
        for (CodeCounterCore.FileStat fs : fileStats) {
            allTableRows.add(new Object[] {
                idx++, fs.path, fs.type, fs.codeLines, fs.commentLines, fs.blankLines, fs.todoLines
            });
            totalCode += fs.codeLines;
            totalComment += fs.commentLines;
            totalBlank += fs.blankLines;
            totalTodo += fs.todoLines;
        }

        // Show first page
        currentPage = 0;
        showCurrentPage();

        // Cache totals and update result text
        cachedTotalCode = totalCode;
        cachedTotalComment = totalComment;
        cachedTotalBlank = totalBlank;
        cachedTotalTodo = totalTodo;
        hasResults = true;
        refreshResultAreaText();
    }

    /**
     * Refresh the result area text using cached totals and current locale strings.
     */
    private void refreshResultAreaText() {
        if (!hasResults) return;
        StringBuilder sb = new StringBuilder();
        sb.append("====== ").append(getString("result.title")).append(" ======\n");
        sb.append(getString("result.totalLines")).append(": ").append(cachedTotalCode + cachedTotalComment + cachedTotalBlank).append("\n");
        sb.append(getString("result.codeLines")).append(": ").append(cachedTotalCode).append("\n");
        sb.append(getString("result.commentLines")).append(": ").append(cachedTotalComment).append("\n");
        sb.append(getString("result.blankLines")).append(": ").append(cachedTotalBlank).append("\n");
        sb.append(getString("result.todoLines")).append(": ").append(cachedTotalTodo).append("\n");
        resultArea.setText(sb.toString());
    }

    // ==================== Pagination ====================

    private int getTotalPages() {
        if (allTableRows.isEmpty()) return 1;
        return (int) Math.ceil((double) allTableRows.size() / pageSize);
    }

    private void goToPage(int page) {
        int totalPages = getTotalPages();
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        currentPage = page;
        showCurrentPage();
    }

    private void showCurrentPage() {
        tableModel.setRowCount(0);
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, allTableRows.size());
        for (int i = start; i < end; i++) {
            tableModel.addRow(allTableRows.get(i));
        }
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        int totalPages = getTotalPages();
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;
        firstPageBtn.setEnabled(hasPrev);
        prevPageBtn.setEnabled(hasPrev);
        nextPageBtn.setEnabled(hasNext);
        lastPageBtn.setEnabled(hasNext);

        if (allTableRows.isEmpty()) {
            pageInfoLabel.setText(getString("pagination.info.empty"));
        } else {
            int start = currentPage * pageSize + 1;
            int end = Math.min((currentPage + 1) * pageSize, allTableRows.size());
            String info = String.format(getString("pagination.info"),
                    start, end, allTableRows.size(), currentPage + 1, totalPages);
            pageInfoLabel.setText(info);
        }
    }

    private void exportCSV() {
        if (allTableRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("message.noData"));
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getString("dialog.exportCsv.title"));
        chooser.setSelectedFile(new java.io.File("code_count.csv"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = chooser.getSelectedFile();
        // Ensure .csv extension
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new java.io.File(file.getAbsolutePath() + ".csv");
        }

        try (java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8);
             java.io.PrintWriter pw = new java.io.PrintWriter(osw)) {

            // Write UTF-8 BOM for Excel compatibility
            osw.write('\uFEFF');

            // Write i18n header row
            pw.println(String.join(",",
                    csvQuote(getString("table.header.index")),
                    csvQuote(getString("table.header.path")),
                    csvQuote(getString("table.header.type")),
                    csvQuote(getString("table.header.code")),
                    csvQuote(getString("table.header.comment")),
                    csvQuote(getString("table.header.blank")),
                    csvQuote(getString("table.header.todo"))));

            // Write data rows
            long totalCode = 0, totalComment = 0, totalBlank = 0, totalTodo = 0;
            for (Object[] row : allTableRows) {
                long code = ((Number) row[3]).longValue();
                long comment = ((Number) row[4]).longValue();
                long blank = ((Number) row[5]).longValue();
                long todo = ((Number) row[6]).longValue();
                totalCode += code;
                totalComment += comment;
                totalBlank += blank;
                totalTodo += todo;

                pw.println(String.join(",",
                        csvQuote(row[0].toString()),
                        csvQuote(row[1].toString()),
                        csvQuote(row[2].toString()),
                        String.valueOf(code),
                        String.valueOf(comment),
                        String.valueOf(blank),
                        String.valueOf(todo)));
            }

            // Write summary row
            pw.println(String.join(",",
                    "",
                    csvQuote(getString("result.totalLines")),
                    "",
                    String.valueOf(totalCode),
                    String.valueOf(totalComment),
                    String.valueOf(totalBlank),
                    String.valueOf(totalTodo)));

            JOptionPane.showMessageDialog(this,
                    getString("message.export.success") + "\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    getString("message.export.fail") + " " + ex.getMessage(),
                    getString("dialog.exportCsv.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Quote a CSV field: wrap in double-quotes and escape any internal double-quotes.
     */
    private static String csvQuote(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void exportXLSX() {
        if (allTableRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("message.noData"));
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getString("dialog.exportXlsx.title"));
        chooser.setSelectedFile(new java.io.File("code_count.xlsx"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new java.io.File(file.getAbsolutePath() + ".xlsx");
        }

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            org.apache.poi.xssf.usermodel.XSSFSheet sheet =
                    workbook.createSheet(getString("result.title"));

            // --- Header style ---
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    new org.apache.poi.xssf.usermodel.XSSFColor(
                            new byte[]{(byte) 68, (byte) 114, (byte) 196}, null));
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());

            // --- Summary style ---
            org.apache.poi.xssf.usermodel.XSSFCellStyle summaryStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryStyle.setFont(summaryFont);

            // --- Number style ---
            org.apache.poi.xssf.usermodel.XSSFCellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            // Write header row
            String[] headers = {
                    getString("table.header.index"),
                    getString("table.header.path"),
                    getString("table.header.type"),
                    getString("table.header.code"),
                    getString("table.header.comment"),
                    getString("table.header.blank"),
                    getString("table.header.todo")
            };
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            // Write data rows
            long totalCode = 0, totalComment = 0, totalBlank = 0, totalTodo = 0;
            for (int i = 0; i < allTableRows.size(); i++) {
                Object[] rowData = allTableRows.get(i);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(i + 1);

                // Index
                row.createCell(0).setCellValue(((Number) rowData[0]).intValue());
                // Path
                row.createCell(1).setCellValue(rowData[1].toString());
                // Type
                row.createCell(2).setCellValue(rowData[2].toString());

                // Numeric columns with number style
                long code = ((Number) rowData[3]).longValue();
                long comment = ((Number) rowData[4]).longValue();
                long blank = ((Number) rowData[5]).longValue();
                long todo = ((Number) rowData[6]).longValue();
                totalCode += code;
                totalComment += comment;
                totalBlank += blank;
                totalTodo += todo;

                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(3);
                c3.setCellValue(code);
                c3.setCellStyle(numberStyle);
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(4);
                c4.setCellValue(comment);
                c4.setCellStyle(numberStyle);
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(5);
                c5.setCellValue(blank);
                c5.setCellStyle(numberStyle);
                org.apache.poi.ss.usermodel.Cell c6 = row.createCell(6);
                c6.setCellValue(todo);
                c6.setCellStyle(numberStyle);
            }

            // Write summary row
            int summaryRowIdx = allTableRows.size() + 1;
            org.apache.poi.ss.usermodel.Row sumRow = sheet.createRow(summaryRowIdx);
            org.apache.poi.ss.usermodel.Cell sumLabel = sumRow.createCell(1);
            sumLabel.setCellValue(getString("result.totalLines"));
            sumLabel.setCellStyle(summaryStyle);

            org.apache.poi.ss.usermodel.Cell sc3 = sumRow.createCell(3);
            sc3.setCellValue(totalCode);
            sc3.setCellStyle(summaryStyle);
            org.apache.poi.ss.usermodel.Cell sc4 = sumRow.createCell(4);
            sc4.setCellValue(totalComment);
            sc4.setCellStyle(summaryStyle);
            org.apache.poi.ss.usermodel.Cell sc5 = sumRow.createCell(5);
            sc5.setCellValue(totalBlank);
            sc5.setCellStyle(summaryStyle);
            org.apache.poi.ss.usermodel.Cell sc6 = sumRow.createCell(6);
            sc6.setCellValue(totalTodo);
            sc6.setCellStyle(summaryStyle);

            // Auto-size columns
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
            }
            // Give path column extra width
            int pathWidth = sheet.getColumnWidth(1);
            if (pathWidth < 10000) sheet.setColumnWidth(1, 10000);

            // Freeze header row
            sheet.createFreezePane(0, 1);

            // Auto-filter
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, summaryRowIdx - 1, 0, headers.length - 1));

            // Write file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                workbook.write(fos);
            }

            JOptionPane.showMessageDialog(this,
                    getString("message.export.xlsx.success") + "\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    getString("message.export.fail") + " " + ex.getMessage(),
                    getString("dialog.exportXlsx.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== PDF Export (Apache PDFBox) ====================
    private void exportPDF() {
        if (allTableRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("message.noData"));
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getString("dialog.exportPdf.title"));
        chooser.setSelectedFile(new java.io.File("code_count.pdf"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF (*.pdf)", "pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new java.io.File(file.getAbsolutePath() + ".pdf");
        }

        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {

            // Load a CJK-capable font
            org.apache.pdfbox.pdmodel.font.PDFont cjkFont = null;
            try {
                String windir = System.getenv("windir");
                if (windir != null) {
                    // Try Microsoft YaHei TTF first
                    java.io.File msyhTtf = new java.io.File(windir + "\\Fonts\\msyh.ttf");
                    if (msyhTtf.exists()) {
                        try {
                            cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, msyhTtf);
                        } catch (Exception e) { /* try next */ }
                    }
                    // Try TTC variant (extract first font from collection)
                    if (cjkFont == null) {
                        java.io.File msyhTtc = new java.io.File(windir + "\\Fonts\\msyh.ttc");
                        if (msyhTtc.exists()) {
                            try {
                                org.apache.fontbox.ttf.TrueTypeCollection ttc =
                                        new org.apache.fontbox.ttf.TrueTypeCollection(msyhTtc);
                                // Try known font names in the collection
                                String[] fontNames = {"MicrosoftYaHei", "Microsoft YaHei", "微软雅黑"};
                                for (String name : fontNames) {
                                    try {
                                        org.apache.fontbox.ttf.TrueTypeFont ttf = ttc.getFontByName(name);
                                        if (ttf != null) {
                                            cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, ttf, true);
                                            break;
                                        }
                                    } catch (Exception ignored) { }
                                }
                                // If none matched by name, load the first font from the collection
                                if (cjkFont == null) {
                                    final org.apache.fontbox.ttf.TrueTypeFont[] firstFont = {null};
                                    ttc.processAllFonts(f -> {
                                        if (firstFont[0] == null) firstFont[0] = f;
                                    });
                                    if (firstFont[0] != null) {
                                        cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, firstFont[0], true);
                                    }
                                }
                            } catch (Exception e) { /* try next */ }
                        }
                    }
                    // Try SimSun TTF
                    if (cjkFont == null) {
                        java.io.File simsun = new java.io.File(windir + "\\Fonts\\simsun.ttf");
                        if (simsun.exists()) {
                            try {
                                cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, simsun);
                            } catch (Exception e) { /* try next */ }
                        }
                    }
                    // Try SimSun TTC
                    if (cjkFont == null) {
                        java.io.File simsunTtc = new java.io.File(windir + "\\Fonts\\simsun.ttc");
                        if (simsunTtc.exists()) {
                            try {
                                org.apache.fontbox.ttf.TrueTypeCollection ttc =
                                        new org.apache.fontbox.ttf.TrueTypeCollection(simsunTtc);
                                final org.apache.fontbox.ttf.TrueTypeFont[] firstFont = {null};
                                ttc.processAllFonts(f -> {
                                    if (firstFont[0] == null) firstFont[0] = f;
                                });
                                if (firstFont[0] != null) {
                                    cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, firstFont[0], true);
                                }
                            } catch (Exception e) { /* try next */ }
                        }
                    }
                    // Try NSimSun
                    if (cjkFont == null) {
                        java.io.File nsimsun = new java.io.File(windir + "\\Fonts\\nsimsun.ttf");
                        if (nsimsun.exists()) {
                            try {
                                cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, nsimsun);
                            } catch (Exception e) { /* try next */ }
                        }
                    }
                }
                // Also try on macOS / Linux
                if (cjkFont == null) {
                    String[] fallbackPaths = {
                            "/System/Library/Fonts/STHeiti Light.ttc",       // macOS
                            "/System/Library/Fonts/PingFang.ttc",            // macOS
                            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf", // Linux
                            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"     // Linux
                    };
                    for (String path : fallbackPaths) {
                        java.io.File f = new java.io.File(path);
                        if (f.exists()) {
                            try {
                                if (path.endsWith(".ttc")) {
                                    org.apache.fontbox.ttf.TrueTypeCollection ttc =
                                            new org.apache.fontbox.ttf.TrueTypeCollection(f);
                                    final org.apache.fontbox.ttf.TrueTypeFont[] firstFont = {null};
                                    ttc.processAllFonts(ff -> {
                                        if (firstFont[0] == null) firstFont[0] = ff;
                                    });
                                    if (firstFont[0] != null) {
                                        cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, firstFont[0], true);
                                    }
                                } else {
                                    cjkFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(doc, f);
                                }
                                if (cjkFont != null) break;
                            } catch (Exception ignored) { }
                        }
                    }
                }
            } catch (Exception fontEx) {
                // ignore font loading errors, fallback below
            }

            // Fallback to Helvetica if no CJK font found
            final boolean hasCjkFont = (cjkFont != null);
            org.apache.pdfbox.pdmodel.font.PDFont baseFont =
                    hasCjkFont ? cjkFont : org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
            org.apache.pdfbox.pdmodel.font.PDFont boldFont =
                    hasCjkFont ? cjkFont : org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;

            // Table column config
            String[] headers = {
                    getString("table.header.index"),
                    getString("table.header.path"),
                    getString("table.header.type"),
                    getString("table.header.code"),
                    getString("table.header.comment"),
                    getString("table.header.blank"),
                    getString("table.header.todo")
            };
            // Column widths (landscape A4 = 842 x 595, usable ~802 with margins)
            float pageWidth = 842f;
            float pageHeight = 595f;
            float marginLeft = 20f;
            float marginRight = 20f;
            float marginTop = 30f;
            float marginBottom = 30f;
            float tableWidth = pageWidth - marginLeft - marginRight;
            float[] colRatios = {0.05f, 0.42f, 0.09f, 0.11f, 0.11f, 0.11f, 0.11f};
            float[] colWidths = new float[colRatios.length];
            for (int i = 0; i < colRatios.length; i++) {
                colWidths[i] = tableWidth * colRatios[i];
            }

            float rowHeight = 16f;
            float headerRowHeight = 20f;
            float titleHeight = 30f;
            float fontSize = 7f;
            float headerFontSize = 8f;
            float titleFontSize = 14f;

            // Calculate totals
            long totalCode = 0, totalComment = 0, totalBlank = 0, totalTodo = 0;
            for (Object[] rowData : allTableRows) {
                totalCode += ((Number) rowData[3]).longValue();
                totalComment += ((Number) rowData[4]).longValue();
                totalBlank += ((Number) rowData[5]).longValue();
                totalTodo += ((Number) rowData[6]).longValue();
            }

            // Determine how many data rows fit per page
            float usableHeight = pageHeight - marginTop - marginBottom - titleHeight - headerRowHeight;
            int rowsPerPage = (int) (usableHeight / rowHeight);
            int totalDataRows = allTableRows.size() + 1; // +1 for summary row
            int totalPages = (int) Math.ceil((double) totalDataRows / rowsPerPage);
            if (totalPages < 1) totalPages = 1;

            int dataIdx = 0;
            for (int page = 0; page < totalPages; page++) {
                org.apache.pdfbox.pdmodel.PDPage pdPage = new org.apache.pdfbox.pdmodel.PDPage(
                        new org.apache.pdfbox.pdmodel.common.PDRectangle(pageWidth, pageHeight));
                doc.addPage(pdPage);

                org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                        new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, pdPage);

                float yPos = pageHeight - marginTop;

                // Title (first page only)
                if (page == 0) {
                    cs.beginText();
                    cs.setFont(boldFont, titleFontSize);
                    String titleText = getString("result.title");
                    if (!hasCjkFont) titleText = sanitizeForPdf(titleText, boldFont);
                    float titleWidth = boldFont.getStringWidth(titleText) / 1000f * titleFontSize;
                    cs.newLineAtOffset((pageWidth - titleWidth) / 2f, yPos - titleFontSize);
                    cs.showText(titleText);
                    cs.endText();
                    yPos -= titleHeight;
                }

                // Draw header row
                float xPos = marginLeft;
                // Header background
                cs.setNonStrokingColor(68, 114, 196);
                cs.addRect(xPos, yPos - headerRowHeight, tableWidth, headerRowHeight);
                cs.fill();
                // Header text
                cs.setNonStrokingColor(255, 255, 255);
                cs.beginText();
                cs.setFont(boldFont, headerFontSize);
                for (int c = 0; c < headers.length; c++) {
                    float cellX = marginLeft;
                    for (int k = 0; k < c; k++) cellX += colWidths[k];
                    cs.newLineAtOffset(c == 0 ? cellX + 2f : 0, c == 0 ? yPos - headerRowHeight + 5f : 0);
                    if (c > 0) cs.newLineAtOffset(colWidths[c - 1], 0);
                    String text = truncateText(headers[c], boldFont, headerFontSize, colWidths[c] - 4f);
                    cs.showText(text);
                }
                cs.endText();
                yPos -= headerRowHeight;

                // Draw data rows
                int rowsOnThisPage = Math.min(rowsPerPage, totalDataRows - page * rowsPerPage);
                for (int r = 0; r < rowsOnThisPage; r++) {
                    boolean isSummaryRow = (dataIdx >= allTableRows.size());
                    Object[] rowData;
                    if (isSummaryRow) {
                        rowData = new Object[]{"", getString("result.totalLines"), "",
                                totalCode, totalComment, totalBlank, totalTodo};
                    } else {
                        rowData = allTableRows.get(dataIdx);
                    }

                    // Row background
                    if (isSummaryRow) {
                        cs.setNonStrokingColor(220, 220, 220);
                    } else if (dataIdx % 2 == 1) {
                        cs.setNonStrokingColor(230, 237, 247);
                    } else {
                        cs.setNonStrokingColor(255, 255, 255);
                    }
                    cs.addRect(marginLeft, yPos - rowHeight, tableWidth, rowHeight);
                    cs.fill();

                    // Row text
                    cs.setNonStrokingColor(0, 0, 0);
                    cs.beginText();
                    org.apache.pdfbox.pdmodel.font.PDFont rowFont = isSummaryRow ? boldFont : baseFont;
                    cs.setFont(rowFont, fontSize);
                    for (int c = 0; c < rowData.length; c++) {
                        float cellX = marginLeft;
                        for (int k = 0; k < c; k++) cellX += colWidths[k];
                        cs.newLineAtOffset(c == 0 ? cellX + 2f : 0, c == 0 ? yPos - rowHeight + 4f : 0);
                        if (c > 0) cs.newLineAtOffset(colWidths[c - 1], 0);
                        String cellText = String.valueOf(rowData[c]);
                        cellText = truncateText(cellText, rowFont, fontSize, colWidths[c] - 4f);
                        cs.showText(cellText);
                    }
                    cs.endText();

                    yPos -= rowHeight;
                    dataIdx++;
                    if (isSummaryRow) break; // summary is always last
                }

                // Draw table grid lines
                cs.setStrokingColor(180, 180, 180);
                cs.setLineWidth(0.5f);
                float gridTop = (page == 0)
                        ? pageHeight - marginTop - titleHeight
                        : pageHeight - marginTop;
                float gridBottom = yPos;
                // Horizontal lines
                float lineY = gridTop;
                cs.moveTo(marginLeft, lineY);
                cs.lineTo(marginLeft + tableWidth, lineY);
                cs.stroke();
                lineY -= headerRowHeight;
                cs.moveTo(marginLeft, lineY);
                cs.lineTo(marginLeft + tableWidth, lineY);
                cs.stroke();
                int drawnRows = rowsOnThisPage;
                float tempY = lineY;
                for (int rr = 0; rr < drawnRows; rr++) {
                    tempY -= rowHeight;
                    cs.moveTo(marginLeft, tempY);
                    cs.lineTo(marginLeft + tableWidth, tempY);
                    cs.stroke();
                }
                // Vertical lines
                float vx = marginLeft;
                for (int c = 0; c <= colWidths.length; c++) {
                    cs.moveTo(vx, gridTop);
                    cs.lineTo(vx, gridBottom);
                    cs.stroke();
                    if (c < colWidths.length) vx += colWidths[c];
                }

                cs.close();
            }

            doc.save(file);

            JOptionPane.showMessageDialog(this,
                    getString("message.export.pdf.success") + "\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    getString("message.export.fail") + " " + ex.getMessage(),
                    getString("dialog.exportPdf.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Truncate text to fit within maxWidth using the given font/size.
     * Appends "..." if truncated. Sanitizes non-encodable characters first.
     */
    private static String truncateText(String text, org.apache.pdfbox.pdmodel.font.PDFont font,
                                       float fontSize, float maxWidth) {
        try {
            if (text == null || text.isEmpty()) return "";
            // Pre-sanitize: replace characters that the font cannot encode
            String safe = sanitizeForPdf(text, font);
            float width = font.getStringWidth(safe) / 1000f * fontSize;
            if (width <= maxWidth) return safe;
            // Binary search for fitting length
            for (int len = safe.length() - 1; len > 0; len--) {
                String truncated = safe.substring(0, len) + "...";
                float tw = font.getStringWidth(truncated) / 1000f * fontSize;
                if (tw <= maxWidth) return truncated;
            }
            return "...";
        } catch (Exception e) {
            return "?";
        }
    }

    /**
     * Remove characters that the PDF font cannot encode.
     */
    private static String sanitizeForPdf(String text, org.apache.pdfbox.pdmodel.font.PDFont font) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            try {
                font.encode(String.valueOf(ch));
                sb.append(ch);
            } catch (Exception e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    // ==================== Word (DOCX) Export ====================
    private void exportWord() {
        if (allTableRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("message.noData"));
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(getString("dialog.exportWord.title"));
        chooser.setSelectedFile(new java.io.File("code_count.docx"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Word (*.docx)", "docx"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".docx")) {
            file = new java.io.File(file.getAbsolutePath() + ".docx");
        }

        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {

            // Title
            org.apache.poi.xwpf.usermodel.XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(getString("result.title"));
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.addBreak();

            // Table headers
            String[] headers = {
                    getString("table.header.index"),
                    getString("table.header.path"),
                    getString("table.header.type"),
                    getString("table.header.code"),
                    getString("table.header.comment"),
                    getString("table.header.blank"),
                    getString("table.header.todo")
            };

            int rows = allTableRows.size() + 2; // header + data + summary
            org.apache.poi.xwpf.usermodel.XWPFTable table =
                    document.createTable(rows, headers.length);
            table.setWidth("100%");

            // Style header row
            org.apache.poi.xwpf.usermodel.XWPFTableRow headerRow = table.getRow(0);
            for (int c = 0; c < headers.length; c++) {
                org.apache.poi.xwpf.usermodel.XWPFTableCell cell = headerRow.getCell(c);
                cell.setColor("4472C4");
                org.apache.poi.xwpf.usermodel.XWPFParagraph p = cell.getParagraphArray(0);
                p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
                org.apache.poi.xwpf.usermodel.XWPFRun run = p.createRun();
                run.setText(headers[c]);
                run.setBold(true);
                run.setFontSize(9);
                run.setColor("FFFFFF");
            }

            // Data rows
            long totalCode = 0, totalComment = 0, totalBlank = 0, totalTodo = 0;
            for (int i = 0; i < allTableRows.size(); i++) {
                Object[] rowData = allTableRows.get(i);
                long code = ((Number) rowData[3]).longValue();
                long comment = ((Number) rowData[4]).longValue();
                long blank = ((Number) rowData[5]).longValue();
                long todo = ((Number) rowData[6]).longValue();
                totalCode += code; totalComment += comment; totalBlank += blank; totalTodo += todo;

                org.apache.poi.xwpf.usermodel.XWPFTableRow row = table.getRow(i + 1);
                // Alternating row colors
                String rowColor = (i % 2 == 1) ? "E6EDF7" : "FFFFFF";
                String[] vals = {
                        String.valueOf(rowData[0]),
                        String.valueOf(rowData[1]),
                        String.valueOf(rowData[2]),
                        String.valueOf(code), String.valueOf(comment),
                        String.valueOf(blank), String.valueOf(todo)
                };
                for (int c = 0; c < vals.length; c++) {
                    org.apache.poi.xwpf.usermodel.XWPFTableCell cell = row.getCell(c);
                    cell.setColor(rowColor);
                    org.apache.poi.xwpf.usermodel.XWPFParagraph p = cell.getParagraphArray(0);
                    if (c >= 3) p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT);
                    org.apache.poi.xwpf.usermodel.XWPFRun run = p.createRun();
                    run.setText(vals[c]);
                    run.setFontSize(8);
                }
            }

            // Summary row
            int sumIdx = allTableRows.size() + 1;
            org.apache.poi.xwpf.usermodel.XWPFTableRow sumRow = table.getRow(sumIdx);
            for (int c = 0; c < headers.length; c++) {
                org.apache.poi.xwpf.usermodel.XWPFTableCell cell = sumRow.getCell(c);
                cell.setColor("DCDCDC");
                org.apache.poi.xwpf.usermodel.XWPFParagraph p = cell.getParagraphArray(0);
                org.apache.poi.xwpf.usermodel.XWPFRun run = p.createRun();
                run.setBold(true);
                run.setFontSize(9);
                switch (c) {
                    case 1: run.setText(getString("result.totalLines")); break;
                    case 3: run.setText(String.valueOf(totalCode));
                            p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT); break;
                    case 4: run.setText(String.valueOf(totalComment));
                            p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT); break;
                    case 5: run.setText(String.valueOf(totalBlank));
                            p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT); break;
                    case 6: run.setText(String.valueOf(totalTodo));
                            p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT); break;
                    default: run.setText(""); break;
                }
            }

            // Write file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                document.write(fos);
            }

            JOptionPane.showMessageDialog(this,
                    getString("message.export.word.success") + "\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    getString("message.export.fail") + " " + ex.getMessage(),
                    getString("dialog.exportWord.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCharts() {
        if (allTableRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, getString("message.noData"));
            return;
        }

        // Collect data from all rows (not just current page)
        List<String> filePaths = new ArrayList<>();
        List<Number> codeList = new ArrayList<>();
        List<Number> commentList = new ArrayList<>();
        List<Number> blankList = new ArrayList<>();
        List<Number> todoList = new ArrayList<>();
        long totalCode = 0, totalComment = 0, totalBlank = 0, totalTodo = 0;

        for (Object[] rowData : allTableRows) {
            // Use short file name for readability
            String fullPath = rowData[1].toString();
            String shortName = fullPath;
            int sep = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
            if (sep >= 0) shortName = fullPath.substring(sep + 1);
            filePaths.add(shortName);

            long code = ((Number) rowData[3]).longValue();
            long comment = ((Number) rowData[4]).longValue();
            long blank = ((Number) rowData[5]).longValue();
            long todo = ((Number) rowData[6]).longValue();
            codeList.add(code);
            commentList.add(comment);
            blankList.add(blank);
            todoList.add(todo);
            totalCode += code;
            totalComment += comment;
            totalBlank += blank;
            totalTodo += todo;
        }

        // --- Bar Chart: per-file comparison ---
        CategoryChart barChart = new CategoryChartBuilder()
                .width(800).height(500)
                .title(getString("chart.title"))
                .xAxisTitle(getString("chart.xAxisTitle"))
                .yAxisTitle(getString("chart.yAxisTitle"))
                .build();

        barChart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        barChart.getStyler().setXAxisLabelRotation(45);
        barChart.getStyler().setPlotGridLinesVisible(true);
        barChart.getStyler().setToolTipsEnabled(true);

        // Limit to top 20 files by total lines to keep chart readable
        int maxFiles = Math.min(filePaths.size(), 20);
        List<String> displayPaths = filePaths.subList(0, maxFiles);
        List<Number> displayCode = codeList.subList(0, maxFiles);
        List<Number> displayComment = commentList.subList(0, maxFiles);
        List<Number> displayBlank = blankList.subList(0, maxFiles);
        List<Number> displayTodo = todoList.subList(0, maxFiles);

        barChart.addSeries(getString("chart.series.code"), displayPaths, displayCode);
        barChart.addSeries(getString("chart.series.comment"), displayPaths, displayComment);
        barChart.addSeries(getString("chart.series.blank"), displayPaths, displayBlank);
        barChart.addSeries(getString("chart.series.todo"), displayPaths, displayTodo);

        // --- Pie Chart: overall summary ---
        PieChart pieChart = new PieChartBuilder()
                .width(500).height(400)
                .title(getString("chart.pie.title"))
                .build();

        pieChart.getStyler().setLegendPosition(LegendPosition.OutsideS);
        pieChart.getStyler().setToolTipsEnabled(true);
        pieChart.getStyler().setPlotContentSize(0.8);

        pieChart.addSeries(getString("chart.series.code"), totalCode);
        pieChart.addSeries(getString("chart.series.comment"), totalComment);
        pieChart.addSeries(getString("chart.series.blank"), totalBlank);
        pieChart.addSeries(getString("chart.series.todo"), totalTodo);

        // --- Show in dialog with tabs ---
        JFrame chartFrame = new JFrame(getString("chart.window.title"));
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chartFrame.setSize(900, 600);
        chartFrame.setLocationRelativeTo(this);

        JTabbedPane tabs = new JTabbedPane();

        // Bar chart tab
        JPanel barPanel = new XChartPanel<>(barChart);
        tabs.addTab(getString("chart.title"), barPanel);

        // Pie chart tab
        JPanel piePanel = new XChartPanel<>(pieChart);
        tabs.addTab(getString("chart.pie.title"), piePanel);

        // By-type bar chart tab
        JPanel byTypePanel = createByTypeBarChart();
        if (byTypePanel != null) {
            tabs.addTab(getString("chart.tab.byType"), byTypePanel);
        }

        chartFrame.getContentPane().add(tabs);
        chartFrame.setVisible(true);
    }

    /**
     * Create a bar chart grouped by file type (extension).
     */
    private JPanel createByTypeBarChart() {
        // Aggregate data by file type from all rows
        Map<String, long[]> typeMap = new LinkedHashMap<>();
        for (Object[] rowData : allTableRows) {
            String type = rowData[2].toString();
            long code = ((Number) rowData[3]).longValue();
            long comment = ((Number) rowData[4]).longValue();
            long blank = ((Number) rowData[5]).longValue();
            long todo = ((Number) rowData[6]).longValue();

            long[] counts = typeMap.computeIfAbsent(type, k -> new long[4]);
            counts[0] += code;
            counts[1] += comment;
            counts[2] += blank;
            counts[3] += todo;
        }

        if (typeMap.isEmpty()) return null;

        List<String> types = new ArrayList<>(typeMap.keySet());
        List<Number> codeSums = new ArrayList<>();
        List<Number> commentSums = new ArrayList<>();
        List<Number> blankSums = new ArrayList<>();
        List<Number> todoSums = new ArrayList<>();

        for (String type : types) {
            long[] c = typeMap.get(type);
            codeSums.add(c[0]);
            commentSums.add(c[1]);
            blankSums.add(c[2]);
            todoSums.add(c[3]);
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(500)
                .title(getString("chart.tab.byType"))
                .xAxisTitle(getString("table.header.type"))
                .yAxisTitle(getString("chart.yAxisTitle"))
                .build();

        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setPlotGridLinesVisible(true);
        chart.getStyler().setToolTipsEnabled(true);

        chart.addSeries(getString("chart.series.code"), types, codeSums);
        chart.addSeries(getString("chart.series.comment"), types, commentSums);
        chart.addSeries(getString("chart.series.blank"), types, blankSums);
        chart.addSeries(getString("chart.series.todo"), types, todoSums);

        return new XChartPanel<>(chart);
    }

    /**
     * Apply preferred/max column widths to the result table.
     * No(40), Path(flex), Type(60), Code/Comment/Blank/TODO(70 each).
     */
    private void applyTableColumnWidths() {
        fileTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        javax.swing.table.TableColumnModel cm = fileTable.getColumnModel();
        // Col 0: No
        cm.getColumn(0).setPreferredWidth(40);
        cm.getColumn(0).setMaxWidth(50);
        // Col 1: File Path (gets remaining space)
        cm.getColumn(1).setPreferredWidth(400);
        // Col 2: Type
        cm.getColumn(2).setPreferredWidth(60);
        cm.getColumn(2).setMaxWidth(80);
        // Col 3: Code, Col 6: TODO
        cm.getColumn(3).setPreferredWidth(70);
        cm.getColumn(3).setMaxWidth(90);
        cm.getColumn(6).setPreferredWidth(70);
        cm.getColumn(6).setMaxWidth(90);
        // Col 4: Comment, Col 5: Blank (wider for title visibility)
        cm.getColumn(4).setPreferredWidth(105);
        cm.getColumn(4).setMaxWidth(125);
        cm.getColumn(5).setPreferredWidth(90);
        cm.getColumn(5).setMaxWidth(110);
    }

    @Override
    public void updateLocale(Locale newLocale) {
        // Update button texts
        browseBtn.setText(getString("button.browse"));
        countBtn.setText(getString("button.count"));
        exportBtn.setText(getString("button.exportAs") + " \u25BE");
        chartBtn.setText(getString("button.charts"));

        // Update template label
        templateLabel.setText(getString("dialog.template.label") + ":");

        // Update custom input panel
        addExtBtn.setText(getString("dialog.add"));
        removeExtBtn.setText(getString("dialog.remove"));
        customExtField.setToolTipText(getString("dialog.add.prompt"));

        // Update panel titled borders
        typePanel.setBorder(BorderFactory.createTitledBorder(getString("title.fileTypes")));
        excludeDirPanel.setBorder(BorderFactory.createTitledBorder(getString("title.excludeDirs")));
        vcsDirLabel.setText(getString("title.excludeDirs.vcs"));
        ideDirLabel.setText(getString("title.excludeDirs.ide"));
        buildDirLabel.setText(getString("title.excludeDirs.build"));
        optionPanel.setBorder(BorderFactory.createTitledBorder(getString("title.options")));

        // Update checkbox texts
        includeBlankCB.setText(getString("option.includeBlank"));
        includeHeaderCB.setText(getString("option.includeHeader"));

        // Update table headers
        tableModel.setColumnIdentifiers(new Object[] {
            getString("table.header.index"),
            getString("table.header.path"),
            getString("table.header.type"),
            getString("table.header.code"),
            getString("table.header.comment"),
            getString("table.header.blank"),
            getString("table.header.todo")
        });
        applyTableColumnWidths();

        // Update pagination labels
        firstPageBtn.setToolTipText(getString("pagination.first"));
        prevPageBtn.setToolTipText(getString("pagination.prev"));
        nextPageBtn.setToolTipText(getString("pagination.next"));
        lastPageBtn.setToolTipText(getString("pagination.last"));
        pageSizeLabel.setText(getString("pagination.pageSize") + ":");
        updatePaginationButtons();

        // Update result area text (bottom summary)
        refreshResultAreaText();

        // Refresh UI
        fileTable.revalidate();
        fileTable.repaint();
        this.revalidate();
        this.repaint();
    }
}

