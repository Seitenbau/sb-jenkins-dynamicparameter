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
package com.seitenbau.jenkins.plugins.dynamicparameter.config;

import java.io.File;
import java.io.IOException;

import jenkins.model.Jenkins;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.Saveable;

/**
 * Dynamic parameter configuration options.
 */
public enum DynamicParameterConfiguration
{
  /** Single class instance. */
  INSTANCE;

  private final transient ConfigurationImpl config = ConfigurationImpl.load();

  /**
   * Get the base directory as instance of {@link File}.
   * @return base directory
   */
  public File getBaseDirectoryFile()
  {
    return config.getBaseDirectory();
  }

  /**
   * Get the base directory.
   * @return canonical path to the base directory
   * @throws IOException if path can not be resolved
   */
  public String getBaseDirectory() throws IOException
  {
    return config.getBaseDirectory().getCanonicalPath();
  }

  /**
   * Set a new base directory.
   * @param newBaseDirectory new directory path
   * @throws IOException if configuration cannot be saved
   */
  public void setBaseDirectory(String newBaseDirectory) throws IOException
  {
    config.setBaseDirectory(newBaseDirectory);
  }

  private static final class ConfigurationImpl implements Saveable
  {
    private static final String CONFIG_FILE = "config.xml";

    private static final String HOME_DIR = "dynamic_parameter";

    private static final String DEFAULT_BASE_DIR = "classpath";

    private File baseDirectory;

    private ConfigurationImpl()
    {
      baseDirectory = getDefaultBaseDirectory();
    }

    public File getBaseDirectory()
    {
      return baseDirectory;
    }

    public void setBaseDirectory(String newBaseDirectory) throws IOException
    {
      baseDirectory = new File(newBaseDirectory);
      save();
    }

    @Override
    public synchronized void save() throws IOException
    {
      if (BulkChange.contains(this))
      {
        return;
      }
      getConfigFile().write(this);
    }

    /**
     * Load the configuration from file or create a new instance.
     * @return
     */
    public static ConfigurationImpl load()
    {
      XmlFile config = getConfigFile();
      if (config.exists())
      {
        try
        {
          return (ConfigurationImpl) config.read();
        }
        catch (IOException e)
        {
          // TODO add logging
          e.printStackTrace();
        }
      }
      return new ConfigurationImpl();
    }

    private static XmlFile getConfigFile()
    {
      File home = getHomeDirectory();
      File config = new File(home, CONFIG_FILE);
      return new XmlFile(Jenkins.XSTREAM, config);
    }

    private static File getHomeDirectory()
    {
      Hudson hudson = Hudson.getInstance();
      if (hudson == null)
      {
        return new File(HOME_DIR);
      }
      else
      {
        return new File(hudson.getRootDir(), HOME_DIR);
      }
    }

    private static File getDefaultBaseDirectory()
    {
      return new File(getHomeDirectory(), DEFAULT_BASE_DIR);
    }

  }

}
