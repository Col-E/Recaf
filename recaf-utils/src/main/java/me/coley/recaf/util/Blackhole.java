package me.coley.recaf.util;

/**
 * Fuck gradle shadowJar.
 * Why the hell can't you just be a normal alternative to maven's packager plugin????
 */
public class Blackhole {
	@SuppressWarnings("all")
	public static void consume(Object object) {
		// Its referenced, now please just include the damn thing ok?
	}
}
