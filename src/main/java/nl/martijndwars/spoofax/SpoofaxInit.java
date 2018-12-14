package nl.martijndwars.spoofax;

import org.gradle.api.GradleException;
import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;

public class SpoofaxInit {
  public static Spoofax spoofax;
  public static SpoofaxMeta spoofaxMeta;

  static {
    try {
      spoofax = new Spoofax(new SpoofaxGradleModule());
      spoofaxMeta = new SpoofaxMeta(spoofax);
    } catch (MetaborgException e) {
      throw new GradleException("Unable to load Spoofax", e);
    }
  }
}