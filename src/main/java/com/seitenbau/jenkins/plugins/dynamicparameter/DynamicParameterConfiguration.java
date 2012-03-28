package com.seitenbau.jenkins.plugins.dynamicparameter;

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
   * Get the class path directory.
   * @return absolute path to the class path directory
   */
  public String getClassPathDirectory()
  {
    return config.getClassPathDirectory();
  }

  /**
   * Set a new class path directory.
   * @param newClassPathDirectory new directory path
   * @throws IOException if configuration cannot be saved
   */
  public void setClassPathDirectory(String newClassPathDirectory) throws IOException
  {
    config.setClassPathDirectory(newClassPathDirectory);
  }

  private static final class ConfigurationImpl implements Saveable
  {
    private static final String CONFIG_FILE = "config.xml";

    private static final String HOME_DIR = "dynamic_parameter";

    private static final String DEFAULT_CLASSPATH_DIR = "classpath";

    private String classPathDirectory;

    private ConfigurationImpl()
    {
      classPathDirectory = getDefaultClassPathDirectory().getAbsolutePath();
    }

    public String getClassPathDirectory()
    {
      return classPathDirectory;
    }

    public void setClassPathDirectory(String newClassPathDirectory) throws IOException
    {
      File newDirectory = new File(newClassPathDirectory);
      classPathDirectory = newDirectory.getAbsolutePath();
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
      return new File(Hudson.getInstance().getRootDir(), HOME_DIR);
    }

    private static File getDefaultClassPathDirectory()
    {
      return new File(getHomeDirectory(), DEFAULT_CLASSPATH_DIR);
    }

  }

}
