package nl.martijndwars.spoofax.tasks;

import nl.martijndwars.spoofax.SpoofaxPlugin;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

import java.util.List;

public class LanguageArchive extends LanguageTask {
  protected final RegularFileProperty outputFile;
  protected final Property<String> strategoFormat;
  protected final Property<String> version;
  protected final Property<List> overrides;

  public LanguageArchive() {
    outputFile = getProject().getObjects().fileProperty();
    strategoFormat = getProject().getObjects().property(String.class);
    version = getProject().getObjects().property(String.class);
    overrides = getProject().getObjects().property(List.class);
  }

  @Input
  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  @Input
  public Property<String> getVersion() {
    return version;
  }

  @Input
  public Property<List> getOverrides() {
    return overrides;
  }

  @OutputFile
  public RegularFileProperty getOutputFile() {
    return outputFile;
  }

  @TaskAction
  public void run() throws MetaborgException {
    SpoofaxPlugin.loadLanguageDependencies(getProject());

    LanguageSpecBuildInput input = buildInput();

    spoofaxMeta.metaBuilder.pkg(input);
    spoofaxMeta.metaBuilder.archive(input);
  }

  @Override
  protected ISpoofaxLanguageSpec languageSpec() throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = super.languageSpec();

    return new GradleSpoofaxLanguageSpec(languageSpec, strategoFormat, version, overrides);
  }
}
