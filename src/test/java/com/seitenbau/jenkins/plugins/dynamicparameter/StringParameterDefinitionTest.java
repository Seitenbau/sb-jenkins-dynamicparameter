package com.seitenbau.jenkins.plugins.dynamicparameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

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
  private StringParameterDefinition param;

  /** Set-up method. */
  @Before
  public final void setUp()
  {
    param = new StringParameterDefinition("test", SCRIPT, "test", null, false);
  }

  /** Test for {@link ChoiceParameterDefinition#getChoices()}. */
  @Test
  public final void testGetDefaultValue()
  {
    assertEquals(SCRIPT_RESULT, param.getDefaultValue());
  }

  /** Test for {@link ChoiceParameterDefinition#createValue(StaplerRequest)}. */
  @Test
  public final void testCreateValue()
  {
    final StaplerRequest req = mock(StaplerRequest.class);
    when(req.getParameterValues(anyString())).thenReturn(new String[] {SCRIPT_RESULT});

    final ParameterValue paramValue = param.createValue(req);

    assertEquals(SCRIPT_RESULT, ((StringParameterValue) paramValue).value);
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
}
