package nl.martijndwars.spoofax;

import com.google.inject.Singleton;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxProjectConfigService;
import org.metaborg.core.config.IProjectConfigBuilder;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.config.IProjectConfigWriter;
import org.metaborg.core.config.ProjectConfigService;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfigBuilder;
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfigService;
import org.metaborg.spoofax.core.config.ISpoofaxProjectConfigWriter;
import org.metaborg.spoofax.core.config.SpoofaxProjectConfigBuilder;
import org.metaborg.spoofax.core.config.SpoofaxProjectConfigService;

public class SpoofaxGradleModule extends SpoofaxModule {
  @Override
  protected void bindEditor() {
    bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
  }

  // TODO: Can we use a different mechanism to only change the one we want? i.e. GradleSpoofaxProjectConfigService
  @Override
  protected void bindProjectConfig() {
    this.bind(IProjectConfigWriter.class).to(ProjectConfigService.class).in(Singleton.class);
    this.bind(SpoofaxProjectConfigService.class).in(Singleton.class);
    this.bind(IProjectConfigService.class).to(GradleSpoofaxProjectConfigService.class).in(Singleton.class);
    this.bind(ISpoofaxProjectConfigService.class).to(SpoofaxProjectConfigService.class);
    this.bind(ISpoofaxProjectConfigWriter.class).to(SpoofaxProjectConfigService.class);
    this.bind(SpoofaxProjectConfigBuilder.class);
    this.bind(IProjectConfigBuilder.class).to(SpoofaxProjectConfigBuilder.class);
    this.bind(ISpoofaxProjectConfigBuilder.class).to(SpoofaxProjectConfigBuilder.class);
  }
}
