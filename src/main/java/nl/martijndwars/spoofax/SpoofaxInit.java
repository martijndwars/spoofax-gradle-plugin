package nl.martijndwars.spoofax;

import com.google.inject.Injector;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
import org.apache.commons.vfs2.FileObject;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxExtensionModule;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.spoofax.meta.core.build.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spt.core.SPTModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SpoofaxInit {
  public static Spoofax spoofax;
  public static SpoofaxMeta spoofaxMeta;

  static {
    try {
      spoofax = new Spoofax(new SpoofaxGradleModule(), new SpoofaxExtensionModule());
      spoofaxMeta = new SpoofaxMeta(spoofax);
    } catch (MetaborgException e) {
      e.printStackTrace();
    }
  }

  public static Spoofax getSpoofax(Project project) {
    return spoofax;
  }

  public static SpoofaxMeta getSpoofaxMeta(Project project) {
    return spoofaxMeta;
  }

  public static Injector getSptInjector(Project project) {
    return spoofaxMeta.injector.createChildInjector(new SPTModule());
  }

  public static IProject spoofaxProject(Project project) {
    File projectDir = project.getProjectDir();
    FileObject location = getSpoofax(project).resourceService.resolve(projectDir);

    return getOrCreateSpoofaxProject(project, location);
  }

  public static IProject getOrCreateSpoofaxProject(Project project, FileObject location) {
    ISimpleProjectService projectService = (ISimpleProjectService) getSpoofax(project).projectService;

    if (projectService.get(location) != null) {
      System.out.println("Use cached IProject for location = " + location);

      return projectService.get(location);
    }

    try {
      System.out.println("Create new IProject for location = " + location);

      return projectService.create(location);
    } catch (MetaborgException e) {
      throw new RuntimeException("Unable to create Spoofax project.", e);
    }
  }

  public static LanguageSpecBuildInput buildInput(Project project) {
    ISpoofaxLanguageSpec languageSpecification = languageSpec(project);

    return new LanguageSpecBuildInput(languageSpecification);
  }

  public static ISpoofaxLanguageSpec languageSpec(Project project) {
    try {
      return getSpoofaxMeta(project).languageSpecService.get(spoofaxProject(project));
    } catch (MetaborgException e) {
      throw new RuntimeException("Unable to retrieve language specification.", e);
    }
  }

  public static LanguageSpecBuildInput overridenBuildInput(
    Project project,
    Property<String> strategoFormat,
    Property<String> languageVersion,
    ListProperty<String> overrides
  ) {
    ISpoofaxLanguageSpec languageSpec = overridenLanguageSpec(project, strategoFormat, languageVersion, overrides);

    return new LanguageSpecBuildInput(languageSpec);
  }

  public static ISpoofaxLanguageSpec overridenLanguageSpec(
    Project project,
    Property<String> strategoFormat,
    Property<String> languageVersion,
    ListProperty<String> overrides
  ) {
    ISpoofaxLanguageSpec languageSpec = languageSpec(project);

    return new GradleSpoofaxLanguageSpec(languageSpec, strategoFormat, languageVersion, overrides);
  }

  private static class SpoofaxTuple {
    public Spoofax spoofax;
    public SpoofaxMeta spoofaxMeta;

    public SpoofaxTuple(Spoofax spoofax, SpoofaxMeta spoofaxMeta) {
      this.spoofax = spoofax;
      this.spoofaxMeta = spoofaxMeta;
    }
  }
}