package nl.martijndwars.spoofax;

import com.google.inject.Singleton;
import nl.martijndwars.spoofax.spoofax.GradleLanguageComponentFactory;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxProjectConfigService;
import org.metaborg.core.config.IProjectConfigBuilder;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.config.IProjectConfigWriter;
import org.metaborg.core.config.ProjectConfigService;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.language.*;
import org.metaborg.core.language.dialect.IDialectIdentifier;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.language.dialect.IDialectService;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.config.*;
import org.metaborg.spoofax.core.language.LanguageDiscoveryService;
import org.metaborg.spoofax.core.language.dialect.DialectIdentifier;
import org.metaborg.spoofax.core.language.dialect.DialectProcessor;
import org.metaborg.spoofax.core.language.dialect.DialectService;

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

  // TODO: Can we use a different mechanism to only change the one we want? i.e. GradleLanguageComponentFactory
  @Override
  protected void bindLanguage() {
    bind(ILanguageService.class).to(LanguageService.class).in(Singleton.class);
    bind(ILanguageIdentifierService.class).to(LanguageIdentifierService.class).in(Singleton.class);

    bind(ILanguageComponentFactory.class).to(GradleLanguageComponentFactory.class).in(Singleton.class);
    bind(ILanguageDiscoveryService.class).to(LanguageDiscoveryService.class).in(Singleton.class);

    bind(IDialectService.class).to(DialectService.class).in(Singleton.class);
    bind(IDialectIdentifier.class).to(DialectIdentifier.class).in(Singleton.class);
    bind(IDialectProcessor.class).to(DialectProcessor.class).in(Singleton.class);
  }
}
