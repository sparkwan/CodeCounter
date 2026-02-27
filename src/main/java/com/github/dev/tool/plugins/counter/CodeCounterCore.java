package com.github.dev.tool.plugins.counter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core logic for counting lines of code in files.
 *
 * New features added:
 * - Per-file breakdown: code lines, comment lines, blank lines, import lines, TODO lines
 * - Ability to exclude directories (e.g. target, build, .git) when walking the tree
 */
public class CodeCounterCore {

    /**
     * Represents file statistics including path, type, and detailed line counts.
     * Supported line types: Code (including imports), Comment, Blank, TODO
     */
    public static class FileStat {
        public String path;   // File path
        public String type;   // File type (extension)
        public long lines;    // Effective total lines (depends on include flags)

        // Detailed breakdown (raw counts)
        public long codeLines;      // Code lines (includes import statements)
        public long commentLines;   // Comment lines
        public long blankLines;     // Blank lines
        public long todoLines;      // Lines containing TODO

        /**
         * Constructs a FileStat with detailed counts.
         *
         * @param path the file path
         * @param type the file extension
         * @param codeLines number of code lines (including imports)
         * @param commentLines number of comment lines
         * @param blankLines number of blank lines
         * @param todoLines number of lines containing TODO
         */
        public FileStat(String path, String type, long codeLines, long commentLines, long blankLines, long todoLines) {
            this.path = path;
            this.type = type;
            this.codeLines = codeLines;
            this.commentLines = commentLines;
            this.blankLines = blankLines;
            this.todoLines = todoLines;
            this.lines = codeLines + commentLines + blankLines;
        }

        /**
         * Sets the effective total lines according to include flags.
         */
        public void setEffectiveLines(boolean includeBlank, boolean includeHeader) {
            long total = 0;
            total += codeLines;
            if (includeBlank) total += blankLines;
            if (includeHeader) total += commentLines;
            this.lines = total;
        }
    }

    /**
     * Checks if a file matches the selected extensions.
     *
     * @param path the file path
     * @param extensions the list of extensions to match (e.g. ".java", "*.java", "java")
     * @return true if the file matches one of the extensions, false otherwise
     */
    private boolean matchExtension(Path path, List<String> extensions) {
        String name = path.getFileName().toString().toLowerCase();
        for (String ext : extensions) {
            String normalized = ext.trim().toLowerCase();
            // Normalize: *.java → .java, *java → java → .java
            if (normalized.startsWith("*.")) {
                normalized = normalized.substring(1);
            } else if (normalized.startsWith("*")) {
                normalized = normalized.substring(1);
            }
            if (!normalized.startsWith(".")) {
                normalized = "." + normalized;
            }
            if (name.endsWith(normalized)) return true;
        }
        return false;
    }

