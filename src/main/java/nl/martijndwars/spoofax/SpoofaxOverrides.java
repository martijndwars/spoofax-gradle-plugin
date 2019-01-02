package nl.martijndwars.spoofax;

import java.util.HashMap;
import java.util.Map;

public class SpoofaxOverrides {
  public static Map<String, String> projectFormat;

  static {
     projectFormat = new HashMap<>();
  }

  public static void set(String project, String strategoFormat) {
    projectFormat.put(project, strategoFormat);
  }

  public static String get(String project) {
    return projectFormat.get(project);
  }
}
