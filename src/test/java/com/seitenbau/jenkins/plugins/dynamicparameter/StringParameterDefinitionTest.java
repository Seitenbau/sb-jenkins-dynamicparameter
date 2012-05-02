package com.seitenbau.jenkins.plugins.dynamicparameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

/** Tests for {@link StringParameterDefinition}. */
public class StringParameterDefinitionTest
{
  /** Script result. */
  private static final String SCRIPT_RESULT = "result";
  /** Groovy script which return a string. */
  private static final String SCRIPT = "return \"" + SCRIPT_RESULT + "\"";

  /** Test object. */
  private StringParameterDefinition stringParameterDefinition;

  /** Set-up method. */
  @Before
  public final void setUp()
  {
    stringParameterDefinition = new StringParameterDefinition("test", SCRIPT, "desc", null, false,
        StringUtils.EMPTY);
  }

  /** Test for {@link StringParameterDefinition#getDefaultValue()}. */
  @Test
  public final void testGetDefaultValue()
  {
    assertEquals(SCRIPT_RESULT, stringParameterDefinition.getDefaultValue());
  }

  /** Test for {@link StringParameterDefinition#createValue(StaplerRequest)}. */
  @Test
  public final void testCreateValue()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[] {SCRIPT_RESULT});

    final ParameterValue paramValue = stringParameterDefinition.createValue(req);

    assertEquals(SCRIPT_RESULT, ((StringParameterValue) paramValue).value);
  }

  /** Test for {@link StringParameterDefinition#createValue(StaplerRequest)}. */
  @Test
  public final void testCreateValueNull()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(null);

    assertNull(stringParameterDefinition.createValue(req));
  }

  /** Test for {@link StringParameterDefinition#createValue(StaplerRequest)}. */
  @Test(expected = IllegalArgumentException.class)
  public final void testCreateValueWrongNumberOfParams()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[2]);

    stringParameterDefinition.createValue(req);
  }

  /** Test for {@link StringParameterDefinition#createValue(StaplerRequest, JSONObject)}. */
  @Test
  public void testCreateValueJSON()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    // final JSONObject jo = mock(JSONObject.class);
    final JSONObject jo = null;

    final StringParameterValue value = new StringParameterValue("testName", "testValue");

    when(req.bindJSON(StringParameterValue.class, jo)).thenReturn(value);
    final ParameterValue result = stringParameterDefinition.createValue(req, jo);

    assertEquals(value, result);
    assertEquals(stringParameterDefinition.getDescription(), result.getDescription());
  }
}
