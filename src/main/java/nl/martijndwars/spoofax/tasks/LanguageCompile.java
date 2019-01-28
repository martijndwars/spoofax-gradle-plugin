package nl.martijndwars.spoofax.tasks;

import com.google.common.collect.Lists;
import nl.martijndwars.spoofax.SpoofaxOverrides;
import nl.martijndwars.spoofax.SpoofaxPlugin;
import org.apache.commons.vfs2.FileObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.CompileGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.messages.StreamMessagePrinter;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.build.SpoofaxLangSpecCommonPaths;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.martijndwars.spoofax.SpoofaxInit.*;

public class LanguageCompile extends AbstractTask {
  private static final ILogger logger = LoggerUtils.logger(LanguageCompile.class);
  public static final String GENERATED_EDITOR_ESV = "target/metaborg/editor.esv.af";
  public static final String CTREE_PROVIDER = "target/metaborg/stratego.ctree";
  public static final String JAR_PROVIDER = "target/metaborg/stratego.jar";

  protected final Property<String> strategoFormat;
  protected final Property<String> languageVersion;
  protected final ListProperty<String> overrides;

  public LanguageCompile() {
    strategoFormat = getProject().getObjects().property(String.class);
    languageVersion = getProject().getObjects().property(String.class);
    overrides = getProject().getObjects().listProperty(String.class);
  }

  @Input
  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  @Input
  public Property<String> getLanguageVersion() {
    return languageVersion;
  }

  @Input
  public ListProperty<String> getOverrides() {
    return overrides;
  }

  // TODO: Ideally we'd use the languagePathService here, but this requires the dependencies (e.g. esv) to be loaded, which is not the case.
  @InputFiles
  public Iterable<File> getInputFiles() {
    ISpoofaxLanguageSpec languageSpec = languageSpec(getProject());
    SpoofaxLangSpecCommonPaths paths = new SpoofaxLangSpecCommonPaths(languageSpec.location());

    // TODO: This list is incomplete
    return Lists.newArrayList(
      toFile(paths.transDir()),
      getProject().file(MetaborgConstants.FILE_CONFIG)
    );
  }

  @OutputDirectory
  public File getOutputDirectory() {
    ISpoofaxLanguageSpec languageSpec = languageSpec(getProject());
    SpoofaxLangSpecCommonPaths paths = new SpoofaxLangSpecCommonPaths(languageSpec.location());

    // TODO: This directory is too general, because LanguageArchive will put the .spoofax-language in here, invalidating the output.
    return toFile(paths.targetDir());
  }

  protected File toFile(@Nullable FileObject fileObject) {
    return getSpoofax(getProject()).resourceService.localFile(fileObject);
  }

  @TaskAction
  public void run() throws MetaborgException, IOException, InterruptedException {
    SpoofaxPlugin.loadLanguageDependencies(getProject());

    LanguageSpecBuildInput input = overridenBuildInput(getProject(), strategoFormat, languageVersion, overrides);
    ISpoofaxLanguageSpec languageSpec = overridenLanguageSpec(getProject(), strategoFormat, languageVersion, overrides);

    // HACK: Store the strategoFormat to be used by Spoofax
    SpoofaxOverrides.set(getProject().getName(), strategoFormat.get());

    getLogger().info("Generating Spoofax sources");

    getSpoofaxMeta(getProject()).metaBuilder.initialize(input);
    getSpoofaxMeta(getProject()).metaBuilder.generateSources(input, null);

    final BuildInputBuilder inputBuilder = new BuildInputBuilder(languageSpec);
    final BuildInput buildInput = inputBuilder
      .withDefaultIncludePaths(true)
      .withSourcesFromDefaultSourceLocations(true)
      .withSelector(new SpoofaxIgnoresSelector())
      .withMessagePrinter(new StreamMessagePrinter(getSpoofax(getProject()).sourceTextService, true, true, logger))
      .withThrowOnErrors(true)
      .withPardonedLanguageStrings(languageSpec.config().pardonedLanguages())
      .addTransformGoal(new CompileGoal())
      .build(getSpoofax(getProject()).dependencyService, getSpoofax(getProject()).languagePathService);

    getSpoofax(getProject()).processorRunner.build(buildInput, null, null).schedule().block();
    getSpoofaxMeta(getProject()).metaBuilder.compile(input);

    // HACK: Modify the editor.esv.af
    fixPackedEsv();
  }

  protected void fixPackedEsv() {
    File file = getProject().file(GENERATED_EDITOR_ESV);
    Path path = file.toPath();

    if (!file.exists()) {
      return;
    }

    try {
      String content = new String(Files.readAllBytes(path), UTF_8);

      switch (strategoFormat.get()) {
        case "ctree":
          content = content.replaceAll(JAR_PROVIDER, CTREE_PROVIDER);
          break;
        case "jar":
          content = content.replaceAll(CTREE_PROVIDER, JAR_PROVIDER);
          break;
      }

      Files.write(path, content.getBytes(UTF_8));
    } catch (IOException e) {
      throw new RuntimeException("Cannot read " + file, e);
    }
  }
}
