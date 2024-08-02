package dev.tocraft.gradle.preprocess;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.*;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public class ApplyPreProcessTask extends DefaultTask {
    private final Property<File> source;
    private final ListProperty<File> targets;
    private final ConfigurableFileCollection outcomingFiles;
    private final ConfigurableFileCollection incomingFiles;

    @Inject
    public ApplyPreProcessTask(final ObjectFactory factory, final TaskProvider<PreProcessTask> preProcessTask) {
        this.targets = factory.listProperty(File.class).convention(preProcessTask.flatMap(PreProcessTask::getSources));
        this.source = factory.property(File.class).convention(preProcessTask.flatMap(PreProcessTask::getTarget));

        this.incomingFiles = factory.fileCollection();
        this.outcomingFiles = factory.fileCollection();
    }

    @Input
    public Property<File> getSource() {
        return source;
    }

    @InputFiles
    public ListProperty<File> getTargets() {
        return targets;
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
     * The actual task action
     */
    @TaskAction
    public void applyPreProcess() {
        Set<File> foundInFiles = new HashSet<>();
        Set<File> foundOutFiles = new HashSet<>();

        // place file in their original source folder
        for (File srcFolder : targets.get()) {
            final File srcFolderFile = srcFolder.isAbsolute() ? srcFolder : new File(getProject().getProjectDir(), srcFolder.getPath());
            Path outBasePath = srcFolderFile.toPath();
            // iterate over the existing files in the targets folders so the preprocessed files can be copied to their exact source folder
            // might be buggy when interfered by externals
            for (File file : getProject().fileTree(outBasePath)) {
                Path relPath = outBasePath.relativize(file.toPath());
                Path outPath = outBasePath.resolve(relPath);
                Path inPath = source.get().toPath().resolve(relPath);
                try {
                    Files.copy(inPath, outPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                foundInFiles.add(inPath.toFile());
                foundOutFiles.add(outPath.toFile());
            }
        }

        this.outcomingFiles.setFrom(foundOutFiles);
        this.incomingFiles.setFrom(foundInFiles);
    }
}
