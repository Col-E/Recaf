package me.coley.recaf.ui.control.config;

import com.google.common.primitives.Longs;
import javafx.beans.NamedArg;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.LongStringConverter;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.bounds.LongLowerBound;
import me.coley.recaf.config.bounds.LongUpperBound;
import me.coley.recaf.util.ReflectUtil;

import java.lang.reflect.Field;

/**
 * Spinner for setting a long property.
 *
 * @author Amejonah
 */
public class ConfigLong extends Spinner<Long> implements Unlabeled {
	/**
	 * @param container
	 * 		Config container.
	 * @param field
	 * 		Config field.
	 */
	public ConfigLong(ConfigContainer container, Field field) {
		long min = getMin(field);
		long max = getMax(field);
		long value = 0;
		if (LongProperty.class.isAssignableFrom(field.getType())) {
			LongProperty property = ReflectUtil.quietGet(container, field);
			value = property.get();
			property.bind(valueProperty());
		} else if (Long.class.equals(field.getType()) || long.class.equals(field.getType())) {
			value = ReflectUtil.quietGet(container, field);
			valueProperty().addListener((observable, oldValue, newValue) -> ReflectUtil.quietSet(container, field, newValue));
		} else {
			throw new IllegalArgumentException("Field " + container.getClass().getName() + "#" + field.getName() + " must be of type long, Long or LongProperty");
		}
		setValueFactory(new ConfigLong.LongSpinnerValueFactory(min, max, Math.max(min, Math.min(value, max))));
		getEditor().setTextFormatter(new TextFormatter<>(change -> {
			Long v = Longs.tryParse(change.getControlNewText());
			if (v == null || !(min <= v && v <= max)) {
				return null;
			}
			return change;
		}));
		setEditable(true);
	}

	private long getMin(Field field) {
		LongLowerBound lowerBound = field.getAnnotation(LongLowerBound.class);
		return lowerBound != null ? lowerBound.value() : Long.MIN_VALUE;
	}

	private long getMax(Field field) {
		LongUpperBound upperBound = field.getAnnotation(LongUpperBound.class);
		return upperBound != null ? upperBound.value() : Long.MAX_VALUE;
	}

	/**
	 * As JFX is a literally "half-of-batteries-included-framework", it provides only int and double ValueFactories.
	 * So copy pasta from {@link SpinnerValueFactory.IntegerSpinnerValueFactory} and changed to Long.
	 */
	public static class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {

		/* *********************************************************************
		 *                                                                     *
		 * Constructors                                                        *
		 *                                                                     *
		 **********************************************************************/

		/**
		 * Constructs a new LongSpinnerValueFactory that sets the initial value
		 * to be equal to the min value, and a default {@code amountToStepBy} of one.
		 *
		 * @param min The minimum allowed long value for the Spinner.
		 * @param max The maximum allowed long value for the Spinner.
		 */
		public LongSpinnerValueFactory(@NamedArg("min") long min,
																	 @NamedArg("max") long max) {
			this(min, max, min);
		}

		/**
		 * Constructs a new IntegerSpinnerValueFactory with a default
		 * {@code amountToStepBy} of one.
		 *
		 * @param min          The minimum allowed integer value for the Spinner.
		 * @param max          The maximum allowed integer value for the Spinner.
		 * @param initialValue The value of the Spinner when first instantiated, must
		 *                     be within the bounds of the min and max arguments, or
		 *                     else the min value will be used.
		 */
		public LongSpinnerValueFactory(@NamedArg("min") long min,
																	 @NamedArg("max") long max,
																	 @NamedArg("initialValue") long initialValue) {
			this(min, max, initialValue, 1);
		}