    /**
     * Retrieves the file extension.
     *
     * @param path the file path
     * @return the file extension, or an empty string if none exists
     */
    private String getExtension(Path path) {
        String name = path.toString();
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx) : "";
    }

    // Helper to determine whether a path should be excluded based on directory names
    private boolean isExcluded(Path path, List<String> excludeDirs) {
        // Always exclude Subversion metadata directories
        if (path == null) return false;
        List<String> excludes = excludeDirs == null ? Collections.emptyList() : excludeDirs;
        for (Path part : path) {
            String seg = part.toString();
            if (seg.equalsIgnoreCase(".svn")) return true; // always ignore .svn
            for (String ex : excludes) {
                if (seg.equalsIgnoreCase(ex)) return true;
            }
        }
        return false;
    }

    /**
     * Reads a file and returns a FileStat with raw breakdowns (code/comment/blank/TODO).
     * Import statements are counted as code lines, not separately.
     * This method does not apply the includeBlank/includeHeader filters; it only gathers raw counts.
     *
     * @param path the file path to read
     * @return a FileStat containing detailed counts for the file
     */
    private FileStat analyzeFile(Path path) {
        long code = 0, comment = 0, blank = 0, todo = 0;
        boolean inBlockComment = false;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    blank++;
                    continue;
                }

                // TODO detection (case-insensitive)
                if (trimmed.toLowerCase().contains("todo")) todo++;

                // block comments (/* ... */) or XML/HTML comments (<!-- ... -->)
                if (inBlockComment) {
                    comment++;
                    if (trimmed.endsWith("*/") || trimmed.endsWith("-->")) {
                        inBlockComment = false;
                    }
                    continue;
                }

                if (trimmed.startsWith("/*") || trimmed.startsWith("/**") || trimmed.startsWith("<!--")) {
                    comment++;
                    if (!(trimmed.endsWith("*/") || trimmed.endsWith("-->"))) {
                        inBlockComment = true;
                    }
                    continue;
                }

                // single-line comment (// ...)
                if (trimmed.startsWith("//")) {
                    comment++;
                    continue;
                }

                // Otherwise consider it code (including import statements)
                code++;
            }
        } catch (IOException ignored) {}

        return new FileStat(path.toString(), getExtension(path), code, comment, blank, todo);
    }

    /**
     * Counts the total number of lines in files under a directory using include flags.
     * Backward-compatible wrapper that uses no excludes.
     */
    public long countLines(Path root, List<String> extensions,
                           boolean includeBlank, boolean includeHeader) {
        return countLines(root, extensions, includeBlank, includeHeader, new ArrayList<>());
    }

    /**
     * Counts the total number of lines in files under a directory with optional excluded directories.
     *
     * @param root the root directory
     * @param extensions the list of file extensions to include
     * @param includeBlank whether to include blank lines
     * @param includeHeader whether to include comments
     * @param excludeDirs list of directory names to exclude (e.g. target, build, .git)
     * @return the total number of lines according to the include flags
     */
    public long countLines(Path root, List<String> extensions,
                           boolean includeBlank, boolean includeHeader,
                           List<String> excludeDirs) {
        long total = 0;
        try {
            // It's fine to collect into a list; Files.walk may be large but we filter/exclude early.
            List<Path> paths = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> !isExcluded(p, excludeDirs))
                        .filter(p -> matchExtension(p, extensions))
                        .forEach(paths::add);
            }

            for (Path p : paths) {
                FileStat stat = analyzeFile(p);
                stat.setEffectiveLines(includeBlank, includeHeader);
                total += stat.lines;
            }
        } catch (IOException ignored) {}
        return total;
    }

    /**
     * Backward-compatible wrapper returning file list with detail; no excludes.
     */
    public List<FileStat> countLinesWithDetail(Path root,
                                               List<String> extensions,
                                               boolean includeBlank,
                                               boolean includeHeader) {
        return countLinesWithDetail(root, extensions, includeBlank, includeHeader, new ArrayList<>());
    }

    /**
     * Counts the number of lines in files under a directory and returns detailed statistics.
     * Supports excluding specific directory names.
     *
     * @param root the root directory
     * @param extensions the list of file extensions to include
     * @param includeBlank whether to include blank lines in the effective total
     * @param includeHeader whether to include comment lines in the effective total
     * @param excludeDirs list of directory names to exclude (case-insensitive)
     * @return a list of FileStat objects containing detailed statistics (their .lines value respects the include flags)
     */
    public List<FileStat> countLinesWithDetail(Path root,
                                               List<String> extensions,
                                               boolean includeBlank,
                                               boolean includeHeader,
                                               List<String> excludeDirs) {
        List<FileStat> list = new ArrayList<>();
        try {
            List<Path> paths = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> !isExcluded(path, excludeDirs))
                        .filter(path -> matchExtension(path, extensions))
                        .forEach(paths::add);
            }

            // Process files in parallel for performance
            paths.parallelStream().forEach(p -> {
                FileStat stat = analyzeFile(p);
                stat.setEffectiveLines(includeBlank, includeHeader);
                synchronized (list) {
                    list.add(stat);
                }
            });
        } catch (IOException ignored) {}
        return list;
    }
}

