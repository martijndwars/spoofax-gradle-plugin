package nl.martijndwars.spoofax.spoofax;

import com.google.common.collect.Sets;
import nl.martijndwars.spoofax.SpoofaxOverrides;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.spoofax.core.dynamicclassloading.DynamicClassLoadingFacet;
import org.metaborg.spoofax.core.esv.ESVReader;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;

import java.util.Set;

public class GradleDynamicClassLoadingFacetFromESV {
  private static final ILogger logger = LoggerUtils.logger(GradleDynamicClassLoadingFacetFromESV.class);

  public static DynamicClassLoadingFacet create(IStrategoAppl esv, FileObject location) throws FileSystemException {
    final Set<FileObject> strategoFiles = providerResources(esv, location);
    final Set<FileObject> jarFiles = Sets.newLinkedHashSet();
    for (FileObject strategoFile : strategoFiles) {
      String projectName = location.getName().getBaseName();
      String strategoFormat = SpoofaxOverrides.get(projectName);
      String strategoFileName = strategoFile.getName().getBaseName();
      if (strategoFormat != null) {
        if (strategoFormat.equals("jar") && strategoFileName.equals("stratego.ctree")) {
          jarFiles.add(location.resolveFile("target/metaborg/stratego.jar"));
          continue;
        }
      }

      final String extension = strategoFile.getName().getExtension();
      switch (extension) {
        case "jar":
          jarFiles.add(strategoFile);
          break;
        case "ctree":
          break;
        default:
          logger.warn("Stratego provider file {} has unknown extension {}, ignoring", strategoFile, extension);
          break;
      }
    }

    return new DynamicClassLoadingFacet(jarFiles);
  }

  private static Set<FileObject> providerResources(IStrategoAppl esv, FileObject location) throws FileSystemException {
    final Set<FileObject> attachedFiles = Sets.newLinkedHashSet();
    for (IStrategoAppl s : ESVReader.collectTerms(esv, "SemanticProvider")) {
      attachedFiles.add(location.resolveFile(ESVReader.termContents(s)));
    }
    return attachedFiles;
  }
}

