package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.mapping.matching.SimilarityMappingOptions;
import software.coley.recaf.services.mapping.matching.SimilarityMappingService;
import software.coley.recaf.services.mapping.matching.SimilarityMappingsReport;
import software.coley.recaf.services.search.SearchFeedback;
import software.coley.recaf.test.CompilerTestBase;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimilarityMappingService}.
 */
class SimilarityMappingServiceTest extends CompilerTestBase {
	private static final int DEFAULT_MAX_FULL_SCORE_CANDIDATES = 25;
	private static final double DEFAULT_SHORTLIST_GAP_THRESHOLD = 0.05;
	private static SimilarityMappingService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(SimilarityMappingService.class);
	}

	@Test
	void baseline1() {
		// Since we're extending the compiler test, any compiled classes will get put in the primary resource.
		// So for these tests we will create the 'clean' copy first, then create the 'obfuscated' copy after.
		// This will result in a workspace looking like:
		//   - Primary: a.class
		//   - Supporting: Demo.class
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					int count;
					void print() {
						System.out.println(this.count);
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					int b;
					void c() {
						System.out.println(this.b);
					}
				}
				""");

		// With fairly tight settings we should still see a full mapping of the class and its members.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 1, 95), SearchFeedback.DEFAULT);
		Mappings mappings = report.getMappings();
		assertThat(mappings.getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(mappings.getMappedFieldName("obf/a", "b", "I")).isEqualTo("count");
		assertThat(mappings.getMappedMethodName("obf/a", "c", "()V")).isEqualTo("print");
		assertThat(mappings.getMappedMethodName("obf/a", "<init>", "()V")).isNull(); // special methods not mapped
		assertThat(report.getAcceptedMatches()).hasSize(1);
		assertThat(report.getUnresolvedClasses()).isEmpty();
	}

	@Test
	void baseline2() {
		// Just a slightly more complex version of the baseline test above.
		compileFull("sample/Model", """
				package sample;
				class Model {
					int value;
				}
				""");
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					Model field;
					Model pass(Model input) {
						return input;
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/x", """
				package obf;
				class x {
					int a;
				}
				""");
		compileFull("obf/a", """
				package obf;
				class a {
					x b;
					x c(x input) {
						return input;
					}
				}
				""");

		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getMappings().getMappedClassName("obf/x")).isEqualTo("sample/Model");
		assertThat(report.getMappings().getMappedFieldName("obf/x", "a", "I")).isEqualTo("value");
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedFieldName("obf/a", "b", "Lobf/x;")).isEqualTo("field");
	}

	@Test
	void rejectsMatchesBelowClassThreshold() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					String text() {
						return "demo";
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					int value;
					void run() {
						for (int i = 0; i < 10; i++)
							value += i;
					}
				}
				""");

		// If the class similarity is lower than our threshold (95%) it shouldn't be mapped.
		// In this case the class has different field counts, and the single method is obviously different.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).isEmpty();
		assertThat(report.getUnresolvedClasses()).extracting(path -> path.getValue().getName()).containsExactly("obf/a");
		assertThat(report.getMappings().isEmpty()).isTrue();
	}

	@Test
	void rejectsAmbiguousRunnerUpMatches() {
		compileFull("sample/DemoOne", """
				package sample;
				class DemoOne {
					void print() {
						System.out.println("demo");
					}
				}
				""");
		compileFull("sample/DemoTwo", """
				package sample;
				class DemoTwo {
					void print() {
						System.out.println("demo");
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					void c() {
						System.out.println("demo");
					}
				}
				""");

		// If the top match is too close to the runner-up (less than the certainty gap) then it shouldn't be mapped.
		// In this case these classes are identical except for their names.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(0, 1, 0), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).isEmpty();
		assertThat(report.getUnresolvedClasses()).extracting(path -> path.getValue().getName()).containsExactly("obf/a");
		assertThat(report.getMappings().isEmpty()).isTrue();
	}

	@Test
	void rejectsSortaSimilarWhenAnotherSimilarMethodAlsoExists() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					int count;
					void print() {
						System.out.println(this.count);
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					int b;
					void c() {
						System.out.println(this.b);
					}
					void d() {
						System.out.println(this.b + 1);
					}
				}
				""");

		// The class, field, and first method should be mapped.
		// The second method in the obf version is less similar so it should get dropped as a candidate of 'print'.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(0, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).hasSize(1);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedFieldName("obf/a", "b", "I")).isEqualTo("count");
		assertThat(report.getMappings().getMappedMethodName("obf/a", "c", "()V")).isEqualTo("print");
		assertThat(report.getMappings().getMappedMethodName("obf/a", "d", "()V")).isNull();
	}

	@Test
	void resolvesManyToOneConflictsDeterministically() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					void print() {
						System.out.println("demo");
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					void c() {
						System.out.println("demo");
					}
				}
				""");
		compileFull("obf/b", """
				package obf;
				class b {
					void c() {
						System.out.println("demo");
					}
				}
				""");

		// If there are multiple candidates for a single class and they are identical in similarity
		// then only one of them should be accepted as a match, and the others should be rejected as unresolved.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(0, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).hasSize(1);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedClassName("obf/b")).isNull();
		assertThat(report.getUnresolvedClasses()).extracting(path -> path.getValue().getName()).containsExactly("obf/b");
	}

	@Test
	void memberThresholdFiltersDirectMemberMappings() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					int count;
					void print() {
						while (System.nanoTime() == 0L) {
							System.out.println("spin");
						}
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					int b;
					void c() {
						System.out.println(this.b);
					}
				}
				""");

		// If the member similarity is lower than our threshold (95%) then the class should still be mapped, but not its members.
		// In this case the field is similar but the method is very different.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(0, 0, 100), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).hasSize(1);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedFieldName("obf/a", "b", "I")).isEqualTo("count");
		assertThat(report.getMappings().getMappedMethodName("obf/a", "c", "()V")).isNull();
	}

	@Test
	void doesNotMapMethodsAlreadyInLibraryFamilies() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					String getSerializedName() {
						return "demo";
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					public String toString() {
						return "demo";
					}
				}
				""");

		// Even if the method is similar, it shouldn't be mapped away from the standard contract "String toString()"
		// as this tends to break things more often than it helps.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedMethodName("obf/a", "toString", "()Ljava/lang/String;")).isNull();
	}

	@Test
	void doesNotMapMethodsToLibraryMethodNames() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					public String toString() {
						return "demo";
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					String c() {
						return "demo";
					}
				}
				""");

		// Even if the method is similar, it shouldn't be mapped to a library methods like "String toString()"
		// as this tends to break things more often than it helps.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedMethodName("obf/a", "c", "()Ljava/lang/String;")).isNull();
	}

	@Test
	void doesMapFromLibraryNameIfSignatureIsNotMatching() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					String toString(Integer value) {
						return "the value is " + value;
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					String c(Integer value) {
						return "the value is " + value;
					}
				}
				""");

		// Unlike the prior test, this method isn't going to violate the standard 'Object#toString'
		// contract since it has a different signature (it takes a parameter).
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Demo");
		assertThat(report.getMappings().getMappedMethodName("obf/a", "c", "(Ljava/lang/Integer;)Ljava/lang/String;")).isEqualTo("toString");
	}

	@Test
	void handlesSameNamedClassesAcrossResources() {
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					int count;
					void print() {
						System.out.println(this.count);
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("sample/Demo", """
				package sample;
				class Demo {
					int a;
					void b() {
						System.out.println(this.a);
					}
				}
				""");

		// Even if the class names are the same, we should still be able to map the members correctly.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(95, 1, 95), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).hasSize(1);
		assertThat(report.getMappings().getMappedClassName("sample/Demo")).isNull();
		assertThat(report.getMappings().getMappedFieldName("sample/Demo", "a", "I")).isEqualTo("count");
		assertThat(report.getMappings().getMappedMethodName("sample/Demo", "b", "()V")).isEqualTo("print");
	}

	@Test
	void considersAdjacentCountBucketsWhenExactBucketExists() {
		compileFull("sample/Correct", """
				package sample;
				class Correct {
					int count;
					void print() {
						System.out.println(this.count);
					}
					void extra() {
						System.out.println(this.count + 1);
					}
				}
				""");
		compileFull("sample/Wrong", """
				package sample;
				class Wrong {
					String value;
					String alpha(int i) {
						return value + i;
					}
					int beta(String s) {
						return s.length();
					}
					long gamma(long v) {
						return v + 2L;
					}
				}
				""");
		WorkspaceResource targetResource = movePrimaryClassesToSupportingResource();
		compileFull("obf/a", """
				package obf;
				class a {
					int b;
					void c() {
						System.out.println(this.b);
					}
					void d() {
						System.out.println(this.b + 1);
					}
					void e() {
						System.out.println(this.b + 2);
					}
				}
				""");

		// Even though 'Wrong' is more similar to 'a' than 'Correct' is with method counts,
		// the 'Correct' class should still be chosen as the mapping since it still exists
		// in an adjacent count bucket and has a much higher similarity score for methods.
		Workspace workspace = workspaceManager.getCurrent();
		SimilarityMappingsReport report = service.analyze(workspace, workspace.getPrimaryResource(), targetResource,
				options(0, 0, 0), SearchFeedback.DEFAULT);
		assertThat(report.getAcceptedMatches()).hasSize(1);
		assertThat(report.getMappings().getMappedClassName("obf/a")).isEqualTo("sample/Correct");
	}

	/**
	 * Take the classes currently in the workspace's primary resource and move them to a new supporting resource.
	 *
	 * @return New supporting resource containing the primary classes.
	 */
	@Nonnull
	private WorkspaceResource movePrimaryClassesToSupportingResource() {
		Workspace workspace = workspaceManager.getCurrent();
		JvmClassBundle primaryBundle = workspace.getPrimaryResource().getJvmClassBundle();
		BasicJvmClassBundle supportingBundle = new BasicJvmClassBundle();
		for (JvmClassInfo classInfo : primaryBundle.valuesAsCopy())
			supportingBundle.initialPut(new JvmClassInfoBuilder(classInfo.getBytecode()).build());
		WorkspaceResource resource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(supportingBundle)
				.build();
		workspace.addSupportingResource(resource);
		primaryBundle.clear();
		return resource;
	}

	@Nonnull
	private static SimilarityMappingOptions options(int classThreshold, int certaintyGap, int memberThreshold) {
		return new SimilarityMappingOptions(
				classThreshold,
				certaintyGap,
				memberThreshold,
				10, // We never have more than 1 or 2 classes in these tests.
				50 // Since we only have a few classes we don't care about having a tight similarity gap for aggressive reduction.
		);
	}
}
