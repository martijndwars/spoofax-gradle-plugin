package nl.martijndwars.spoofax;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import nl.martijndwars.spoofax.tasks.LanguageArchive;
import nl.martijndwars.spoofax.tasks.LanguageClean;
import nl.martijndwars.spoofax.tasks.LanguageCompile;
import org.apache.commons.vfs2.FileObject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskDependency;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;

import static nl.martijndwars.spoofax.SpoofaxInit.spoofax;

public class SpoofaxPlugin implements Plugin<Project> {
  /**
   * The name of the configuration that holds compile language dependencies.
   */
  public static final String COMPILE_LANGUAGE_CONFIGURATION_NAME = "compileLanguage";

  /**
   * The name of the configuration that holds source language dependencies.
   */
  public static final String SOURCE_LANGUAGE_CONFIGURATION_NAME = "sourceLanguage";

  /**
   * The name of the configuration that holds the built Spoofax language artifact.
   */
  private static final String SPOOFAX_LANGUAGE_CONFIGURATION_NAME = "spoofaxLanguage";

  /**
   * The name of the task that compiles the language project.
   */
  public static final String COMPILE_LANGUAGE_TASK_NAME = "compileLanguage";

  /**
   * The name of the task that cleans the language project.
   */
  public static final String CLEAN_LANGUAGE_TASK_NAME = "cleanLanguage";

  /**
   * The name of the task that archives the language artifacts.
   */
  public static final String ARCHIVE_LANGUAGE_TASK_NAME = "archiveLanguage";

  /**
   * The name of the type of language archive.
   */
  public static final String SPOOFAX_LANGUAGE_TYPE = "spoofax-language";

  protected final BaseRepositoryFactory repositoryFactory;

  @Inject
  public SpoofaxPlugin(BaseRepositoryFactory repositoryFactory) {
    this.repositoryFactory = repositoryFactory;
  }

  @Override
  public void apply(Project project) {
    PluginManager pluginManager = project.getPluginManager();
    pluginManager.apply(org.gradle.api.plugins.BasePlugin.class);

    configureRepository(project);
    configureConfigurations(project);
    configureBuild(project);
    configureArtifact(project);
    configureExtension(project);

    // Defer evaluation until after the repositories are defined
    project.afterEvaluate(innerProject -> {
      try {
        loadLanguageDependencies(innerProject);
      } catch (MetaborgException e) {
        throw new RuntimeException("An unexpected error occurred while loading language dependencies.", e);
      }
    });
  }

  private void configureRepository(Project project) {
    RepositoryHandler repositories = project.getRepositories();
    DefaultRepositoryHandler handler = (DefaultRepositoryHandler) repositories;
    SpoofaxRepositoryHandlerConvention repositoryConvention = new SpoofaxRepositoryHandlerConvention(handler, repositoryFactory);

    new DslObject(repositories).getConvention().getPlugins().put("spoofax", repositoryConvention);
  }

  private void configureConfigurations(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();

    Configuration compileLanguageConfiguration = configurations.create(COMPILE_LANGUAGE_CONFIGURATION_NAME);
    compileLanguageConfiguration.setTransitive(false);

    Configuration sourceLanguageConfiguration = configurations.create(SOURCE_LANGUAGE_CONFIGURATION_NAME);
    sourceLanguageConfiguration.setTransitive(false);

    Configuration spoofaxLanguageConfiguration = configurations.create(SPOOFAX_LANGUAGE_CONFIGURATION_NAME);
    spoofaxLanguageConfiguration.setTransitive(false);

    Configuration defaultConfiguration = configurations.getByName(Dependency.DEFAULT_CONFIGURATION);
    defaultConfiguration.extendsFrom(spoofaxLanguageConfiguration);
  }

  private void configureBuild(Project project) {
    TaskContainer tasks = project.getTasks();

    LanguageClean cleanLanguageTask = tasks.create(CLEAN_LANGUAGE_TASK_NAME, LanguageClean.class);
    cleanLanguageTask.setGroup(BasePlugin.BUILD_GROUP);
    cleanLanguageTask.setDescription("Clean a Spoofax language project.");

    LanguageCompile compileLanguageTask = tasks.create(COMPILE_LANGUAGE_TASK_NAME, LanguageCompile.class);
    compileLanguageTask.setGroup(BasePlugin.BUILD_GROUP);
    compileLanguageTask.setDescription("Compile a Spoofax language project.");

    LanguageArchive archiveLanguageTask = tasks.create(ARCHIVE_LANGUAGE_TASK_NAME, LanguageArchive.class);
    archiveLanguageTask.setGroup(BasePlugin.BUILD_GROUP);
    archiveLanguageTask.setDescription("Archive a Spoofax language project.");
    archiveLanguageTask.dependsOn(compileLanguageTask);

    // Hook into the lifecycle tasks exposed by the base plugin
    Task cleanTask = tasks.getByName(BasePlugin.CLEAN_TASK_NAME);
    cleanTask.dependsOn(cleanLanguageTask);

    Task assembleTask = tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME);
    assembleTask.dependsOn(archiveLanguageTask);

