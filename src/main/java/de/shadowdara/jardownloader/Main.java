package de.shadowdara.jardownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Main {
    public static String version = "0.1.3";
    public static String download_file = "dependencies.txt";

    public static void main(String[] args) throws IOException {
        System.out.println("JAR-DOWNLOADER v" + version);

        // add entry point with -i and then a download File!
        if (args.length >= 2 && Objects.equals(args[0], "-i")) {
            Path dependencyFile = Paths.get(args[1]);

            if (Files.exists(dependencyFile)) {
                System.out.println("Reading dependencies from: " + dependencyFile);

                try (BufferedReader reader = Files.newBufferedReader(dependencyFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue; // Kommentar oder leere Zeile überspringen
                        }

                        if (line.startsWith("http://") || line.startsWith("https://")) {
                            // Aktuelles Verzeichnis als Download-Ziel verwenden
                            Path downloadDir = Paths.get(".");
                            downloadFile(line, downloadDir);
                        } else {
                            System.out.println("Invalid line (ignored): " + line);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading dependency file: " + e.getMessage());
                }

                return;
            } else {
                System.err.println("Dependency file not found: " + dependencyFile);
                return;
            }
        }


        // Pfad zum Verzeichnis mit JARs, z.B. aktuelles Verzeichnis
        Path searchPath = Paths.get(args.length > 0 ? args[0] : ".");
        // Download-Zielordner
        Path downloadDir = Paths.get(args.length > 1 ? args[1] : "");

        // Alle .jar-Dateien rekursiv finden
        Files.walk(searchPath)
                .filter(p -> p.toString().endsWith(".jar"))
                .forEach(jarPath -> {
                    System.out.println("Searching in: " + jarPath);

                    try (ZipFile jarFile = new ZipFile(jarPath.toFile())) {
                        ZipEntry depEntry = jarFile.getEntry(download_file);

                        // Falls dependencies.txt nicht im Root, suche rekursiv
                        if (depEntry == null) {
                            depEntry = findEntryRecursive(jarFile, download_file);
                        }

                        if (depEntry != null) {
                            System.out.println("Found: " + download_file);

                            try (InputStream is = jarFile.getInputStream(depEntry);
                                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();
                                    if (line.startsWith("#")) {
                                        // Skip
                                    }
                                    else if (line.startsWith("http://") || line.startsWith("https://")) {
                                        downloadFile(line, downloadDir);
                                    }
                                }
                            }
                        } else {
                            System.out.println(download_file + " not found in " + jarPath.getFileName());
                        }
                    } catch (IOException e) {
                        System.err.println("Error while parsing " + jarPath + ": " + e.getMessage());
                    }
                });
    }

    // Hilfsmethode: Sucht eine Datei rekursiv im ZIP (nicht direkt unterstützt, daher alle Einträge durchgehen)
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

    // Download einer Datei von URL in das Zielverzeichnis
    private static void downloadFile(String urlStr, Path downloadDir) {
        try {
            URL url = new URL(urlStr);
            String fileName = Paths.get(url.getPath()).getFileName().toString();
            Path destination = downloadDir.resolve(fileName);

            if (Files.exists(destination)) {
                System.out.println("File already existing: " + fileName);
                return;
            }

            System.out.println("Downloading: " + urlStr);

            try (InputStream in = url.openStream()) {
                Files.copy(in, destination);
            }

            System.out.println("Saved as: " + fileName);

        } catch (IOException e) {
            System.err.println("Error while downloading " + urlStr + ": " + e.getMessage());
        }
    }
}
