package velox.api.layer1.simplified.demo;

import velox.api.layer1.common.Log;
import velox.api.layer1.settings.StrategySettingsVersion;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.HashMap;

@StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})
public class UserSettings {

//	<parameterName, parameterValue>
//	Map<String, Object> settings = new HashMap<>();
	Map<String, Map<String, Object>> settings = new HashMap<>();
	
	public void setSettings(Map<String, Map<String, Object>> settings) {
		this.settings = settings;
	}
	
	public boolean isEmpty() {
		if(settings.isEmpty())
			return true;
		return false;
	}

	public Map<String, Map<String, Object>> getSettings() {
		return settings;
	}
	
	

//	public void changeInstrumentSettings(Object obj, Field field){
//
//		String name = field.getAnnotation(Parameter.class).name();
//
//		try {
//			Object value = field.get(obj);
//			settings.put(name, value);
//			Log.info("");
//		} catch (IllegalArgumentException | IllegalAccessException e) {
//			e.printStackTrace();
//		}	
//	}
	


}
