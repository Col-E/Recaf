package software.coley.recaf.util;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;

import java.text.Format;

/**
 * Synchronized implementation of {@link SimpleStringProperty}.
 *
 * @author xDark
 */
public class SynchronizedSimpleStringProperty extends SimpleStringProperty {
	/**
	 * @param initialValue
	 * 		Initial value.
	 */
	public SynchronizedSimpleStringProperty(String initialValue) {
		super(initialValue);
	}

	@Override
	public synchronized void addListener(ChangeListener<? super String> listener) {
		super.addListener(listener);
	}

	@Override
	public synchronized void addListener(InvalidationListener listener) {
		super.addListener(listener);
	}

	@Override
	public synchronized void removeListener(ChangeListener<? super String> listener) {
		super.removeListener(listener);
	}

	@Override
	public synchronized void removeListener(InvalidationListener listener) {
		super.removeListener(listener);
	}

	@Override
	public synchronized void bind(ObservableValue<? extends String> newObservable) {
		super.bind(newObservable);
	}

	@Override
	public synchronized void bindBidirectional(Property<String> other) {
		super.bindBidirectional(other);
	}

	@Override
	public synchronized void bindBidirectional(Property<?> other, Format format) {
		super.bindBidirectional(other, format);
	}

	@Override
	protected synchronized void invalidated() {
		super.invalidated();
	}

	@Override
	public synchronized String get() {
		return super.get();
	}

	@Override
	public synchronized void set(String newValue) {
		super.set(newValue);
	}

	@Override
	public synchronized void unbind() {
		super.unbind();
	}

	@Override
	public synchronized <T> void bindBidirectional(Property<T> other, StringConverter<T> converter) {
		super.bindBidirectional(other, converter);
	}

	@Override
	public synchronized void unbindBidirectional(Property<String> other) {
		super.unbindBidirectional(other);
	}

	@Override
	public synchronized void unbindBidirectional(Object other) {
		super.unbindBidirectional(other);
	}
}