    // This project's compileLanguage task depends on the archiveLanguage task of each of this project's project dependencies
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration compileLanguageConfiguration = configurations.getByName(COMPILE_LANGUAGE_CONFIGURATION_NAME);
    TaskDependency dependency = compileLanguageConfiguration.getTaskDependencyFromProjectDependency(true, ARCHIVE_LANGUAGE_TASK_NAME);
    compileLanguageTask.dependsOn(dependency);
  }

  private void configureArtifact(Project project) {
    TaskContainer tasks = project.getTasks();
    LanguageArchive archiveLanguageTask = (LanguageArchive) tasks.getByName(ARCHIVE_LANGUAGE_TASK_NAME);
    File archiveFile = archiveLanguageTask.getArchiveFile();

    ArtifactHandler artifacts = project.getArtifacts();
    PublishArtifact artifact = artifacts.add(SPOOFAX_LANGUAGE_CONFIGURATION_NAME, archiveFile, configureArtifact -> {
      configureArtifact.builtBy(ARCHIVE_LANGUAGE_TASK_NAME);
    });
  }

  private void configureExtension(Project project) {
    ExtensionContainer extensions = project.getExtensions();
    SpoofaxExtension extension = extensions.create("spoofax", SpoofaxExtension.class, project);

    project.getTasks().named(COMPILE_LANGUAGE_TASK_NAME, LanguageCompile.class).configure(languageCompile -> {
      languageCompile.getStrategoFormat().set(extension.getStrategoFormat());
    });
  }

  private void loadLanguageDependencies(Project project) throws MetaborgException {
    project.getLogger().info("Loading language components from dependencies");

    IProjectConfig config = spoofaxProject(project).config();

    ConfigurationContainer configurations = project.getConfigurations();
    Configuration compileLanguageConfiguration = configurations.getByName(COMPILE_LANGUAGE_CONFIGURATION_NAME);
    Configuration sourceLanguageConfiguration = configurations.getByName(SOURCE_LANGUAGE_CONFIGURATION_NAME);

    Collection<Dependency> compileDependencies = createDependencies(project, config.compileDeps());
    Collection<Dependency> sourceDependencies = createDependencies(project, config.sourceDeps());

    compileLanguageConfiguration.getDependencies().addAll(compileDependencies);
    sourceLanguageConfiguration.getDependencies().addAll(sourceDependencies);

    loadLanguages(project, compileLanguageConfiguration.getIncoming().getFiles());
    loadLanguages(project, sourceLanguageConfiguration.getIncoming().getFiles());
  }

  protected IProject spoofaxProject(Project project) throws MetaborgException {
    File projectDir = project.getProjectDir();
    FileObject location = spoofax.resourceService.resolve(projectDir);

    return getOrCreateProject(location);
  }

  protected IProject getOrCreateProject(FileObject location) throws MetaborgException {
    ISimpleProjectService projectService = (ISimpleProjectService) spoofax.projectService;

    if (projectService.get(location) != null) {
      return projectService.get(location);
    }

    return projectService.create(location);
  }

  protected Collection<Dependency> createDependencies(Project project, Collection<LanguageIdentifier> languageIdentifiers) {
    return languageIdentifiers.stream()
        .map(languageIdentifier -> createDependency(project, languageIdentifier))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  protected Dependency createDependency(Project project, LanguageIdentifier languageIdentifier) {
    DependencyHandler dependencies = project.getDependencies();

    return dependencies.create(languageIdentifier.toString());
  }

  protected void loadLanguages(Project project, FileCollection files) throws MetaborgException {
    for (File file : files) {
      loadLanguage(project, file);
    }
  }

  protected void loadLanguage(Project project, File file) throws MetaborgException {
    project.getLogger().debug("Loading language component from file: " + file);

    FileObject archiveFile = spoofax.resourceService.resolve(file);

    ILanguageImpl languageImpl = spoofax.languageDiscoveryService.languageFromArchive(archiveFile);

    for (ILanguageComponent languageComponent : languageImpl.components()) {
      project.getLogger().info("Loaded {}", languageComponent);
    }
  }
}
