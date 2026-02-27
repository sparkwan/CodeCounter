package com.github.dev.tool.plugins.counter;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CodeCounterCoreTest {

    @Test
    public void testSvnIsExcluded() throws Exception {
        Path tmp = Files.createTempDirectory("cc-test-");
        try {
            // Create a normal file and a file inside .svn
            Path normalDir = tmp.resolve("src");
            Files.createDirectories(normalDir);
            Path normalFile = normalDir.resolve("A.java");
            Files.writeString(normalFile, "public class A {\n// TODO\n}\n");

            Path svnDir = tmp.resolve(".svn").resolve("pristine");
            Files.createDirectories(svnDir);
            Path svnFile = svnDir.resolve("B.java");
            Files.writeString(svnFile, "public class B {\n}\n");

            CodeCounterCore core = new CodeCounterCore();
            List<CodeCounterCore.FileStat> list = core.countLinesWithDetail(tmp,
                    Arrays.asList(".java"), true, true, Arrays.asList("target","build",".git",".svn"));

            // Should only contain A.java and not B.java
            boolean hasA = list.stream().anyMatch(f -> f.path.endsWith("A.java"));
            boolean hasB = list.stream().anyMatch(f -> f.path.endsWith("B.java"));
            assertTrue(hasA, "Expected A.java to be counted");
            assertFalse(hasB, "Expected B.java under .svn to be excluded");
        } finally {
            // cleanup
            try { Files.walk(tmp).sorted((a,b)->b.compareTo(a)).forEach(p->p.toFile().delete()); } catch (Throwable ignored) {}
        }
    }
}

