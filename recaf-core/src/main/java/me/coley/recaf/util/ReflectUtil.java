package me.coley.recaf.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection utilities.
 *
 * @author Matt Coley
 * @author xDark
 */
public final class ReflectUtil {
	private static final Map<Class<?>, ThrowableGetter<?>> GETTERS = new HashMap<>();
	private static final Map<Class<?>, ThrowableSetter<?>> SETTERS = new HashMap<>();
	private static final ThrowableGetter<?> DEFAULT_GETTER = Field::get;
	private static final ThrowableSetter<?> DEFAULT_SETTER = Field::set;

	/**
	 * Deny all constructions.
	 */
	private ReflectUtil() {
	}

	/**
	 * @param declaringClass
	 * 		class that declares a field.
	 * @param name
	 * 		the name of the field.
	 *
	 * @return the {@link Field} object for the specified field in the
	 * class.
	 *
	 * @throws NoSuchFieldException
	 * 		if a field with the specified name is not found.
	 */
	public static Field getDeclaredField(Class<?> declaringClass, String name) throws NoSuchFieldException {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field;
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
	 * Copy field values in 'from' into 'to'.
	 *
	 * @param from
	 * 		Instance with values to copy.
	 * @param to
	 * 		Destination to copy values into.
	 */
	public static void copyTo(Object from, Object to) {
		if (from == null || to == null)
			return;
		// Types must match
		Class<?> type = to.getClass();
		if (!type.equals(from.getClass()))
			return;
		// Copy field values in 'from' into 'to'
		for (Field field : type.getDeclaredFields()) {
			field.setAccessible(true);
			Object value = quietGet(from, field);
			quietSet(to, field, value);
		}
	}

	static {
		// Getters
		GETTERS.put(boolean.class, (ReflectUtil.ThrowableGetter<Boolean>) Field::getBoolean);
		GETTERS.put(byte.class, (ReflectUtil.ThrowableGetter<Byte>) Field::getByte);
		GETTERS.put(char.class, (ReflectUtil.ThrowableGetter<Character>) Field::getChar);
		GETTERS.put(short.class, (ReflectUtil.ThrowableGetter<Short>) Field::getShort);
		GETTERS.put(int.class, (ReflectUtil.ThrowableGetter<Integer>) Field::getInt);
		GETTERS.put(long.class, (ReflectUtil.ThrowableGetter<Long>) Field::getLong);
		GETTERS.put(float.class, (ReflectUtil.ThrowableGetter<Float>) Field::getFloat);
		GETTERS.put(double.class, (ReflectUtil.ThrowableGetter<Double>) Field::getDouble);
		// Setters
		SETTERS.put(boolean.class, (ReflectUtil.ThrowableSetter<Boolean>) Field::setBoolean);
		SETTERS.put(byte.class, (ReflectUtil.ThrowableSetter<Byte>) Field::setByte);
		SETTERS.put(char.class, (ReflectUtil.ThrowableSetter<Character>) Field::setChar);
		SETTERS.put(short.class, (ReflectUtil.ThrowableSetter<Short>) Field::setShort);
		SETTERS.put(int.class, (ReflectUtil.ThrowableSetter<Integer>) Field::setInt);
		SETTERS.put(long.class, (ReflectUtil.ThrowableSetter<Long>) Field::setLong);
		SETTERS.put(float.class, (ReflectUtil.ThrowableSetter<Float>) Field::setFloat);
		SETTERS.put(double.class, (ReflectUtil.ThrowableSetter<Double>) Field::setDouble);
	}

	/**
	 * Functional interface for field set operations.
	 *
	 * @param <T>
	 * 		Return type.
	 */
	interface ThrowableSetter<T> {
		void set(Field field, Object instance, T value) throws IllegalAccessException;
	}

	/**
	 * Functional interface for field get operations.
	 *
	 * @param <T>
	 * 		Parameter type.
	 */
	interface ThrowableGetter<T> {
		T get(Field field, Object instance) throws IllegalAccessException;
	}
}
