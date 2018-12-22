package nl.martijndwars.spoofax.tasks;

import com.google.common.collect.Iterables;
import nl.martijndwars.spoofax.SpoofaxInit;
import nl.martijndwars.spoofax.SpoofaxPlugin;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spt.core.SPTRunner;

public class LanguageCheck extends LanguageTask {
  public static final String GROUP_ID = "org.metaborg";
  public static final String LANG_SPT_ID = "org.metaborg.meta.lang.spt";

  protected final Property<String> strategoFormat;
  protected final Property<String> languageVersion;
  protected final ListProperty<String> overrides;
  protected final Property<String> languageUnderTest;

  public LanguageCheck() {
    strategoFormat = getProject().getObjects().property(String.class);
    languageVersion = getProject().getObjects().property(String.class);
    overrides = getProject().getObjects().listProperty(String.class);
    languageUnderTest = getProject().getObjects().property(String.class);
  }

  @Input
  public Property<String> getStrategoFormat() {
    return strategoFormat;
  }

  @Input
  public Property<String> getLanguageVersion() {
    return languageVersion;
  }

  @Input
  public ListProperty<String> getOverrides() {
    return overrides;
  }

  @Input
  @Optional
  public Property<String> getLanguageUnderTest() {
    return languageUnderTest;
  }

  @TaskAction
  public void run() throws MetaborgException {
    SpoofaxPlugin.loadLanguageDependencies(getProject());

    if (!languageUnderTest.isPresent()) {
      getLogger().info("No language under test specified, loading self.");
      SpoofaxPlugin.loadLanguage(getProject(), getProject().getProjectDir());
    }

    LanguageIdentifier languageUnderTestIdentifier = getLanguageUnderTestIdentifier();
    getLogger().info("Language under test is " + languageUnderTestIdentifier);

    ILanguageImpl sptLanguageImpl = getSptLanguageImpl();

    if (sptLanguageImpl == null) {
      getLogger().info("Skipping tests because SPT language implementation is not a dependency");
      return;
    }

    ILanguageImpl languageImpl = spoofax.languageService.getImpl(languageUnderTestIdentifier);

    if (languageImpl == null) {
      getLogger().info("Skipping tests because language under test was not found.");
      return;
    }

    getLogger().info("Running SPT tests");
    sptInjector.getInstance(SPTRunner.class).test(languageSpec(), sptLanguageImpl, languageImpl);
  }

  protected ILanguageImpl getSptLanguageImpl() {
    Iterable<? extends ILanguageImpl> sptLangs = spoofax.languageService.getAllImpls(GROUP_ID, LANG_SPT_ID);

    final int sptLangsSize = Iterables.size(sptLangs);

    if (sptLangsSize == 0) {
      return null;
    }

    if (sptLangsSize > 1) {
      throw new RuntimeException("Multiple SPT language implementations were found.");
    }

    return Iterables.get(sptLangs, 0);
  }

  protected LanguageIdentifier getLanguageUnderTestIdentifier() throws MetaborgException {
    if (languageUnderTest.isPresent()) {
      return LanguageIdentifier.parseFull(languageUnderTest.get());
    } else {
      return languageSpec().config().identifier();
    }
  }

  @Override
  protected ISpoofaxLanguageSpec languageSpec() throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = SpoofaxInit.languageSpec(getProject());

    return new GradleSpoofaxLanguageSpec(languageSpec, strategoFormat, languageVersion, overrides);
  }
}
