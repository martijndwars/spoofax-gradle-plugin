package nl.martijndwars.spoofax.tasks;

import nl.martijndwars.spoofax.SpoofaxPlugin;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.CompileGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.messages.StreamMessagePrinter;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import java.io.IOException;

import static nl.martijndwars.spoofax.SpoofaxInit.*;

public class LanguageCompile extends AbstractTask {
  private static final ILogger logger = LoggerUtils.logger(LanguageCompile.class);

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

  @TaskAction
  public void run() throws MetaborgException, IOException, InterruptedException {
    SpoofaxPlugin.loadLanguageDependencies(getProject());

    LanguageSpecBuildInput input = overridenBuildInput(getProject(), strategoFormat, languageVersion, overrides);
    ISpoofaxLanguageSpec languageSpec = overridenLanguageSpec(getProject(), strategoFormat, languageVersion, overrides);

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
  }
}
