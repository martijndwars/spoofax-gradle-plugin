package nl.martijndwars.spoofax.tasks;

import java.io.IOException;

import nl.martijndwars.spoofax.SpoofaxPlugin;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
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
import org.metaborg.spoofax.meta.core.config.StrategoFormat;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class LanguageCompile extends LanguageTask {
  private static final ILogger logger = LoggerUtils.logger(LanguageCompile.class);

  protected final Property<String> strategoFormat;

  public LanguageCompile() {
    strategoFormat = getProject().getObjects().property(String.class);
  }

  @Input
  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  @TaskAction
  public void run() {
    try {
      SpoofaxPlugin.loadLanguageDependencies(getProject());

      LanguageSpecBuildInput input = buildInput();
      ISpoofaxLanguageSpec languageSpec = languageSpec();

      getLogger().info("Generating Spoofax sources");

      spoofaxMeta.metaBuilder.initialize(input);
      spoofaxMeta.metaBuilder.generateSources(input, null);

      final BuildInputBuilder inputBuilder = new BuildInputBuilder(languageSpec);
      final BuildInput buildInput = inputBuilder
          .withDefaultIncludePaths(true)
          .withSourcesFromDefaultSourceLocations(true)
          .withSelector(new SpoofaxIgnoresSelector())
          .withMessagePrinter(new StreamMessagePrinter(spoofax.sourceTextService, true, true, logger))
          .withThrowOnErrors(true)
          .withPardonedLanguageStrings(languageSpec.config().pardonedLanguages())
          .addTransformGoal(new CompileGoal())
          .build(spoofax.dependencyService, spoofax.languagePathService);

      spoofax.processorRunner.build(buildInput, null, null).schedule().block();

      spoofaxMeta.metaBuilder.compile(input);
    } catch (MetaborgException | IOException | InterruptedException e) {
      throw new RuntimeException("An unexpected error occurred while compiling the language.", e);
    }
  }

  @Override
  protected ISpoofaxLanguageSpec languageSpec() throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = super.languageSpec();

    if (!strategoFormat.isPresent()) {
      return languageSpec;
    }

    StrategoFormat strategoFormatEnum = StrategoFormat.valueOf(strategoFormat.get());

    return new GradleSpoofaxLanguageSpec(languageSpec, strategoFormatEnum);
  }
}
