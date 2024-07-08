package dev.tocraft.gradle.preprocess;

public class ParseException extends RuntimeException {
    /**
     * The Line where the exception happened
     */
    private int lineNumber = -1;
    /**
     * The file where the exception occured
     */
    private String fileName = "";

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String messsage, int lineNumber, String fileName) {
        this(messsage);
        setLineNumber(lineNumber);
        setFileName(fileName);
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (lineNumber > -1 ? " In line: " + lineNumber : "") + (fileName != null && !fileName.isBlank() ? " of file: " + fileName : "");
    }
}
