package software.coley.recaf.util.collect.primitive;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for our primitive collection implementations.
 */
class PrimitiveCollectionsTest {
	private static final int[] INT_COLLISION_KEYS = {0, 16, 32};

	/**
	 * Tests for {@link Int2IntMap}.
	 */
	@Nested
	class Int2IntMapTests {
		@Test
		void resizePreservesEntries() {
			Int2IntMap map = new Int2IntMap(2);
			for (int i = 0; i < 64; i++)
				assertEquals(-1, map.put(i, i * 2));

			assertEquals(64, map.size());
			for (int i = 0; i < 64; i++)
				assertEquals(i * 2, map.get(i));
		}

		@Test
		void removeMaintainsCollisionChain() {
			Int2IntMap map = new Int2IntMap(4);
			map.put(INT_COLLISION_KEYS[0], 10);
			map.put(INT_COLLISION_KEYS[1], 20);
			map.put(INT_COLLISION_KEYS[2], 30);

			assertEquals(10, map.remove(INT_COLLISION_KEYS[0]));
			assertEquals(20, map.get(INT_COLLISION_KEYS[1]));
			assertEquals(30, map.get(INT_COLLISION_KEYS[2]));
			assertEquals(2, map.size());

			assertEquals(20, map.remove(INT_COLLISION_KEYS[1]));
			assertEquals(30, map.get(INT_COLLISION_KEYS[2]));
			assertTrue(map.containsKey(INT_COLLISION_KEYS[2]));
			assertEquals(1, map.size());

			assertEquals(30, map.remove(INT_COLLISION_KEYS[2]));
			assertEquals(-1, map.get(INT_COLLISION_KEYS[2]));
			assertFalse(map.containsKey(INT_COLLISION_KEYS[2]));
			assertEquals(0, map.size());
		}

		@Test
		void supportsHelperMethods() {
			Int2IntMap map = new Int2IntMap(4);

			assertEquals(5, map.increment(7, 5));
			assertEquals(8, map.increment(7, 3));
			assertEquals(8, map.computeIfAbsent(7, key -> 99));
			assertEquals(11, map.computeIfAbsent(11, key -> key));
			assertEquals(123, map.getOrDefault(123, 123));

			Map<Integer, Integer> copy = new HashMap<>();
			map.forEach(copy::put);

			assertEquals(Map.of(
					7, 8,
					11, 11
			), copy);
			assertArrayEquals(new int[]{7, 11}, Arrays.stream(map.keys()).sorted().toArray());
		}

		@Test
		void equality() {
			Int2IntMap map1 = new Int2IntMap(4);
			Int2IntMap map2 = new Int2IntMap(4);

			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(1, 10);
			assertNotEquals(map1, map2);

			map2.put(1, 10);
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(16, 20);
			map1.put(32, 30);
			map2.put(16, 20);
			map2.put(32, 30);
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(16, 25);
			assertNotEquals(map1, map2);
		}
	}

	/**
	 * Tests for {@link Int2ObjectMap}.
	 */
	@Nested
	class Int2ObjectMapTests {
		@Test
		void resizePreservesEntries() {
			Int2ObjectMap<String> map = new Int2ObjectMap<>(2);
			for (int i = 0; i < 64; i++)
				assertNull(map.put(i, "value-" + i));

			assertEquals(64, map.size());
			for (int i = 0; i < 64; i++)
				assertEquals("value-" + i, map.get(i));
		}

