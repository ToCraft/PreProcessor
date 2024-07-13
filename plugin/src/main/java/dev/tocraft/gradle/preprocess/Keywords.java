package dev.tocraft.gradle.preprocess;

/**
 * The defined keywords that will be taken into account by the preprocessor
 */
public record Keywords(String IF, String ELSEIF, String ELSE, String ENDIF, String EVAL) {
    public static final Keywords DEFAULT_KEYWORDS = new Keywords("//#if", "//#elseif", "//#else", "//#endif", "//$$");
}