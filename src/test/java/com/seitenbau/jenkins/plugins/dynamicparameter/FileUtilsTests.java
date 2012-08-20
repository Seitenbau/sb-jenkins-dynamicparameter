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
package com.seitenbau.jenkins.plugins.dynamicparameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.seitenbau.jenkins.plugins.dynamicparameter.util.FileUtils;

/**
 * Tests for {@link FileUtils}.
 */
public class FileUtilsTests
{
  /**
   * Test for {@link FileUtils#isWindows()}.
   */
  @Test
  public void testIsWindows()
  {
    String osName = System.getProperty("os.name");
    boolean isWindows = osName.toLowerCase().contains("windows");
    assertEquals(isWindows, FileUtils.isWindows());
  }

  /**
   * Test for {@link FileUtils#isDescendant(String, String)}.
   */
  @Test
  public void testIsDescendant()
  {
    assertTrue(FileUtils.isDescendant("root", "root/dir1"));
    assertTrue(FileUtils.isDescendant("root", "root/dir2/dir2/dir3"));
    assertTrue(FileUtils.isDescendant("\\root", "\\root\\dir1"));
    assertTrue(FileUtils.isDescendant("root", "root\\dir2\\dir2\\dir3"));
    // Issue https://github.com/Seitenbau/sb-jenkins-dynamicparameter/issues/7
    // assertFalse(FileUtils.isDescendant("root", "root1"));
    assertFalse(FileUtils.isDescendant("root", "root"));
  }
}