		@Test
		void removeMaintainsCollisionChain() {
			Int2ObjectMap<String> map = new Int2ObjectMap<>(4);
			map.put(INT_COLLISION_KEYS[0], "a");
			map.put(INT_COLLISION_KEYS[1], "b");
			map.put(INT_COLLISION_KEYS[2], "c");

			assertEquals("a", map.remove(INT_COLLISION_KEYS[0]));
			assertEquals("b", map.get(INT_COLLISION_KEYS[1]));
			assertEquals("c", map.get(INT_COLLISION_KEYS[2]));
			assertEquals(2, map.size());

			assertEquals("b", map.remove(INT_COLLISION_KEYS[1]));
			assertEquals("c", map.get(INT_COLLISION_KEYS[2]));
			assertTrue(map.containsKey(INT_COLLISION_KEYS[2]));
			assertEquals(1, map.size());

			assertEquals("c", map.remove(INT_COLLISION_KEYS[2]));
			assertNull(map.get(INT_COLLISION_KEYS[2]));
			assertFalse(map.containsKey(INT_COLLISION_KEYS[2]));
			assertEquals(0, map.size());
		}

		@Test
		void supportsHelperMethods() {
			Int2ObjectMap<String> map = new Int2ObjectMap<>(4);

			assertEquals("value-5", map.computeIfAbsent(5, key -> "value-" + key));
			assertEquals("value-5", map.computeIfAbsent(5, key -> "other"));
			assertEquals("fallback", map.getOrDefault(10, "fallback"));

			Map<Integer, String> copy = new HashMap<>();
			map.put(8, "value-8");
			map.forEach(copy::put);

			assertEquals(Map.of(
					5, "value-5",
					8, "value-8"
			), copy);
			assertArrayEquals(new int[]{5, 8}, Arrays.stream(map.keys()).sorted().toArray());
		}

		@Test
		void equality() {
			Int2ObjectMap<String> map1 = new Int2ObjectMap<>(4);
			Int2ObjectMap<String> map2 = new Int2ObjectMap<>(4);

			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(1, "a");
			assertNotEquals(map1, map2);

			map2.put(1, "a");
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(16, "b");
			map1.put(32, "c");
			map2.put(16, "b");
			map2.put(32, "c");
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(16, "other");
			assertNotEquals(map1, map2);
		}
	}

	/**
	 * Tests for {@link Object2IntMap}.
	 */
	@Nested
	class Object2IntMapTests {
		@Test
		void resizePreservesEntries() {
			Object2IntMap<Key> map = new Object2IntMap<>(2);
			for (int i = 0; i < 64; i++)
				assertEquals(-1, map.put(new Key("key-" + i, i), i));

			assertEquals(64, map.size());
			for (int i = 0; i < 64; i++)
				assertEquals(i, map.get(new Key("key-" + i, i)));
		}

		@Test
		void removeMaintainsCollisionChain() {
			Object2IntMap<Key> map = new Object2IntMap<>(4);
			Key a = collidingKey("a");
			Key b = collidingKey("b");
			Key c = collidingKey("c");
			map.put(a, 10);
			map.put(b, 20);
			map.put(c, 30);

			assertEquals(10, map.remove(a));
			assertEquals(20, map.get(b));
			assertEquals(30, map.get(c));
			assertEquals(2, map.size());

			assertEquals(20, map.remove(b));
			assertEquals(30, map.get(c));
			assertTrue(map.containsKey(c));
			assertEquals(1, map.size());

			assertEquals(30, map.remove(c));
			assertEquals(-1, map.get(c));
			assertFalse(map.containsKey(c));
			assertEquals(0, map.size());
		}

