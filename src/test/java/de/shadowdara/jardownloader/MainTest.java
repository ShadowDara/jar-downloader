package de.shadowdara.jardownloader;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    Path tempOutputDir;

    @BeforeEach
    void setup() throws IOException {
        tempOutputDir = Files.createTempDirectory("jd_test_");
    }

    @Test
    void testDependenciesTxtParsing() throws Exception {
        Path depFile = Path.of(getClass().getResource("/dependencies.txt").toURI());

        assertTrue(Files.exists(depFile));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out));

        Main.main(new String[]{"-i", depFile.toString()});

        System.setOut(original);

        String output = out.toString();

        assertTrue(output.contains("Reading dependencies"));
        assertTrue(output.contains("https://example.org/file1.jar"));
        assertTrue(output.contains("https://example.org/file2.jar"));
    }

    @Test
    void testInvalidDependencies() throws Exception {
        Path depFile = Path.of(getClass().getResource("/invalid_deps.txt").toURI());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(out));

        Main.main(new String[]{"-i", depFile.toString()});

        System.setOut(orig);

        String result = out.toString();

        assertTrue(result.contains("Invalid line"));
        assertTrue(result.contains("https://example.org/file3.jar"));
    }

    @Test
    void testJarDependencyExtraction() throws Exception {
        Path jarPath = Path.of(getClass().getResource("/testjar.jar").toURI());

        assertTrue(Files.exists(jarPath));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(out));

        Main.main(new String[]{jarPath.getParent().toString(), tempOutputDir.toString()});

        System.setOut(orig);

        String result = out.toString();

        assertTrue(result.contains("Searching in"));
        assertTrue(result.contains("Found: dependencies.txt"));
        assertTrue(result.contains("https://example.org/jarinside.jar"));
    }

    @Test
    void testFindEntryRecursive() throws Exception {
        Path jarPath = Path.of(getClass().getResource("/testjar.jar").toURI());

        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            var method = Main.class.getDeclaredMethod("findEntryRecursive", ZipFile.class, String.class);
            method.setAccessible(true);

            var entry = method.invoke(null, zip, "dependencies.txt");
            assertNotNull(entry);
        }
    }
}
