package nl.martijndwars.spoofax;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxProjectConfigService;
import nl.martijndwars.spoofax.tasks.*;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.config.IProjectConfigService;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static nl.martijndwars.spoofax.SpoofaxInit.*;
import static nl.martijndwars.spoofax.SpoofaxPluginConstants.*;
import static nl.martijndwars.spoofax.Utils.archiveFileName;
import static org.gradle.api.plugins.JavaPlugin.*;

@NonNullApi
public class SpoofaxPlugin implements Plugin<Project> {
  public static final String SPOOFAX_CORE_DEPENDENCY = "org.metaborg:org.metaborg.spoofax.core:2.5.1";

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
    pluginManager.apply(JavaPlugin.class);
    pluginManager.apply(EclipseCompilerPlugin.class);

    configureRepository(project);
    configureExtension(project);
    configureConfigurations(project);
    configureTasks(project);
    configureJava(project);

    // Delay configuration until the SpoofaxPluginExtension is configured and repositories have been defined
    project.afterEvaluate(innerProject -> {
      configureEcj(innerProject);
      configureArtifact(innerProject);
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

    Configuration runtimeElementsConfiguration = configurations.getByName(RUNTIME_ELEMENTS_CONFIGURATION_NAME);
    runtimeElementsConfiguration.extendsFrom(spoofaxLanguageConfiguration);
  }

  private void configureTasks(Project project) {
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

    // Map extension properties to task properties
    SpoofaxPluginExtension extension = project.getExtensions().getByType(SpoofaxPluginExtension.class);

    compileLanguageTask.getStrategoFormat().set(extension.getStrategoFormat());
    compileLanguageTask.getLanguageVersion().set(extension.getLanguageVersion());
    compileLanguageTask.getOverrides().set(extension.getOverrides());

    archiveLanguageTask.getStrategoFormat().set(extension.getStrategoFormat());
    archiveLanguageTask.getLanguageVersion().set(extension.getLanguageVersion());
    archiveLanguageTask.getOverrides().set(extension.getOverrides());

    spxLanguageTask.getStrategoFormat().set(extension.getStrategoFormat());
    spxLanguageTask.getLanguageVersion().set(extension.getLanguageVersion());
    spxLanguageTask.getOverrides().set(extension.getOverrides());

    checkLanguageTask.getStrategoFormat().set(extension.getStrategoFormat());
    checkLanguageTask.getLanguageVersion().set(extension.getLanguageVersion());
    checkLanguageTask.getOverrides().set(extension.getOverrides());

    // Hook into the lifecycle tasks exposed by the base plugin
    Task cleanTask = tasks.getByName(BasePlugin.CLEAN_TASK_NAME);
    cleanTask.dependsOn(cleanLanguageTask);

    Task assembleTask = tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME);
    assembleTask.dependsOn(archiveLanguageTask);

    Task checkTask = tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME);
    checkTask.dependsOn(checkLanguageTask);

