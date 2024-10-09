package dev.tocraft.gradle.preprocess.tasks;

import dev.tocraft.gradle.preprocess.util.ReMapper;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * The actual remap task
 */
public class RemapTask extends AbstractPreProcessTask {
    private final MapProperty<String, String> map;
    /**
     * @param factory some object factory to create the properties
     */
    @Inject
    public RemapTask(final ObjectFactory factory) {
        super(factory);
        this.map = factory.mapProperty(String.class, String.class);
    }

    /**
     * @return the map that will be used for remapping
     */
    @Input
    public MapProperty<String, String> getMap() {
        return map;
    }

    @Internal
    @Override
    public String getDescription() {
        return "Remap files.";
    }

    @Override
    public void handlePreProcess(List<File> inFiles, List<File> outFiles) {
        ReMapper remapper = new ReMapper(map.get());

        for (int i = 0; i < inFiles.size(); i++) {
            File inFile = inFiles.get(i);
            File outFile = outFiles.get(i);
            remapper.convertFile(inFile, outFile);
        }
    }
}
