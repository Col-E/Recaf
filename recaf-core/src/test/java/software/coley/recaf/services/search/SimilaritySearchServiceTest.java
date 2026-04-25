package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.services.search.similarity.MemberOrderMode;
import software.coley.recaf.services.search.similarity.ParameterMatchMode;
import software.coley.recaf.services.search.similarity.ReturnMatchMode;
import software.coley.recaf.services.search.similarity.SimilarClassSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarClassSearchResult;
import software.coley.recaf.services.search.similarity.SimilarClassSearchScope;
import software.coley.recaf.services.search.similarity.SimilarMethodSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarMethodSearchResult;
import software.coley.recaf.test.CompilerTestBase;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link SimilaritySearchService}.
 */
class SimilaritySearchServiceTest extends CompilerTestBase {
	private static SimilaritySearchService searchService;

	// TODO: When we have JASM support for Android classes we should fix the tests to also cover those.
	//  - From real samples it looks like the normalization works

	@BeforeAll
	static void setup() {
		searchService = recaf.get(SimilaritySearchService.class);
	}

	@Test
	void testIdentity() {
		setupWorkspace(
				"class A { void run1() { System.out.println(\"foo\"); } }",
				"class B { void run2() { System.out.print(\"bar\"); } }"
		);

		// Both methods are identical, so they should be 100% similar to each other.
		//   "But wait, they call different print methods!"
		// Yes, but we normalize instruction contents. Refer to 'MethodInstructionNormalizer' for details.
		searchSimilarMethods("A.run1()V", nearExactMethod(), results -> single(results).satisfies(result -> {
			assertThat(result.similarity()).isEqualTo(1.0);

			ClassMemberPathNode path = result.path();
			assertThat(path.getValue().getName()).isEqualTo("run2");
		}));
		searchSimilarMethods("B.run2()V", nearExactMethod(), results -> single(results).satisfies(result -> {
			assertThat(result.similarity()).isEqualTo(1.0);

			ClassMemberPathNode path = result.path();
			assertThat(path.getValue().getName()).isEqualTo("run1");
		}));

		// Same for class similarity.
		searchSimilarClasses("A", nearExactClass(), results -> {
			getResultCls(results, "B").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
		});
		searchSimilarClasses("B", nearExactClass(), results -> {
			getResultCls(results, "A").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
		});

		setupWorkspace(
				"class A { void run1() { if (cond()) System.out.println(\"foo\"); else System.out.print(\"fizz\");   } boolean cond() { return true; } }",
				"class B { void run2() { if (!cond()) System.out.print(\"bar\");  else System.out.println(\"buzz\"); } boolean cond() { return true; } }"
		);

		// Same idea as above, but in this case we have a simple if/else method.
		// The control flow graphs for each of these should be identical.
		// Structurally the methods are identical, but the condition is flipped.
		searchSimilarMethods("A.run1()V", nearExactMethod(), results -> single(results).satisfies(result -> {
			assertThat(result.similarity()).isEqualTo(1.0);

			ClassMemberPathNode path = result.path();
			assertThat(path.getValue().getName()).isEqualTo("run2");
		}));
		searchSimilarMethods("B.run2()V", nearExactMethod(), results -> single(results).satisfies(result -> {
			assertThat(result.similarity()).isEqualTo(1.0);

			ClassMemberPathNode path = result.path();
			assertThat(path.getValue().getName()).isEqualTo("run1");
		}));

		// Again
		searchSimilarClasses("A", nearExactClass(), results -> {
			getResultCls(results, "B").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
		});
		searchSimilarClasses("B", nearExactClass(), results -> {
			getResultCls(results, "A").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
		});
	}

	@Test
	void testBodyNoBody() {
		setupWorkspace(
				"abstract class A { void run1() { System.out.println(\"a\"); } }",
				"abstract class B { void run2() { System.out.println(\"b\"); } }",
				"abstract class C { abstract void run3(); }"
		);

		// For method searches, the searching a non-abstract won't yield the abstract as a match as it cannot have its content fingerprinted.
		searchSimilarMethods("A.run1()V", allMethods(), results -> {
			getResult(results, "run2").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
			getResultNullable(results, "run3").isNull();
		});

		// For class searches, the abstract method will penalize the method portion of the similarity.
		// However, the classes also have implicit constructors which are identical, so the overall similarity will still be fairly high.
		searchSimilarClasses("A", allClasses(), results -> {
			getResultCls(results, "B").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
			getResultCls(results, "C").satisfies(r -> assertThat(r.similarity()).isBetween(0.7, 0.71));
		});

		// If we eliminate the implicit constructor from the equation, then the similarity drops significantly
		// now that the only method in the class is the abstract one and thus cannot be fingerprinted/compared by content.
		setupWorkspace(
				"interface A { default void run1() { System.out.println(\"a\"); } }",
				"interface B { default void run2() { System.out.println(\"b\"); } }",
				"interface C { void run3(); }"
		);
		searchSimilarClasses("A", allClasses(), results -> {
			getResultCls(results, "B").satisfies(r -> assertThat(r.similarity()).isEqualTo(1.0));
			getResultCls(results, "C").satisfies(r -> assertThat(r.similarity()).isBetween(0.4, 0.41));
		});
	}

