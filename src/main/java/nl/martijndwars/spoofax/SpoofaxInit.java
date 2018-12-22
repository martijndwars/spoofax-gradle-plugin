package nl.martijndwars.spoofax;

import com.google.inject.Injector;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
import org.apache.commons.vfs2.FileObject;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spt.core.SPTModule;

import java.io.File;

public class SpoofaxInit {
  public static Spoofax spoofax;
  public static SpoofaxMeta spoofaxMeta;
  public static Injector sptInjector;

  static {
    try {
      spoofax = new Spoofax(new SpoofaxGradleModule());
      spoofaxMeta = new SpoofaxMeta(spoofax);
      sptInjector = spoofaxMeta.injector.createChildInjector(new SPTModule());
    } catch (MetaborgException e) {
      throw new GradleException("Unable to load Spoofax", e);
    }
  }

  public static IProject spoofaxProject(Project project) throws MetaborgException {
    File projectDir = project.getProjectDir();
    FileObject location = spoofax.resourceService.resolve(projectDir);

    return getOrCreateSpoofaxProject(location);
  }

  public static IProject getOrCreateSpoofaxProject(FileObject location) throws MetaborgException {
    ISimpleProjectService projectService = (ISimpleProjectService) spoofax.projectService;

    if (projectService.get(location) != null) {
      return projectService.get(location);
    }

    return projectService.create(location);
  }

  public static LanguageSpecBuildInput buildInput(Project project) throws MetaborgException {
    ISpoofaxLanguageSpec languageSpecification = languageSpec(project);

    return new LanguageSpecBuildInput(languageSpecification);
  }

  public static ISpoofaxLanguageSpec languageSpec(Project project) throws MetaborgException {
    return spoofaxMeta.languageSpecService.get(spoofaxProject(project));
  }

  public static LanguageSpecBuildInput overridenBuildInput(
    Project project,
    Property<String> strategoFormat,
    Property<String> languageVersion,
    ListProperty<String> overrides
  ) throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = overridenLanguageSpec(project, strategoFormat, languageVersion, overrides);

    return new LanguageSpecBuildInput(languageSpec);
  }

  public static ISpoofaxLanguageSpec overridenLanguageSpec(
    Project project,
    Property<String> strategoFormat,
    Property<String> languageVersion,
    ListProperty<String> overrides
  ) throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = languageSpec(project);

    return new GradleSpoofaxLanguageSpec(languageSpec, strategoFormat, languageVersion, overrides);
  }
}