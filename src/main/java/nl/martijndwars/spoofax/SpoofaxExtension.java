package nl.martijndwars.spoofax;

import com.google.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class SpoofaxExtension {
  protected final Property<String> strategoFormat;
  protected final Property<String> version;
  protected final ListProperty<String> overrides;

  @Inject
  public SpoofaxExtension(Project project) {
    this.strategoFormat = project.getObjects().property(String.class);
    this.version = project.getObjects().property(String.class);
    this.overrides = project.getObjects().listProperty(String.class);
  }

  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  public Property<String> getVersion() {
    return version;
  }

  public ListProperty<String> getOverrides() {
    return overrides;
  }
}
