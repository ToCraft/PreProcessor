package dev.tocraft.gradle.preprocess;

/**
 * The defined keywords that will be taken into account by the preprocessor
 */
public enum Keywords {
    IF("//#if"), ELSEIF("//#elseif"), ELSE("//#else"), ENDIF("//#endif"), EVAL("//$$");

    Keywords(String keyword) {
        this.keyword = keyword;
    }

    public final String keyword;
}