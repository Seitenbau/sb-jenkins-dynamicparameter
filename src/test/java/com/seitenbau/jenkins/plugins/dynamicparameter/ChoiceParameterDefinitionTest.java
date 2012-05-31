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

import static com.seitenbau.jenkins.plugins.dynamicparameter.ChoiceParameterDefinitionParameterBuilder.choiceParameterDefinitionParameter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import java.util.Collections;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
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

  /** Test object builder. */
  private ChoiceParameterDefinitionParameterBuilder defaultChoiceParameterBuilder;

  /** Test object. */
  private ChoiceParameterDefinition choiceParameterDefinition;

  /**
   * Set-up method.
   */
  @Before
  public final void setUp()
  {
    defaultChoiceParameterBuilder = choiceParameterDefinitionParameter();
    // @formatter:off
    defaultChoiceParameterBuilder
      .withDescription("description")
      .withName("test")
      .withScript(SCRIPT_STRINGS);
    // @formatter:on

    choiceParameterDefinition = createChoiceParameterDefinition(defaultChoiceParameterBuilder);
  }

  private static ChoiceParameterDefinition createChoiceParameterDefinition(
      ChoiceParameterDefinitionParameterBuilder choiceParameterBuilder)
  {
    ChoiceParameterDefinitionParameter parameter = choiceParameterBuilder.build();
    return new ChoiceParameterDefinition(parameter.getName(), parameter.getScript(),
        parameter.getDescription(), parameter.getUuid(), parameter.isRemote(), StringUtils.EMPTY);
  }

  /**
   * Test for {@link ChoiceParameterDefinition#getChoices()}.
   */
  @Test
  public final void testGetChoices()
  {
    final List<Object> result = choiceParameterDefinition.getChoices();
    assertEqualLists(SCRIPT_STRINGS_RESULT, result.toArray(new Object[result.size()]));
  }

  /**
   * Test for {@link ChoiceParameterDefinition#getChoices()}.
   */
  @Test
  public final void testGetChoicesNull()
  {
    choiceParameterDefinition = createChoiceParameterDefinition(defaultChoiceParameterBuilder
        .withScript("null"));
    assertEquals(Collections.EMPTY_LIST, choiceParameterDefinition.getChoices());
  }

  /**
   * Test for {@link ChoiceParameterDefinition#getChoices()}.
   */
  @Test
  public final void testGetChoicesRemote()
  {
    choiceParameterDefinition = createChoiceParameterDefinition(defaultChoiceParameterBuilder
        .withRemote(true));
    final List<Object> result = choiceParameterDefinition.getChoices();
    assertEqualLists(SCRIPT_STRINGS_RESULT, result.toArray(new Object[result.size()]));
  }

  /**
   * Test for
   * {@link ChoiceParameterDefinition#createValue(StaplerRequest)}.
   */
  @Test
  public final void testCreateValue()
  {
    final String value = SCRIPT_STRINGS_RESULT[1].toString();

    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[] {value});

    final ParameterValue paramValue = choiceParameterDefinition.createValue(req);

    assertNotNull(paramValue);
    assertTrue(paramValue instanceof StringParameterValue);
    assertEquals(value, ((StringParameterValue) paramValue).value);
  }

  /**
   * Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}.
   */
  @Test
  @Ignore
  public final void testCreateValueNull()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(null);

    assertNull(choiceParameterDefinition.createValue(req));
  }

  /**
   * Test for
   * {@link ChoiceParameterDefinition#createValue(StaplerRequest)}.
   */
  @Test
  public final void testDefaultValue()
  {
    final String value = SCRIPT_STRINGS_RESULT[0].toString();

    final ParameterValue paramValue = choiceParameterDefinition.getDefaultParameterValue();

    assertNotNull(paramValue);
    assertTrue(paramValue instanceof StringParameterValue);
    assertEquals(value, ((StringParameterValue) paramValue).value);
  }
  
  /**
   * Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}.
   */
  @Test(expected = IllegalArgumentException.class)
  public final void testCreateValueWrongNumberOfParams()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[2]);

    choiceParameterDefinition.createValue(req);
  }

  /**
   * Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}.
   */
  @Test(expected = IllegalArgumentException.class)
  public final void testCreateValueWrongChoice()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[] {"invalid"});

    choiceParameterDefinition.createValue(req);
  }

  /**
   * Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest, JSONObject)}.
   */
  @Test
  public void testCreateValueJSON()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    // final JSONObject jo = mock(JSONObject.class); // mockito cannot mock final classes
    final JSONObject jo = null;

    final StringParameterValue value = new StringParameterValue("value",
        (String) SCRIPT_STRINGS_RESULT[1]);

    when(req.bindJSON(StringParameterValue.class, jo)).thenReturn(value);
    final ParameterValue result = choiceParameterDefinition.createValue(req, jo);

    assertEquals(value, result);
    assertEquals(choiceParameterDefinition.getDescription(), result.getDescription());
  }

  /**
   * Compare two {@link Object} lists, by using the {@link Object#toString()} method.
   * @param expectedList expected list
   * @param actualList actual list
   */
  private static void assertEqualLists(final Object[] expectedList, final Object[] actualList)
  {
    assertEquals("The two arrays have different sizes", actualList.length, expectedList.length);

    for (int i = 0; i < expectedList.length; i++)
    {
      final Object actual = actualList[i];
      final Object expected = expectedList[i];

      if (actual == null && expected != null || actual != null && expected == null)
      {
        fail(String.format("Elements at index %d differ: expected <%s> but was <%s>", i, expected,
            actual));
      }

      if (actual != null && expected != null)
      {
        assertEquals("Elements at index " + i + " differ", expected.toString(), actual.toString());
      }
    }
  }
}
