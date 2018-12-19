package nl.martijndwars.spoofax;

import com.google.inject.Injector;
import org.gradle.api.GradleException;
import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.spt.core.SPTModule;

public class SpoofaxInit {
  public static Spoofax spoofax;
  public static SpoofaxMeta spoofaxMeta;
  public static Injector sptInjector;

  static {
    try {
      spoofax = new Spoofax(new SpoofaxGradleModule());
      spoofaxMeta = new SpoofaxMeta(spoofax);
      sptInjector = spoofaxMeta.injector.createChildInjector(new SPTModule());
    } catch (MetaborgException e) {
      throw new GradleException("Unable to load Spoofax", e);
    }
  }
}