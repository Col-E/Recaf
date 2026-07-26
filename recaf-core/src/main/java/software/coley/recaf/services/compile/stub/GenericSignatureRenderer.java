package software.coley.recaf.services.compile.stub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple signature parser/renderer good enough to cover most common cases needed for {@link ClassStubGenerator}.
 *
 * @author Matt coley
 */
public class GenericSignatureRenderer {
	private GenericSignatureRenderer() {}

	@Nonnull
	public static List<String> renderInterfaces(@Nullable String signature) {
		if (signature == null || signature.isEmpty())
			return Collections.emptyList();
		try {
			Cursor cursor = new Cursor(signature);

			// Read class type parameters
			if (cursor.peek('<'))
				cursor.skipTypeParameters();

			// Read superclass
			cursor.readType();

			// Read interfaces
			List<String> result = new ArrayList<>();
			while (!cursor.done())
				result.add(cursor.readType());

			return result;
		} catch (Exception ex) {
			return Collections.emptyList();
		}
	}

	private static final class Cursor {
		private final String value;
		private int index;

		private Cursor(@Nonnull String value) {
			this.value = value;
		}

		private boolean done() {
			return index >= value.length();
		}

		private boolean peek(char c) {
			return !done() && value.charAt(index) == c;
		}

		private void skipTypeParameters() {
			// Continue until we find the matching closing '>' for the opening '<' that was just read.
			int depth = 0;
			do {
				char c = value.charAt(index++);
				if (c == '<') depth++;
				else if (c == '>') depth--;
			} while (depth > 0 && !done());

			// Fail if the signature is unbalanced.
			if (depth != 0)
				throw new IllegalArgumentException();
		}

		@Nonnull
		private String readType() {
			// Abort if reading beyond bounds, or if the type is not a class type.
			if (done() || value.charAt(index++) != 'L')
				throw new IllegalArgumentException();

			// Get the start index of the class name, and continue until we find the end of the class name.
			int start = index;
			while (!done() && value.charAt(index) != ';' && value.charAt(index) != '<')
				index++;
			if (start == index)
				throw new IllegalArgumentException();

			// Read the class name, replacing '/' and '$' with '.' to match Java's package/class naming convention.
			String name = value.substring(start, index).replace('/', '.').replace('$', '.');

			// If the next character is a '<', then we have type parameters to read.
			if (peek('<')) {
				index++;

				// Read the type parameters. Stop at next '>'.
				List<String> arguments = new ArrayList<>();
				while (!done() && !peek('>'))
					arguments.add(readArgument());
				if (done())
					throw new IllegalArgumentException();

				index++;

				// Append the type parameters to the class name.
				name += "<" + String.join(", ", arguments) + ">";
			}

			// Abort if reading beyond bounds, or if the type is not terminated with a ';'.
			if (done() || value.charAt(index++) != ';')
				throw new IllegalArgumentException();

			// Otherwise we're done and have reconstructed the source form of the signature.
			return name;
		}

		@Nonnull
		private String readArgument() {
			// Abort if reading beyond bounds.
			if (done())
				throw new IllegalArgumentException();

			// Read the next character to determine the type of argument.
			char c = value.charAt(index);
			if (c == '+') {
				index++;
				return "? extends " + readType();
			} else if (c == '-') {
				index++;
				return "? super " + readType();
			} else if (c == '*') {
				index++;
				return "?";
			} else {
				return readType();
			}
		}
	}
}
