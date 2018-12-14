package nl.martijndwars.spoofax.tasks;

import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.CleanInputBuilder;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;

public class LanguageClean extends LanguageTask {
  @TaskAction
  public void run() throws MetaborgException, InterruptedException {
    CleanInput input = new CleanInputBuilder(spoofaxProject())
        .withSelector(new SpoofaxIgnoresSelector())
        .build(spoofax.dependencyService);

    spoofax.processorRunner.clean(input, null, null).schedule().block();
    spoofaxMeta.metaBuilder.clean(buildInput());
  }
}
