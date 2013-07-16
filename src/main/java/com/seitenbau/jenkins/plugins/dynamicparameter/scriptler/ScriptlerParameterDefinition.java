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
package com.seitenbau.jenkins.plugins.dynamicparameter.scriptler;

import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.scriptler.config.Parameter;
import org.jenkinsci.plugins.scriptler.config.Script;
import org.jenkinsci.plugins.scriptler.util.ScriptHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import com.seitenbau.jenkins.plugins.dynamicparameter.BaseParameterDefinition;
import com.seitenbau.jenkins.plugins.dynamicparameter.util.JenkinsUtils;

/** Base class for all dynamic parameters using Scriptler scripts. */
public abstract class ScriptlerParameterDefinition extends BaseParameterDefinition
{
  /** Serial version UID. */
  private static final long serialVersionUID = -8947340128208418404L;

  /** Scriptler script id. */
  private final String _scriptlerScriptId;

  /** Script parameters. */
  private final ScriptParameter[] _parameters;

  /**
   * Constructor.
   * @param name parameter name
   * @param description parameter description
   * @param uuid UUID of the parameter definition
   * @param scriptlerScriptId Scriptler script identifier
   * @param parameters script parameters
   * @param remote flag showing if the script should be executed remotely
   */
  protected ScriptlerParameterDefinition(String name, String description, String uuid,
      String scriptlerScriptId, ScriptParameter[] parameters, boolean remote)
  {
    super(name, description, uuid, remote);
    _scriptlerScriptId = scriptlerScriptId;
    _parameters = parameters;
  }

  /**
   * Get Scriptler script id.
   * @return scriptler script id
   */
  public final String getScriptlerScriptId()
  {
    return _scriptlerScriptId;
  }

  /**
   * Get script parameters.
   * @return array with script parameters
   */
  public final ScriptParameter[] getParameters()
  {
    return _parameters;
  }

  @Override
  protected ParameterizedScriptCall prepareLocalCall() throws Exception
  {
    return prepareCall();
  }

  @Override
  protected ParameterizedScriptCall prepareRemoteCall(VirtualChannel channel) throws Exception
  {
    return prepareCall();
  }

  /**
   * Prepare a call of the script.
   * @return call instance
   * @throws Exception if the script with the given Scriptler identifier does not exist
   */
  private ParameterizedScriptCall prepareCall() throws Exception
  {
    String scriptId = getScriptlerScriptId();
    Script script = ScriptHelper.getScript(scriptId, true);
    if (script == null)
    {
      throw new Exception(String.format("No script with Scriplter ID '%s' exists", scriptId));
    }

    // Read all parameters from job configuration
    Map<String, String> parameters = getParametersAsMap();

    // Set default values, in case the value has not been set in job configuration
    for (Parameter parameter : script.getParameters()) {
        if (!parameters.containsKey(parameter.getName())) {
            parameters.put(parameter.getName(), parameter.getValue());
        }
    }

    ParameterizedScriptCall call = new ParameterizedScriptCall(script.script, parameters);
    return call;
  }

  /**
   * Convert the list of parameters to a map.
   * @return a {@link Map} with script parameters
   */
  private Map<String, String> getParametersAsMap()
  {
    ScriptParameter[] parameters = getParameters();
    Map<String, String> map = new HashMap<String, String>(parameters.length);
    for (Parameter parameter : parameters)
    {
      map.put(parameter.getName(), parameter.getValue());
    }
    return map;
  }

  /**
   * Script parameter which has a data bound constructor.
   */
  public static final class ScriptParameter extends Parameter
  {
    /** Serial version UID. */
    private static final long serialVersionUID = -2154469334885184388L;

    /**
     * Data bound constructor.
     * @param name parameter name
     * @param value parameter value
     */
    @DataBoundConstructor
    public ScriptParameter(String name, String value)
    {
      super(name, value);
    }
  }

  /**
   * Remote call implementation.
   */
  public static final class ParameterizedScriptCall implements Callable<Object, Throwable>
  {
    private static final long serialVersionUID = -8281488869664773282L;

    private final String _remoteScript;

    private final Map<String, String> _parameters;

    /**
     * Constructor.
     * @param script script to execute
     * @param parameters parameters
     */
    public ParameterizedScriptCall(String script, Map<String, String> parameters)
    {
      _remoteScript = script;
      _parameters = parameters;
    }

    @Override
    public Object call()
    {
      return JenkinsUtils.execute(_remoteScript, _parameters);
    }

  }
}
