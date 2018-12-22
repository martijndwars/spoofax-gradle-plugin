package nl.martijndwars.spoofax;

import com.google.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskContainer;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.meta.core.project.ILanguageSpec;

public class SpoofaxPluginExtension {
  protected final Project project;
  protected final Property<String> strategoFormat;
  protected final Property<String> languageVersion;
  protected final ListProperty<String> overrides;

  @Inject
  public SpoofaxPluginExtension(Project project) {
    this.project = project;
    this.strategoFormat = project.getObjects().property(String.class);
    this.languageVersion = project.getObjects().property(String.class);
    this.overrides = project.getObjects().listProperty(String.class);
  }

  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  public Property<String> getLanguageVersion() {
    return languageVersion;
  }

  public ListProperty<String> getOverrides() {
    return overrides;
  }

  public void component(MavenPublication publication) throws MetaborgException {
    TaskContainer tasks = project.getTasks();

    ILanguageSpec languageSpec = SpoofaxInit.languageSpec(project);
    LanguageIdentifier identifier = languageSpec.config().identifier();

    publication.artifact(tasks.getByName(SpoofaxPlugin.SPX_LANGUAGE_TASK_NAME));
    publication.setGroupId(identifier.groupId);
    publication.setArtifactId(identifier.id);

    if (languageVersion.get().isEmpty()) {
      publication.setVersion(identifier.version.toString());
    } else {
      publication.setVersion(languageVersion.get());
    }

    // TODO: Add dependencies to the pom's xml.
  }
}