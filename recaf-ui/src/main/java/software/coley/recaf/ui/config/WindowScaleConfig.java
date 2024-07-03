package software.coley.recaf.ui.config;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import software.coley.observables.ObservableDouble;
import software.coley.recaf.config.*;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.services.config.KeyedConfigComponentFactory;

/**
 * Config for window scaling.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WindowScaleConfig extends BasicConfigContainer {
	public static final String ID = "window-scale";
	private final ObservableDouble scale = new ObservableDouble(1);
	private Slider slider;

	@Inject
	public WindowScaleConfig(@Nonnull ConfigComponentManager componentManager) {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("scale", double.class, scale));

		componentManager.register(this, "scale", false, (container, value) -> {
			slider = new Slider(0.5, 2.0, getScale());
			slider.setSnapToTicks(true);
			slider.setShowTickLabels(true);
			slider.setBlockIncrement(0.5);
			slider.setMajorTickUnit(0.5);
			slider.setMinorTickCount(5);
			slider.valueProperty().addListener((ob, old, cur) -> scale.setValue(cur.doubleValue()));
			return slider;
		});
	}

	/**
	 * @return Window scale.
	 */
	public double getScale() {
		return Math.clamp(scale.getValue(), 0.5, 2);
	}
}
