package nl.martijndwars.spoofax;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class SpoofaxExtension {
  protected final Property<String> strategoFormat;

  public SpoofaxExtension(Project project) {
    this.strategoFormat = project.getObjects().property(String.class);
  }

  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }
}
