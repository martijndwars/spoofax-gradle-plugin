package nl.martijndwars.spoofax;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.metaborg.core.language.LanguageContributionIdentifier;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.meta.core.project.ILanguageSpec;

import java.util.Collection;
import java.util.List;

public class Utils {
  public static final String SPOOFAX_EXTENSION = "spoofax-language";

  public static String archiveFileName(ILanguageSpec languageSpec) {
    LanguageIdentifier identifier = languageSpec.config().identifier();

    return identifier.toFileString() + "." + SPOOFAX_EXTENSION;
  }

  public static Collection<LanguageIdentifier> transformDeps(List<String> overrides, Collection<LanguageIdentifier> deps) {
    List<OverrideDependency> overrideDependencies = Lists.transform(overrides, OverrideDependency::new);

    Collection<LanguageIdentifier> transformedDeps = Collections2.transform(deps, dep ->
      transformDep(overrideDependencies, dep)
    );

    // Create a new list because transformedDeps is a view of deps and cannot be serialized
    return Lists.newArrayList(transformedDeps);
  }

  public static LanguageIdentifier transformDep(List<OverrideDependency> overrides, LanguageIdentifier languageIdentifier) {
    OverrideDependency override = getOverrideById(overrides, languageIdentifier.groupId, languageIdentifier.id);

    if (override == null) {
      return languageIdentifier;
    }

    LanguageVersion version = LanguageVersion.parse(override.version);

    return new LanguageIdentifier(languageIdentifier, version);
  }

  private static OverrideDependency getOverrideById(List<OverrideDependency> overrides, String groupId, String id) {
    for (OverrideDependency override : overrides) {
      if (override.groupId.equals(groupId) && override.id.equals(id)) {
        return override;
      }
    }

    return null;
  }

  public static Collection<LanguageContributionIdentifier> transformContribs(List<String> overrides, Collection<LanguageContributionIdentifier> contribs) {
    List<OverrideDependency> overrideDependencies = Lists.transform(overrides, OverrideDependency::new);

    Collection<LanguageContributionIdentifier> transformedContribs = Collections2.transform(contribs, contrib ->
      new LanguageContributionIdentifier(transformDep(overrideDependencies, contrib.id), contrib.name)
    );

    // Create a new list because transformedDeps is a view of deps and cannot be serialized
    return Lists.newArrayList(transformedContribs);
  }

  static class OverrideDependency {
    public final String groupId;
    public final String id;
    public final String version;

    public OverrideDependency(String notation) {
      String[] split = notation.split(":");

      if (split.length < 3) {
        throw new IllegalArgumentException("Notation does not conform to groupId:id:version notation.");
      }

      this.groupId = split[0];
      this.id = split[1];
      this.version = split[2];
    }
  }
}
