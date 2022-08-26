package me.coley.recaf.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Various utilities pertaining to {@link StackTraceElement}.
 *
 * @author Matt Coley
 */
public class StackTraceUtil {
	/**
	 * @param ex
	 * 		Exception to cut off.
	 * @param classUsage
	 * 		Class usage to stop the exception at.
	 *
	 * @return Trace of the exception from the start going back up to the given class usage.
	 */
	public static StackTraceElement[] cutOffToUsage(Exception ex, Class<?> classUsage) {
		return cutOff(ex, element -> element.getClassName().equals(classUsage.getName()));
	}

	/**
	 * @param ex
	 * 		Exception to cut off.
	 * @param methodUsage
	 * 		Method usage to stop the exception at.
	 *
	 * @return Trace of the exception from the start going back up to the given method usage.
	 */
	public static StackTraceElement[] cutOffToUsage(Exception ex, Method methodUsage) {
		return cutOff(ex, element -> element.getMethodName().equals(methodUsage.getName()) &&
				element.getClassName().equals(methodUsage.getDeclaringClass().getName()));
	}

	/**
	 * @param ex
	 * 		Exception to cut off.
	 * @param filter
	 * 		Filter to determine where stop point is at.
	 *
	 * @return Trace of the exception from the start going back up to where the filter first matches.
	 */
	public static StackTraceElement[] cutOff(Exception ex, Predicate<StackTraceElement> filter) {
		StackTraceElement[] trace = ex.getStackTrace();
		int cutOff = 1;
		for (int i = 1; i < trace.length; i++) {
			StackTraceElement element = trace[i];
			if (filter.test(element))
				break;
			cutOff = i;
		}
		StackTraceElement[] range = Arrays.copyOfRange(trace, 0, cutOff);
		ex.setStackTrace(range);
		return range;
	}
}
