package nl.martijndwars.spoofax.spoofax;

import nl.martijndwars.spoofax.SpoofaxExtension;
import org.apache.commons.vfs2.FileObject;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class GradleSpoofaxLanguageSpec implements ISpoofaxLanguageSpec {
  protected final ISpoofaxLanguageSpec spoofaxLanguageSpec;
  protected final Property<String> strategoFormat;
  protected final Property<String> version;
  protected final ListProperty<String> overrides;

  public GradleSpoofaxLanguageSpec(ISpoofaxLanguageSpec spoofaxLanguageSpec, Property<String> strategoFormat, Property<String> version, ListProperty<String> overrides) {
    this.spoofaxLanguageSpec = spoofaxLanguageSpec;
    this.strategoFormat = strategoFormat;
    this.version = version;
    this.overrides = overrides;
  }

  public GradleSpoofaxLanguageSpec(ISpoofaxLanguageSpec spoofaxLanguageSpec, SpoofaxExtension spoofaxExtension) {
    this.spoofaxLanguageSpec = spoofaxLanguageSpec;
    this.strategoFormat = spoofaxExtension.getStrategoFormat();
    this.version = spoofaxExtension.getVersion();
    this.overrides = spoofaxExtension.getOverrides();
  }

  @Override
  public FileObject location() {
    return spoofaxLanguageSpec.location();
  }

  @Override
  public ISpoofaxLanguageSpecConfig config() {
    return new GradleSpoofaxLanguageSpecConfig(spoofaxLanguageSpec.config(), strategoFormat, version, overrides);
  }
}