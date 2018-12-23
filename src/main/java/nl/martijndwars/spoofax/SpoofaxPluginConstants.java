package nl.martijndwars.spoofax;

public class SpoofaxPluginConstants {
  /**
   * The name of the configuration that holds compile language dependencies.
   */
  public static final String COMPILE_LANGUAGE_CONFIGURATION_NAME = "compileLanguage";

  /**
   * The name of the configuration that holds source language dependencies.
   */
  public static final String SOURCE_LANGUAGE_CONFIGURATION_NAME = "sourceLanguage";

  /**
   * The name of the configuration that extends compileLanguage and sourceLanguage.
   */
  public static final String LANGUAGE_CONFIGURATION_NAME = "language";

  /**
   * The name of the configuration that holds the built Spoofax language artifact.
   */
  public static final String SPOOFAX_LANGUAGE_CONFIGURATION_NAME = "spoofaxLanguage";

  /**
   * The name of the task that compiles the language project.
   */
  public static final String COMPILE_LANGUAGE_TASK_NAME = "compileLanguage";

  /**
   * The name of the task that cleans the language project.
   */
  public static final String CLEAN_LANGUAGE_TASK_NAME = "cleanLanguage";

  /**
   * The name of the task that archives the language artifacts.
   */
  public static final String ARCHIVE_LANGUAGE_TASK_NAME = "archiveLanguage";

  /**
   * The name of the task that is an AbstractArchiveTask.
   */
  public static final String SPX_LANGUAGE_TASK_NAME = "spx";

  /**
   * The name of the task that verifies the language artifacts.
   */
  public static final String CHECK_LANGUAGE_TASK_NAME = "checkLanguage";

  /**
   * The name of the plugin extension.
   */
  public static final String PLUGIN_EXTENSION_NAME = "spoofax";

  /**
   * An empty value means 'do not override the language spec'.
   */
  public static final String EMPTY_VALUE = "";

  /**
   * The extension of the file that we generate.
   */
  public static final String SPOOFAX_EXTENSION = "spoofax-language";
}
