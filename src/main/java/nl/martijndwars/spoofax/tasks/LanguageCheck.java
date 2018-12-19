package nl.martijndwars.spoofax.tasks;

import com.google.common.collect.Iterables;
import nl.martijndwars.spoofax.SpoofaxPlugin;
import nl.martijndwars.spoofax.spoofax.GradleSpoofaxLanguageSpec;
import org.gradle.api.tasks.TaskAction;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spt.core.SPTRunner;

public class LanguageCheck extends LanguageTask {
  public static final String GROUP_ID = "org.metaborg";
  public static final String LANG_SPT_ID = "org.metaborg.meta.lang.spt";

  @TaskAction
  public void run() throws MetaborgException {
    SpoofaxPlugin.loadLanguageDependencies(getProject());

    ILanguageImpl sptLanguageImpl = getSptLanguageImpl();

    if (sptLanguageImpl == null) {
      return;
    }

    ILanguageImpl languageImpl = spoofax.languageService.getImpl(languageSpec().config().identifier());

    if (languageImpl == null) {
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

  @Override
  protected ISpoofaxLanguageSpec languageSpec() throws MetaborgException {
    ISpoofaxLanguageSpec languageSpec = super.languageSpec();

    return new GradleSpoofaxLanguageSpec(languageSpec, null, null, null);
  }
}
