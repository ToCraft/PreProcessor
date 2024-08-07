package dev.tocraft.gradle.preprocess;

/**
 * Exception while parsing / reading the preprocessor code in a file
 */
public class ParseException extends RuntimeException {
    /**
     * The Line where the exception happened
     */
    private int lineNumber = -1;
    /**
     * The file where the exception occurred
     */
    private String fileName = "";

    /**
     * @param message the error message
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * @param message the error message
     * @param lineNumber the line where the parsing exception happened
     * @param fileName the file where parsing exception happened
     */
    public ParseException(String message, int lineNumber, String fileName) {
        this(message);
        setLineNumber(lineNumber);
        setFileName(fileName);
    }

    /**
     * @param lineNumber the line where the parsing exception happened
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * @param fileName the file where parsing exception happened
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (lineNumber > -1 ? " In line: " + lineNumber : "") + (fileName != null && !fileName.isBlank() ? " of file: " + fileName : "");
    }
}
