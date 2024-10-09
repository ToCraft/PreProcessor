package dev.tocraft.gradle.preprocess.util;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sometime in the future, the PreProcess will also auto-handle the imports
 */
@SuppressWarnings("unused")
@ApiStatus.Experimental
public class ImportManager {
    private static List<String> organizeImports(List<String> sourceLines) {
        Set<String> imports = new TreeSet<>();
        String packageLine = "";
        List<String> codeLines = new ArrayList<>();

        for (String line : sourceLines) {
            if (line.trim().startsWith("import ")) {
                imports.add(getImportClassName(line));
            } else if (line.trim().startsWith("package ")) {
                packageLine = line;
            } else {
                codeLines.add(line);
            }
        }

        while (codeLines.get(0).trim().isEmpty()) {
            codeLines.remove(0);
        }

        // Reconstruct the modified lines
        List<String> modifiedLines = new ArrayList<>();
        modifiedLines.add(packageLine);
        modifiedLines.add("");

        System.out.println(codeLines);

        modifiedLines.addAll(imports.stream().filter(imp -> {
            String[] impArr = imp.split("\\.");
            return anyStringInListContains(codeLines, impArr[impArr.length - 1]);
        }).map(imp -> "import " + imp + ";").collect(Collectors.toList()));
        modifiedLines.add("");

        modifiedLines.addAll(codeLines);

        return modifiedLines;
    }

    private static String getImportClassName(String importLine) {
        Pattern pattern = Pattern.compile("import\\s+([\\w.]+);");
        Matcher matcher = pattern.matcher(importLine);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    private static boolean anyStringInListContains(List<String> list, String str) {
        for (String s : list) {
            if (!s.trim().startsWith("//") && s.trim().contains(str)) {
                return true;
            }
        }
        return false;
    }
}
