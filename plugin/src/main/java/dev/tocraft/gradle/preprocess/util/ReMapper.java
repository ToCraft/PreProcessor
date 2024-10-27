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
     * @param lines    the file, already read as lines
     * @return the preprocessed lines
     */
    public List<String> convertSource(List<String> lines) {
        List<String> rmLines = new ArrayList<>();

        for (String line : lines) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                line = line.replaceAll(entry.getKey(), entry.getValue());
            }
            rmLines.add(line);
        }

        return rmLines;
    }
}
