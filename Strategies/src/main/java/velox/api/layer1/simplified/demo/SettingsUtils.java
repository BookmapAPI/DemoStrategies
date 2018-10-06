package velox.api.layer1.simplified.demo;

import java.lang.annotation.Annotation;
import java.util.Map;

public abstract class SettingsUtils {

	/*
	 * This is the draft of the class dealing with settings (annotated fields in the user's
	 * class). 
	 * 
	 * 1) the settings container (a map, an object, whatever) is checked
	 * whether it contains any settings (like a map gets checked if it is not null). 
	 * If it does, steps 2 and 3 are  skipped.
	 * 
	 * 2) If it is empty
	 * values are read from annotated fields and thus become initial values.
	 * 
	 * 3) These values get saved to the settings container.
	 * 
	 * 4) Now the settings container is not empty and and settings values are acquired from it. 
	 */
	abstract boolean checkIfSettingsExist(Object object);

	abstract Map<String, Map<String, Object>> readSettingsFromAnnotations(Object object, Class<? extends Annotation> annotationClass);

	abstract void saveSettings();

	abstract void setValuesFromSettings();
	

}
