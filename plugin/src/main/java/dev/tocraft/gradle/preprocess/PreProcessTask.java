package dev.tocraft.gradle.preprocess;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The actual preprocessor task
 */
public class PreProcessTask extends DefaultTask {
    private final MapProperty<String, Object> vars;
    private final MapProperty<String, Keywords> keywords;
    private final Property<File> target;
    private final ListProperty<File> sources;
    private final ConfigurableFileCollection outcomingFiles;
    private final ConfigurableFileCollection incomingFiles;

    /**
     * @param factory some object factory to create the properties
     */
    @Inject
    public PreProcessTask(final ObjectFactory factory) {
        this.vars = factory.mapProperty(String.class, Object.class);
        this.sources = factory.listProperty(File.class);
        this.keywords = factory.mapProperty(String.class, Keywords.class);
        this.target = factory.property(File.class);

        this.incomingFiles = factory.fileCollection();
        this.outcomingFiles = factory.fileCollection();
    }

    private static final class Entry {
        private final String relPath;
        private final Path inBase;
        private final Path outBase;

        public Entry(String relPath, Path inBase, Path outBase) {
            this.relPath = relPath;
            this.inBase = inBase;
            this.outBase = outBase;
        }
    }

    /**
     * @return the target folder where the preprocessed files will be written to
     */
    @Input
    public Property<File> getTarget() {
        return target;
    }

    /**
     * @return the directories where the files, that shall be preprocessed, lie
     */
    @InputFiles
    public ListProperty<File> getSources() {
        return sources;
    }

    /**
     * @return the vars that shall be used for the custom if-statements
     * @see dev.tocraft.gradle.preprocess.PreprocessExtension#vars
     */
    @Input
    public MapProperty<String, Object> getVars() {
        return vars;
    }

    /**
     * @return custom keywords, where the key is something the target file name should end with (e.g. '.json') and the Keywords are the custom keywords for this file type.
     * @see dev.tocraft.gradle.preprocess.PreprocessExtension#keywords
     */
    @Input
    public MapProperty<String, Keywords> getKeywords() {
        return keywords;
    }

    /**
     * @return the preprocessed files
     */
    @OutputFiles
    public FileCollection getOutcomingFiles() {
        return this.outcomingFiles;
    }

    /**
     * @return the files to be preprocessed
     */
    @Internal
    public FileCollection getIncomingFiles() {
        return this.incomingFiles;
    }

    @Internal
    @Override
    public String getDescription() {
        return "PreProcess files.";
    }

    /**
     * The actual preprocess action
     */
    @TaskAction
    public void preprocess() {
        if (sources.get().isEmpty()) {
            throw new ParseException("No sources defined or source folder is empty!");
        }

        PreProcessor preProcessor = new PreProcessor(vars.get(), keywords.get());

        List<Entry> sourceFiles = new ArrayList<>();

        for (File srcFolder : sources.get()) {
            final File srcFolderFile = srcFolder.isAbsolute() ? srcFolder : new File(this.getProject().getProjectDir(), srcFolder.getPath());
            Path inBasePath = srcFolderFile.toPath();
            for (File file : this.getProject().fileTree(inBasePath)) {
                Path relPath = inBasePath.relativize(file.toPath());
                sourceFiles.add(new Entry(relPath.toString(), inBasePath, target.get().toPath()));
            }
        }

        getProject().getLogger().info("Source folders in use: {}", sources);

        getProject().delete(target.get());

        Set<File> foundInFiles = new HashSet<>();
        Set<File> foundOutFiles = new HashSet<>();

        for (Entry entry : sourceFiles) {
            File inFile = entry.inBase.resolve(entry.relPath).toFile();
            File outFile = entry.outBase.resolve(entry.relPath).toFile();

            preProcessor.convertFile(inFile, outFile);

            foundInFiles.add(inFile);
            foundOutFiles.add(outFile);
        }

        this.outcomingFiles.setFrom(foundOutFiles);
        this.incomingFiles.setFrom(foundInFiles);

        try {
            Path infoFile = target.get().toPath().getParent().resolve(getName() + ".txt");
            //noinspection ResultOfMethodCallIgnored
            infoFile.getParent().toFile().mkdirs();
            Files.write(infoFile, ("Target: " + getTarget().get().toPath() + "\nSources: " + getSources().get() + "\nTotal Files: " + sourceFiles.size()).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getProject().getLogger().info("PreProcessed Successfully");
    }
}
