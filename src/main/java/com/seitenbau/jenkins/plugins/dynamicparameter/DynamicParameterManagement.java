package com.seitenbau.jenkins.plugins.dynamicparameter;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.util.FormValidation;

/** Plugin configuration. */
@Extension
public class DynamicParameterManagement extends ManagementLink
{
  private static final String[] EMPTY_ARRAY = new String[0];

  @Override
  public String getIconFileName()
  {
    return "notepad.png";
  }

  @Override
  public String getUrlName()
  {
    return "dynamicparameter";
  }

  @Override
  public String getDisplayName()
  {
    return "Dynamic Parameter";
  }

  @Override
  public String getDescription()
  {
    return "Settings for dynamic parameters";
  }

  public String getClassPathDir()
  {
    return ParameterDefinitionBase.DEFAULT_CLASSPATH;
  }

  /**
   * Set class path directory
   * @param classPath new class path directory
   */
  public void setClassPathDir(String classPath)
  {

  }

  public FormValidation doCheckClassPathDir(@QueryParameter String value)
  {
    File directory = new File(value);
    if (directory.isDirectory())
    {
      return FormValidation.ok();
    }
    else
    {
      return FormValidation.error("The given path is not a directory!");
    }
  }

  public String[] getUploadedFiles()
  {
    File classPathDir = new File(getClassPathDir());
    if (classPathDir.isDirectory())
    {
      return classPathDir.list();
    }
    return EMPTY_ARRAY;
  }
}
