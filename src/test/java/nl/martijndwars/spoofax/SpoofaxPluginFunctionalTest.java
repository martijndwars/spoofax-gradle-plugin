package nl.martijndwars.spoofax;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpoofaxPluginFunctionalTest {
  public static String BASE_DIR = "src/test/resources/examples";

  @Test
  void testSingleProject() {
    File projectDir = new File(BASE_DIR + "/single");
    File archiveFile = new File(projectDir, "target/single-1.2.3.spoofax-language");

    TaskOutcome taskOutcome = runGradleTask(projectDir, ":publishToMavenLocal");

    assertEquals(SUCCESS, taskOutcome);
    assertTrue(archiveFile.exists());
  }

  @Test
  void testJarformatProject() {
    File projectDir = new File(BASE_DIR + "/jarformat");
    File archiveFile = new File(projectDir, "target/jarformat-0.1.0-SNAPSHOT.spoofax-language");

    TaskOutcome taskOutcome = runGradleTask(projectDir, ":check");

    assertEquals(SUCCESS, taskOutcome);
    assertTrue(archiveFile.exists());
  }

  @Test
  void testTesterProjectTestItself() {
    File projectDir = new File(BASE_DIR + "/tester");

    TaskOutcome taskOutcome = runGradleTask(projectDir, ":foo.lang:check");

    assertEquals(SUCCESS, taskOutcome);
  }

  @Test
  void testTesterProjectTestAnother() {
    File projectDir = new File(BASE_DIR + "/tester");

    TaskOutcome taskOutcome = runGradleTask(projectDir, ":foo.tests:check");

    assertEquals(SUCCESS, taskOutcome);
  }

  @Test
  void testBuildAndPublishSingleProject() {
    File projectDir = new File(BASE_DIR + "/multi");
    File archiveFile = new File(projectDir, "projectB/target/projectB-1.2.3-SNAPSHOT.spoofax-language");

    TaskOutcome taskOutcome = runGradleTask(projectDir, ":projectB:publishToMavenLocal");

    assertEquals(SUCCESS, taskOutcome);
    assertTrue(archiveFile.exists());
  }

  @Test
  void testBuildAndPublishMultiProject() {
    File projectDir = new File(BASE_DIR + "/multi");
    File archiveFile = new File(projectDir, "projectA/target/projectA-1.0.1-SNAPSHOT.spoofax-language");

    TaskOutcome taskOutcome = runGradleTask(projectDir, ":projectA:publishToMavenLocal");

    assertEquals(SUCCESS, taskOutcome);
    assertTrue(archiveFile.exists());
  }

  @Test
  void testIncrementalProjectNoChange() {
    File projectDir = new File(BASE_DIR + "/incremental");

    BuildResult buildResult1 = runGradle(projectDir, "clean", ":publishToMavenLocal", "--info");

    Assertions.assertAll(
      () -> assertEquals(SUCCESS, buildResult1.task(":compileLanguage").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":compileJava").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":archiveLanguage").getOutcome())
    );

    BuildResult buildResult2 = runGradle(projectDir, ":publishToMavenLocal", "--info");

    Assertions.assertAll(
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":compileLanguage").getOutcome()),
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":compileJava").getOutcome()),
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":archiveLanguage").getOutcome())
    );
  }

  @Test
  void testIncrementalProjectWithChangeInTransDir() throws IOException {
    File sourceDir = new File(BASE_DIR + "/incremental");
    File projectDir = Files.createTempDirectory("incremental").toFile();
    FileUtils.copyDirectory(sourceDir, projectDir);

    BuildResult buildResult1 = runGradle(projectDir, "clean", ":publishToMavenLocal");

    Assertions.assertAll(
      () -> assertEquals(SUCCESS, buildResult1.task(":compileLanguage").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":compileJava").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":archiveLanguage").getOutcome())
    );

    Path mainStrFile = projectDir.toPath().resolve("trans/incremental.str");
    Files.write(mainStrFile, "\nfoo = id\n".getBytes(), APPEND);
    BuildResult buildResult2 = runGradle(projectDir, ":publishToMavenLocal");

    Assertions.assertAll(
      () -> assertEquals(SUCCESS, buildResult2.task(":compileLanguage").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult2.task(":compileJava").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult2.task(":archiveLanguage").getOutcome())
    );

    projectDir.delete();
  }

  @Test
  void testIncrementalProjectWithChangeOutsideTransDir() throws IOException {
    File sourceDir = new File(BASE_DIR + "/incremental");
    File projectDir = Files.createTempDirectory("incremental").toFile();
    FileUtils.copyDirectory(sourceDir, projectDir);

    BuildResult buildResult1 = runGradle(projectDir, "clean", ":publishToMavenLocal");

    Assertions.assertAll(
      () -> assertEquals(SUCCESS, buildResult1.task(":compileLanguage").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":compileJava").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":archiveLanguage").getOutcome())
    );

    Path newStrFile = projectDir.toPath().resolve("incremental/incremental.str");
    Files.createDirectory(newStrFile.getParent());
    Files.write(newStrFile, "module incremental/incremental".getBytes(), CREATE_NEW);

    BuildResult buildResult2 = runGradle(projectDir, ":publishToMavenLocal");

    Assertions.assertAll(
      () -> assertEquals(SUCCESS, buildResult2.task(":compileLanguage").getOutcome()),
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":compileJava").getOutcome()),
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":archiveLanguage").getOutcome())
    );

    projectDir.delete();
  }

  protected TaskOutcome runGradleTask(File projectDir, String task) {
    BuildResult buildResult = runGradle(projectDir, "clean", task);

    return buildResult.task(task).getOutcome();
  }

  protected BuildResult runGradle(File projectDir, String... args) {
    return GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDir)
      .withArguments(args)
      .build();
  }
}
