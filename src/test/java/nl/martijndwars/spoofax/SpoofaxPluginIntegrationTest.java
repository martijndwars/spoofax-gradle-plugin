package nl.martijndwars.spoofax;

import org.gradle.api.Project;
import org.gradle.api.plugins.PluginManager;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static junit.framework.TestCase.assertNotNull;
import static nl.martijndwars.spoofax.SpoofaxPluginConstants.*;

public class SpoofaxPluginIntegrationTest {
  @Test
  void createTasksWhenPluginIsApplied() throws IOException {
    File temporaryFolder = Files.createTempDirectory("spoofax-plugin-test").toFile();
    Project project = ProjectBuilder.builder().withProjectDir(temporaryFolder).build();

    PluginManager pluginManager = project.getPluginManager();
    pluginManager.apply(SpoofaxPlugin.class);

    assertNotNull(project.getTasks().findByName(CLEAN_LANGUAGE_TASK_NAME));
    assertNotNull(project.getTasks().findByName(COMPILE_LANGUAGE_TASK_NAME));
    assertNotNull(project.getTasks().findByName(ARCHIVE_LANGUAGE_TASK_NAME));
    assertNotNull(project.getTasks().findByName(SPX_LANGUAGE_TASK_NAME));
    assertNotNull(project.getTasks().findByName(CHECK_LANGUAGE_TASK_NAME));
  }
}
