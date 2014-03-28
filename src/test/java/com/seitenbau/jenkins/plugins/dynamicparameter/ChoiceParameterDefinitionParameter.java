package com.seitenbau.jenkins.plugins.dynamicparameter;

class ChoiceParameterDefinitionParameter
{
  private String name;

  private String script;

  private String description;

  private String uuid;

  private boolean remote;
  
  private boolean sandbox;

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getScript()
  {
    return script;
  }

  public void setScript(String script)
  {
    this.script = script;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public String getUuid()
  {
    return uuid;
  }

  public void setUuid(String uuid)
  {
    this.uuid = uuid;
  }

  public boolean isRemote()
  {
    return remote;
  }

  public void setRemote(boolean remote)
  {
    this.remote = remote;
  }

  public boolean isSandbox() {
	return sandbox;
  }

  public void setSandbox(boolean sandbox) {
	this.sandbox = sandbox;
  }
  
}
