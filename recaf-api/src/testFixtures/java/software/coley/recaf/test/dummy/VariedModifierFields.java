package software.coley.recaf.test.dummy;

/**
 * Dummy class to test existence of fields with varing modifiers.
 */
@SuppressWarnings("all")
public class VariedModifierFields {
	static int staticField = 0;
	volatile int volatileField;
	transient int transientField;
}
