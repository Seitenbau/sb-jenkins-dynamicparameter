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

import hudson.model.Label;
import hudson.model.ParameterDefinition;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptler.config.Parameter;
import org.jenkinsci.plugins.scriptler.config.Script;
import org.jenkinsci.plugins.scriptler.config.ScriptlerConfiguration;
import org.jenkinsci.plugins.scriptler.util.ScriptHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import com.seitenbau.jenkins.plugins.dynamicparameter.util.JenkinsUtils;

/** Base class for all dynamic parameters using Scriptler scripts. */
public abstract class ScriptlerParameterDefinitionBase extends ParameterDefinition
{
  /** Serial version UID. */
  private static final long serialVersionUID = -8947340128208418404L;

  /** Logger. */
  protected static final Logger logger = Logger.getLogger(ScriptlerParameterDefinitionBase.class
      .getName());

  /** UUID identifying the current parameter. */
  private final UUID _uuid;

  /** Flag showing if the script should be executed remotely. */
  private final boolean _remote;

  /** Scriptler script id. */
  private final String _scriptlerScriptId;

  /** Script parameters. */
  private final Parameter[] _parameters;

  protected ScriptlerParameterDefinitionBase(String name, String description, String uuid,
      String scriptlerScriptId, ScriptParameter[] parameters, boolean remote)
  {
    super(name, description);
    _scriptlerScriptId = scriptlerScriptId;
    _parameters = parameters;
    _remote = remote;
    if (StringUtils.length(uuid) == 0)
    {
      _uuid = UUID.randomUUID();
    }
    else
    {
      _uuid = UUID.fromString(uuid);
    }
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
  public final Parameter[] getParameters()
  {
    return _parameters;
  }

  /**
   * Should the script be executed to on a remote slave?
   * @return {@code true} if the script should be executed remotely
   */
  public final boolean isRemote()
  {
    return _remote;
  }

  /**
   * Get unique id for this parameter definition.
   * @return the _uuid
   */
  public final UUID getUUID()
  {
    return _uuid;
  }

  /**
   * Execute the script and return the result value.
   * @return result from the script
   */
  protected Object generateValue()
  {
    Script script = getScript();
    if (script == null)
    {
      return null;
    }

    Map<String, String> parameters = getParametersAsMap();

    if (isRemote())
    {
      Label label = JenkinsUtils.findProjectLabel(getUUID());
      if (label == null)
      {
        logger.warning(String.format(
            "No label is assigned to project; script for parameter '%s' will be executed on master",
            getName()));
      }
      else
      {
        VirtualChannel channel = JenkinsUtils.findActiveChannel(label);
        if (channel == null)
        {
          logger.warning(String.format(
              "Cannot find an active node of the label '%s' where to execute the script",
              label.getDisplayName()));
        }
        else
        {
          return executeAt(script.script, parameters, channel);
        }
      }
    }
    return JenkinsUtils.execute(script.script, parameters);
  }

  private Map<String, String> getParametersAsMap()
  {
    Parameter[] parameters = getParameters();
    Map<String, String> map = new HashMap<String, String>(parameters.length);
    for (Parameter parameter : parameters)
    {
      map.put(parameter.getName(), parameter.getValue());
    }
    return map;
  }

  private Script getScript()
  {
    String scriptId = getScriptlerScriptId();
    Script script = ScriptHelper.getScript(scriptId, true);
    if (script == null)
    {
      logger.severe(String.format("No script with Scriplter ID '%s' exists", scriptId));
      return null;
    }
    return script;
  }

  /**
   * Execute the script at the given node.
   * @param channel node channel
   * @return result from the script
   */
  private Object executeAt(String script, Map<String, String> parameters, VirtualChannel channel)
  {
    try
    {
      RemoteCall call = new RemoteCall(script, parameters);
      return channel.call(call);
    }
    catch (Throwable e)
    {
      String msg = String.format("Error during executing script for parameter '%s'", getName());
      logger.log(Level.SEVERE, msg, e);
    }
    return null;
  }

  /**
   * Check if the Scriptler plugin is available.
   * @return {@code true} if Scriptler is installed
   */
  protected static boolean isScriptlerAvailable()
  {
    return JenkinsUtils.isPluginAvailable("scriptler");
  }

  protected static Set<Script> getAllScriptlerScripts()
  {
    Set<Script> scripts = ScriptlerConfiguration.getConfiguration().getScripts();
    return scripts;
  }

  /**
   * Remote call implementation.
   */
  public static final class RemoteCall implements Callable<Object, Throwable>
  {
    private static final long serialVersionUID = -8281488869664773282L;

    private final String _remoteScript;

    private final Map<String, String> _parameters;

    /**
     * Constructor.
     * @param script script to execute
     * @param parameters parameters
     */
    public RemoteCall(String script, Map<String, String> parameters)
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

  /** Script parameter which has a data bound constructor. */
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

}
