package me.coley.recaf.ui.component;

import java.lang.reflect.Field;
import me.coley.recaf.config.Conf;
import me.coley.recaf.util.Reflect;

public class ReflectiveConfigSheet extends ReflectivePropertySheet {
	public ReflectiveConfigSheet(Object... instances) {
		super(instances);
	}

	@Override
	protected void setupItems(Object instance) {
		for (Field field : Reflect.fields(instance.getClass())) {
			// Require conf annotation
			Conf conf = field.getDeclaredAnnotation(Conf.class);
			if (conf == null) continue;
			else if (conf.hide()) continue;
			// Setup item & add to list
			getItems().add(new ReflectiveConfigItem(instance, field, conf.category(), conf.key()));
		}
	}
}
