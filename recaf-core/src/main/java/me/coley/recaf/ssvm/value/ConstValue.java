package me.coley.recaf.ssvm.value;

import dev.xdark.ssvm.value.Value;

/**
 * Common {@link Value} type used to check if the value holds a constant that can be folded.
 * <br>
 * Supported types include:
 * <ul>
 *     <li>Primitives <i>({@code int}/{@code long}/{@code float}/{@code double})</i></li>
 *     <li>{@code java.lang.String}</li>
 * </ul>
 *
 * @author Matt Coley
 * @see ConstNumericValue
 * @see ConstStringValue
 */
public interface ConstValue extends TrackedValue {
}
