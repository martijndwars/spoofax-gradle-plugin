package nl.martijndwars.spoofax.spoofax;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.ILanguageComponentConfig;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.context.IContextFactory;
import org.metaborg.core.context.IContextStrategy;
import org.metaborg.core.language.IComponentCreationConfigRequest;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzer;
import org.metaborg.spoofax.core.language.ComponentFactoryRequest;
import org.metaborg.spoofax.core.language.LanguageComponentFactory;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeFacet;
import org.metaborg.spoofax.core.syntax.SyntaxFacet;
import org.metaborg.spoofax.core.syntax.SyntaxFacetFromESV;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.io.binary.TermReader;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class GradleLanguageComponentFactory extends LanguageComponentFactory {
  private static final ILogger logger = LoggerUtils.logger(LanguageComponentFactory.class);

  private final IResourceService resourceService;
  private final ILanguageComponentConfigService componentConfigService;
  private final ITermFactoryService termFactoryService;

  @Inject
  public GradleLanguageComponentFactory(
    IResourceService resourceService,
    ILanguageComponentConfigService componentConfigService,
    ITermFactoryService termFactoryService,
    Map<String, IContextFactory> contextFactories,
    Map<String, IContextStrategy> contextStrategies,
    Map<String, ISpoofaxAnalyzer> analyzers) {
    super(resourceService, componentConfigService, termFactoryService, contextFactories, contextStrategies, analyzers);

    this.resourceService = resourceService;
    this.componentConfigService = componentConfigService;
    this.termFactoryService = termFactoryService;
  }

  @Override
  public IComponentCreationConfigRequest requestFromDirectory(FileObject directory) throws MetaborgException {
    try {
      if(!directory.exists()) {
        throw new MetaborgException(
          logger.format("Cannot request component creation from directory {}, it does not exist", directory));
      }
      if(!directory.isFolder()) {
        throw new MetaborgException(
          logger.format("Cannot request component creation from {}, it is not a directory", directory));
      }

      if(!componentConfigService.available(directory)) {
        throw new MetaborgException(logger.format(
          "Cannot request component creation from directory {}, there is no component config file inside the directory",
          directory));
      }

      return request(directory);
    } catch(IOException e) {
      throw new MetaborgException(
        logger.format("Cannot request component creation from directory {}, unexpected I/O error", directory),
        e);
    }
  }

  @Override
  public IComponentCreationConfigRequest requestFromArchive(FileObject archiveFile) throws MetaborgException {
    try {
      if(!archiveFile.exists()) {
        throw new MetaborgException(logger
          .format("Cannot request component creation from archive file {}, it does not exist", archiveFile));
      }
      if(!archiveFile.isFile()) {
        throw new MetaborgException(logger
          .format("Cannot request component creation from archive file {}, it is not a file", archiveFile));
      }

      final String archiveFileUri = archiveFile.getName().getURI();
      final FileObject archiveContents = resourceService.resolve("zip:" + archiveFileUri + "!/");
      if(!archiveContents.exists() || !archiveContents.isFolder()) {
        throw new MetaborgException(logger.format(
          "Cannot request component creation from archive file {}, it is not a zip archive", archiveFile));
      }

      if(!componentConfigService.available(archiveContents)) {
        throw new MetaborgException(logger.format(
          "Cannot request component creation from archive file {}, there is no component config file inside the archive",
          archiveFile));
      }

      return request(archiveContents);
    } catch(IOException e) {
      throw new MetaborgException(logger.format(
        "Cannot request component creation from archive file {}, unexpected I/O error", archiveFile), e);
    }
  }

  public IComponentCreationConfigRequest request(FileObject root) throws MetaborgException {
    final Collection<String> errors = Lists.newLinkedList();
    final Collection<Throwable> exceptions = Lists.newLinkedList();

    final ConfigRequest<ILanguageComponentConfig> configRequest = componentConfigService.get(root);
    if(!configRequest.valid()) {
      for(IMessage message : configRequest.errors()) {
        errors.add(message.message());
        final Throwable exception = message.exception();
        if(exception != null) {
          exceptions.add(exception);
        }
      }
    }

    final ILanguageComponentConfig config = configRequest.config();
    if(config == null) {
      final String message = logger.format("Cannot retrieve language component configuration at {}", root);
      errors.add(message);
      return new ComponentFactoryRequest(root, errors, exceptions);
    }

    final IStrategoAppl esvTerm;
    try {
      final FileObject esvFile = root.resolveFile("target/metaborg/editor.esv.af");
      if(!esvFile.exists()) {
        esvTerm = null;
      } else {
        esvTerm = esvTerm(root, esvFile);
      }
    } catch(ParseError | IOException | MetaborgException e) {
      exceptions.add(e);
      return new ComponentFactoryRequest(root, errors, exceptions);
    }

    SyntaxFacet syntaxFacet = null;
    StrategoRuntimeFacet strategoRuntimeFacet = null;
    if(esvTerm != null) {
      try {
        syntaxFacet = SyntaxFacetFromESV.create(esvTerm, root);
        if(syntaxFacet != null) {
          Iterables.addAll(errors, syntaxFacet.available());
        }
      } catch(FileSystemException e) {
        exceptions.add(e);
      }

      try {
        strategoRuntimeFacet = GradleStrategoRuntimeFacetFromESV.create(esvTerm, root);
        if(strategoRuntimeFacet != null) {
          Iterables.addAll(errors, strategoRuntimeFacet.available(resourceService));
        }
      } catch(IOException e) {
        exceptions.add(e);
      }
    }

    final ComponentFactoryRequest request;
    if(errors.isEmpty() && exceptions.isEmpty()) {
      request = new ComponentFactoryRequest(root, config, esvTerm, syntaxFacet, strategoRuntimeFacet);
    } else {
      request = new ComponentFactoryRequest(root, errors, exceptions);
    }
    return request;
  }

  private IStrategoAppl esvTerm(FileObject location, FileObject esvFile)
    throws ParseError, IOException, MetaborgException {
    final TermReader reader =
      new TermReader(termFactoryService.getGeneric().getFactoryWithStorageType(IStrategoTerm.MUTABLE));
    final IStrategoTerm term = reader.parseFromStream(esvFile.getContent().getInputStream());
    if(term.getTermType() != IStrategoTerm.APPL) {
      final String message = logger.format(
        "Cannot discover language at {}, ESV file at {} does not contain a valid ESV term", location, esvFile);
      throw new MetaborgException(message);
    }
    return (IStrategoAppl) term;
  }
}
