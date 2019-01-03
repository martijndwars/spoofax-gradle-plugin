package nl.martijndwars.spoofax.tasks;

import nl.martijndwars.spoofax.SpoofaxPlugin;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;

import java.io.File;

import static nl.martijndwars.spoofax.SpoofaxInit.*;

public class LanguageArchive extends AbstractTask {
  protected final RegularFileProperty outputFile;
  protected final Property<String> strategoFormat;
  protected final Property<String> languageVersion;
  protected final ListProperty<String> overrides;

  public LanguageArchive() {
    outputFile = getProject().getObjects().fileProperty();
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

  @OutputFile
  public RegularFileProperty getOutputFile() {
    return outputFile;
  }

  public Provider<File> getLazyOutputFile() {
    LanguageSpecBuildInput buildInput = overridenBuildInput(getProject(), strategoFormat, languageVersion, overrides);
    LanguageIdentifier identifier = buildInput.languageSpec().config().identifier();

    return languageVersion.map(version -> {
      String actualVersion = getActualVersion(identifier, version);

      return getProject().file("target/" + identifier.id + "-" + actualVersion + ".spoofax-language");
    });
  }

  protected String getActualVersion(LanguageIdentifier identifier, String version) {
    if (version.isEmpty()) {
      return identifier.version.toString();
    } else {
      return version;
    }
  }

  @TaskAction
  public void run() throws MetaborgException {
    SpoofaxPlugin.loadLanguageDependencies(getProject());

    LanguageSpecBuildInput input = overridenBuildInput(getProject(), strategoFormat, languageVersion, overrides);

    getSpoofaxMeta(getProject()).metaBuilder.pkg(input);
    getSpoofaxMeta(getProject()).metaBuilder.archive(input);
  }
}
