/**
 * Get the value of the parent property and request the select options for the child property 
 * using the parent property value as an argument. 
 * @param projectName - the job name
 * @param parentPropertyName - the name of the parent property (structured form element)
 * @param childPropertyName - the name of the child property (structured form element)
 */
function getChoiceValues(projectName, parentPropertyName, childPropertyName){
	new Ajax.Updater(projectName+'_'+childPropertyName, '/plugin/dynamicparameter/getParameterValues', {
		  parameters: { 
		  	projectName: projectName, 
		  	parentPropertyValue: getStructuredFormElementValue(parentPropertyName), 
		  	propertyName: childPropertyName}
	});
}

/**
 * Obtain the value of a specific parameter
 * @param structuredFormElementName the name of the structured form element
 * @returns the value of the structured form element
 */
function getStructuredFormElementValue(structuredFormElementName){
	var valObj = getStructuredFormElement(structuredFormElementName);
	if(valObj != null){
		return valObj.getValue();
	}
	return null;
}

/**
 * Set the id field on the form element to the supplied value
 * @param structuredFormElementName the name of the structured form element
 * @returns the element node or null if not found
 */
function getStructuredFormElement(structuredFormElementName){
	var theParamDiv = $$('div[name=parameter]').detect(function(p) {
		var nameObj = $A(p.getElementsByTagName("input")).detect(function(f){
			return f.name == 'name' && $F(f) == structuredFormElementName;
		})
		return (nameObj != null && nameObj.getValue() == structuredFormElementName)
	})
	if(theParamDiv != null){
        var objList = $A(theParamDiv.getElementsByTagName("select"));
        if(objList == null || objList.size() == 0){
        	objList = $A(theParamDiv.getElementsByTagName("input"));
        }
		//alert('objList:' + objList.collect( function(o) { return o.outerHTML}).join(';'));
	    var valObj = objList.compact().detect(function(f){
			return f.name == 'value'
		})
		return valObj;
	}
	return null
}

/**
 * Set the id field on the form element to the supplied value
 * @param formElementId the id to set
 * @param structuredFormElementName the name of the structured form element
 */
function addIdToFormElement(formElementId, structuredFormElementName){
	var valObj = getStructuredFormElement(structuredFormElementName);
	if(valObj != null){
		valObj.setAttribute("id", formElementId);
	}else{
		if(console) {
			console.error('ERROR: Structured Form Element Not Found: '+structuredFormElementName)
		} 
		else {
			alert('ERROR: Structured Form Element Not Found: '+structuredFormElementName)
		}
	}
}