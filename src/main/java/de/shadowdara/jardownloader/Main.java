package de.shadowdara.jardownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * JAR-Downloader
 *
 * Dieses Programm durchsucht:
 *  1) eine einzelne Dependency-Datei   (-i pfad/zur/dependencies.txt)
 *  2) oder einen Ordner voller .jar-Dateien
 *
 * Aus jeder gefundenen dependencies.txt werden HTTP/HTTPS-URLs gelesen
 * und die Dateien automatisch heruntergeladen.
 *
 * Funktionen:
 *  - Liest dependencies.txt aus normalen Dateien
 *  - Liest dependencies.txt aus JARs (auch in Unterordnern)
 *  - Überspringt bereits vorhandene Dateien
 */
public class Main {

    /** Version des Programms */
    public static String version = "0.1.3";

    /** Name der Datei, die nach Abhängigkeiten durchsucht wird */
    public static String download_file = "dependencies.txt";


    /**
     * Einstiegspunkt.
     *
     * Argumente:
     *  - "-i <pfad>" → Lies direkte dependencies.txt
     *  - "<such-ordner> <download-ordner>" → Durchsuche JARs
     */
    public static void main(String[] args) throws IOException {
        System.out.println("JAR-DOWNLOADER v" + version);
        System.out.println("================================");
        System.out.println();

        // ---------------------------------------------------------
        // 1) Direkter Modus: -i pfad/zur/dependencies.txt
        // ---------------------------------------------------------
        if (args.length >= 2 && Objects.equals(args[0], "-i")) {

            // Get the Dependency File from the 2nd System Arg
            Path dependencyFile = Paths.get(args[1]);

            if (!Files.exists(dependencyFile)) {
                System.err.println("Dependency file not found: " + dependencyFile);
                return;
            }

            System.out.println("Reading dependencies from file: " + dependencyFile);
            readDependencyFile(dependencyFile, Paths.get("."));
            return; // fertig
        }

        // HELP Method
        if (args[0] == "help") {
            help();
        }

        // ---------------------------------------------------------
        // 2) Automatisches Durchsuchen eines Ordners
        // ---------------------------------------------------------
        Path searchPath  = Paths.get(args.length > 0 ? args[0] : ".");
        Path downloadDir = Paths.get(args.length > 1 ? args[1] : ".");

        System.out.println("Searching for .jar files in: " + searchPath.toAbsolutePath());
        System.out.println("Downloading dependencies into: " + downloadDir.toAbsolutePath());
        System.out.println();

        findFiles(searchPath, downloadDir);
    }


    // =====================================================================================
    //  HILFSMETHODE: Lese dependencies.txt aus normaler Datei
    // =====================================================================================

    /**
     * Liest eine dependencies.txt-Datei ein und lädt alle URLs herunter.
     *
     * @param file Der Pfad zur dependencies.txt
     * @param downloadDir Zielordner für Downloads
     */
    private static void readDependencyFile(Path file, Path downloadDir) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            // List for the Downloaded Jars
            List<String> libs = List.of();

            String line;
            while ((line = reader.readLine()) != null) {

                line = line.trim();

                // Leer oder Kommentar → überspringen
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Nur HTTP/HTTPS als gültige Dependencies
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    URL url = new URL(line);

                    // Get the Filename
                    String fileName = Paths.get(url.getPath()).getFileName().toString();

                    // Add name to shortcute List
                    libs.add(fileName);

                    // Download the Library
                    downloadFile(line, downloadDir);
                } else {
                    System.out.println("[IGNORED] Invalid line: " + line);
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading dependency file '" + file + "': " + e.getMessage());
        }
    }


    // =====================================================================================
    //  HILFSMETHODE: Finde JAR-Dateien und suche in ihnen nach dependencies.txt
    // =====================================================================================

    /**
     * Durchsucht das angegebene Verzeichnis rekursiv nach JAR-Dateien.
     * Jede JAR wird geöffnet und nach einer dependencies.txt durchsucht.
     *
     * @param searchPath Ordner, der durchsucht wird
     * @param downloadDir Zielordner für Downloads
     */
    private static void findFiles(Path searchPath, Path downloadDir) throws IOException {

        Files.walk(searchPath)
                .filter(path -> path.toString().endsWith(".jar"))
                .forEach(jarPath -> {
                    System.out.println("→ Inspecting JAR: " + jarPath.getFileName());

                    try (ZipFile jarFile = new ZipFile(jarPath.toFile())) {

                        // Schritt 1: Versuche Datei im Root zu finden
                        ZipEntry entry = jarFile.getEntry(download_file);

                        // Schritt 2: Falls nicht im Root, rekursiv suchen
                        if (entry == null) {
                            entry = findEntryRecursive(jarFile, download_file);
                        }

                        if (entry != null) {
                            System.out.println("   Found " + download_file);

                            try (InputStream is = jarFile.getInputStream(entry);
                                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                                String line;

                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();

                                    if (line.startsWith("#") || line.isEmpty()) {
                                        continue;
                                    }

                                    if (line.startsWith("http://") || line.startsWith("https://")) {
                                        downloadFile(line, downloadDir);
                                    }
                                }

                            }

                        } else {
                            System.out.println("   No dependencies.txt found.");
                        }

                    } catch (IOException e) {
                        System.err.println("Error parsing JAR '" + jarPath + "': " + e.getMessage());
                    }

                    System.out.println();
                });
    }


    // =====================================================================================
    //  HILFSMETHODE: Suchet rekursiv nach Datei im ZIP (Unterordner etc.)
    // =====================================================================================

    /**
     * Durchsucht ein ZIP (z. B. JAR) rekursiv nach einer Datei.
     *
     * @param zipFile Das geöffnete ZIP-File
     * @param filename Dateiname, nach dem gesucht wird
     * @return Gefundener ZipEntry oder null
     */
    private static ZipEntry findEntryRecursive(ZipFile zipFile, String filename) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(filename)) {
                return entry;
            }
        }

        return null;
    }


    // =====================================================================================
    //  HILFSMETHODE: Eine Datei von einer URL herunterladen
    // =====================================================================================

    /**
     * Lädt eine Datei über HTTP/HTTPS herunter.
     *
     * @param urlStr URL als String
     * @param downloadDir Zielordner
     */
    private static void downloadFile(String urlStr, Path downloadDir) {
        try {

            URL url = new URL(urlStr);

            // Dateiname extrahieren
            String fileName = Paths.get(url.getPath()).getFileName().toString();
            Path destination = downloadDir.resolve(fileName);

            // Datei existiert bereits?
            if (Files.exists(destination)) {
                System.out.println("   [SKIPPED] Already exists: " + fileName);
                return;
            }

            System.out.println("   Downloading: " + urlStr);

            // Datei speichern
            try (InputStream input = url.openStream()) {
                Files.copy(input, destination);
            }

            System.out.println("   Download complete → " + fileName);

        } catch (IOException e) {
            System.err.println("   ERROR downloading " + urlStr + ": " + e.getMessage());
        }
    }

    /**
     * Print the Help Message to the Terminal
     *
     */
    private static void help() {
        System.out.println("Help for jardownloader");
    }
}
