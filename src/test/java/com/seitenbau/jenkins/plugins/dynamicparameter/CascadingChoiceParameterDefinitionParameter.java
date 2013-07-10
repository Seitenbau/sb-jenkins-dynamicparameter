package com.seitenbau.jenkins.plugins.dynamicparameter;

class CascadingChoiceParameterDefinitionParameter extends ChoiceParameterDefinitionParameter
{
  private String parentPropertyName;

  public String getParentPropertyName()
  {
    return parentPropertyName;
  }
  
  public void setParentPropertyName(String parentPropertyName)
  {
    this.parentPropertyName = parentPropertyName;
  }
}
