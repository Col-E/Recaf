package me.coley.recaf.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.NormalScope;
import jakarta.inject.Inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Bean CDI utils.
 *
 * @author Matt Coley
 */
public class BeanUtils {
	private static final Set<Class<?>> RECOGNIZED_SCOPES = new HashSet<>();

	/**
	 * @param cls
	 * 		Class to check.
	 *
	 * @return {@code true} if any fields or constructors have {@link Inject}.
	 */
	public static boolean hasInjects(Class<?> cls) {
		// TODO: Cache
		for (Constructor<?> constructor : cls.getConstructors()) {
			if (hasInjects(constructor))
				return true;
		}
		for (Field declaredField : cls.getDeclaredFields()) {
			if (hasInjects(declaredField))
				return true;
		}
		return false;
	}

	/**
	 * @param constructor
	 * 		Constructor to check.
	 *
	 * @return {@code true} if {@link Inject} exists.
	 */
	public static boolean hasInjects(Constructor<?> constructor) {
		return constructor.getDeclaredAnnotation(Inject.class) != null;
	}

	/**
	 * @param field
	 * 		Field to check.
	 *
	 * @return {@code true} if {@link Inject} exists.
	 */
	public static boolean hasInjects(Field field) {
		return field.getDeclaredAnnotation(Inject.class) != null;
	}

	/**
	 * @param cls
	 * 		Class to check.
	 *
	 * @return {@code true} if a recognized scope is annotating the class.
	 */
	public static boolean isBean(Class<?> cls) {
		// TODO: Cache
		for (Annotation annotation : cls.getAnnotations())
			if (RECOGNIZED_SCOPES.contains(annotation.annotationType()))
				return true;
		return false;
	}

	/**
	 * @param cls
	 * 		Class to check.
	 *
	 * @return {@code true} if {@link WorkspaceScoped} is annotating the class.
	 */
	public static boolean isWorkspaceBean(Class<?> cls) {
		// TODO: Cache
		for (Annotation annotation : cls.getAnnotations())
			if (annotation.annotationType().equals(WorkspaceScoped.class))
				return true;
		return false;
	}

	static {
		RECOGNIZED_SCOPES.add(NormalScope.class);
		RECOGNIZED_SCOPES.add(ApplicationScoped.class);
		RECOGNIZED_SCOPES.add(WorkspaceScoped.class);
	}
}
