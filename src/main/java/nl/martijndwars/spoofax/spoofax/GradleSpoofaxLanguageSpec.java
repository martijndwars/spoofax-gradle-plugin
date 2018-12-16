package nl.martijndwars.spoofax.spoofax;

import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.gradle.api.provider.Property;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class GradleSpoofaxLanguageSpec implements ISpoofaxLanguageSpec {
  protected final ISpoofaxLanguageSpec spoofaxLanguageSpec;
  protected final Property<String> strategoFormat;
  protected final Property<String> version;
  protected final Property<List> overrides;

  public GradleSpoofaxLanguageSpec(ISpoofaxLanguageSpec spoofaxLanguageSpec, Property<String> strategoFormat, Property<String> version, Property<List> overrides) {
    this.spoofaxLanguageSpec = spoofaxLanguageSpec;
    this.strategoFormat = strategoFormat;
    this.version = version;
    this.overrides = overrides;
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