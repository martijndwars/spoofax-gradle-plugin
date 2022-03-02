package nl.martijndwars.spoofax.spoofax;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import mb.nabl2.config.NaBL2Config;
import mb.statix.spoofax.IStatixProjectConfig;
import nl.martijndwars.spoofax.Utils;
import org.apache.commons.vfs2.FileObject;
import org.gradle.api.artifacts.Dependency;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.config.ISourceConfig;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfig;
import org.metaborg.spoofax.core.config.SpoofaxProjectConfigService;

public class GradleSpoofaxProjectConfigService implements IProjectConfigService {
  protected final SpoofaxProjectConfigService projectConfigService;
  protected List<String> overrides;

  @Inject
  public GradleSpoofaxProjectConfigService(SpoofaxProjectConfigService projectConfigService) {
    this.projectConfigService = projectConfigService;
  }

  // TODO: In a multi-project build, multiple projects call setOverrides on the same object. We now concat these (HACK).
  public void setOverrides(List<String> overrides) {
    if (this.overrides == null) {
      this.overrides = overrides;
    }

    Iterable<String> combinedOverrides = Iterables.unmodifiableIterable(Iterables.concat(this.overrides, overrides));

    this.overrides = Lists.newArrayList(combinedOverrides);
  }

  public List<String> getOverrides() {
    return overrides;
  }

  @Override
  public boolean available(FileObject fileObject) {
    return projectConfigService.available(fileObject);
  }

  @Override
  public ConfigRequest<? extends IProjectConfig> get(FileObject fileObject) {
    ConfigRequest<ISpoofaxProjectConfig> projectConfigConfigRequest = projectConfigService.get(fileObject);

    return new ConfigRequest<ISpoofaxProjectConfig>() {
      @Override
      public ISpoofaxProjectConfig config() {
        ISpoofaxProjectConfig projectConfig = projectConfigConfigRequest.config();

        return new ISpoofaxProjectConfig() {
          @Override
          public Collection<ISourceConfig> sources() {
            return projectConfig.sources();
          }

          @Override
          public NaBL2Config nabl2Config() {
            return projectConfig.nabl2Config();
          }

          @Override
          public IStatixProjectConfig statixConfig() {
            return projectConfig.statixConfig();
          }

          @Override
          public String metaborgVersion() {
            return projectConfig.metaborgVersion();
          }

          @Override
          public Collection<LanguageIdentifier> compileDeps() {
            return projectConfig.compileDeps();
          }

          @Override
          public Collection<LanguageIdentifier> sourceDeps() {
            List<String> overrides = getOverrides();

            return Utils.transformDeps(overrides, projectConfig.sourceDeps());
          }

          @Override
          public Collection<LanguageIdentifier> javaDeps() {
            return projectConfig.javaDeps();
          }
        };
      }
    };
  }

  @Override
  public IProjectConfig defaultConfig(FileObject fileObject) {
    return null;
  }
}
