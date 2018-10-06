package velox.api.layer1.simplified.demo;

import velox.api.layer1.common.Log;
import velox.gui.StrategyPanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class InstanceUtils extends SettingsUtils {

	@Override//delete
	public boolean checkIfSettingsExist(Object settings) {
			return false;
	}

	public void setValuesFromSettings(UserSettings settings, Object instance, Class<? extends Annotation> annotationClass) {
//		< fieldName, <K annotation field name, V annotation field value>>
		Map<String, Map<String, Object>> settingsMap = settings.getSettings();
		
		
		
		Field[] fieldList = instance.getClass().getDeclaredFields();

		for (Field field : fieldList) {
			if (field.isAnnotationPresent(annotationClass)) {
				String fieldName = field.getName(); 
				Map<String, Object> parameterMap = settingsMap.get(fieldName);
				field.setAccessible(true);
				Object value = parameterMap.get(fieldName);
				
				try {
					field.set(instance, value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public Map<String, Map<String, Object>> readSettingsFromAnnotations(Object instance, Class<? extends Annotation> annotationClass) {
//		< fieldName, <K annotation field name, V annotation field value>>
		Map<String, Map<String, Object>> settingsMap = new HashMap<>();
		Field[] fieldList = instance.getClass().getDeclaredFields();

		for (Field field : fieldList) {
			if (field.isAnnotationPresent(annotationClass)) {
				Map<String, Object> parameterMap = new HashMap<>();
				putValuesFromAnnotation(annotationClass, field, parameterMap);
				putDefaultFieldValue(field, instance, parameterMap);
				String parameterName = field.getName();
				settingsMap.put(parameterName, parameterMap);
			}
		}
		return settingsMap;
	}

	private void putValuesFromAnnotation(Class<? extends Annotation> annotationClass, Field field, Map<String, Object> parameterMap) {
		Annotation annotation = field.getAnnotation(annotationClass);
		Method[] methods = annotationClass.getDeclaredMethods();

		for (Method method : methods) {
			String name = method.getName();
			
			try {
				Object value = method.invoke(annotation);
				parameterMap.put(name, value);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	private void putDefaultFieldValue(Field field, Object object, Map<String, Object> parameterMap) {
		try {
			Object defaultValue = field.get(object);
			parameterMap.put("defaultValue", defaultValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	void saveSettings() {
		// TODO Auto-generated method stub

	}

	@Override
	void setValuesFromSettings() {
		// TODO Auto-generated method stub

	}
	
	
	public List<StrategyPanel> addCustomGUI(Object source, UserSettings settings, String panelName) {
		Field[] fieldList = source.getClass().getDeclaredFields();

		// The panel gets created
		StrategyPanel panel = new StrategyPanel(panelName , new GridBagLayout());
		int y = 0;
		List<Boolean> flags = new ArrayList<>();
		Map<Field, Object> fieldsAndSpinners = new HashMap<>();
		JButton button = new JButton("Apply");
		button.setEnabled(false);

		for (Field field : fieldList) {
			if (field.isAnnotationPresent(Parameter.class)) {
				flags.add(false);

				Parameter parameter = field.getAnnotation(Parameter.class);
				String name = parameter.name();
				JLabel jlabel = new JLabel(name);


				Type type = field.getType();
				Class<?> clazz = (Class<?>) type;

				JComponent component = null;

				if (Number.class.isAssignableFrom(clazz)) {
					Log.info("Number");
					JSpinner spinner = setNumricalSpinner(clazz, field, source, parameter, fieldsAndSpinners, button,
							flags, y);
					component = spinner;
					addNumericActionListener(source, fieldsAndSpinners, button, flags);
				}

				if (String.class.isAssignableFrom(clazz)) {
					Log.info("String");
				}
				if (Boolean.class.isAssignableFrom(clazz)) {
					Log.info("Boolean");
					JCheckBox checkBox = setCheckBox(field, source, parameter, fieldsAndSpinners, button, flags, y);
					component = checkBox;
					addNumericActionListener(source, fieldsAndSpinners, button, flags);

				}

				panel.add(jlabel, getConstraints(1, y));
				panel.add(component, getConstraints(2, y));
				y++;
			}

		}

		panel.add(button, getConstraints(0, y + 1));

		List<StrategyPanel> panels = new ArrayList<>();
		panels.add(panel);

		return panels;

	}
	
	private <T> JSpinner setNumricalSpinner(Class<T> clazz, Field field, Object source, Parameter parameter,
			Map<Field, Object> fieldsAndSpinners, JButton button, List<Boolean> flags, int y) {

		Constructor[] constructors = clazz.getDeclaredConstructors();
		Constructor constructor = constructors[0];
		constructors[0].setAccessible(true);

		Double value = null;

		Object objValue = null;
		try {
			objValue = field.get(source);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		if (clazz == Integer.class) {
			// long l = Math.round((float) objValue);
			value = new Double((Integer) objValue);
		} else {
			value = (double) objValue;
		}

		Double stepSize = (double) parameter.step();
		Double minimum = 100.0;
		Double maximum = 1000000.0;
		// try {
		// minimum = (T) constructor.newInstance(100);
		// maximum = (T) constructor.newInstance(1_000_000);
		// } catch (InstantiationException | IllegalAccessException |
		// IllegalArgumentException
		// | InvocationTargetException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }

		// T stepSize = step;
		SpinnerNumberModel sModel = new SpinnerNumberModel((double) value, (double) minimum, (double) maximum,
				(double) stepSize);

		JSpinner spinner = new JSpinner(sModel);
		((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(6);

		fieldsAndSpinners.put(field, spinner);

		Field[] workaroundArray = new Field[] { field };

		

		final int index = y;

		spinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {

				Object eventSource = event.getSource();
				JSpinner sp = (JSpinner) eventSource;
				Double tempValue = (Double) sp.getValue();
				Object comparable = null;

				try {
					Field workaroundAnnotatedField = workaroundArray[0];
					Object obTemp = field.get(source);
					comparable = obTemp;
					if (Integer.class.isAssignableFrom(clazz)) {
						comparable = new Double((double)((int) obTemp));
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}

				if (tempValue.equals(comparable)) {
					flags.set(index, false);
				} else {
					flags.set(index, true);
				}

				boolean isEnabled = false;
				for (boolean flag : flags) {
					if (flag == true) {
						isEnabled = true;
					}
				}

				button.setEnabled(isEnabled);

			}
		});

		return spinner;
	}
	
	private <T> void addNumericActionListener(Object source, Map<Field, Object> fieldsAndSpinners, JButton button,
			List<Boolean> flags) {

		button.addActionListener(e -> {
			for (Field field : fieldsAndSpinners.keySet()) {
				Type type = field.getType();
				Class<?> clazz = (Class<?>) type;

				Object value = null;
				if (Number.class.isAssignableFrom(clazz)) {
					JSpinner spinner = (JSpinner) fieldsAndSpinners.get(field);
					T spinnerValue = (T) spinner.getValue();
					value = spinnerValue;
					if (Integer.class.isAssignableFrom(clazz)) {
						value = new Integer((int)((double) spinnerValue));
					}
				} else if (Boolean.class.isAssignableFrom(clazz)) {
					JCheckBox checkBox = (JCheckBox) fieldsAndSpinners.get(field);
					value = checkBox.isSelected();
				}

				try {
					field.set(source, value);

//					UserSettings userSettings = (UserSettings) getSettingsFor(null);
//					if (userSettings == null) {
//						userSettings = new UserSettings();
//					}
//					userSettings.changeGlobalSettings(source, field);
//					settingsChanged(null, userSettings);

				} catch (IllegalArgumentException | IllegalAccessException e1) {
					e1.printStackTrace();
				}

			}

//			initialize();
//			reInitialize();

			for (int i = 0; i < flags.size(); i++) {
				flags.set(i, false);
			}
			button.setEnabled(false);

		});
	}
	
	private GridBagConstraints getConstraints(int x, int y) {
		GridBagConstraints gbConst = new GridBagConstraints();
		gbConst.gridx = x;
		gbConst.gridy = y;
		// gbConst.weightx = 1;
		gbConst.insets = new Insets(5, 5, 5, 5);
		gbConst.fill = GridBagConstraints.VERTICAL;
		return gbConst;
	}
	
	private JCheckBox setCheckBox(Field field, Object source, Parameter parameter, Map<Field, Object> fieldsAndSpinners,
			JButton button, List<Boolean> flags, int y) {

		Boolean isBoxSelected = false;
		try {
			isBoxSelected = (Boolean) field.get(source);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		JCheckBox checkBox = new JCheckBox("");
		checkBox.setSelected(isBoxSelected);

		fieldsAndSpinners.put(field, checkBox);

		Field[] workaroundArray = new Field[] { field };

		Field workaroundAnnotatedField = workaroundArray[0];

		final int index = y;

		checkBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {

				Boolean isSelected;
				if (event.getStateChange() == ItemEvent.SELECTED) {
					isSelected = true;
				} else {
					isSelected = false;
				}

				Boolean comparable = null;

				try {
					comparable = (Boolean) workaroundAnnotatedField.get(source);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}

				if (isSelected.equals(comparable)) {
					flags.set(index, false);
				} else {
					flags.set(index, true);
				}

				boolean isEnabled = false;
				for (boolean flag : flags) {
					if (flag == true) {
						isEnabled = true;
					}
				}

				button.setEnabled(isEnabled);

			}
		});

		return checkBox;
	}

}
