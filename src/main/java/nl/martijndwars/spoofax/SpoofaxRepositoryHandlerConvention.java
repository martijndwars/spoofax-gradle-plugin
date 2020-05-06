package nl.martijndwars.spoofax;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;

public class SpoofaxRepositoryHandlerConvention {
  public static final String METABORG_RELEASES_NAME = "metaborgReleases";
  public static final String METABORG_RELEASES_URL = "https://artifacts.metaborg.org/content/repositories/releases/";

  public static final String METABORG_SNAPSHOTS_NAME = "metaborgSnapshots";
  public static final String METABORG_SNAPSHOTS_URL = "https://artifacts.metaborg.org/content/repositories/snapshots/";

  public static final String METABORG_PUBLIC_NAME = "metaborgPublic";
  public static final String METABORG_PUBLIC_URL = "https://artifacts.metaborg.org/content/groups/public/";

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
    this.metaborgPublic();
    this.sugarLang();
    this.useTheSource();
  }

  public MavenArtifactRepository metaborgReleases() {
    return container.addRepository(createRepository(METABORG_RELEASES_URL), METABORG_RELEASES_NAME, repository ->
      repository.mavenContent(content -> {
        content.releasesOnly();
        content.includeGroup("org.metaborg");
        content.includeGroup("org.apache.commons");
      })
    );
  }

  public MavenArtifactRepository metaborgSnapshots() {
    return container.addRepository(createRepository(METABORG_SNAPSHOTS_URL), METABORG_SNAPSHOTS_NAME, repository ->
      repository.mavenContent(content -> {
        content.snapshotsOnly();
        content.includeGroup("org.metaborg");
        content.includeGroup("org.apache.commons");
      })
    );
  }

  public MavenArtifactRepository metaborgPublic() {
    return container.addRepository(createRepository(METABORG_PUBLIC_URL), METABORG_PUBLIC_NAME, repository ->
      repository.mavenContent(content -> {
        content.includeGroup("build.pluto");
        content.includeGroup("com.cedarsoftware");
      })
    );
  }

  public MavenArtifactRepository sugarLang() {
    return container.addRepository(createRepository(SUGAR_REPO_URL), SUGAR_REPO_NAME, repository ->
      repository.mavenContent(content -> {
        content.includeGroup("org.sugarj");
      })
    );
  }

  public MavenArtifactRepository useTheSource() {
    return container.addRepository(createRepository(USE_THE_SOURCE_REPO_URL), USE_THE_SOURCE_REPO_NAME, repository ->
      repository.mavenContent(content -> {
        content.includeGroup("io.usethesource");
      })
    );
  }

  private MavenArtifactRepository createRepository(String url) {
    MavenArtifactRepository mavenRepository = repositoryFactory.createMavenRepository();
    mavenRepository.setUrl(url);

    return mavenRepository;
  }
}
