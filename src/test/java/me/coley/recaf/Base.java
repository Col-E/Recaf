package me.coley.recaf;

import org.junit.jupiter.api.BeforeAll;

public class Base {
	@BeforeAll
	public static void setup() {
		Recaf.setupLogging();
	}
}
