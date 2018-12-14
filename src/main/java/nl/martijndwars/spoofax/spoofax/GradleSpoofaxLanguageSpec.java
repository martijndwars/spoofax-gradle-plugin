package nl.martijndwars.spoofax.spoofax;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.config.StrategoFormat;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class GradleSpoofaxLanguageSpec implements ISpoofaxLanguageSpec {
  protected final ISpoofaxLanguageSpec spoofaxLanguageSpec;
  protected final StrategoFormat strategoFormat;

  public GradleSpoofaxLanguageSpec(ISpoofaxLanguageSpec spoofaxLanguageSpec, StrategoFormat strategoFormat) {
    this.spoofaxLanguageSpec = spoofaxLanguageSpec;
    this.strategoFormat = strategoFormat;
  }

  @Override
  public FileObject location() {
    return spoofaxLanguageSpec.location();
  }

  @Override
  public ISpoofaxLanguageSpecConfig config() {
    return new GradleSpoofaxLanguageSpecConfig(spoofaxLanguageSpec.config(), strategoFormat);
  }
}