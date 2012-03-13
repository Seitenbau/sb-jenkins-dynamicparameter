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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

/** Tests for {@link ChoiceParameterDefinition}. */
public class ChoiceParameterDefinitionTest
{
  /** Groovy script which returns strings. */
  private static final String SCRIPT_STRINGS = "def result = []; "
      + "3.times { i -> result[i] = \"value_${i}\" }; "
      + "result";

  /** Result of the Groovy script which returns strings. */
  private static final Object[] SCRIPT_STRINGS_RESULT = new Object[] {
      "value_0", "value_1", "value_2"};

  /** Test object. */
  private ChoiceParameterDefinition param;

  /** Set-up method. */
  @Before
  public final void setUp()
  {
    param = new ChoiceParameterDefinition("test", SCRIPT_STRINGS, "test", null,
        false);
  }

  /** Test for {@link ChoiceParameterDefinition#getChoices()}. */
  @Test
  public final void testGetChoices()
  {
    final List<Object> result = param.getChoices();
    assertEqualLists(result.toArray(new Object[result.size()]),
        SCRIPT_STRINGS_RESULT);
  }

  /** Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}. */
  @Test
  public final void testCreateValue()
  {
    final String value = SCRIPT_STRINGS_RESULT[1].toString();

    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[] {value});

    final ParameterValue paramValue = param.createValue(req);

    assertNotNull(paramValue);
    assertTrue(paramValue instanceof StringParameterValue);
    assertEquals(value, ((StringParameterValue) paramValue).value);
  }

  /** Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}. */
  @Test
  public final void testCreateValueNull()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(null);

    assertNull(param.createValue(req));
  }

  /** Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}. */
  @Test(expected = IllegalArgumentException.class)
  public final void testCreateValueWrongNumberOfParams()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[2]);

    param.createValue(req);
  }

  /**
   * Test for
   * {@link ChoiceParameterDefinition#createValue(StaplerRequest)}.
   */
  @Test(expected = IllegalArgumentException.class)
  public final void testCreateValueWrongChoice()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(
        new String[] {"invalid"});

    param.createValue(req);
  }

  /**
   * Compare two {@link Object} lists, by using the
   * {@link Object#toString()} method.
   * @param actualList actual list
   * @param expectedList expected list
   */
  private static void assertEqualLists(final Object[] actualList,
      final Object[] expectedList)
  {
    assertEquals("The two arrays have different sizes", actualList.length,
        expectedList.length);

    for (int i = 0; i < expectedList.length; i++)
    {
      final Object actual = actualList[i];
      final Object expected = expectedList[i];

      if (actual == null && expected != null ||
          actual != null && expected == null)
      {
        fail("Elements at index " + i + " differ: expected <" + expected
            + "> but was <" + actual + ">");
      }

      if (actual != null && expected != null)
      {
        assertEquals("Elements at index " + i + " differ",
            expected.toString(), actual.toString());
      }
    }
  }
}
