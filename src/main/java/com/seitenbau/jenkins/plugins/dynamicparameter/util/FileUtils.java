package com.seitenbau.jenkins.plugins.dynamicparameter.util;

import java.io.File;

public class FileUtils extends org.apache.commons.io.FileUtils
{
  public static boolean isWindows()
  {
    return File.separatorChar == '\\';
  }

  public static boolean isDescendant(String root, String descendant)
  {
    String rootPath = root;
    String descendantPath = descendant;
    if (isWindows())
    {
      rootPath = root.toUpperCase();
      descendantPath = descendant.toUpperCase();
    }

    if (descendantPath.equals(rootPath) || !descendantPath.startsWith(rootPath))
    {
      return false;
    }

    return true;
  }
}
