package nl.martijndwars.spoofax;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;

public class SpoofaxRepositoryHandlerConvention {
  public static final String METABORG_RELEASES_NAME = "metaborgReleases";
  public static final String METABORG_RELEASES_URL = "https://artifacts.metaborg.org/content/repositories/releases/";

  public static final String METABORG_SNAPSHOTS_NAME = "metaborgSnapshots";
  public static final String METABORG_SNAPSHOTS_URL = "https://artifacts.metaborg.org/content/repositories/snapshots/";

  public static final String PLUTO_REPO_NAME = "plutoBuild";
  public static final String PLUTO_REPO_URL = "https://pluto-build.github.io/mvnrepository/";

  public static final String SUGAR_REPO_NAME = "sugarLang";
  public static final String SUGAR_REPO_URL = "https://sugar-lang.github.io/mvnrepository/";

  public static final String USE_THE_SOURCE_REPO_NAME = "useTheSource";
  public static final String USE_THE_SOURCE_REPO_URL = "http://nexus.usethesource.io/content/repositories/public/";

  private final DefaultRepositoryHandler container;
  private final BaseRepositoryFactory repositoryFactory;

  public SpoofaxRepositoryHandlerConvention(DefaultRepositoryHandler container, BaseRepositoryFactory repositoryFactory) {
    this.container = container;
    this.repositoryFactory = repositoryFactory;
  }

  public void spoofaxRepos() {
    this.metaborgReleases();
    this.metaborgSnapshots();
    this.plutoBuild();
    this.sugarLang();
    this.useTheSource();
  }

  public MavenArtifactRepository metaborgReleases() {
    return container.addRepository(createRepository(METABORG_RELEASES_URL), METABORG_RELEASES_NAME);
  }

  public MavenArtifactRepository metaborgSnapshots() {
    return container.addRepository(createRepository(METABORG_SNAPSHOTS_URL), METABORG_SNAPSHOTS_NAME);
  }

  public MavenArtifactRepository plutoBuild() {
    return container.addRepository(createRepository(PLUTO_REPO_URL), PLUTO_REPO_NAME);
  }

  public MavenArtifactRepository sugarLang() {
    return container.addRepository(createRepository(SUGAR_REPO_URL), SUGAR_REPO_NAME);
  }

  public MavenArtifactRepository useTheSource() {
    return container.addRepository(createRepository(USE_THE_SOURCE_REPO_URL), USE_THE_SOURCE_REPO_NAME);
  }

  private MavenArtifactRepository createRepository(String url) {
    MavenArtifactRepository mavenRepository = repositoryFactory.createMavenRepository();
    mavenRepository.setUrl(url);

    return mavenRepository;
  }
}
