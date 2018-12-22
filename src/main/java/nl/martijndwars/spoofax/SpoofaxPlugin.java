package nl.martijndwars.spoofax;

import com.google.common.collect.Collections2;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxProjectConfigService;
import nl.martijndwars.spoofax.tasks.*;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskDependency;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static nl.martijndwars.spoofax.SpoofaxInit.spoofax;
import static nl.martijndwars.spoofax.SpoofaxInit.spoofaxMeta;

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
   * The name of the configuration that extends compileLanguage & sourceLanguage
   */
  public static final String LANGUAGE_CONFIGURATION_NAME = "language";

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
   * The name of the task that is an AbstractArchiveTask.
   */
  public static final String SPX_LANGUAGE_TASK_NAME = "spx";

  /**
   * The name of the task that verifies the language artifacts.
   */
  public static final String CHECK_LANGUAGE_TASK_NAME = "checkLanguage";

  protected final BaseRepositoryFactory repositoryFactory;
  protected final DependencyFactory dependencyFactory;

  @Inject
  public SpoofaxPlugin(BaseRepositoryFactory repositoryFactory, DependencyFactory dependencyFactory) {
    this.repositoryFactory = repositoryFactory;
    this.dependencyFactory = dependencyFactory;
  }

  @Override
  public void apply(Project project) {
    PluginManager pluginManager = project.getPluginManager();
    pluginManager.apply(org.gradle.api.plugins.BasePlugin.class);

    configureRepository(project);
    configureConfigurations(project);
    configureBuild(project);
    configureExtension(project);
    configureArtifact(project);

    // Delay configuration until the languageVersion and overrides (on SpoofaxExtension) are set
    project.afterEvaluate(innerProject -> {
      configureArchiveTask(innerProject);
      configureOverrides(innerProject);
      configureLanugageDependencies(innerProject);
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

    Configuration languageConfiguration = configurations.create(LANGUAGE_CONFIGURATION_NAME);
    languageConfiguration.extendsFrom(compileLanguageConfiguration, sourceLanguageConfiguration);

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

    LanguageSpx spxLanguageTask = tasks.create(SPX_LANGUAGE_TASK_NAME, LanguageSpx.class);
    spxLanguageTask.setGroup(BasePlugin.BUILD_GROUP);
    spxLanguageTask.setDescription("Archive a Spoofax language project.");
    spxLanguageTask.dependsOn(archiveLanguageTask);

    LanguageCheck checkLanguageTask = tasks.create(CHECK_LANGUAGE_TASK_NAME, LanguageCheck.class);
    checkLanguageTask.setGroup(BasePlugin.BUILD_GROUP);
    checkLanguageTask.setDescription("Verify a Spoofax language project.");
    checkLanguageTask.dependsOn(archiveLanguageTask);

    // Hook into the lifecycle tasks exposed by the base plugin
    Task cleanTask = tasks.getByName(BasePlugin.CLEAN_TASK_NAME);
    cleanTask.dependsOn(cleanLanguageTask);

    Task assembleTask = tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME);
    assembleTask.dependsOn(archiveLanguageTask);

    // This project's compileLanguage task depends on the archiveLanguage task of each of this project's project dependencies
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration configuration = configurations.getByName(LANGUAGE_CONFIGURATION_NAME);
    TaskDependency taskDependency = configuration.getTaskDependencyFromProjectDependency(true, ARCHIVE_LANGUAGE_TASK_NAME);
    compileLanguageTask.dependsOn(taskDependency);
  }

  private void configureExtension(Project project) {
    SpoofaxExtension extension = getOrCreateExtension(project);

    project.getTasks().named(COMPILE_LANGUAGE_TASK_NAME, LanguageCompile.class).configure(languageCompile -> {
      languageCompile.getStrategoFormat().set(extension.getStrategoFormat());
      languageCompile.getLanguageVersion().set(extension.getLanguageVersion());
      languageCompile.getOverrides().set(extension.getOverrides());
    });

    project.getTasks().named(ARCHIVE_LANGUAGE_TASK_NAME, LanguageArchive.class).configure(languageArchive -> {
      languageArchive.getStrategoFormat().set(extension.getStrategoFormat());
      languageArchive.getLanguageVersion().set(extension.getLanguageVersion());
      languageArchive.getOverrides().set(extension.getOverrides());
    });

    project.getTasks().named(SPX_LANGUAGE_TASK_NAME, LanguageSpx.class).configure(languageSpx -> {
      languageSpx.getStrategoFormat().set(extension.getStrategoFormat());
      languageSpx.getLanguageVersion().set(extension.getLanguageVersion());
      languageSpx.getOverrides().set(extension.getOverrides());
    });

    project.getTasks().named(CHECK_LANGUAGE_TASK_NAME, LanguageCheck.class).configure(languageCheck -> {
      languageCheck.getStrategoFormat().set(extension.getStrategoFormat());
      languageCheck.getLanguageVersion().set(extension.getLanguageVersion());
      languageCheck.getOverrides().set(extension.getOverrides());
    });
  }

  private void configureArtifact(Project project) {
    TaskContainer tasks = project.getTasks();
    LanguageArchive archiveLanguageTask = (LanguageArchive) tasks.getByName(ARCHIVE_LANGUAGE_TASK_NAME);
    RegularFileProperty outputFile = archiveLanguageTask.getOutputFile();

    ArtifactHandler artifacts = project.getArtifacts();
    artifacts.add(SPOOFAX_LANGUAGE_CONFIGURATION_NAME, outputFile, configureArtifact ->
      configureArtifact.builtBy(archiveLanguageTask)
    );
  }

  private void configureArchiveTask(Project project) {
    TaskContainer tasks = project.getTasks();
    tasks.named(ARCHIVE_LANGUAGE_TASK_NAME, LanguageArchive.class).configure(languageArchive -> {
      try {
        File outputFile = project.file("target/" + Utils.archiveFileName(languageSpec(project)));

        languageArchive.getOutputFile().set(outputFile);
      } catch (MetaborgException e) {
        e.printStackTrace();
      }
    });

    tasks.named(SPX_LANGUAGE_TASK_NAME, LanguageSpx.class).configure(languageSpx -> {
      try {
        File inputFile = project.file("target/" + Utils.archiveFileName(languageSpec(project)));

        languageSpx.getInputFile().set(inputFile);
        
        languageSpx.setBaseName(languageSpec(project).config().name());
        languageSpx.setVersion(getOrCreateExtension(project).getLanguageVersion().get());
      } catch (MetaborgException e) {
        e.printStackTrace();
      }
    });
  }

  private void configureOverrides(Project project) {
    SpoofaxExtension spoofaxExtension = getOrCreateExtension(project);
    List<String> overrides = spoofaxExtension.getOverrides().get();

    GradleSpoofaxProjectConfigService projectConfigService = (GradleSpoofaxProjectConfigService) spoofax.injector.getInstance(IProjectConfigService.class);
    projectConfigService.setOverrides(overrides);

    for (Object override : overrides) {
      project.getLogger().info("Override language dependency " + override);
    }
  }

  private void configureLanugageDependencies(Project project) {
    project.getLogger().info("Configure language dependencies");

    try {
      IProjectConfig config = spoofaxProject(project).config();

      ConfigurationContainer configurations = project.getConfigurations();
      Configuration compileLanguageConfiguration = configurations.getByName(COMPILE_LANGUAGE_CONFIGURATION_NAME);
      Configuration sourceLanguageConfiguration = configurations.getByName(SOURCE_LANGUAGE_CONFIGURATION_NAME);

      Collection<Dependency> compileDependencies = createDependencies(project, config.compileDeps());
      Collection<Dependency> sourceDependencies = createDependencies(project, config.sourceDeps());

      compileLanguageConfiguration.getDependencies().addAll(compileDependencies);

      for (Dependency dependency : sourceDependencies) {
        if (!contains(sourceLanguageConfiguration.getDependencies(), dependency)) {
          project.getLogger().info("Add " + dependency + " to the sourceLanguage configuration.");

          sourceLanguageConfiguration.getDependencies().add(dependency);
        } else {
          project.getLogger().info("Do not add " + dependency + ", because there is already a dependency with the same <group>:<name>");
        }
      }
    } catch (MetaborgException e) {
      throw new RuntimeException("Unable to load projct config.");
    }
  }

  private boolean contains(DependencySet dependencies, Dependency dependency) {
    for (Dependency d : dependencies) {
      if (Objects.equals(d.getGroup(), dependency.getGroup())) {
        if (Objects.equals(d.getName(), dependency.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  public static SpoofaxExtension getOrCreateExtension(Project project) {
    ExtensionContainer extensions = project.getExtensions();
    SpoofaxExtension spoofaxExtension = extensions.findByType(SpoofaxExtension.class);

    if (spoofaxExtension != null) {
      return spoofaxExtension;
    }

    return extensions.create("spoofax", SpoofaxExtension.class, project);
  }

  public static void loadLanguageDependencies(Project project) throws MetaborgException {
    project.getLogger().info("Loading language components from dependencies");

    loadCompileLanguageDependencies(project);
    loadSourceLanguageDependencies(project);
  }

  public static void loadCompileLanguageDependencies(Project project) throws MetaborgException {
    project.getLogger().info("Loading compile language components dependencies");

    ConfigurationContainer configurations = project.getConfigurations();
    Configuration compileLanguageConfiguration = configurations.getByName(COMPILE_LANGUAGE_CONFIGURATION_NAME);
    FileCollection compileLanguageFiles = compileLanguageConfiguration.getIncoming().getFiles();
    loadLanguages(project, compileLanguageFiles);
  }

  public static void loadSourceLanguageDependencies(Project project) throws MetaborgException {
    project.getLogger().info("Loading source language components dependencies");

    ConfigurationContainer configurations = project.getConfigurations();
    Configuration compileLanguageConfiguration = configurations.getByName(SOURCE_LANGUAGE_CONFIGURATION_NAME);
    FileCollection compileLanguageFiles = compileLanguageConfiguration.getIncoming().getFiles();
    loadLanguages(project, compileLanguageFiles);
  }

  protected ISpoofaxLanguageSpec languageSpec(Project project) throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = spoofaxMeta.languageSpecService.get(spoofaxProject(project));
    SpoofaxExtension spoofaxExtension = getOrCreateExtension(project);

    return new GradleSpoofaxLanguageSpec(languageSpec, spoofaxExtension);
  }

  public static IProject spoofaxProject(Project project) throws MetaborgException {
    File projectDir = project.getProjectDir();
    FileObject location = spoofax.resourceService.resolve(projectDir);

    return getOrCreateProject(location);
  }

  public static IProject getOrCreateProject(FileObject location) throws MetaborgException {
    ISimpleProjectService projectService = (ISimpleProjectService) spoofax.projectService;

    if (projectService.get(location) != null) {
      return projectService.get(location);
    }

    return projectService.create(location);
  }

  public static Collection<Dependency> createDependencies(Project project, Collection<LanguageIdentifier> identifiers) {
    return Collections2.transform(identifiers, languageIdentifier ->
      createDependency(project, languageIdentifier)
    );
  }

  public static Dependency createDependency(Project project, LanguageIdentifier languageIdentifier) {
    DependencyHandler dependencies = project.getDependencies();

    return dependencies.create(languageIdentifier.toString());
  }

  public static void loadLanguages(Project project, FileCollection files) throws MetaborgException {
    for (File file : files) {
      loadLanguage(project, file);
    }
  }

  public static synchronized void loadLanguage(Project project, File file) throws MetaborgException {
    project.getLogger().info("Loading language component from: " + file);
    FileObject archiveFile = spoofax.resourceService.resolve(file);

    try {
      ILanguageImpl languageImpl = loadLanguage(archiveFile);

      for (ILanguageComponent languageComponent : languageImpl.components()) {
        project.getLogger().info("Loaded {}", languageComponent);
      }
    } catch (FileSystemException e) {
      throw new RuntimeException("Unable to load language from " + file, e);
    }
  }

  protected static ILanguageImpl loadLanguage(FileObject archiveFile) throws FileSystemException, MetaborgException {
    if (archiveFile.isFile()) {
      return spoofax.languageDiscoveryService.languageFromArchive(archiveFile);
    } else {
      return spoofax.languageDiscoveryService.languageFromDirectory(archiveFile);
    }
  }
}
