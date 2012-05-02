/*
 * Copyright 2012 Seitenbau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seitenbau.jenkins.plugins.dynamicparameter.util;

import java.io.File;

/**
 * Extended file utility methods.
 */
public class FileUtils extends org.apache.commons.io.FileUtils
{
  /**
   * Check if the current system is windows.
   * @return {@code true} if file system delimiter is backslash
   */
  public static boolean isWindows()
  {
    return File.separatorChar == '\\';
  }

  /**
   * Check if a file is descendant of the given root. The method does not access the file system,
   * but uses the provided paths to check if the given root is a prefix of the given path;
   * therefore, the caller MUST take care to make the two paths absolute.
   * @param root root path
   * @param descendant descendant path
   * @return {@code true} if a descendant
   */
  public static boolean isDescendant(String root, String descendant)
  {
    String rootPath = root;
    String descendantPath = descendant;
    if (isWindows())
    {
      rootPath = root.toUpperCase().replace('\\', '/') + '/';
      descendantPath = descendant.toUpperCase().replace('\\', '/');
    }

    if (descendantPath.equals(rootPath) || !descendantPath.startsWith(rootPath))
    {
      return false;
    }

    return true;
  }
}
