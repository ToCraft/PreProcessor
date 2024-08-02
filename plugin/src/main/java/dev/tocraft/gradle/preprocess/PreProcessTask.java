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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreProcessTask extends DefaultTask {
    private final MapProperty<String, Object> vars;
    private final MapProperty<String, Keywords> keywords;
    private final Property<File> target;
    private final ListProperty<File> sources;
    private final ConfigurableFileCollection outcomingFiles;
    private final ConfigurableFileCollection incomingFiles;

    @Inject
    public PreProcessTask(final ObjectFactory factory) {
        this.vars = factory.mapProperty(String.class, Object.class);
        this.sources = factory.listProperty(File.class);
        this.keywords = factory.mapProperty(String.class, Keywords.class);
        this.target = factory.property(File.class).convention(getProject().getLayout().getBuildDirectory().getAsFile().map(f -> new File(f, "preprocessor" + File.separatorChar + this.getTaskIdentity().name)));

        this.incomingFiles = factory.fileCollection();
        this.outcomingFiles = factory.fileCollection();
    }

    public record Entry(String relPath, Path inBase, Path outBase) {
    }

    @Input
    public Property<File> getTarget() {
        return target;
    }

    @InputFiles
    public ListProperty<File> getSources() {
        return sources;
    }

    @Input
    public MapProperty<String, Object> getVars() {
        return vars;
    }

    @Input
    public MapProperty<String, Keywords> getKeywords() {
        return keywords;
    }

    @OutputFiles
    public FileCollection getOutcomingFiles() {
        return this.outcomingFiles;
    }

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

        getProject().getLogger().info("PreProcessed Successfully");
    }
}
