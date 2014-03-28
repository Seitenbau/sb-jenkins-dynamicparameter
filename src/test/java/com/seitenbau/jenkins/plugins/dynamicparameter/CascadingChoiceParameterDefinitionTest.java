package com.seitenbau.jenkins.plugins.dynamicparameter;

import static com.seitenbau.jenkins.plugins.dynamicparameter.CascadingChoiceParameterDefinitionParameterBuilder.cascadingChoiceParameterDefinitionParameter;
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
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;

/** Tests for {@link ChoiceParameterDefinition}. */
public class CascadingChoiceParameterDefinitionTest extends HudsonTestCase
{
  /** Groovy script which returns strings. */
  private static final String CHOICES_SCRIPT = "def theval = ['1111', '2222'].collect {  \"${parentProperty}.${it}\"}; " +
  		"theval";
  /** argument to be passed to the script */
  private static final String CHOICES_SCRIPT_ARG_A = "aaaa";
  
  /** Expected result of the groovy script with the 'A' argument */
  private static final Object[] CHOICES_SCRIPT_RESULTS = new Object[] {
      "aaaa.1111", "aaaa.2222"};

  /** Test object builder. */
  private CascadingChoiceParameterDefinitionParameterBuilder defaultCascadingChoiceParameterBuilder;

  /** Test object. */
  private CascadingChoiceParameterDefinition cascadingChoiceParameterDefinition;

  /**
   * Set-up method.
   */
  @Before
  public final void setUp() throws Exception
  {
	  super.setUp();
	  defaultCascadingChoiceParameterBuilder = cascadingChoiceParameterDefinitionParameter();
    // @formatter:off
    defaultCascadingChoiceParameterBuilder
      .withDescription("description")
      .withName("childProperty")
      .withParentPropertyName("parentProperty")
      .withScript(CHOICES_SCRIPT);
    // @formatter:on

    cascadingChoiceParameterDefinition = createCascadingChoiceParameterDefinition(defaultCascadingChoiceParameterBuilder);
  }

  private static CascadingChoiceParameterDefinition createCascadingChoiceParameterDefinition(
		  CascadingChoiceParameterDefinitionParameterBuilder cascadingChoiceParameterBuilder)
  {
	  CascadingChoiceParameterDefinitionParameter parameter = cascadingChoiceParameterBuilder.build();
    return new CascadingChoiceParameterDefinition(parameter.getName(), parameter.getParentPropertyName(), parameter.getScript(),
        parameter.isSandbox(), parameter.getDescription(), parameter.getUuid(), parameter.isRemote(), false, StringUtils.EMPTY);
  }

  /**
   * Test for {@link CascadingChoiceParameterDefinition#getChoices()}.
   */
  @Test
  public final void testGetChoices()
  {
	ScriptApproval.get().preapprove(cascadingChoiceParameterDefinition.getScript(), GroovyLanguage.get());
    final List<Object> result = cascadingChoiceParameterDefinition.getChoices(CHOICES_SCRIPT_ARG_A);
    assertEqualLists(CHOICES_SCRIPT_RESULTS, result.toArray(new Object[result.size()]));
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