	@Test
	void testSimilarXorLoops() {
		// These methods have slightly different implementations of the same basic algorithm.
		// They should be very similar, but not identical.
		setupWorkspace("""
				class A {
				    String xor1(String input, int xor) {
				        char[] chars = new char[input.length()];
				        for (int i = 0; i < input.length(); i++)
				            chars[i] = (char) (input.charAt(i) ^ xor);
				        return String.valueOf(chars);
				    }
				}
				""", """
				class B {
					String xor2(String input, int xor1, int xor2) {
						char[] chars = new char[input.length()];
						for (int i = 0; i < input.length(); i++)
							chars[i] = (char) ((xor1 ^ xor2) ^ input.charAt(i));
						return String.valueOf(chars);
					}
				}
				""", """
				class C {
				 String xor3(String input, int xor1) {
				  char[] chars = new char[input.length()];
				  for (int i = 0; i < input.length(); i++)
				   chars[i] = (char) ((xor1 + "magic".hashCode()) ^ input.charAt(i));
				  return String.valueOf(chars);
				 }
				}
				""", """
				class D {
				 String xor4(String input, int xor) {
				  char[] chars = new char[input.length()];
				  while (true) {
				   int i = 0;
				   if (i >= input.length())
				    break;
				   chars[i] = (char) (xor ^ input.charAt(i));
				   i++;
				  }
				  return String.valueOf(chars);
				 }
				}
				"""
		);
		searchSimilarMethods("A.xor1(Ljava/lang/String;I)Ljava/lang/String;", allMethods(), results -> {
			getResult(results, "xor2").satisfies(r -> assertThat(r.similarity()).isGreaterThan(0.85));
			getResult(results, "xor3").satisfies(r -> assertThat(r.similarity()).isGreaterThan(0.85));
			getResult(results, "xor4").satisfies(r -> assertThat(r.similarity()).isGreaterThan(0.85));
		});
	}

	@Test
	void testNonSimilarStructures() {
		// These methods have different parameters, different control flows, different sizes, but the same return type.
		// Their scores will be very low, but still have a small amount in common.
		setupWorkspace("""
				class A {
				    String xor(String input, int xor) {
				        char[] chars = new char[input.length()];
				        for (int i = 0; i < input.length(); i++)
				            chars[i] = (char) (input.charAt(i) ^ xor);
				        return String.valueOf(chars);
				    }
				
				    String constant() {
				        return "constant";
				    }
				}
				"""
		);
		searchSimilarMethods("A.xor(Ljava/lang/String;I)Ljava/lang/String;", allMethods(), results -> {
			getResult(results, "constant").satisfies(r -> assertThat(r.similarity()).isBetween(0.29, 0.30));
		});
	}

	@Nonnull
	private static ObjectAssert<SimilarMethodSearchResult> single(@Nonnull List<SimilarMethodSearchResult> results) {
		return assertThat(results).singleElement();
	}

	private void searchSimilarMethods(@Nonnull String signature,
	                                  @Nonnull SimilarMethodSearchOptions options,
	                                  @Nonnull Consumer<List<SimilarMethodSearchResult>> resultConsumer) {
		// Break "owner.method()V" into parts.
		int ownerEnd = signature.indexOf('.');
		int nameEnd = signature.indexOf('(', ownerEnd);
		if (ownerEnd < 0 || nameEnd < 0)
			fail("Invalid method signature: " + signature);
		String owner = signature.substring(0, ownerEnd);
		String name = signature.substring(ownerEnd + 1, nameEnd);
		String desc = signature.substring(nameEnd);
		searchSimilarMethods(owner, name, desc, options, resultConsumer);
	}

	private void searchSimilarClasses(@Nonnull String type, @Nonnull SimilarClassSearchOptions options,
	                                  @Nonnull Consumer<List<SimilarClassSearchResult>> resultConsumer) {
		List<SimilarClassSearchResult> results = searchService.searchClasses(classPath(type), options);
		resultConsumer.accept(results);
	}

	private void searchSimilarMethods(@Nonnull String owner, @Nonnull String name, @Nonnull String desc,
	                                  @Nonnull SimilarMethodSearchOptions options,
	                                  @Nonnull Consumer<List<SimilarMethodSearchResult>> resultConsumer) {
		List<SimilarMethodSearchResult> results = searchService.searchMethods(memberPath(owner, name, desc), options);
		resultConsumer.accept(results);
	}

	@Nonnull
	private static ObjectAssert<SimilarMethodSearchResult> getResult(@Nonnull List<SimilarMethodSearchResult> results,
	                                                                 @Nonnull String methodName) {
		return getResultNullable(results, methodName).isNotNull();
	}

