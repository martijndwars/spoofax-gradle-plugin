package nl.martijndwars.spoofax;

import java.util.List;

import com.google.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class SpoofaxExtension {
  protected final Property<String> strategoFormat;
  protected final Property<String> version;
  protected final Property<List> overrides;

  @Inject
  public SpoofaxExtension(Project project) {
    this.strategoFormat = project.getObjects().property(String.class);
    this.version = project.getObjects().property(String.class);
    this.overrides = project.getObjects().property(List.class);
  }

  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  public Property<String> getVersion() {
    return version;
  }

  public Property<List> getOverrides() {
    return overrides;
  }
}
