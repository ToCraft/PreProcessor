package dev.tocraft.gradle.preprocess;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The actual preprocessing is happening here
 */
public class PreProcessor {
    /**
     * @see dev.tocraft.gradle.preprocess.PreprocessExtension#vars
     */
    private final Map<String, Object> vars;
    /**
     * @see dev.tocraft.gradle.preprocess.PreprocessExtension#keywords
     */
    private final Map<String, Keywords> keywordsMap;

    /**
     * @param vars the vars that shall be used for the custom if-statements
     */
    public PreProcessor(Map<String, Object> vars) {
        this(vars, new HashMap<>());
    }

    /**
     * @param vars the vars that shall be used for the custom if-statements
     * @param keywordsMap custom keywords, where the key is something the target file name should end with (e.g. '.json') and the Keywords are the custom keywords for this file type.
     */
    public PreProcessor(Map<String, Object> vars, Map<String, Keywords> keywordsMap) {
        this.vars = vars;
        this.keywordsMap = keywordsMap;
    }

    private static final Pattern EXPR_PATTERN = Pattern.compile("(.+)(==|!=|<=|>=|<|>)(.+)");
    private static final String OR_PATTERN = Pattern.quote("||");
    private static final String AND_PATTERN = Pattern.quote("&&");

    private String getVarValue(@Nullable String key) {
        if (key != null) {
            Object value = vars.get(key);
            return value != null ? value.toString() : key;
        } else {
            return null;
        }
    }

    /**
     * @param condition will be read and evaluated
     * @return the value of the evaluated condition
     */
    public boolean evalExpression(String condition) {
        return evalExpression(condition, -1, null);
    }

    /**
     * @param condition will be read and evaluated
     * @param lineNumber required for error throwing
     * @param fileName required for error throwing
     * @return the value of the evaluated condition
     */
    public boolean evalExpression(String condition, int lineNumber, @Nullable String fileName) {
        String[] parts = condition.split(OR_PATTERN);
        if (parts.length > 1) {
            return Arrays.stream(parts).anyMatch(it -> evalExpression(it.trim(), lineNumber, fileName));
        }
        parts = condition.split(AND_PATTERN);
        if (parts.length > 1) {
            return Arrays.stream(parts).allMatch(it -> evalExpression(it.trim(), lineNumber, fileName));
        }

        Matcher matcher = EXPR_PATTERN.matcher(condition);
        if (matcher.matches()) {
            try {
                int lhs = Integer.parseInt(getVarValue(matcher.group(1).trim()));
                int rhs = Integer.parseInt(getVarValue(matcher.group(3).trim()));
                return switch (matcher.group(2)) {
                    case "==" -> lhs == rhs;
                    case "!=" -> lhs != rhs;
                    case ">=" -> lhs >= rhs;
                    case "<=" -> lhs <= rhs;
                    case ">" -> lhs > rhs;
                    case "<" -> lhs < rhs;
                    default -> throw new ParseException("Invalid Expression!", lineNumber, fileName);
                };
            } catch (NumberFormatException e) {
                throw new ParseException(e.getMessage(), lineNumber, fileName);
            }
        }

        String result = getVarValue(condition);

        if (result != null && !result.equals(condition)) {
            try {
                return Integer.parseInt(result) != 0;
            } catch (NumberFormatException ignored) {
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean _evalCondition(String condition, int lineNumber, String fileName) {
        if (!condition.startsWith(" ")) {
            throw new ParseException("Expected space before condition!", lineNumber, fileName);
        } else {
            return evalExpression(condition.trim(), lineNumber, fileName);
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
     * @param lines the file, already read as lines
     * @param fileName the file name for error throwing
     * @return the preprocessed lines
     */
    public List<String> convertSource(List<String> lines, @Nullable String fileName) {
        Stack<IfStackEntry> stack = new Stack<>();
        Stack<Integer> indentStack = new Stack<>();
        boolean active = true;
        int n = 0;

        Keywords keywords = keywordsMap.getOrDefault(getExtension(fileName), Keywords.DEFAULT_KEYWORDS);

        List<String> mappedLines = new ArrayList<>();
        for (String line : lines) {
            n++;

            String trimmed = line.trim();
            int indentation = line.length() - line.trim().length();
            if (trimmed.startsWith(keywords.IF())) {
                boolean result = _evalCondition(trimmed.substring(keywords.IF().length()), n, fileName);
                stack.push(new IfStackEntry(result, false, result));
                indentStack.push(indentation);
                active = active && result;
                mappedLines.add(line);
            } else if (trimmed.startsWith(keywords.ELSEIF())) {
                if (stack.isEmpty()) {
                    throw new ParseException("elseif without If-Statement!", n, fileName);
                }
                if (stack.lastElement().elseFound) {
                    throw new ParseException("elseif after else!", n, fileName);
                }

                indentStack.pop();
                indentStack.push(indentation);

                if (stack.lastElement().trueFound) {
                    IfStackEntry last = stack.pop();
                    stack.push(new IfStackEntry(false, last.elseFound, last.trueFound));
                    active = false;
                } else {
                    boolean result = _evalCondition(trimmed.substring(keywords.ELSEIF().length()), n, fileName);
                    stack.pop();
                    stack.push(new IfStackEntry(result, false, result));
                    active = stack.stream().allMatch(it -> it.currentValue);
                }
                mappedLines.add(line);
            } else if (trimmed.startsWith(keywords.ELSE())) {
                if (stack.isEmpty()) {
                    throw new ParseException("Unexpected else", n, fileName);
                }
                IfStackEntry entry = stack.pop();
                stack.push(new IfStackEntry(!entry.trueFound, true, entry.trueFound));
                indentStack.pop();
                indentStack.push(indentation);
                active = stack.stream().allMatch(it -> it.currentValue);
                mappedLines.add(line);
            } else if (trimmed.startsWith(keywords.ENDIF())) {
                if (stack.isEmpty()) {
                    throw new ParseException("endif without If-Statement!", n, fileName);
                }
                stack.pop();
                indentStack.pop();
                active = stack.stream().allMatch(it -> it.currentValue);
                mappedLines.add(line);
            } else {
                if (active) {
                    if (trimmed.startsWith(keywords.EVAL())) {
                        mappedLines.add(line.replaceFirst(Matcher.quoteReplacement(keywords.EVAL()) + " ?", ""));
                    } else {
                        mappedLines.add(line);
                    }
                } else {
                    int currIndent = indentStack.peek();
                    if (trimmed.isEmpty()) {
                        mappedLines.add(" ".repeat(currIndent) + keywords.EVAL());
                    } else if (!trimmed.startsWith(keywords.EVAL()) && currIndent <= indentation) {
                        mappedLines.add(" ".repeat(currIndent) + keywords.EVAL() + " " + line.substring(currIndent));
                    } else {
                        mappedLines.add(line);
                    }
                }
            }
        }

        if (!stack.isEmpty()) {
            throw new ParseException("Missing endif!", n, fileName);
        } else {
            return mappedLines;
        }
    }

    /**
     * @param inFile the file that shall be preprocessed
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

    private record IfStackEntry(Boolean currentValue, Boolean elseFound, Boolean trueFound) {

    }

    private static String getExtension(@Nullable String fileName) {
        String extension = "";
        if (fileName != null) {
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1);
            }
        }
        return extension.toLowerCase().strip();
    }
}
