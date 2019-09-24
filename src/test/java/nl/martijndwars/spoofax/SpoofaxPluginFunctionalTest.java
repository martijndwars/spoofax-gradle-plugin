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
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;
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
    File projectDir = createTemporaryProject(sourceDir);

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
    File projectDir = createTemporaryProject(sourceDir);

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

  /**
   * Test that if we change the version of the language that we build, then the `compileLanguage`
   * task is up to date, but the `archiveLanguage` task is not.
   *
   * @throws IOException
   */
  @Test
  void testChangeLanguageVersionCompileUpTodate() throws IOException {
    File sourceDir = new File(BASE_DIR + "/version");
    File projectDir = createTemporaryProject(sourceDir);

    BuildResult buildResult1 = runGradle(projectDir, "clean", ":publishToMavenLocal");

    Assertions.assertAll(
      () -> assertEquals(SUCCESS, buildResult1.task(":compileLanguage").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":compileJava").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult1.task(":archiveLanguage").getOutcome())
    );

    Path path = projectDir.toPath().resolve("gradle.properties");
    String content = new String(Files.readAllBytes(path));
    String newContent = content.replaceAll(Pattern.quote("1.2.3"), "3.2.1");
    Files.write(path, newContent.getBytes(), TRUNCATE_EXISTING);

    BuildResult buildResult2 = runGradle(projectDir, ":publishToMavenLocal");

    Assertions.assertAll(
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":compileLanguage").getOutcome()),
      () -> assertEquals(UP_TO_DATE, buildResult2.task(":compileJava").getOutcome()),
      () -> assertEquals(SUCCESS, buildResult2.task(":archiveLanguage").getOutcome())
    );
  }

  /**
   * Copy the Spoofax project to a temporary directory so it can be modified without changing the
   * state of subsequent tests.
   *
   * @param sourceDir
   * @return
   */
  protected File createTemporaryProject(File sourceDir) {
    try {
      String prefix = sourceDir.getName();
      File targetDir = Files.createTempDirectory(prefix).toFile();
      FileUtils.copyDirectory(sourceDir, targetDir);

      return targetDir;
    } catch (IOException e) {
      throw new RuntimeException("Could not copy project to temporary directory.", e);
    }
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
