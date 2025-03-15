package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import sun.reflect.ReflectionFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
	private static final ThrowableGetter<?> DEFAULT_GETTER = ReflectUtil::get;
	private static final ThrowableSetter<?> DEFAULT_SETTER = ReflectUtil::set;
	private static MethodHandles.Lookup trustedLookup;

	/**
	 * Deny all constructions.
	 */
	private ReflectUtil() {
	}

	/**
	 * Initialize reflection access patching.
	 */
	public static void patch() {
		// Intentionally empty. The patching happens in the static initializer, which the caller will trigger
		// by calling this method.
	}

	/**
	 * @return Trusted lookup with all-access permissions.
	 */
	@Nonnull
	public static MethodHandles.Lookup lookup() {
		if (trustedLookup != null)
			return trustedLookup;
		try {
			Constructor<MethodHandles.Lookup> lookupCtor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
			Constructor<?> ctor = ReflectionFactory.getReflectionFactory()
					.newConstructorForSerialization(MethodHandles.Lookup.class, lookupCtor);
			MethodHandles.Lookup lookup = (MethodHandles.Lookup) ctor.newInstance(MethodHandles.class);
			trustedLookup = (MethodHandles.Lookup) lookup
					.findStaticGetter(MethodHandles.Lookup.class, "IMPL_LOOKUP", MethodHandles.Lookup.class)
					.invokeExact();
			return trustedLookup;
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}

	/**
	 * @param declaringClass
	 * 		Class that declares the field.
	 * @param name
	 * 		Name of the field.
	 *
	 * @return {@link Field} object for the specified field in the class.
	 *
	 * @throws NoSuchFieldException
	 * 		When a field with the specified name is not found.
	 */
	@Nonnull
	public static Field getDeclaredField(Class<?> declaringClass, String name) throws NoSuchFieldException {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/**
	 * @param declaringClass
	 * 		Class that declares the method.
	 * @param name
	 * 		Name of the method.
	 * @param args
	 * 		Argument types of the method.
	 *
	 * @return {@link Method} object for the specified field in the class.
	 *
	 * @throws NoSuchMethodException
	 * 		When a method with the specified name and argument specification is not found.
	 */
	@Nonnull
	public static Method getDeclaredMethod(Class<?> declaringClass, String name, Class<?>... args)
			throws NoSuchMethodException {
		Method method = declaringClass.getDeclaredMethod(name, args);
		method.setAccessible(true);
		return method;
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
	@Nullable
	public static <T> T quietGet(Object instance, Field field) {
		try {
			ThrowableGetter<T> getter = (ThrowableGetter<T>) GETTERS.getOrDefault(field.getType(), DEFAULT_GETTER);
			return getter.get(field, instance);
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException("Getter failure: " + instance.getClass() + "." + field.getName(), ex);
		}
	}

	/**
	 * @param type
	 * 		Class to construct.
	 * @param argTypes
	 * 		Argument types.
	 * @param args
	 * 		Argument values.
	 * @param <T>
	 * 		Assumed class type.
	 *
	 * @return New instance of class.
	 */
	@Nonnull
	public static <T> T quietNew(Class<T> type, Class<?>[] argTypes, Object[] args) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor(argTypes);
			constructor.setAccessible(true);
			return constructor.newInstance(args);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Constructor failure: " + type.getName(), ex);
		}
	}

	/**
	 * @param type
	 * 		Class the method is defined in.
	 * @param instance
	 * 		Instance to invoke in, or {@code null} for static.
	 * @param name
	 * 		Method name.
	 * @param argTypes
	 * 		Argument types.
	 * @param args
	 * 		Argument values.
	 * @param <T>
	 * 		Assumed class type.
	 *
	 * @return Return value of invoke.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T quietInvoke(Class<?> type, Object instance, String name, Class<?>[] argTypes, Object[] args) {
		try {
			Method method = type.getDeclaredMethod(name, argTypes);
			method.setAccessible(true);
			return (T) method.invoke(instance, args);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Invoke failure: " + type.getName(), ex);
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
			if ((field.getModifiers() & Modifier.STATIC) > 0)
				continue;
			field.setAccessible(true);
			Object value = quietGet(from, field);
			quietSet(to, field, value);
		}
	}

	/**
	 * Propagates throwable.
	 *
	 * @param t
	 * 		Throwable to propagate.
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Throwable> void propagate(Throwable t) throws X {
		throw (X) t;
	}

	/**
	 * @param field
	 * 		Field to get from.
	 * @param instance
	 * 		Instance of the class.
	 *
	 * @return Field value in the instance.
	 *
	 * @throws IllegalAccessException
	 * 		When the field could not be accessed.
	 */
	@Nullable
	private static Object get(Field field, Object instance) throws IllegalAccessException {
		field.setAccessible(true);
		return field.get(instance);
	}

	/**
	 * @param field
	 * 		Field to set.
	 * @param instance
	 * 		Instance of the class.
	 * @param value
	 * 		Value to set.
	 *
	 * @throws IllegalAccessException
	 * 		When the field could not be accessed.
	 */
	private static void set(Field field, Object instance, Object value) throws IllegalAccessException {
		field.setAccessible(true);
		field.set(instance, value);
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

		// Bypass access restrictions
		AccessPatcher.patch();
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
