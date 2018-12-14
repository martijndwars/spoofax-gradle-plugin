package nl.martijndwars.spoofax;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;

public class SpoofaxRepositoryHandlerConvention {
  public static final String METABORG_RELEASES_URL = "https://artifacts.metaborg.org/content/repositories/releases/";
  public static final String METABORG_SNAPSHOTS_URL = "https://artifacts.metaborg.org/content/repositories/snapshots/";
  public static final String METABORG_RELEASES_NAME = "metaborgReleases";
  public static final String METABORG_SNAPSHOTS_NAME = "metaborgSnapshots";

  private final DefaultRepositoryHandler container;
  private final BaseRepositoryFactory repositoryFactory;

  public SpoofaxRepositoryHandlerConvention(DefaultRepositoryHandler container, BaseRepositoryFactory repositoryFactory) {
    this.container = container;
    this.repositoryFactory = repositoryFactory;
  }

  public MavenArtifactRepository metaborgReleases() {
    return container.addRepository(createRepository(METABORG_RELEASES_URL), METABORG_RELEASES_NAME);
  }

  public MavenArtifactRepository metaborgSnapshots() {
    return container.addRepository(createRepository(METABORG_SNAPSHOTS_URL), METABORG_SNAPSHOTS_NAME);
  }

  private MavenArtifactRepository createRepository(String url) {
    MavenArtifactRepository mavenRepository = repositoryFactory.createMavenRepository();
    mavenRepository.setUrl(url);

    return mavenRepository;
  }
}
