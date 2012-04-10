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

import groovy.lang.GroovyShell;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.QueryParameter;

/** Base class for all dynamic parameters. */
public abstract class ParameterDefinitionBase extends ParameterDefinition
{
  /** Serial version UID. */
  private static final long serialVersionUID = 8640419054353526544L;

  /** Class path on remote slaves. */
  private static final String DEFAULT_REMOTE_CLASSPATH = "dynamic_parameter_classpath";

  private static final char CLASSPATH_DELIMITER = ',';

  private static final String CLASSPATH_SPLITTER = "\\s*+" + CLASSPATH_DELIMITER + "\\s*+";

  /** Logger. */
  protected static final Logger logger = Logger.getLogger(ParameterDefinitionBase.class.getName());

  /** Script, which generates the parameter value. */
  private final String _script;

  /** UUID identifying the current parameter. */
  private final UUID _uuid;

  /** Flag showing if the script should be executed remotely. */
  private final boolean _remote;

  /** Local class path. */
  private final FilePath _localBaseDirectory;

  /** Remote class path. */
  private final String _remoteBaseDirectory;

  /** Class path. */
  private final String _classPath;

  /**
   * Constructor.
   * @param name parameter name
   * @param script script, which generates the parameter value
   * @param description parameter description
   * @param uuid identifier (optional)
   * @param remote execute the script on a remote node
   */
  protected ParameterDefinitionBase(String name, String script, String description, String uuid,
      boolean remote, String classPath)
  {
    super(name, description);

    _localBaseDirectory = new FilePath(DynamicParameterConfiguration.INSTANCE.getBaseDirectoryFile());
    _remoteBaseDirectory = DEFAULT_REMOTE_CLASSPATH;
    _classPath = classPath;
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
   * Local class path directory.
   * @return directory on the local node
   */
  public final FilePath getLocalBaseDirectory()
  {
    return _localBaseDirectory;
  }

  /**
   * Remote class path directory.
   * @return directory on a remote node
   */
  public final String getRemoteClassPath()
  {
    return _remoteBaseDirectory;
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
   * Get class paths as a single string.
   * @return class paths
   */
  public final String getClassPath()
  {
    return _classPath;
  }

  public final String[] getClassPathList()
  {
    return _classPath.split(CLASSPATH_SPLITTER);
  }

  /**
   * Execute the script and return the result value.
   * @return result from the script
   */
  protected final Object generateValue()
  {
    if (isRemote())
    {
      final VirtualChannel channel = findActiveChannel();
      if (channel != null)
      {
        return executeAt(channel);
      }
    }
    return execute(getScript(), setupLocalClassPaths());
  }

  /**
   * Execute the script locally.
   * @param script script to execute
   * @return result from the script
   */
  private static Object execute(String script)
  {
    CompilerConfiguration config = new CompilerConfiguration();
    GroovyShell groovyShell = new GroovyShell(config);
    Object evaluate = groovyShell.evaluate(script);
    return evaluate;
  }

  /**
   * Execute the script locally using the given class path.
   * @param script script to execute
   * @param classPaths class paths
   * @return result from the script
   * @throws Exception
   */
  private static Object execute(String script, FilePath[] classPaths)
  {
    try
    {
      // set class path
      ArrayList<String> classPathList = new ArrayList<String>(classPaths.length);
      CompilerConfiguration config = new CompilerConfiguration();
      for(FilePath path : classPaths)
      {
        String classPathString = path.absolutize().toURI().toURL().getPath();
        classPathList.add(classPathString);
      }
      config.setClasspathList(classPathList);

      // execute script
      GroovyShell groovyShell = new GroovyShell(config);
      Object evaluate = groovyShell.evaluate(script);

      return evaluate;
    }
    catch (Exception e)
    {
      logger.log(Level.SEVERE, "Cannot access class path", e);
      return null;
    }
  }

  private FilePath[] setupLocalClassPaths()
  {
    String[] paths = getClassPathList();
    FilePath[] localClassPaths = new FilePath[paths.length];
    for (int i = 0; i < localClassPaths.length; i++)
    {
      String path = paths[i];
      FilePath localClassPath = new FilePath(getLocalBaseDirectory(), path);
      localClassPaths[i] = localClassPath;
    }
    return localClassPaths;
  }

  /**
   * Copy the local classpath directory to a remote node.
   * @param channel node channel
   * @return remote classpaths
   * @throws IOException if copy to remote fails
   * @throws InterruptedException if copy to remote fails
   */
  private FilePath[] setupRemoteClassPaths(VirtualChannel channel) throws IOException,
      InterruptedException
  {
    // TODO check if classpath is up-to-date and does not need a new copy
    String[] paths = getClassPathList();
    FilePath[] remoteClassPaths = new FilePath[paths.length];
    FilePath remoteBaseDirectory = new FilePath(channel, getRemoteClassPath());

    for (int i = 0; i < remoteClassPaths.length; i++)
    {
      String path = paths[i];

      FilePath localClassPath = new FilePath(getLocalBaseDirectory(), path);
      FilePath remoteClassPath = new FilePath(remoteBaseDirectory, path);

      localClassPath.copyRecursiveTo(remoteClassPath);

      remoteClassPaths[i] = remoteClassPath;
    }

    return remoteClassPaths;
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
      FilePath[] remoteClassPaths = setupRemoteClassPaths(channel);
      RemoteCall call = new RemoteCall(getScript(), remoteClassPaths);
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
   * Find an active node channel for the label of the current project.
   * @return active node channel or {@code null} if none found
   */
  private VirtualChannel findActiveChannel()
  {
    Label label = findCurrentProjectLabel();

    if (label == null)
    {
      logger.warning(String.format(
          "No label is assigned to project; script for parameter '%s' will be executed on master",
          getName()));
      return null;
    }

    Iterator<Node> iterator = label.getNodes().iterator();
    while (iterator.hasNext())
    {
      final VirtualChannel channel = iterator.next().getChannel();
      if (channel != null)
      {
        return channel;
      }
    }

    logger.warning(String.format(
        "Cannot find an active node of the label '%s' where to execute the script",
        label.getDisplayName()));
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

    private final String _remoteScript;

    private final FilePath[] _classPaths;

    /**
     * Constructor.
     * @param script script to execute
     * @param classPaths class paths
     */
    public RemoteCall(String script, FilePath[] classPaths)
    {
      _remoteScript = script;
      _classPaths = classPaths;
    }

    @Override
    public Object call()
    {
      if (_classPaths == null)
      {
        return execute(_remoteScript);
      }
      else
      {
        return execute(_remoteScript, _classPaths);
      }
    }

  }

  /** Base parameter descriptor. */
  public static class BaseDescriptor extends ParameterDescriptor
  {
    /**
     * Autocomplete class path selection field.
     * @param value entered value
     * @return list of candidates
     */
    public AutoCompletionCandidates doAutoCompleteClassPath(@QueryParameter String value)
    {
      String[] entered = value.toLowerCase().split(CLASSPATH_SPLITTER);

      String prefix;
      if (entered.length == 0)
      {
        prefix = "";
      }
      else
      {
        prefix = entered[entered.length - 1].toLowerCase();
      }

      AutoCompletionCandidates c = new AutoCompletionCandidates();

      File baseDirectory = DynamicParameterConfiguration.INSTANCE.getBaseDirectoryFile();
      String[] directories = baseDirectory.list();
      if(directories != null)
      {
        for (String directory : directories)
        {
          String lowerCaseDirectory = directory.toLowerCase();
          if (lowerCaseDirectory.startsWith(prefix)
              && !ArrayUtils.contains(entered, lowerCaseDirectory))
          {
            c.add(directory);
          }
        }
      }
      return c;
    }

  }

}
