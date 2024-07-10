/*
 * This source file was generated by the Gradle 'init' task
 */
package dev.tocraft.gradle.preprocess;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static org.gradle.internal.impldep.org.junit.Assert.assertEquals;

/**
 * A simple functional test for the 'org.example.greeting' plugin.
 */
class PreProcessorPluginFunctionalTest {
    @TempDir
    File projectDir;

    private File getBuildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File getSettingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    private File getTestJavaFile() {
        return new File(projectDir, "src/main/java/test/Test.java");
    }

    @Test
    void canApplyPreprocessTask() throws IOException {
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),
                """
                        plugins {
                            id('java')
                            id('dev.tocraft.preprocessor')
                        }
                        
                        preprocess {
                            vars.put("a", "1");
                        }
                        """);

        writeString(getTestJavaFile(), """
                package test;
                
                class Test {
                    public void main(String... args) {
                        //#if a
                        //$$ System.out.println("Test succeeded.");
                        //#else
                        System.out.println("Test failed.");
                        //#endif
                    }
                }
                """);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("applyPreProcessJava");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        // Verify the result
        for (BuildTask task : result.getTasks()) {
            assertEquals(TaskOutcome.SUCCESS, task.getOutcome());
        }

        assertEquals("""
                package test;
                
                class Test {
                    public void main(String... args) {
                        //#if a
                        System.out.println("Test succeeded.");
                        //#else
                        //$$ System.out.println("Test failed.");
                        //#endif
                    }
                }
                """, Files.readString(getTestJavaFile().toPath()));
    }

    private void writeString(File file, String string) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}