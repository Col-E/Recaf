package software.coley.recaf.ui.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.ui.pane.editing.android.AndroidClassEditorType;
import software.coley.recaf.ui.pane.editing.ClassPane;
import software.coley.recaf.ui.pane.editing.jvm.JvmClassEditorType;

/**
 * Config for {@link ClassPane} and its child types, plus any components they declare.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ClassEditingConfig extends BasicConfigContainer {
	public static final String ID = "class-editing";
	private final ObservableObject<AndroidClassEditorType> defaultAndroidEditor = new ObservableObject<>(AndroidClassEditorType.DECOMPILE);
	private final ObservableObject<JvmClassEditorType> defaultJvmEditor = new ObservableObject<>(JvmClassEditorType.DECOMPILE);

	@Inject
	public ClassEditingConfig() {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("default-android-editor", AndroidClassEditorType.class, defaultAndroidEditor));
		addValue(new BasicConfigValue<>("default-jvm-editor", JvmClassEditorType.class, defaultJvmEditor));
	}

	/**
	 * @return Default editor to display {@link AndroidClassInfo} content with.
	 */
	public ObservableObject<AndroidClassEditorType> getDefaultAndroidEditor() {
		return defaultAndroidEditor;
	}

	/**
	 * @return Default editor to display {@link JvmClassInfo} content with.
	 */
	public ObservableObject<JvmClassEditorType> getDefaultJvmEditor() {
		return defaultJvmEditor;
	}
}
