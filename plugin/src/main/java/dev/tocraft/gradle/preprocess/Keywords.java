package dev.tocraft.gradle.preprocess;

/**
 * The defined keywords that will be taken into account by the preprocessor
 */
public final class Keywords {
    private final String IF;
    private final String ELSEIF;
    private final String ELSE;
    private final String ENDIF;
    private final String EVAL;

    public Keywords(String IF, String ELSEIF, String ELSE, String ENDIF, String EVAL) {
        this.IF = IF;
        this.ELSEIF = ELSEIF;
        this.ELSE = ELSE;
        this.ENDIF = ENDIF;
        this.EVAL = EVAL;
    }

    public String IF() {
        return IF;
    }

    public String ELSEIF() {
        return ELSEIF;
    }

    public String ELSE() {
        return ELSE;
    }

    public String ENDIF() {
        return ENDIF;
    }

    public String EVAL() {
        return EVAL;
    }

    /**
     * Default Keywords and fallback, if no custom keywords are defined for the target file
     */
    public static final Keywords DEFAULT_KEYWORDS = new Keywords("//#if", "//#elseif", "//#else", "//#endif", "//$$");
}