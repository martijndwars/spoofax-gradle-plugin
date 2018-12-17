package nl.martijndwars.spoofax.tasks;

import java.io.File;

import nl.martijndwars.spoofax.SpoofaxPlugin;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;

public class LanguageArchive extends LanguageTask {
  protected File archiveFile;

  public LanguageArchive() throws MetaborgException {
    LanguageSpecBuildInput buildInput = this.buildInput();

    this.archiveFile = getProject().file("target/" + archiveFileName(buildInput));
  }

  @OutputFile
  public File getArchiveFile() {
    return archiveFile;
  }

  @TaskAction
  public void run() {
    try {
      SpoofaxPlugin.loadLanguageDependencies(getProject());

      LanguageSpecBuildInput input = buildInput();

      spoofaxMeta.metaBuilder.pkg(input);
      spoofaxMeta.metaBuilder.archive(input);
    } catch (MetaborgException e) {
      throw new RuntimeException("An unexpected error occurred while archiving the language.", e);
    }
  }

  protected static String archiveFileName(LanguageSpecBuildInput buildInput) {
    LanguageIdentifier identifier = buildInput.languageSpec().config().identifier();

    return identifier.toFileString() + ".spoofax-language";
  }
}
