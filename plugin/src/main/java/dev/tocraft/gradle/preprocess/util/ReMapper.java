package dev.tocraft.gradle.preprocess.util;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReMapper {
    private final Map<String, String> map;

    public ReMapper(Map<String, String> map) {
        this.map = map;
    }

    /**
     * @param inFile  the file that shall be preprocessed
     * @param outFile the file where the preprocessed lines shall be written to
     */
    public void convertFile(File inFile, File outFile) {
        try {
            List<String> lines = Files.readAllLines(inFile.toPath());
            lines = convertSource(lines, inFile.getName());
            //noinspection ResultOfMethodCallIgnored
            outFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(outFile)) {
                for (String line : lines) {
                    writer.write(line + "\n");
                }
            }
        } catch (IOException e) {
            // some error while reading. Just copy the file
            try {
                //noinspection ResultOfMethodCallIgnored
                outFile.getParentFile().mkdirs();
                Files.copy(inFile.toPath(), outFile.toPath());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * @param lines the file, already read as lines
     * @return the preprocessed lines
     */
    public List<String> convertSource(List<String> lines) {
        return convertSource(lines, null);
    }

    /**
     * @param lines    the file, already read as lines
     * @param fileName the file name for error throwing
     * @return the preprocessed lines
     */
    public List<String> convertSource(List<String> lines, @Nullable String fileName) {
        List<String> rmLines = new ArrayList<>();

        for (String line : lines) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String rmLine = line.replaceAll(entry.getKey(), entry.getValue());
                rmLines.add(rmLine);
            }
        }

        return rmLines;
    }
}