		@Test
		void supportsHelperMethodsAndNullContract() {
			Object2IntMap<Key> map = new Object2IntMap<>(4);
			Key key = new Key("value", 123);

			assertEquals(5, map.computeIfAbsent(key, ignored -> 5));
			assertEquals(5, map.computeIfAbsent(key, ignored -> 9));
			assertEquals(22, map.getOrDefault(new Key("missing", 1), 22));

			Map<Key, Integer> copy = new HashMap<>();
			map.put(new Key("other", 2), 8);
			map.forEach(copy::put);

			assertEquals(2, copy.size());
			assertEquals(5, copy.get(key));
			assertEquals(8, copy.get(new Key("other", 2)));

			assertThrows(NullPointerException.class, () -> map.put(null, 1));
			assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, ignored -> 1));
			assertEquals(-1, map.get(null));
			assertEquals(11, map.getOrDefault(null, 11));
			assertEquals(-1, map.remove(null));
			assertFalse(map.containsKey(null));
		}

		@Test
		void equality() {
			Object2IntMap<Key> map1 = new Object2IntMap<>(4);
			Object2IntMap<Key> map2 = new Object2IntMap<>(4);

			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			Key key1 = new Key("a", 123);
			map1.put(key1, 10);
			assertNotEquals(map1, map2);

			map2.put(key1, 10);
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			Key key2 = new Key("b", 456);
			Key key3 = new Key("c", 789);
			map1.put(key2, 20);
			map1.put(key3, 30);
			map2.put(key2, 20);
			map2.put(key3, 30);
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(key2, 25);
			assertNotEquals(map1, map2);
		}
	}

	/**
	 * Tests for {@link Object2LongMap}.
	 */
	@Nested
	class Object2LongMapTests {
		@Test
		void resizePreservesEntries() {
			Object2LongMap<Key> map = new Object2LongMap<>(2);
			for (int i = 0; i < 64; i++)
				assertEquals(-1L, map.put(new Key("key-" + i, i), i * 10L));

			assertEquals(64, map.size());
			for (int i = 0; i < 64; i++)
				assertEquals(i * 10L, map.get(new Key("key-" + i, i)));
		}

		@Test
		void removeMaintainsCollisionChain() {
			Object2LongMap<Key> map = new Object2LongMap<>(4);
			Key a = collidingKey("a");
			Key b = collidingKey("b");
			Key c = collidingKey("c");
			map.put(a, 10L);
			map.put(b, 20L);
			map.put(c, 30L);

			assertEquals(10L, map.remove(a));
			assertEquals(20L, map.get(b));
			assertEquals(30L, map.get(c));
			assertEquals(2, map.size());

			assertEquals(20L, map.remove(b));
			assertEquals(30L, map.get(c));
			assertTrue(map.containsKey(c));
			assertEquals(1, map.size());

			assertEquals(30L, map.remove(c));
			assertEquals(-1L, map.get(c));
			assertFalse(map.containsKey(c));
			assertEquals(0, map.size());
		}

		@Test
		void supportsHelperMethodsAndNullContract() {
			Object2LongMap<Key> map = new Object2LongMap<>(4);
			Key key = new Key("value", 123);
			Key other = new Key("other", 456);
			map.put(key, 5L);
			map.put(other, 8L);

			Map<Key, Long> copy = new HashMap<>();
			map.forEach(copy::put);

			assertEquals(2, copy.size());
			assertEquals(5L, copy.get(key));
			assertEquals(8L, copy.get(other));
			assertEquals(22L, map.getOrDefault(new Key("missing", 1), 22L));

			assertThrows(NullPointerException.class, () -> map.put(null, 1L));
			assertEquals(-1L, map.get(null));
			assertEquals(11L, map.getOrDefault(null, 11L));
			assertEquals(-1L, map.remove(null));
			assertFalse(map.containsKey(null));
		}

		@Test
		void equality() {
			Object2LongMap<Key> map1 = new Object2LongMap<>(4);
			Object2LongMap<Key> map2 = new Object2LongMap<>(4);

			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			Key key1 = new Key("a", 123);
			map1.put(key1, 10L);
			assertNotEquals(map1, map2);

			map2.put(key1, 10L);
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			Key key2 = new Key("b", 456);
			Key key3 = new Key("c", 789);
			map1.put(key2, 20L);
			map1.put(key3, 30L);
			map2.put(key2, 20L);
			map2.put(key3, 30L);
			assertEquals(map1, map2);
			assertEquals(map1.hashCode(), map2.hashCode());

			map1.put(key2, 25L);
			assertNotEquals(map1, map2);
		}
	}

	@Nonnull
	private static Key collidingKey(@Nonnull String value) {
		return new Key(value, 0);
	}

	private record Key(@Nonnull String value, int hash) {
		@Override
		public int hashCode() {
			return hash;
		}
	}
}
