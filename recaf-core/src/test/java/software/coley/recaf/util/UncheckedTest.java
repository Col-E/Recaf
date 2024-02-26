package software.coley.recaf.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Unchecked}.
 */
public class UncheckedTest {
	@Test
	void testBoxing() {
		UncheckedRunnable r = () -> {};
		assertSame(r, Unchecked.runnable(r));

		UncheckedSupplier<?> s = () -> null;
		assertSame(s, Unchecked.supply(s));

		UncheckedConsumer<?> c = (p) -> {};
		assertSame(c, Unchecked.consumer(c));

		UncheckedBiConsumer<?, ?> bc = (p1, p2) -> {};
		assertSame(bc, Unchecked.bconsumer(bc));

		UncheckedFunction<?, ?> f = (p) -> null;
		assertSame(f, Unchecked.function(f));

		UncheckedBiFunction<?, ?, ?> bf = (p1, p2) -> null;
		assertSame(bf, Unchecked.bfunction(bf));
	}

	@Test
	void testValidCast() {
		List<String> list = new ArrayList<>();
		list.add("foo");
		ArrayList<String> castList = Unchecked.cast(list);
		assertSame(list, castList);
	}

	@Test
	void testInvalidCast() {
		List<String> list = new ArrayList<>();
		list.add("foo");
		assertThrows(ClassCastException.class, () -> {
			Set<String> set = Unchecked.cast(list);
			fail("Shouldn't be castable: " + set);
		});
	}

	@Test
	void testRun() {
		int zero = 0;
		assertThrows(ArithmeticException.class, () ->
				Unchecked.run(() -> System.out.println(10 / zero))
		);
	}

	@Test
	void testGet() {
		int zero = 0;
		assertThrows(ArithmeticException.class, () ->
				Unchecked.get(() -> (10 / zero))
		);
	}

	@Test
	void testGetOr() {
		int zero = 0;
		int value = assertDoesNotThrow(() ->
				Unchecked.getOr(() -> (10 / zero), 7)
		);
		assertEquals(7, value);
	}

	@Test
	void testAccept() {
		assertThrows(ArithmeticException.class, () ->
				Unchecked.accept(v -> System.out.println(10 / v), 0)
		);
	}

	@Test
	void testBAccept() {
		assertThrows(ArithmeticException.class, () ->
				Unchecked.baccept((a, b) -> System.out.println(a / b), 10, 0)
		);
	}

	@Test
	void testMap() {
		assertThrows(ArithmeticException.class, () ->
				Unchecked.map(v -> 10 / v, 0)
		);
	}

	@Test
	void testBMap() {
		assertThrows(ArithmeticException.class, () ->
				Unchecked.bmap((a, b) -> a / b, 10, 0)
		);
	}
}
