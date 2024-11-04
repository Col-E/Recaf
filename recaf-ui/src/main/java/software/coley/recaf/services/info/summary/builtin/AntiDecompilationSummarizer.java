package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.services.mapping.gen.filter.IncludeKeywordNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonAsciiNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeWhitespaceNameFilter;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.pane.MappingGeneratorPane;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.visitors.DuplicateAnnotationRemovingVisitor;
import software.coley.recaf.util.visitors.IllegalAnnotationRemovingVisitor;
import software.coley.recaf.util.visitors.IllegalSignatureRemovingVisitor;
import software.coley.recaf.util.visitors.LongAnnotationRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Summarizer that allows patching of common anti-dcompilation tricks.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AntiDecompilationSummarizer implements ResourceSummarizer {
	private static final int LONG_ANNO = 256;
	private static final int BUTTON_WIDTH = 210;
	private static final NameGeneratorFilter ILLEGAL_NAME_FILTER =
			new IncludeWhitespaceNameFilter(new IncludeNonAsciiNameFilter(new IncludeKeywordNameFilter(null)));
	private static final Logger logger = Logging.get(AntiDecompilationSummarizer.class);
	private final Instance<MappingGeneratorPane> generatorPaneProvider;
	private final WindowFactory windowFactory;

	@Inject
	public AntiDecompilationSummarizer(@Nonnull Instance<MappingGeneratorPane> generatorPaneProvider,
	                                   @Nonnull WindowFactory windowFactory) {
		this.generatorPaneProvider = generatorPaneProvider;
		this.windowFactory = windowFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		Set<JvmClassInfo> classesWithInvalidSignatures = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<JvmClassInfo> classesWithDuplicateAnnotations = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<JvmClassInfo> classesWithIllegalNames = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<JvmClassInfo, Set<String>> classesWithLongAnnotations = new IdentityHashMap<>();
		Set<JvmClassInfo> classesWithIllegalAnnos = Collections.newSetFromMap(new IdentityHashMap<>());
		resource.jvmClassBundleStream().forEach(bundle -> {
			bundle.forEach(cls -> {
				// Check for invalid signatures in the class.
				if (!cls.hasValidSignatures())
					classesWithInvalidSignatures.add(cls);

				// Check for duplicate annotations, which is not allowed at source level.
				// Commonly paired with bogus long annotation names.
				duplicates:
				{
					List<AnnotationInfo> annotations = cls.getAnnotations();
					Set<String> uniqueNames = annotations.stream()
							.map(AnnotationInfo::getDescriptor)
							.collect(Collectors.toSet());
					if (annotations.size() != uniqueNames.size()) {
						classesWithDuplicateAnnotations.add(cls);
						break duplicates;
					}

					for (FieldMember field : cls.getFields()) {
						annotations = field.getAnnotations();
						uniqueNames = annotations.stream()
								.map(AnnotationInfo::getDescriptor)
								.collect(Collectors.toSet());
						if (annotations.size() != uniqueNames.size()) {
							classesWithDuplicateAnnotations.add(cls);
							break duplicates;
						}
					}

					for (MethodMember method : cls.getMethods()) {
						annotations = method.getAnnotations();
						uniqueNames = annotations.stream()
								.map(AnnotationInfo::getDescriptor)
								.collect(Collectors.toSet());
						if (annotations.size() != uniqueNames.size()) {
							classesWithDuplicateAnnotations.add(cls);
							break duplicates;
						}
					}
				}

				// Check for annotation names with obnoxiously long names. These are generally added to classes
				// to slow down decompilers without any hit to runtime performance since they go unused.
				bogusLong:
				{
					for (AnnotationInfo annotation : cls.getAnnotations()) {
						String descriptor = annotation.getDescriptor();
						if (descriptor.length() > LONG_ANNO) {
							classesWithLongAnnotations.computeIfAbsent(cls, c -> new HashSet<>()).add(descriptor);
							break bogusLong;
						}
					}

					for (FieldMember field : cls.getFields()) {
						for (AnnotationInfo annotation : field.getAnnotations()) {
							String descriptor = annotation.getDescriptor();
							if (descriptor.length() > LONG_ANNO) {
								classesWithLongAnnotations.computeIfAbsent(cls, c -> new HashSet<>()).add(descriptor);
								break bogusLong;
							}
						}
					}

					for (MethodMember method : cls.getMethods()) {
						for (AnnotationInfo annotation : method.getAnnotations()) {
							String descriptor = annotation.getDescriptor();
							if (descriptor.length() > LONG_ANNO) {
								classesWithLongAnnotations.computeIfAbsent(cls, c -> new HashSet<>()).add(descriptor);
								break bogusLong;
							}
						}
					}
				}

				// Check for annotation names with empty names. These are used to attempt triggering OOBE errors
				// in analysis and editing features.
				bogusAnnoName:
				{
					for (AnnotationInfo annotation : cls.getAnnotations()) {
						String descriptor = annotation.getDescriptor();
						if (!Types.isValidDesc(descriptor)) {
							classesWithIllegalAnnos.add(cls);
							break bogusAnnoName;
						}
					}

					for (FieldMember field : cls.getFields()) {
						for (AnnotationInfo annotation : field.getAnnotations()) {
							String descriptor = annotation.getDescriptor();
							if (!Types.isValidDesc(descriptor)) {
								classesWithIllegalAnnos.add(cls);
								break bogusAnnoName;
							}
						}
					}

					for (MethodMember method : cls.getMethods()) {
						for (AnnotationInfo annotation : method.getAnnotations()) {
							String descriptor = annotation.getDescriptor();
							if (!Types.isValidDesc(descriptor)) {
								classesWithIllegalAnnos.add(cls);
								break bogusAnnoName;
							}
						}
					}
				}

				// Check for illegally declared names.
				illegalNames:
				{
					if (ILLEGAL_NAME_FILTER.shouldMapClass(cls)) {
						classesWithIllegalNames.add(cls);
						break illegalNames;
					}
					for (FieldMember field : cls.getFields()) {
						if (ILLEGAL_NAME_FILTER.shouldMapField(cls, field)) {
							classesWithIllegalNames.add(cls);
							break illegalNames;
						}
					}
					for (MethodMember method : cls.getMethods()) {
						if (ILLEGAL_NAME_FILTER.shouldMapMethod(cls, method)) {
							classesWithIllegalNames.add(cls);
							break illegalNames;
						}
					}
				}
			});
		});

		Set<JvmClassInfo> classesWithCyclicInheritance = Collections.newSetFromMap(new IdentityHashMap<>());
		Graph graph = new Graph(workspace);
		graph.handleClassesWithCycles(classesWithCyclicInheritance::add);

		// Add entries for automatic cleaning of found issues.
		int cycleCount = classesWithCyclicInheritance.size();
		int invalidSigCount = classesWithInvalidSignatures.size();
		int dupAnnoCount = classesWithDuplicateAnnotations.size();
		int longAnnoCount = classesWithLongAnnotations.size();
		int illegalAnnoCount = classesWithIllegalAnnos.size();
		int illegalNameCount = classesWithIllegalNames.size();
		if (cycleCount > 0 ||
				invalidSigCount > 0 ||
				dupAnnoCount > 0 ||
				longAnnoCount > 0 ||
				illegalAnnoCount > 0 ||
				illegalNameCount > 0) {
			ExecutorService service = ThreadPoolFactory.newSingleThreadExecutor("anti-decompile-patching");
			Label title = new BoundLabel(Lang.getBinding("service.analysis.anti-decompile"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);

			// Option to remove cycles
			if (cycleCount > 0) {
				BoundLabel label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-remove", cycleCount));
				Button action = new ActionButton(CarbonIcons.TRASH_CAN, Lang.getBinding("service.analysis.anti-decompile.cyclic"), () -> {
					CompletableFuture.supplyAsync(() -> {
						int patched = 0;
						for (JvmClassInfo classInfo : classesWithCyclicInheritance) {
							ClassPathNode path = workspace.findClass(classInfo.getName());
							if (path != null) {
								Bundle<?> bundle = path.getValueOfType(Bundle.class);
								if (bundle != null) {
									bundle.remove(classInfo.getName());
									patched++;
								}
							}
						}
						return patched;
					}, service).whenCompleteAsync((count, error) -> {
						if (error == null) {
							label.rebind(Lang.format("service.analysis.anti-decompile.label-remove", cycleCount - count));
							logger.info("Removed {} illegal cyclic classes", count);
						} else {
							logger.error("Failed removing cyclic classes", error);
						}
					}, FxThreadUtil.executor());
				}).once().width(BUTTON_WIDTH);
				consumer.appendSummary(box(action, label));
			}

			// Option to remove invalid signatures
			if (invalidSigCount > 0) {
				BoundLabel label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", invalidSigCount));
				Button action = new ActionButton(CarbonIcons.CLEAN, Lang.getBinding("service.analysis.anti-decompile.illegal-sig"), () -> {
					CompletableFuture.supplyAsync(() -> {
						int patched = 0;
						for (JvmClassInfo classInfo : classesWithInvalidSignatures) {
							ClassPathNode path = workspace.findClass(classInfo.getName());
							if (path != null) {
								var bundle = path.getValueOfType(ClassBundle.class);
								if (bundle != null) {
									// Patch class to remove illegal signatures.
									ClassWriter writer = new ClassWriter(0);
									classInfo.getClassReader().accept(new IllegalSignatureRemovingVisitor(writer), 0);
									JvmClassInfo patchedInfo = classInfo.toJvmClassBuilder().withBytecode(writer.toByteArray()).build();

									// Replace class
									bundle.put(patchedInfo);
									patched++;
								}
							}
						}
						return patched;
					}, service).whenCompleteAsync((count, error) -> {
						if (error == null) {
							label.rebind(Lang.format("service.analysis.anti-decompile.label-patch", invalidSigCount - count));
							logger.info("Patched {} classes with illegal signatures", count);
						} else {
							logger.error("Failed patching illegal signatures", error);
						}
					}, FxThreadUtil.executor());

				}).once().width(BUTTON_WIDTH);
				consumer.appendSummary(box(action, label));
			}

			// Option to remove duplicate annotations
			if (dupAnnoCount > 0) {
				BoundLabel label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", dupAnnoCount));
				Button action = new ActionButton(CarbonIcons.CLEAN, Lang.getBinding("service.analysis.anti-decompile.duplicate-annos"), () -> {
					CompletableFuture.supplyAsync(() -> {
						int patched = 0;
						for (JvmClassInfo classInfo : classesWithDuplicateAnnotations) {
							ClassPathNode path = workspace.findClass(classInfo.getName());
							if (path != null) {
								var bundle = path.getValueOfType(ClassBundle.class);
								if (bundle != null) {
									// Patch class to remove duplicate annotations.
									ClassWriter writer = new ClassWriter(0);
									classInfo.getClassReader().accept(new DuplicateAnnotationRemovingVisitor(writer), 0);
									JvmClassInfo patchedInfo = classInfo.toJvmClassBuilder().withBytecode(writer.toByteArray()).build();

									// Replace class
									bundle.put(patchedInfo);
									patched++;
								}
							}
						}
						return patched;
					}, service).whenCompleteAsync((count, error) -> {
						if (error == null) {
							label.rebind(Lang.format("service.analysis.anti-decompile.label-patch", dupAnnoCount - count));
							logger.info("Patched {} classes with duplicate annotations", count);
						} else {
							logger.error("Failed patching classes with duplicate annotations", error);
						}
					}, FxThreadUtil.executor());
				}).once().width(BUTTON_WIDTH);
				consumer.appendSummary(box(action, label));
			}

			// Option to remove long named annotations
			if (longAnnoCount > 0) {
				BoundLabel label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", longAnnoCount));
				Button action = new ActionButton(CarbonIcons.CLEAN, Lang.getBinding("service.analysis.anti-decompile.long-annos"), () -> {
					CompletableFuture.supplyAsync(() -> {
						int patched = 0;
						for (JvmClassInfo classInfo : classesWithLongAnnotations.keySet()) {
							ClassPathNode path = workspace.findClass(classInfo.getName());
							if (path != null) {
								var bundle = path.getValueOfType(ClassBundle.class);
								if (bundle != null) {
									// Patch class to remove long annotations.
									ClassWriter writer = new ClassWriter(0);
									classInfo.getClassReader().accept(new LongAnnotationRemovingVisitor(writer, LONG_ANNO), 0);
									JvmClassInfo patchedInfo = classInfo.toJvmClassBuilder().withBytecode(writer.toByteArray()).build();

									// Replace class
									bundle.put(patchedInfo);
									patched++;
								}
							}
						}
						return patched;
					}, service).whenCompleteAsync((count, error) -> {
						if (error == null) {
							label.rebind(Lang.format("service.analysis.anti-decompile.label-patch", longAnnoCount - count));
							logger.info("Patched {} classes with long annotations", count);
						} else {
							logger.error("Failed patching classes with long annotations", error);
						}
					}, FxThreadUtil.executor());
				}).once().width(BUTTON_WIDTH);
				consumer.appendSummary(box(action, label));
			}

			// Option to remove empty named annotations
			if (illegalAnnoCount > 0) {
				BoundLabel label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", illegalAnnoCount));
				Button action = new ActionButton(CarbonIcons.CLEAN, Lang.getBinding("service.analysis.anti-decompile.illegal-annos"), () -> {
					CompletableFuture.supplyAsync(() -> {
						int patched = 0;
						for (JvmClassInfo classInfo : classesWithIllegalAnnos) {
							ClassPathNode path = workspace.findClass(classInfo.getName());
							if (path != null) {
								var bundle = path.getValueOfType(ClassBundle.class);
								if (bundle != null) {
									// Patch class to remove illegal annotations.
									ClassWriter writer = new ClassWriter(0);
									classInfo.getClassReader().accept(new IllegalAnnotationRemovingVisitor(writer), 0);
									JvmClassInfo patchedInfo = classInfo.toJvmClassBuilder().withBytecode(writer.toByteArray()).build();

									// Replace class
									bundle.put(patchedInfo);
									patched++;
								}
							}
						}
						return patched;
					}, service).whenCompleteAsync((count, error) -> {
						if (error == null) {
							label.rebind(Lang.format("service.analysis.anti-decompile.label-patch", illegalAnnoCount - count));
							logger.info("Patched {} classes with illegal annotations", count);
						} else {
							logger.error("Failed patching classes with illegal annotations", error);
						}
					}, FxThreadUtil.executor());
				}).once().width(BUTTON_WIDTH);
				consumer.appendSummary(box(action, label));
			}

			// Option to open mapping generator
			if (illegalNameCount > 0) {
				Button action = new ActionButton(CarbonIcons.LICENSE_MAINTENANCE, Lang.getBinding("service.analysis.anti-decompile.illegal-name"), () -> {
					CompletableFuture.runAsync(() -> {
						MappingGeneratorPane mappingGeneratorPane = generatorPaneProvider.get();
						mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeNonAsciiNames());
						mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeKeywordNames());
						mappingGeneratorPane.addConfiguredFilter(new MappingGeneratorPane.IncludeWhitespaceNames());
						mappingGeneratorPane.generate();
						RecafScene scene = new RecafScene(mappingGeneratorPane);
						FxThreadUtil.run(() -> {
							Stage window = windowFactory.createAnonymousStage(scene, getBinding("mapgen"), 800, 400);
							window.show();
							window.requestFocus();

							// Because our service is application scoped, the injected mapping generator panes won't
							// be automatically destroyed until all of Recaf is closed. Thus, for optimal GC usage we
							// need to manually invoke the destruction of our injected mapping generator panes.
							// We can do this when the stage is closed.
							window.setOnHidden(e -> generatorPaneProvider.destroy(mappingGeneratorPane));
						});
					}, service).exceptionally(t -> {
						logger.error("Failed to open mapping viewer", t);
						return null;
					});
				}).width(BUTTON_WIDTH);
				Label label = new BoundLabel(Lang.format("service.analysis.anti-decompile.label-patch", illegalNameCount));
				consumer.appendSummary(box(action, label));
			}

			return true;
		}

		return false;
	}

	@Nonnull
	private static Node box(@Nonnull Node left, @Nonnull Node right) {
		HBox box = new HBox(left, right);
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_LEFT);
		return box;
	}


	/**
	 * Simple class hierarchy graph for detecting cycles.
	 */
	private static class Graph {
		private final Map<JvmClassInfo, ClassVertex> vertices;

		/**
		 * @param workspace
		 * 		Workspace to pull from.
		 */
		public Graph(@Nonnull Workspace workspace) {
			// Initiate graph vertices.
			vertices = workspace.getPrimaryResource().jvmClassBundleStream()
					.flatMap(Bundle::stream)
					.collect(Collectors.toMap(Function.identity(), ClassVertex::new, (a, b) -> a, IdentityHashMap::new));

			// Link edges together.
			vertices.forEach((key, vertex) -> {
				String parent = key.getSuperName();
				addEdge(workspace, vertex, parent);
				for (String parentInterface : key.getInterfaces())
					addEdge(workspace, vertex, parentInterface);
			});
		}

		/**
		 * @param workspace
		 * 		Containing workspace for class lookup.
		 * @param vertex
		 * 		Vertex of child type.
		 * @param parentName
		 * 		Name of parent type.
		 */
		private void addEdge(@Nonnull Workspace workspace, @Nonnull ClassVertex vertex, @Nullable String parentName) {
			// No parent name, skip.
			if (parentName == null) return;

			// Check if vertex already has relation, if so skip.
			if (vertex.hasParent(parentName)) return;

			// Check if class is known to workspace, skip if not.
			ClassPathNode parentPath = workspace.findJvmClass(parentName);
			if (parentPath == null) return;

			// Get the vertex from the associated class, shouldn't be skipped.
			JvmClassInfo parentClass = parentPath.getValue().asJvmClass();
			ClassVertex parentVertex = vertices.get(parentClass);
			if (parentVertex == null) return;

			// Add the child -> parent edge.
			vertex.addParent(parentVertex);
		}

		/**
		 * @param cyclicConsumer
		 * 		Consumer to act on classes with cyclic inheritance.
		 */
		public void handleClassesWithCycles(@Nonnull Consumer<JvmClassInfo> cyclicConsumer) {
			vertices.values().forEach(ClassVertex::resetVisited);
			for (ClassVertex vertex : vertices.values()) {
				if (vertex.isNotVisited() && hasCycle(vertex)) {
					cyclicConsumer.accept(vertex.getClassValue());
				}
			}
		}

		/**
		 * @param sourceVertex
		 * 		Vertex to check.
		 *
		 * @return {@code true} if it has cycles.
		 */
		private boolean hasCycle(@Nonnull ClassVertex sourceVertex) {
			// Check if prior iteration found this was cyclic.
			if (sourceVertex.isCyclic())
				return true;

			sourceVertex.setBeingVisited(true);
			for (ClassVertex neighbor : sourceVertex.getParents()) {
				if (neighbor.isCyclic()) {
					// A previously visited vertex marked found that this vertex was part of a cycle.
					return true;
				} else if (neighbor.isBeingVisited()) {
					// Backward edge exists.
					neighbor.setCyclic(true);
					return true;
				} else if (neighbor.isNotVisited() && hasCycle(neighbor)) {
					// Check next link in the graph path.
					neighbor.setCyclic(true);
					return true;
				}
			}
			sourceVertex.setBeingVisited(false);
			sourceVertex.setVisited(true);
			return false;
		}
	}

	/**
	 * Graph vertex for {@link Graph}.
	 */
	private static class ClassVertex {
		private final JvmClassInfo cls;
		private final Set<ClassVertex> parents = Collections.newSetFromMap(new IdentityHashMap<>());
		private boolean visited;
		private boolean beingVisited;
		private boolean cyclic;

		/**
		 * @param cls
		 * 		Wrapped class.
		 */
		private ClassVertex(@Nonnull JvmClassInfo cls) {
			this.cls = cls;
		}

		/**
		 * @param parentVertex
		 * 		Vertex of parent class.
		 */
		public void addParent(@Nonnull ClassVertex parentVertex) {
			parents.add(parentVertex);
		}

		/**
		 * @param parentName
		 * 		Name to check.
		 *
		 * @return {@code true} when this vertex has the given name as a direct parent.
		 */
		public boolean hasParent(@Nonnull String parentName) {
			return parents.stream().anyMatch(parent -> Objects.equals(parent.getClassValue().getName(), parentName));
		}

		/**
		 * @return Wrapped class.
		 */
		@Nonnull
		public JvmClassInfo getClassValue() {
			return cls;
		}

		/**
		 * Reset visit state.
		 */
		public void resetVisited() {
			setVisited(false);
			setBeingVisited(false);
		}

		/**
		 * @param cyclic
		 * 		New cyclic state.
		 */
		public void setCyclic(boolean cyclic) {
			this.cyclic = cyclic;
		}

		/**
		 * @return {@code true} when this vertex was found to be part of a cycle.
		 */
		public boolean isCyclic() {
			return cyclic;
		}

		/**
		 * @return {@code true} when not yet visited.
		 */
		public boolean isNotVisited() {
			return !visited;
		}

		/**
		 * @param visited
		 * 		New visited state.
		 */
		public void setVisited(boolean visited) {
			this.visited = visited;
		}

		/**
		 * @return Current visitation state, for {@link Graph#hasCycle(ClassVertex)}.
		 */
		public boolean isBeingVisited() {
			return beingVisited;
		}

		/**
		 * @param beingVisited
		 * 		New visitation state.
		 */
		public void setBeingVisited(boolean beingVisited) {
			this.beingVisited = beingVisited;
		}

		/**
		 * @return Set of direct parents.
		 */
		@Nonnull
		public Set<ClassVertex> getParents() {
			return parents;
		}
	}
}
