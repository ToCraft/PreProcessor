package dev.tocraft.gradle.preprocess.tasks;

import dev.tocraft.gradle.preprocess.data.Keywords;
import dev.tocraft.gradle.preprocess.util.PreProcessor;
import dev.tocraft.gradle.preprocess.data.PreprocessExtension;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

/**
 * The actual preprocessor task
 */
public class PreProcessTask extends AbstractPreProcessTask {
    private final MapProperty<String, Object> vars;
    private final MapProperty<String, Keywords> keywords;
    /**
     * @param factory some object factory to create the properties
     */
    @Inject
    public PreProcessTask(final ObjectFactory factory) {
        super(factory);
        this.vars = factory.mapProperty(String.class, Object.class);
        this.keywords = factory.mapProperty(String.class, Keywords.class);
    }

    /**
     * @return the vars that shall be used for the custom if-statements
     * @see PreprocessExtension#vars
     */
    @Input
    public MapProperty<String, Object> getVars() {
        return vars;
    }

    /**
     * @return custom keywords, where the key is something the target file name should end with (e.g. '.json') and the Keywords are the custom keywords for this file type.
     * @see PreprocessExtension#keywords
     */
    @Input
    public MapProperty<String, Keywords> getKeywords() {
        return keywords;
    }

    @Internal
    @Override
    public String getDescription() {
        return "PreProcess files.";
    }

    @Override
    public void handlePreProcess(List<File> inFiles, List<File> outFiles) {
        PreProcessor preProcessor = new PreProcessor(vars.get(), keywords.get());

        for (int i = 0; i < inFiles.size(); i++) {
            File inFile = inFiles.get(i);
            File outFile = outFiles.get(i);
            preProcessor.convertFile(inFile, outFile);
        }
    }
}
