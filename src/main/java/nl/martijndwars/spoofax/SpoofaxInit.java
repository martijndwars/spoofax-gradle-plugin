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
  public static Map<Project, SpoofaxTuple> spoofaxCache;

  static {
    spoofaxCache = new HashMap<>();
  }

  public static Spoofax getSpoofax(Project project) {
    SpoofaxTuple spoofaxTuple = getSpoofaxTuple(project);

    return spoofaxTuple.spoofax;
  }

  public static SpoofaxMeta getSpoofaxMeta(Project project) {
    SpoofaxTuple spoofaxTuple = getSpoofaxTuple(project);

    return spoofaxTuple.spoofaxMeta;
  }

  public static Injector getSptInjector(Project project) {
    return getSpoofaxMeta(project).injector.createChildInjector(new SPTModule());
  }

  private static SpoofaxTuple getSpoofaxTuple(Project project) {
    if (spoofaxCache.containsKey(project)) {
      return spoofaxCache.get(project);
    }

    try {
      Spoofax spoofax = new Spoofax(new SpoofaxGradleModule(), new SpoofaxExtensionModule());
      SpoofaxMeta spoofaxMeta = new SpoofaxMeta(spoofax);
      SpoofaxTuple spoofaxTuple = new SpoofaxTuple(spoofax, spoofaxMeta);
      spoofaxCache.put(project, spoofaxTuple);

      return spoofaxTuple;
    } catch (Exception e) {
      throw new RuntimeException("Unable to create the Spoofax instance.", e);
    }
  }

  public static IProject spoofaxProject(Project project) throws MetaborgException {
    File projectDir = project.getProjectDir();
    FileObject location = getSpoofax(project).resourceService.resolve(projectDir);

    return getOrCreateSpoofaxProject(project, location);
  }

  public static IProject getOrCreateSpoofaxProject(Project project, FileObject location) throws MetaborgException {
    ISimpleProjectService projectService = (ISimpleProjectService) getSpoofax(project).projectService;

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
    return getSpoofaxMeta(project).languageSpecService.get(spoofaxProject(project));
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

  private static class SpoofaxTuple {
    public Spoofax spoofax;
    public SpoofaxMeta spoofaxMeta;

    public SpoofaxTuple(Spoofax spoofax, SpoofaxMeta spoofaxMeta) {
      this.spoofax = spoofax;
      this.spoofaxMeta = spoofaxMeta;
    }
  }
}