		/**
		 * Constructs a new IntegerSpinnerValueFactory.
		 *
		 * @param min            The minimum allowed integer value for the Spinner.
		 * @param max            The maximum allowed integer value for the Spinner.
		 * @param initialValue   The value of the Spinner when first instantiated, must
		 *                       be within the bounds of the min and max arguments, or
		 *                       else the min value will be used.
		 * @param amountToStepBy The amount to increment or decrement by, per step.
		 */
		public LongSpinnerValueFactory(@NamedArg("min") long min,
																	 @NamedArg("max") long max,
																	 @NamedArg("initialValue") long initialValue,
																	 @NamedArg("amountToStepBy") long amountToStepBy) {
			setMin(min);
			setMax(max);
			setAmountToStepBy(amountToStepBy);
			setConverter(new LongStringConverter());

			valueProperty().addListener((o, oldValue, newValue) -> {
				// when the value is set, we need to react to ensure it is a
				// valid value (and if not, blow up appropriately)
				if (newValue < getMin()) {
					setValue(getMin());
				} else if (newValue > getMax()) {
					setValue(getMax());
				}
			});
			setValue(initialValue >= min && initialValue <= max ? initialValue : min);
		}


		/* *********************************************************************
		 *                                                                     *
		 * Properties                                                          *
		 *                                                                     *
		 **********************************************************************/

		// --- min
		private LongProperty min = new SimpleLongProperty(this, "min") {
			@Override
			protected void invalidated() {
				Long currentValue = LongSpinnerValueFactory.this.getValue();
				if (currentValue == null) {
					return;
				}

				long newMin = get();
				if (newMin > getMax()) {
					setMin(getMax());
					return;
				}

				if (currentValue < newMin) {
					LongSpinnerValueFactory.this.setValue(newMin);
				}
			}
		};

		public final void setMin(long value) {
			min.set(value);
		}

		public final long getMin() {
			return min.get();
		}

		/**
		 * Sets the minimum allowable value for this value factory
		 *
		 * @return the minimum allowable value for this value factory
		 */
		public final LongProperty minProperty() {
			return min;
		}

		// --- max
		private LongProperty max = new SimpleLongProperty(this, "max") {
			@Override
			protected void invalidated() {
				Long currentValue = LongSpinnerValueFactory.this.getValue();
				if (currentValue == null) {
					return;
				}

				long newMax = get();
				if (newMax < getMin()) {
					setMax(getMin());
					return;
				}

				if (currentValue > newMax) {
					LongSpinnerValueFactory.this.setValue(newMax);
				}
			}
		};

		public final void setMax(long value) {
			max.set(value);
		}

		public final long getMax() {
			return max.get();
		}

		/**
		 * Sets the maximum allowable value for this value factory
		 *
		 * @return the maximum allowable value for this value factory
		 */
		public final LongProperty maxProperty() {
			return max;
		}

		// --- amountToStepBy
		private LongProperty amountToStepBy = new SimpleLongProperty(this, "amountToStepBy");

		public final void setAmountToStepBy(long value) {
			amountToStepBy.set(value);
		}

		public final long getAmountToStepBy() {
			return amountToStepBy.get();
		}

		/**
		 * Sets the amount to increment or decrement by, per step.
		 *
		 * @return the amount to increment or decrement by, per step
		 */
		public final LongProperty amountToStepByProperty() {
			return amountToStepBy;
		}



		/* *********************************************************************
		 *                                                                     *
		 * Overridden methods                                                  *
		 *                                                                     *
		 **********************************************************************/

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void decrement(int steps) {
			final long min = getMin();
			final long max = getMax();
			final long newIndex = getValue() - steps * getAmountToStepBy();
			setValue(newIndex >= min ? newIndex : (isWrapAround() ? wrapValue(newIndex, min, max) + 1 : min));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void increment(int steps) {
			final long min = getMin();
			final long max = getMax();
			final long currentValue = getValue();
			final long newIndex = currentValue + steps * getAmountToStepBy();
			setValue(newIndex <= max ? newIndex : (isWrapAround() ? wrapValue(newIndex, min, max) - 1 : max));
		}

		/*
		 * Convenience method to support wrapping values around their min / max
		 * constraints. Used by the SpinnerValueFactory implementations when
		 * the Spinner wrapAround property is true.
		 */
		private static long wrapValue(long value, long min, long max) {
			if (max == 0) {
				throw new RuntimeException();
			}

			long r = value % max;
			if (r > min && max < min) {
				r = r + max - min;
			} else if (r < min && max > min) {
				r = r + max - min;
			}
			return r;
		}
	}
}
