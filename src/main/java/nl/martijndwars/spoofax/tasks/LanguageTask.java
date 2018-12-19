package nl.martijndwars.spoofax.tasks;

import java.io.File;

import com.google.inject.Injector;
import nl.martijndwars.spoofax.SpoofaxInit;
import org.apache.commons.vfs2.FileObject;
import org.gradle.api.DefaultTask;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

public class LanguageTask extends DefaultTask {
  protected final Spoofax spoofax = SpoofaxInit.spoofax;
  protected final SpoofaxMeta spoofaxMeta = SpoofaxInit.spoofaxMeta;
  protected final Injector sptInjector = SpoofaxInit.sptInjector;

  protected IProject spoofaxProject() throws MetaborgException {
    File projectDir = getProject().getProjectDir();
    FileObject location = spoofax.resourceService.resolve(projectDir);

    return getOrCreateSpoofaxProject(location);
  }

  protected IProject getOrCreateSpoofaxProject(FileObject location) throws MetaborgException {
    ISimpleProjectService projectService = (ISimpleProjectService) spoofax.projectService;

    if (projectService.get(location) != null) {
      return projectService.get(location);
    }

    return projectService.create(location);
  }

  protected LanguageSpecBuildInput buildInput() throws MetaborgException {
    ISpoofaxLanguageSpec languageSpecification = languageSpec();

    return new LanguageSpecBuildInput(languageSpecification);
  }

  protected ISpoofaxLanguageSpec languageSpec() throws MetaborgException {
    return spoofaxMeta.languageSpecService.get(spoofaxProject());
  }
}