	@Nonnull
	private static ObjectAssert<SimilarMethodSearchResult> getResultNullable(@Nonnull List<SimilarMethodSearchResult> results,
	                                                                         @Nonnull String methodName) {
		for (SimilarMethodSearchResult result : results)
			if (result.path().getValue().getName().equals(methodName))
				return assertThat(result);
		return new ObjectAssert<>(null);
	}

	@Nonnull
	private static ObjectAssert<SimilarClassSearchResult> getResultCls(@Nonnull List<SimilarClassSearchResult> results,
	                                                                   @Nonnull String methodName) {
		return getResultNullableCls(results, methodName).isNotNull();
	}

	@Nonnull
	private static ObjectAssert<SimilarClassSearchResult> getResultNullableCls(@Nonnull List<SimilarClassSearchResult> results,
	                                                                           @Nonnull String type) {
		for (SimilarClassSearchResult result : results)
			if (result.path().getValue().getName().equals(type))
				return assertThat(result);
		return new ObjectAssert<>(null);
	}

	@Nonnull
	private static SimilarClassSearchOptions nearExactClass() {
		return new SimilarClassSearchOptions(
				95,
				ParameterMatchMode.EXACT_COUNT_AND_ORDER,
				ReturnMatchMode.EXACT_TYPE,
				MemberOrderMode.ORDERED,
				MemberOrderMode.ORDERED,
				SimilarClassSearchOptions.DEFAULT_METHOD_WEIGHT,
				SimilarClassSearchOptions.DEFAULT_FIELD_WEIGHT,
				SimilarClassSearchScope.selfResource()
		);
	}

	@Nonnull
	private static SimilarClassSearchOptions allClasses() {
		return new SimilarClassSearchOptions(
				0,
				ParameterMatchMode.ANYTHING,
				ReturnMatchMode.ANY_ASSIGNABLE,
				MemberOrderMode.ORDERED,
				MemberOrderMode.ORDERED,
				SimilarClassSearchOptions.DEFAULT_METHOD_WEIGHT,
				SimilarClassSearchOptions.DEFAULT_FIELD_WEIGHT,
				SimilarClassSearchScope.selfResource()
		);
	}

	@Nonnull
	private static SimilarMethodSearchOptions nearExactMethod() {
		return new SimilarMethodSearchOptions(
				95,
				ParameterMatchMode.EXACT_COUNT_AND_ORDER,
				ReturnMatchMode.EXACT_TYPE,
				true
		);
	}

	@Nonnull
	private static SimilarMethodSearchOptions allMethods() {
		return new SimilarMethodSearchOptions(
				0,
				ParameterMatchMode.ANYTHING,
				ReturnMatchMode.ANY_ASSIGNABLE,
				true
		);
	}

	private void setupWorkspace(String... sources) {
		clearClasses();
		for (String src : sources) {
			JavaSource parsed = source(src);
			compileFull(parsed.internalName(), parsed.contents());
		}
	}

	@Nonnull
	private static JavaSource source(@Nonnull String declaration) {
		String internalName = getClassName(declaration);
		return new JavaSource(internalName, declaration);
	}

	@Nonnull
	private static String getClassName(@Nonnull String declaration) {
		if (declaration.contains("class "))
			return declaration.lines()
					.filter(line -> line.contains("class "))
					.findFirst()
					.map(line -> line.substring(line.indexOf("class ") + 6).split("\\s")[0])
					.orElseThrow(() -> new IllegalArgumentException("Failed to extract class name from declaration:\n" + declaration))
					.replace('.', '/');
		if (declaration.contains("interface "))
			return declaration.lines()
					.filter(line -> line.contains("interface "))
					.findFirst()
					.map(line -> line.substring(line.indexOf("interface ") + 10).split("\\s")[0])
					.orElseThrow(() -> new IllegalArgumentException("Failed to extract class name from declaration:\n" + declaration))
					.replace('.', '/');
		if (declaration.contains("enum "))
			return declaration.lines()
					.filter(line -> line.contains("enum "))
					.findFirst()
					.map(line -> line.substring(line.indexOf("enum ") + 5).split("\\s")[0])
					.orElseThrow(() -> new IllegalArgumentException("Failed to extract class name from declaration:\n" + declaration))
					.replace('.', '/');
		return fail("Failed to extract class name from declaration:\n" + declaration);
	}

	private record JavaSource(@Nonnull String internalName, @Nonnull String declaration) {
		@Nonnull
		private String contents() {
			String packageName = packageName();
			if (packageName == null)
				return declaration;
			return "package " + packageName + ";\n\n" + declaration;
		}

		private String packageName() {
			int packageSeparator = internalName.lastIndexOf('/');
			if (packageSeparator < 0)
				return null;
			return internalName.substring(0, packageSeparator).replace('/', '.');
		}
	}
}
