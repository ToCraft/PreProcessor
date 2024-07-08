package dev.tocraft.gradle.preprocess;

import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreProcessor {
    private final Map<String, Object> vars;

    public PreProcessor(Map<String, Object> vars) {
        this.vars = vars;
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

    public boolean evalExpression(String condition) {
        return evalExpression(condition, -1, null);
    }

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

    public List<String> convertSource(List<String> lines) {
        return convertSource(lines, null);
    }

    public List<String> convertSource(List<String> lines, @Nullable String fileName) {
        Stack<IfStackEntry> stack = new Stack<>();
        Stack<Integer> indentStack = new Stack<>();
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicInteger n = new AtomicInteger();

        List<String> mappedLines = lines.stream().map(line -> {
            n.getAndIncrement();

            String trimmed = line.trim();
            String mapped;
            int indentation = line.length() - line.trim().length();
            if (trimmed.startsWith(Keywords.IF.keyword)) {
                boolean result = _evalCondition(trimmed.substring(Keywords.IF.keyword.length()), n.get(), fileName);
                stack.push(new IfStackEntry(result, false, result));
                indentStack.push(indentation);
                active.set(active.get() && result);
                mapped = line;
            } else if (trimmed.startsWith(Keywords.ELSEIF.keyword)) {
                if (stack.isEmpty()) {
                    throw new ParseException("elseif without If-Statement!", n.get(), fileName);
                }
                if (stack.lastElement().elseFound) {
                    throw new ParseException("elseif after else!", n.get(), fileName);
                }

                indentStack.pop();
                indentStack.push(indentation);

                if (stack.lastElement().trueFound) {
                    IfStackEntry last = stack.pop();
                    stack.push(new IfStackEntry(false, last.elseFound, last.trueFound));
                    active.set(false);
                } else {
                    boolean result = _evalCondition(trimmed.substring(Keywords.ELSEIF.keyword.length()), n.get(), fileName);
                    stack.pop();
                    stack.push(new IfStackEntry(result, false, result));
                    active.set(stack.stream().allMatch(it -> it.currentValue));
                }
                mapped = line;
            } else if (trimmed.startsWith(Keywords.ELSE.keyword)) {
                if (stack.isEmpty()) {
                    throw new ParseException("Unexpected else", n.get(), fileName);
                }
                IfStackEntry entry = stack.pop();
                stack.push(new IfStackEntry(!entry.trueFound, true, entry.trueFound));
                indentStack.pop();
                indentStack.push(indentation);
                active.set(stack.stream().allMatch(it -> it.currentValue));
                mapped = line;
            } else if (trimmed.startsWith(Keywords.ENDIF.keyword)) {
                if (stack.isEmpty()) {
                    throw new ParseException("endif without If-Statement!", n.get(), fileName);
                }
                stack.pop();
                indentStack.pop();
                active.set(stack.stream().allMatch(it -> it.currentValue));
                mapped = line;
            } else {
                if (active.get()) {
                    if (trimmed.startsWith(Keywords.EVAL.keyword)) {
                        mapped = line.replaceFirst(Matcher.quoteReplacement(Keywords.EVAL.keyword) + " ?", "");
                    } else {
                        mapped = line;
                    }
                } else {
                    int currIndent = indentStack.peek();
                    if (trimmed.isEmpty()) {
                        mapped = " ".repeat(currIndent) + Keywords.EVAL.keyword;
                    } else if (!trimmed.startsWith(Keywords.EVAL.keyword) && currIndent <= indentation) {
                        mapped = " ".repeat(currIndent) + Keywords.EVAL.keyword + " " + line.substring(currIndent);
                    } else {
                        mapped = line;
                    }
                }
            }

            return mapped;
        }).toList();

        if (!stack.isEmpty()) {
            throw new ParseException("Missing endif!", n.get(), fileName);
        } else {
            return mappedLines;
        }
    }

    public void convertFile(File inFile, File outFile, Logger logger) {
        logger.info("Got that once");
        try {
            List<String> lines = Files.readAllLines(inFile.toPath());
            lines = convertSource(lines, inFile.getName());
            //noinspection ResultOfMethodCallIgnored
            outFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(outFile)) {
                for (String line : lines) {
                    logger.info(line);
                    writer.write(line + "\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record IfStackEntry(Boolean currentValue, Boolean elseFound, Boolean trueFound) {

    }
}
