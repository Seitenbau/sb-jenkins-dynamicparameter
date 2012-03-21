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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

/** Base class for all dynamic parameters. */
public abstract class ParameterDefinitionBase extends ParameterDefinition
{
  /** Serial version UID. */
  private static final long serialVersionUID = 8640419054353526544L;

  /** Logger. */
  protected static final Logger logger = Logger.getLogger(ParameterDefinitionBase.class.getName());

  /** Script, which generates the parameter value. */
  private final String _script;

  /** UUID identifying the current parameter. */
  private final UUID _uuid;

  /** Flag showing if the script should be executed remotely. */
  private final boolean _remote;

  /**
   * Constructor.
   * @param name parameter name
   * @param script script, which generates the parameter value
   * @param description parameter description
   * @param uuid identifier (optional)
   * @param remote execute the script on a remote node
   */
  protected ParameterDefinitionBase(String name, String script, String description, String uuid,
      boolean remote)
  {
    super(name, description);
    _script = script;
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
   * Get the script, which generates the parameter value.
   * @return the script as string
   */
  public final String getScript()
  {
    return _script;
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
  protected final Object generateValue()
  {
    if (isRemote())
    {
      Label label = findCurrentProjectLabel();
      if (label != null)
      {
        return executeAt(label);
      }
    }
    return execute(getScript());
  }

  /**
   * Execute the script locally.
   * @return result from the script
   */
  private static Object execute(String script)
  {
    Binding binding = new Binding();
    GroovyShell groovyShell = new GroovyShell(binding);
    Object evaluate = groovyShell.evaluate(script);
    return evaluate;
  }

  /**
   * Execute the script at one of the nodes with the given label.
   * @param label node label
   * @return result from the script
   */
  private Object executeAt(Label label)
  {
    final VirtualChannel channel = findActiveChannel(label);
    if (channel == null)
    {
      logger.warning(
          String.format("Cannot find a node of the label '%s' where to execute the script",
          label.getDisplayName()));
      return null;
    }
    return executeAt(channel);
  }

  /**
   * Execute the script at the given node.
   * @param channel node channel
   * @return result from the script
   */
  private Object executeAt(VirtualChannel channel)
  {
    try
    {
      RemoteCall call = new RemoteCall(getScript());
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
   * Find an active node channel of a given label.
   * @param label label which nodes to search
   * @return active node channel or {@code null} if none found
   */
  private static VirtualChannel findActiveChannel(Label label)
  {
    Iterator<Node> iterator = label.getNodes().iterator();
    while (iterator.hasNext())
    {
      final VirtualChannel channel = iterator.next().getChannel();
      if (channel != null)
      {
        return channel;
      }
    }
    return null;
  }

  /**
   * Find the label assigned to the current project.
   * @return {@code null} if the label of the current project cannot be found
   */
  @SuppressWarnings("rawtypes")
  private Label findCurrentProjectLabel()
  {
    Hudson instance = Hudson.getInstance();
    if (instance != null)
    {
      List<AbstractProject> projects = instance.getItems(AbstractProject.class);
      for (AbstractProject project : projects)
      {
        if (isThisParameterDefintionOf(project))
        {
          return project.getAssignedLabel();
        }
      }
    }
    return null;
  }

  /**
   * Returns true if this parameter definition is a definition of the given project.
   * @param project the project to search for this parameter definition.
   * @return {@code true} if the project contains this parameter definition.
   */
  @SuppressWarnings("rawtypes")
  private boolean isThisParameterDefintionOf(AbstractProject project)
  {
    List<ParameterDefinition> parameterDefinitions = getProjectParameterDefinitions(project);
    for (ParameterDefinition pd : parameterDefinitions)
    {
      if (pd instanceof ParameterDefinitionBase)
      {
        ParameterDefinitionBase parameterDefinition = (ParameterDefinitionBase) pd;
        UUID parameterUUID = parameterDefinition.getUUID();
        if (ObjectUtils.equals(parameterUUID, this.getUUID()))
        {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get the parameter definitions for the given project.
   * @param project the project for which the parameter definitions should be found
   * @return parameter definitions or an empty list
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<ParameterDefinition> getProjectParameterDefinitions(AbstractProject project)
  {
    ParametersDefinitionProperty parametersDefinitionProperty =
        (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
    if (parametersDefinitionProperty != null)
    {
      List<ParameterDefinition> parameterDefinitions = parametersDefinitionProperty
          .getParameterDefinitions();
      if (parameterDefinitions != null)
      {
        return parameterDefinitions;
      }
    }
    return Collections.EMPTY_LIST;
  }

  /**
   * Remote call implementation.
   */
  public static final class RemoteCall implements Callable<Object, Throwable>
  {
    private static final long serialVersionUID = -8281488869664773282L;

    private final String remoteScript;

    /**
     * Constructor.
     * @param script script to execute
     */
    public RemoteCall(String script)
    {
      remoteScript = script;
    }

    @Override
    public Object call()
    {
      return execute(remoteScript);
    }
  }

}
