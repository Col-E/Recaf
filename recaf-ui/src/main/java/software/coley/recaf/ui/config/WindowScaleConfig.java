package software.coley.recaf.ui.config;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import software.coley.observables.ObservableDouble;
import software.coley.recaf.config.*;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.util.FxThreadUtil;

/**
 * Config for window scaling.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class WindowScaleConfig extends BasicConfigContainer {
	public static final String ID = "window-scale";
	private static final double MIN = 0.5;
	private static final double MAX = 2.0;
	private final ObservableDouble scale = new ObservableDouble(1);
	private final DoubleProperty scaleProperty = new SimpleDoubleProperty(1);
	private Slider slider;

	@Inject
	public WindowScaleConfig(@Nonnull ConfigComponentManager componentManager) {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("scale", double.class, scale));

		scaleProperty.set(getScale());
		scale.addChangeListener((_, _, cur) -> FxThreadUtil.run(() -> scaleProperty.set(Math.clamp(cur, MIN, MAX))));

		componentManager.register(this, "scale", false, (container, value) -> {
			slider = new Slider(MIN, MAX, getScale());
			slider.setSnapToTicks(true);
			slider.setShowTickLabels(true);
			slider.setBlockIncrement(0.25);
			slider.setMajorTickUnit(0.5);
			slider.setMinorTickCount(1);

			//Live readout of the current setting
			var readout = new Label();
			readout.textProperty().bind(Bindings.createStringBinding(() -> Math.round(slider.getValue() * 100) + "%", slider.valueProperty()));
			readout.getStyleClass().add(Styles.TEXT_SUBTLE);
			readout.setMinWidth(Region.USE_PREF_SIZE);
			readout.setPrefWidth(45);
			readout.setAlignment(Pos.CENTER_RIGHT);

			//Commit only when not dragging
			slider.valueProperty().addListener((_, _, cur) -> {
				if (!slider.isValueChanging())
					commitScale(cur.doubleValue());
			});
			slider.valueChangingProperty().addListener((_, _, isChanging) -> {
				if (!isChanging)
					commitScale(slider.getValue());
			});

			var box = new HBox(10, slider, readout);
			box.setAlignment(Pos.CENTER_LEFT);
			HBox.setHgrow(slider, Priority.ALWAYS);
			return box;
		});
	}

	private void commitScale(double value) {
		var clamped = Math.clamp(value, MIN, MAX);
		if (Double.compare(clamped, scale.getValue()) != 0)
			scale.setValue(clamped);
	}

	@Nonnull
	public DoubleProperty scaleProperty() {
		return scaleProperty;
	}

	/**
	 * @return Window scale.
	 */
	public double getScale() {
		return Math.clamp(scale.getValue(), MIN, MAX);
	}
}
