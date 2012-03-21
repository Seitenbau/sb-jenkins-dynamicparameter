package com.seitenbau.jenkins.plugins.dynamicparameter;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.RootAction;

@Extension
public class DynamicParameterManagement extends ManagementLink
{

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
}
