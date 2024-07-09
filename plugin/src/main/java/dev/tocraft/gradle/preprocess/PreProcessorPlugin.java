/*
 * This source file was generated by the Gradle 'init' task
 */
package dev.tocraft.gradle.preprocess;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;

import java.io.File;

@SuppressWarnings({"unused", "deprecation"})
public class PreProcessorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        PreProcessTask preProcessTask = project.getTasks().create(PreProcessTask.ID, PreProcessTask.class);

        project.getTasks().register("applyPreProcessor", Copy.class, task -> {
            task.getOutputs().upToDateWhen(a -> false);
            task.dependsOn(preProcessTask);
            task.from(preProcessTask.getTarget().get());
            for (File file : preProcessTask.getSources().get()) {
                task.into(file);
            }
        });
    }
}
