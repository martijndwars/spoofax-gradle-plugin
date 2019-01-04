package nl.martijndwars.spoofax;

import com.google.common.collect.Lists;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.compile.JavaCompile;

@NonNullApi
public class EclipseCompilerPlugin implements Plugin<Project> {
  public static final String ECJ_CONFIGURATION = "ecj";
  public static final String ECJ_DEPENDENCY = "org.eclipse.jdt:ecj:3.16.0";
  public static final String ECJ_MAIN = "org.eclipse.jdt.internal.compiler.batch.Main";
  public static final String ECJ_CP = "-classpath";
  public static final String ECJ_OPTS = "-nowarn";

  @Override
  public void apply(Project project) {
    project.afterEvaluate(this::configureEcj);
  }

  private void configureEcj(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration configuration = configurations.create(ECJ_CONFIGURATION);

    DependencyHandler dependencies = project.getDependencies();
    dependencies.add(ECJ_CONFIGURATION, ECJ_DEPENDENCY);

    TaskContainer tasks = project.getTasks();
    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class).configure(task -> {
      CompileOptions compileOptions = task.getOptions();
      compileOptions.setFork(true);

      ForkOptions forkOptions = compileOptions.getForkOptions();
      forkOptions.setExecutable("java");
      forkOptions.setJvmArgs(Lists.newArrayList(ECJ_CP, configuration.getAsPath(), ECJ_MAIN, ECJ_OPTS));
    });
  }
}
