package nl.martijndwars.spoofax.tasks;

import nl.martijndwars.spoofax.SpoofaxPlugin;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.CleanInputBuilder;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;

import static nl.martijndwars.spoofax.SpoofaxInit.*;

public class LanguageClean extends AbstractTask {
  @TaskAction
  public void run() throws MetaborgException, InterruptedException {
    SpoofaxPlugin.loadCompileLanguageDependencies(getProject());

    CleanInput input = new CleanInputBuilder(spoofaxProject(getProject()))
      .withSelector(new SpoofaxIgnoresSelector())
      .build(getSpoofax(getProject()).dependencyService);

    getSpoofax(getProject()).processorRunner.clean(input, null, null).schedule().block();
    getSpoofaxMeta(getProject()).metaBuilder.clean(buildInput(getProject()));
  }
}
