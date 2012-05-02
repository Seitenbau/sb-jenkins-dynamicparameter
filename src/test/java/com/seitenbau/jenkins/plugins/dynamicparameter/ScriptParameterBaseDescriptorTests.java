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

import static com.seitenbau.jenkins.plugins.dynamicparameter.ScriptParameterDefinition.BaseDescriptor.splitClassPaths;
import static org.junit.Assert.assertArrayEquals;
import hudson.model.AutoCompletionCandidates;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.seitenbau.jenkins.plugins.dynamicparameter.ScriptParameterDefinition.BaseDescriptor;
import com.seitenbau.jenkins.plugins.dynamicparameter.config.DynamicParameterConfiguration;

/**
 * Tests for {@link BaseParameterDefinition}.
 */
public class ScriptParameterBaseDescriptorTests
{
  private static final String CLASSPATH = "src/test/resources/test-classpath";

  private static final String[] PATHS = new String[] {"cp-first", "cp-second", "cp-fourth"};

  private BaseDescriptor sut;

  /**
   * Static set up method.
   * @throws IOException
   */
  @BeforeClass
  public static void beforeClass() throws IOException
  {
    for (String path : PATHS)
    {
      new File(CLASSPATH, path).mkdirs();
    }
    DynamicParameterConfiguration.INSTANCE.setBaseDirectory(CLASSPATH);
  }

  /**
   * Static clean up method.
   * @throws IOException
   */
  @AfterClass
  public static void afterClass() throws IOException
  {
    FileUtils.deleteDirectory(new File(CLASSPATH));
  }

  /**
   * Set up method.
   * @throws IOException
   */
  @Before
  public void setUp()
  {
    sut = new BaseDescriptor();
  }

  /**
   * Test for {@link BaseDescriptor#splitClassPaths(String)}.
   */
  @Test
  public void testSplitClassPaths()
  {
    assertArrayEquals(new String[] {"val1", "val2", "val3"}, splitClassPaths("val1, val2, val3"));
    assertArrayEquals(new String[] {"val1; val2", "val3"}, splitClassPaths("val1; val2  , val3"));
    assertArrayEquals(new String[] {"val1"}, splitClassPaths("val1"));
    assertArrayEquals(new String[] {}, splitClassPaths(""));
  }

  /**
   * Test for {@link BaseDescriptor#doAutoCompleteClassPath(String)}.
   */
  @Test
  public void testDoAutoCompleteClassPath()
  {
    AutoCompletionCandidates candidates = sut.doAutoCompleteClassPath("cp-f");
    assertArrayEquals(new String[] {"cp-first", "cp-fourth"},
        candidates.getValues().toArray(new String[0]));

    candidates = sut.doAutoCompleteClassPath("cp-fourth, cp-f");
    assertArrayEquals(new String[] {"cp-first"}, candidates.getValues().toArray(new String[0]));

    candidates = sut.doAutoCompleteClassPath("cp-fi");
    assertArrayEquals(new String[] {"cp-first"}, candidates.getValues().toArray(new String[0]));

    candidates = sut.doAutoCompleteClassPath("cp-s");
    assertArrayEquals(new String[] {"cp-second"}, candidates.getValues().toArray(new String[0]));

    candidates = sut.doAutoCompleteClassPath("fi");
    assertArrayEquals(new String[] {}, candidates.getValues().toArray(new String[0]));

    candidates = sut.doAutoCompleteClassPath("");
    assertArrayEquals(new String[] {"cp-first", "cp-fourth", "cp-second"}, candidates.getValues()
        .toArray(new String[0]));
  }
}
