package software.coley.recaf.test.dummy;

import jakarta.annotation.Nonnull;

public record DummyRecord(int foo, @Nonnull String bar) {
	@Nonnull
	public String fooPlus(int other) {
		return String.valueOf(foo + other);
	}
}
