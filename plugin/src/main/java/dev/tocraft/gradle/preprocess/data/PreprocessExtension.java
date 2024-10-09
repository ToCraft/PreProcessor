package dev.tocraft.gradle.preprocess.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Gradle Extension for setting general PreProcessor variables
 */
public class PreprocessExtension {
    /**
     * the vars that shall be used for the custom if-statements
     */
    public Map<String, Object> vars = new HashMap<>();
    /**
     * custom keywords, where the key is something the target file name should end with (e.g. '.json') and the Keywords are the custom keywords for this file type.
     */
    public Map<String, Keywords> keywords = new HashMap<>();
}
