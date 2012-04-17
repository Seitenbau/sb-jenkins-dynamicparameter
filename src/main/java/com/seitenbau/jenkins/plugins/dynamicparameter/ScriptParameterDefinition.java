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

import hudson.FilePath;
import hudson.model.AutoCompletionCandidates;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;

import com.seitenbau.jenkins.plugins.dynamicparameter.config.DynamicParameterConfiguration;
import com.seitenbau.jenkins.plugins.dynamicparameter.util.JenkinsUtils;

/** Base class for all dynamic parameters. */
public abstract class ScriptParameterDefinition extends BaseParameterDefinition
{
  /** Serial version UID. */
  private static final long serialVersionUID = 8640419054353526544L;

  /** Class path on remote slaves. */
  private static final String DEFAULT_REMOTE_CLASSPATH = "dynamic_parameter_classpath";

  /** Class path delimiter symbol. */
  private static final char CLASSPATH_DELIMITER = ',';

  /** Regular expression to split concatenated class paths. */
  private static final String CLASSPATH_SPLITTER = "\\s*+" + CLASSPATH_DELIMITER + "\\s*+";

  /** Script, which generates the parameter value. */
  private final String _script;

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
  protected ScriptParameterDefinition(String name, String script, String description, String uuid,
      boolean remote, String classPath)
  {
    super(name, description, uuid, remote);

    _localBaseDirectory = new FilePath(DynamicParameterConfiguration.INSTANCE.getBaseDirectoryFile());
    _remoteBaseDirectory = DEFAULT_REMOTE_CLASSPATH;
    _classPath = classPath;
    _script = script;
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
   * Get class paths as a single string.
   * @return class paths
   */
  public final String getClassPath()
  {
    return _classPath;
  }

  /**
   * Get class paths as a list.
   * @return class paths
   */
  public final String[] getClassPathList()
  {
    return _classPath.split(CLASSPATH_SPLITTER);
  }

  @Override
  protected ClasspathScriptCall prepareLocalCall()
  {
    ClasspathScriptCall call = new ClasspathScriptCall(getScript(), setupLocalClassPaths());
    return call;
  }

  @Override
  protected ClasspathScriptCall prepareRemoteCall(VirtualChannel channel) throws IOException,
      InterruptedException
  {
    ClasspathScriptCall call = new ClasspathScriptCall(getScript(), setupRemoteClassPaths(channel));
    return call;
  }

  /**
   * Set up the class path directory on the local node.
   * @return local class paths
   */
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
   * Copy local class path directories to a remote node.
   * @param channel node channel
   * @return remote class paths
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
   * Base parameter descriptor.
   */
  public static class BaseDescriptor extends ParameterDescriptor
  {
    /**
     * Auto-complete class path selection field.
     * @param value entered value
     * @return list of candidates
     */
    public AutoCompletionCandidates doAutoCompleteClassPath(@QueryParameter String value)
    {
      String[] entered = splitClassPaths(value);

      String prefix;
      if (entered.length == 0)
      {
        prefix = StringUtils.EMPTY;
      }
      else
      {
        prefix = entered[entered.length - 1];
      }

      AutoCompletionCandidates candidates = new AutoCompletionCandidates();

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
            candidates.add(directory);
          }
        }
      }
      return candidates;
    }

    /**
     * Split a string of class paths into separate values.
     * @param value concatenated class paths
     * @return an array of parsed values or an empty array
     */
    public static String[] splitClassPaths(String value)
    {
      if (StringUtils.isEmpty(value))
      {
        return ArrayUtils.EMPTY_STRING_ARRAY;
      }
      else
      {
        return value.toLowerCase().split(CLASSPATH_SPLITTER);
      }
    }

  }

  /**
   * Remote call implementation.
   */
  public static final class ClasspathScriptCall implements Callable<Object, Throwable>
  {
    private static final long serialVersionUID = -8281488869664773282L;

    private final String _remoteScript;

    private final FilePath[] _classPaths;

    /**
     * Constructor.
     * @param script script to execute
     * @param classPaths class paths
     */
    public ClasspathScriptCall(String script, FilePath[] classPaths)
    {
      _remoteScript = script;
      _classPaths = classPaths;
    }

    @Override
    public Object call()
    {
      return JenkinsUtils.execute(_remoteScript, _classPaths);
    }

  }
}