    // This project's compileLanguage task depends on the archiveLanguage task of each of this project's project dependencies
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration configuration = configurations.getByName(LANGUAGE_CONFIGURATION_NAME);
    TaskDependency taskDependency = configuration.getTaskDependencyFromProjectDependency(true, ARCHIVE_LANGUAGE_TASK_NAME);
    compileLanguageTask.dependsOn(taskDependency);
  }

  private void configureJava(Project project) {
    // Remove the jar artifact that was added by the Java plugin (we only build a .spoofax-language artifact)
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration runtimeConfiguration = configurations.getByName(RUNTIME_ELEMENTS_CONFIGURATION_NAME);
    runtimeConfiguration.getArtifacts().removeIf(artifact ->
      artifact.getType().equals(ArtifactTypeDefinition.JAR_TYPE)
    );

    Configuration runtimeElementsConfiguration = configurations.getByName(RUNTIME_ELEMENTS_CONFIGURATION_NAME);
    runtimeElementsConfiguration.getArtifacts().removeIf(artifact ->
      artifact.getType().equals(ArtifactTypeDefinition.JAR_TYPE)
    );

    // Modify source set to use the classic Spoofax language directory layout
    Convention convention = project.getConvention();
    JavaPluginConvention javaPluginConvention = convention.getPlugin(JavaPluginConvention.class);
    SourceSetContainer sourceSets = javaPluginConvention.getSourceSets();
    SourceSet sourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    sourceSet.java(files -> files.setSrcDirs(getJavaSourceDirs()));

    // Add compileOnly dependency on org.metaborg.spoofax.core to the project
    DependencyHandler dependencies = project.getDependencies();
    Dependency dependency = dependencies.create(SPOOFAX_CORE_DEPENDENCY);
    Configuration compileOnlyConfiguration = configurations.getByName(COMPILE_ONLY_CONFIGURATION_NAME);
    compileOnlyConfiguration.getDependencies().add(dependency);

    // Hook into the tasks exposed by the java plugin
    TaskContainer tasks = project.getTasks();
    Task compileJava = tasks.getByName(COMPILE_JAVA_TASK_NAME);
    Task compileLanguage = tasks.getByName(COMPILE_LANGUAGE_TASK_NAME);
    Task archiveLanguage = tasks.getByName(ARCHIVE_LANGUAGE_TASK_NAME);

    compileJava.dependsOn(compileLanguage);
    archiveLanguage.dependsOn(compileJava);
  }

  // TODO: Use SpoofaxCommonPaths here?
  private Iterable<String> getJavaSourceDirs() {
    return Lists.newArrayList(
      "src/main/strategies",
      "src/main/ds",
      "src-gen/stratego-java",
      "src-gen/ds-java"
    );
  }

  private void configureExtension(Project project) {
    ExtensionContainer extensions = project.getExtensions();
    extensions.create(EXTENSION_NAME, SpoofaxPluginExtension.class, project);
  }

  private void configureEcj(Project project) {
    TaskContainer tasks = project.getTasks();
    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class).configure(task ->
      // NOTE: Use anonymous class instead of lambda (https://github.com/gradle/gradle/issues/5510)
      task.doLast(new Action<Task>() {
        @Override
        public void execute(Task task) {
          project.copy(copySpec -> {
            copySpec.from(project.getBuildDir() + "/classes/java/main");
            copySpec.into("target/classes");
          });
        }
      })
    );
  }

  private void configureArtifact(Project project) {
    TaskContainer tasks = project.getTasks();
    LanguageArchive archiveLanguageTask = (LanguageArchive) tasks.getByName(ARCHIVE_LANGUAGE_TASK_NAME);

    ArtifactHandler artifacts = project.getArtifacts();
    artifacts.add(SPOOFAX_LANGUAGE_CONFIGURATION_NAME, archiveLanguageTask.getLazyOutputFile(), configureArtifact ->
      configureArtifact.builtBy(archiveLanguageTask)
    );
  }

  private void configureArchiveTask(Project project) {
    TaskContainer tasks = project.getTasks();

    tasks.named(ARCHIVE_LANGUAGE_TASK_NAME, LanguageArchive.class).configure(languageArchive -> {
      Property<String> strategoFormat = languageArchive.getStrategoFormat();
      Property<String> languageVersion = languageArchive.getLanguageVersion();
      ListProperty<String> overrides = languageArchive.getOverrides();
      ISpoofaxLanguageSpec languageSpec = overridenLanguageSpec(project, strategoFormat, languageVersion, overrides);

      File outputFile = project.file("target/" + archiveFileName(languageSpec));

      languageArchive.getOutputFile().set(outputFile);
    });

    tasks.named(SPX_LANGUAGE_TASK_NAME, LanguageSpx.class).configure(languageSpx -> {
      Property<String> strategoFormat = languageSpx.getStrategoFormat();
      Property<String> languageVersion = languageSpx.getLanguageVersion();
      ListProperty<String> overrides = languageSpx.getOverrides();
      ISpoofaxLanguageSpec languageSpec = overridenLanguageSpec(project, strategoFormat, languageVersion, overrides);

      // TODO: The input to languageSpx should be the output of languageArchive defined above
      File inputFile = project.file("target/" + archiveFileName(languageSpec));

      languageSpx.getInputFile().set(inputFile);
      languageSpx.getArchiveBaseName().set(languageSpec.config().name());
      languageSpx.getArchiveVersion().set(languageSpec.config().identifier().version.toString());
    });
  }

  private void configureOverrides(Project project) {
    SpoofaxPluginExtension spoofaxPluginExtension = getExtension(project);
    List<String> overrides = spoofaxPluginExtension.getOverrides().get();

    GradleSpoofaxProjectConfigService projectConfigService = (GradleSpoofaxProjectConfigService) getSpoofax(project).injector.getInstance(IProjectConfigService.class);
    projectConfigService.setOverrides(overrides);

    for (Object override : overrides) {
      project.getLogger().info("Override language dependency " + override);
    }
  }

  private void configureLanugageDependencies(Project project) {
    project.getLogger().info("Configure language dependencies");

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

  public static SpoofaxPluginExtension getExtension(Project project) {
    ExtensionContainer extensions = project.getExtensions();

    return extensions.findByType(SpoofaxPluginExtension.class);
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
    Configuration sourceLanguageConfiguration = configurations.getByName(SOURCE_LANGUAGE_CONFIGURATION_NAME);
    FileCollection sourceLanguageFiles = sourceLanguageConfiguration.getIncoming().getFiles();
    FileCollection spoofaxSourceLanguageFiles = sourceLanguageFiles.filter(file -> file.getName().endsWith("spoofax-language"));

    loadLanguages(project, spoofaxSourceLanguageFiles);
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
    FileObject archiveFile = getSpoofax(project).resourceService.resolve(file);

    try {
      ILanguageImpl languageImpl = loadLanguage(project, archiveFile);

      for (ILanguageComponent languageComponent : languageImpl.components()) {
        project.getLogger().info("Loaded {}", languageComponent);
      }
    } catch (FileSystemException e) {
      throw new RuntimeException("Unable to load language from " + file, e);
    }
  }

  protected static ILanguageImpl loadLanguage(Project project, FileObject archiveFile) throws FileSystemException, MetaborgException {
    if (archiveFile.isFile()) {
      return getSpoofax(project).languageDiscoveryService.languageFromArchive(archiveFile);
    } else {
      return getSpoofax(project).languageDiscoveryService.languageFromDirectory(archiveFile);
    }
  }
}
