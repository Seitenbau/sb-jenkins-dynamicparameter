
package com.seitenbau.jenkins.plugins.dynamicparameter;

public class CascadingChoiceParameterDefinitionParameterBuilder extends
    CascadingChoiceParameterDefinitionParameterBuilderBase<CascadingChoiceParameterDefinitionParameterBuilder>
{
  public static CascadingChoiceParameterDefinitionParameterBuilder cascadingChoiceParameterDefinitionParameter()
  {
    return new CascadingChoiceParameterDefinitionParameterBuilder();
  }

  public CascadingChoiceParameterDefinitionParameterBuilder()
  {
    super(new CascadingChoiceParameterDefinitionParameter());
  }

  public CascadingChoiceParameterDefinitionParameter build()
  {
    return getInstance();
  }
}

class CascadingChoiceParameterDefinitionParameterBuilderBase<GeneratorT extends CascadingChoiceParameterDefinitionParameterBuilderBase<GeneratorT>>
{
  private CascadingChoiceParameterDefinitionParameter instance;

  protected CascadingChoiceParameterDefinitionParameterBuilderBase(
      CascadingChoiceParameterDefinitionParameter aInstance)
  {
    instance = aInstance;
  }

  protected CascadingChoiceParameterDefinitionParameter getInstance()
  {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withName(String aValue)
  {
    instance.setName(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withParentPropertyName(String aValue)
  {
    instance.setParentPropertyName(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withScript(String aValue)
  {
    instance.setScript(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withDescription(String aValue)
  {
    instance.setDescription(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withUuid(String aValue)
  {
    instance.setUuid(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withRemote(boolean aValue)
  {
    instance.setRemote(aValue);

    return (GeneratorT) this;
  }
}
