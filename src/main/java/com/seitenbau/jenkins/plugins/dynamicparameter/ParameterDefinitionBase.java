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

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.remoting.Callable;

/** Base class for all dynamic parameters. */
public abstract class ParameterDefinitionBase extends ParameterDefinition
{
  /** Serial version UID. */
  private static final long serialVersionUID = 8640419054353526544L;

  /** Logger. */
  protected static final Logger LOGGER = Logger
      .getLogger(ParameterDefinitionBase.class.getName());

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
  protected ParameterDefinitionBase(final String name, final String script,
      final String description, final String uuid, final boolean remote)
  {

    super(name, description);

    _script = script;
    _remote = remote;

    if (uuid == null || uuid.length() == 0)
    {
      _uuid = UUID.randomUUID();
    }
    else
    {
      _uuid = UUID.fromString(uuid);
    }
  }

  /** @return script, which generates the parameter value */
  public final String getScript()
  {
    return _script;
  }

  /** @return if the script should be executed remotely */
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
    if (_remote)
    {
      final Label label = getCurrentProject().getAssignedLabel();
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
    return new GroovyShell(new Binding()).evaluate(_script);
  }

  /**
   * Execute the script at one of the nodes with the given label.
   * @param label node label
   * @return result from the script
   */
  private Object executeAt(final Label label)
  {

    try
    {
      final Node node = label.getNodes().iterator().next();

      return node.getChannel().call(new Callable<Object, Throwable>()
      {

        private static final long serialVersionUID = 1L;

        public Object call()
        {
          return execute();
        }
      });
    }
    catch (final Throwable e)
    {
      LOGGER.log(Level.SEVERE, "Script for parameter '" + getName()
          + "' could not be executed!", e);
      return null;
    }
  }

  /** @return project, where the parameter is defined */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private AbstractProject getCurrentProject()
  {

    final List<AbstractProject> jobs = Hudson.getInstance().getItems(
        AbstractProject.class);

    for (AbstractProject project : jobs)
    {

      final ParametersDefinitionProperty prop =
          (ParametersDefinitionProperty) project.getProperty(
              ParametersDefinitionProperty.class);

      if (prop != null)
      {
        final List<ParameterDefinition> parameterDefinitions = prop
            .getParameterDefinitions();

        if (parameterDefinitions != null)
        {
          for (final ParameterDefinition pd : parameterDefinitions)
          {
            if (pd instanceof ParameterDefinitionBase)
            {
              final ParameterDefinitionBase p = (ParameterDefinitionBase) pd;
              if (p._uuid != null && p._uuid.equals(_uuid))
              {
                return project;
              }
            }
          }
        }
      }
    }

    return null;
  }
}
