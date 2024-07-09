package dev.tocraft.gradle.preprocess;

import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PreProcessTask extends DefaultTask {
    public static final String ID = "preprocess";

    public final List<FileReference> entries = new ArrayList<>();
    private final MapProperty<String, Object> vars;
    private final Property<File> target;
    private final ListProperty<File> sources;

    @Inject
    public PreProcessTask(final ObjectFactory factory) {
        this.vars = factory.mapProperty(String.class, Object.class);
        this.sources = factory.listProperty(File.class);

        //noinspection deprecation
        this.target = factory.property(File.class).convention(new File(this.getProject().getBuildDir(), "preprocessor" + File.separatorChar + this.getTaskIdentity().name));
    }

    public record FileReference(@NotNull Set<File> source, @NotNull File generated) {
    }

    private record Entry(String relPath, Path inBase, Path outBase) {
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

    /**
     * The actual preprocess action
     */
    @TaskAction
    public void preprocess() {
        if (sources.get().isEmpty()) {
            throw new ParseException("No sources defined or source folder is empty!");
        }

        PreProcessor preProcessor = new PreProcessor(vars.get());

        List<Entry> sourceFiles = new ArrayList<>();
        for (File srcFolder : sources.get()) {
            final File srcFolderFile = srcFolder.isAbsolute() ? srcFolder : new File(this.getProject().getProjectDir(), srcFolder.getPath());
            Path inBasePath = srcFolderFile.toPath();
            for (File file : this.getProject().fileTree(inBasePath)) {
                Path relPath = inBasePath.relativize(file.toPath());
                sourceFiles.add(new Entry(relPath.toString(), inBasePath, target.get().toPath()));
            }
        }

        getProject().getLogger().info("Source folders in use: {}", sources.get());

        getProject().delete(entries.stream().map(it -> it.generated).toArray());

        for (Entry entry : sourceFiles) {
            File file = entry.inBase.resolve(entry.relPath).toFile();
            File outFile = entry.outBase.resolve(entry.relPath).toFile();

            preProcessor.convertFile(file, outFile);
        }

        getProject().getLogger().info("PreProcessed Successfully");
    }
}
