package dev.tocraft.gradle.preprocess.tasks;

import dev.tocraft.gradle.preprocess.data.Keywords;
import dev.tocraft.gradle.preprocess.util.ParseException;
import dev.tocraft.gradle.preprocess.util.PreProcessor;
import dev.tocraft.gradle.preprocess.data.PreprocessExtension;
import dev.tocraft.gradle.preprocess.util.ReMapper;
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
import java.io.FileWriter;
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
    private final MapProperty<String, String> remap;
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
        this.remap = factory.mapProperty(String.class, String.class);
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
     * @return the map that will be used for remapping
     */
    @Input
    public MapProperty<String, String> getRemap() {
        return remap;
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
        ReMapper reMapper = new ReMapper(remap.get());

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

        // iterate backwards so files can overwrite each other
        for (int i =  sourceFiles.size() - 1; i >= 0; i--) {
            Entry entry = sourceFiles.get(i);

            File inFile = entry.inBase.resolve(entry.relPath).toFile();
            File outFile = entry.outBase.resolve(entry.relPath).toFile();

            convertFile(preProcessor, reMapper, inFile, outFile);

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

    /**
     * @param inFile  the file that shall be preprocessed
     * @param outFile the file where the preprocessed lines shall be written to
     */
    public static void convertFile(PreProcessor preProcessor, ReMapper reMapper, File inFile, File outFile) {
        try {
            List<String> lines = Files.readAllLines(inFile.toPath());
            lines = preProcessor.convertSource(lines, inFile.getName());
            lines = reMapper.convertSource(lines);

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
}
