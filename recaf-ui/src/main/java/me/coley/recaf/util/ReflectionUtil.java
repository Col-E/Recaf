package me.coley.recaf.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection utility for field get/set operations.
 *
 * @author Matt Coley
 */
public class ReflectionUtil {
	private static final Map<Class<?>, ThrowableGetter<?>> GETTERS = new HashMap<>();
	private static final Map<Class<?>, ThrowableSetter<?>> SETTERS = new HashMap<>();
	private static final ThrowableGetter<?> DEFAULT_GETTER = Field::get;
	private static final ThrowableSetter<?> DEFAULT_SETTER = Field::set;

	static {
		// Getters
		GETTERS.put(boolean.class, (ThrowableGetter<Boolean>) Field::getBoolean);
		GETTERS.put(byte.class, (ThrowableGetter<Byte>) Field::getByte);
		GETTERS.put(char.class, (ThrowableGetter<Character>) Field::getChar);
		GETTERS.put(short.class, (ThrowableGetter<Short>) Field::getShort);
		GETTERS.put(int.class, (ThrowableGetter<Integer>) Field::getInt);
		GETTERS.put(long.class, (ThrowableGetter<Long>) Field::getLong);
		GETTERS.put(float.class, (ThrowableGetter<Float>) Field::getFloat);
		GETTERS.put(double.class, (ThrowableGetter<Double>) Field::getDouble);
		// Setters
		SETTERS.put(boolean.class, (ThrowableSetter<Boolean>) Field::setBoolean);
		SETTERS.put(byte.class, (ThrowableSetter<Byte>) Field::setByte);
		SETTERS.put(char.class, (ThrowableSetter<Character>) Field::setChar);
		SETTERS.put(short.class, (ThrowableSetter<Short>) Field::setShort);
		SETTERS.put(int.class, (ThrowableSetter<Integer>) Field::setInt);
		SETTERS.put(long.class, (ThrowableSetter<Long>) Field::setLong);
		SETTERS.put(float.class, (ThrowableSetter<Float>) Field::setFloat);
		SETTERS.put(double.class, (ThrowableSetter<Double>) Field::setDouble);
	}

	/**
	 * @param instance
	 * 		Field owner instance.
	 * @param field
	 * 		Field to set value of.
	 * @param value
	 * 		Value to set.
	 * @param <T>
	 * 		Assumed field type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void quietSet(Object instance, Field field, T value) {
		try {
			ThrowableSetter<T> setter = (ThrowableSetter<T>) SETTERS.getOrDefault(field.getType(), DEFAULT_SETTER);
			setter.set(field, instance, value);
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException("Setter failure: " + instance.getClass() + "." + field.getName(), ex);
		}
	}

	/**
	 * @param instance
	 * 		Field owner instance.
	 * @param field
	 * 		Field to get value from.
	 * @param <T>
	 * 		Assumed field type.
	 *
	 * @return Value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T quietGet(Object instance, Field field) {
		try {
			ThrowableGetter<T> getter = (ThrowableGetter<T>) GETTERS.getOrDefault(field.getType(), DEFAULT_GETTER);
			return getter.get(field, instance);
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException("Getter failure: " + instance.getClass() + "." + field.getName(), ex);
		}
	}

	/**
	 * Functional interface for field set operations.
	 *
	 * @param <T>
	 * 		Return type.
	 */
	private interface ThrowableSetter<T> {
		void set(Field field, Object instance, T value) throws IllegalAccessException;
	}

	/**
	 * Functional interface for field get operations.
	 *
	 * @param <T>
	 * 		Parameter type.
	 */
	private interface ThrowableGetter<T> {
		T get(Field field, Object instance) throws IllegalAccessException;
	}
}
