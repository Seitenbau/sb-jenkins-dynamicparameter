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
   * Execute the script and return the result value.
   * @return result from the script
   */
  protected final Object getValue()
  {
    if (isRemote())
    {
      Label label = getCurrentProjectLabel();
      if (label != null)
      {
        return executeAt(label);
      }
    }
    return execute();
  }

  /**
   * Execute the script locally.
   * @return result from the script
   */
  private Object execute()
  {
    Binding binding = new Binding();
    GroovyShell groovyShell = new GroovyShell(binding);
    Object evaluate = groovyShell.evaluate(_script);
    return evaluate;
  }

  /**
   * Execute the script at one of the nodes with the given label.
   * @param label node label
   * @return result from the script
   */
  private Object executeAt(Label label)
  {
    try
    {
      Iterator<Node> iterator = label.getNodes().iterator();
      while (iterator.hasNext())
      {
        final VirtualChannel channel = iterator.next().getChannel();
        if (channel != null)
        {
          return channel.call(new Callable<Object, Throwable>()
          {
            private static final long serialVersionUID = 1L;

            @Override
            public Object call()
            {
              return execute();
            }
          });
        }
      }
      logger.warning(
          String.format("Cannot find a node of the label '%s' where to execute the script",
              label.getDisplayName()));
    }
    catch (Throwable e)
    {
      String msg = String.format("Error during executing script for parameter '%s'", getName());
      logger.log(Level.SEVERE, msg, e);
    }
    return null;
  }

  /** @return project, where the parameter is defined */
  @SuppressWarnings("rawtypes")
  private Label getCurrentProjectLabel()
  {
    Hudson instance = Hudson.getInstance();
    if (instance != null)
    {
      List<AbstractProject> projects = instance.getItems(AbstractProject.class);
      for (AbstractProject project : projects)
      {
          if(isThisParameterDefintionOf(project))
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
   * @return true when project contains this parameter defintion.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private boolean isThisParameterDefintionOf(AbstractProject project)
  {
      ParametersDefinitionProperty parametersDefinition = (ParametersDefinitionProperty)
              project.getProperty(ParametersDefinitionProperty.class);
      if (parametersDefinition != null)
      {
        List<ParameterDefinition> parameterDefinitions = parametersDefinition.getParameterDefinitions();
        if (parameterDefinitions != null)
        {
          for (ParameterDefinition pd : parameterDefinitions)
          {
            if (pd instanceof ParameterDefinitionBase)
            {
              ParameterDefinitionBase parameterDefinition = (ParameterDefinitionBase) pd;
              UUID parameterUUID = parameterDefinition._uuid;
              if (ObjectUtils.equals(parameterUUID, this._uuid))
              {
                return true;
              }
            }
          }
        }
      }
      return false;
  }

}
