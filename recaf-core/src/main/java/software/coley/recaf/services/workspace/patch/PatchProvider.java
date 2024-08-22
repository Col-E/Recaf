package software.coley.recaf.services.workspace.patch;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.error.Result;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.*;
import software.coley.recaf.path.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.workspace.patch.model.JvmAssemblerPatch;
import software.coley.recaf.services.workspace.patch.model.RemovePath;
import software.coley.recaf.services.workspace.patch.model.TextFilePatch;
import software.coley.recaf.services.workspace.patch.model.WorkspacePatch;
import software.coley.recaf.util.StringDiff;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service to provide and handle serialization of {@link WorkspacePatch}s.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PatchProvider implements Service {
	public static final String SERVICE_ID = "resource-patch-provider";
	private static final Logger logger = Logging.get(PatchProvider.class);
	private final AssemblerPipelineManager assemblerPipelineManager;
	private final ResourcePatchProviderConfig config;

	@Inject
	public PatchProvider(@Nonnull AssemblerPipelineManager assemblerPipelineManager,
	                     @Nonnull ResourcePatchProviderConfig config) {
		this.assemblerPipelineManager = assemblerPipelineManager;
		this.config = config;
	}

	/**
	 * Maps a workspace patch into JSON.
	 *
	 * @param patch
	 * 		Patch to serialize.
	 *
	 * @return JSON string representation of the patch.
	 */
	@Nonnull
	public String serializePatch(@Nonnull WorkspacePatch patch) {
		return PatchSerialization.serialize(patch);
	}

	/**
	 * Maps a JSON file into a workspace patch.
	 *
	 * @param workspace
	 * 		Workspace to apply the patch to.
	 * @param patchPath
	 * 		Path to the JSON file outlining patch contents.
	 *
	 * @return A workspace patch instance.
	 *
	 * @throws IOException
	 * 		When the JSON file couldn't be read.
	 * @throws PatchGenerationException
	 * 		When the JSON file couldn't be parsed, or its contents could not be found in the workspace.
	 */
	@Nonnull
	public WorkspacePatch deserializePatch(@Nonnull Workspace workspace, @Nonnull Path patchPath) throws IOException, PatchGenerationException {
		return deserializePatch(workspace, Files.readString(patchPath));
	}

	/**
	 * Maps a JSON file into a workspace patch.
	 *
	 * @param workspace
	 * 		Workspace to apply the patch to.
	 * @param patchContents
	 * 		JSON outlining patch contents.
	 *
	 * @return A workspace patch instance.
	 *
	 * @throws PatchGenerationException
	 * 		When the JSON file couldn't be parsed, or its contents could not be found in the workspace.
	 */
	@Nonnull
	public WorkspacePatch deserializePatch(@Nonnull Workspace workspace, @Nonnull String patchContents) throws PatchGenerationException {
		return PatchSerialization.deserialize(workspace, patchContents);
	}

	/**
	 * Creates a patch which models all changes in the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to generate a patch for.
	 *
	 * @return Patch modeling all changes made in the workspace.
	 *
	 * @throws PatchGenerationException
	 * 		When the patch couldn't be made for any reason.
	 */
	@Nonnull
	public WorkspacePatch createPatch(@Nonnull Workspace workspace) throws PatchGenerationException {
		List<RemovePath> removals = new ArrayList<>();
		List<JvmAssemblerPatch> jvmAssemblerPatches = new ArrayList<>();
		List<TextFilePatch> textFilePatches = new ArrayList<>();
		PatchConsumer<ClassPathNode, JvmClassInfo> classConsumer = (classPath, initial, current) -> {
			DirectoryPathNode parent = Objects.requireNonNull(classPath.getParent());
			ClassPathNode initialPath = parent.child(initial);
			ClassPathNode currentPath = parent.child(current);
			Result<String> initialDisassembleRes = assemblerPipelineManager.getJvmAssemblerPipeline().disassemble(initialPath);
			Result<String> currentDisassembleRes = assemblerPipelineManager.getJvmAssemblerPipeline().disassemble(currentPath);
			if (!initialDisassembleRes.hasValue())
				throw new PatchGenerationException("Failed to disassemble initial state of '" + initial.getName() + "'");
			if (initialDisassembleRes.hasErr())
				throw new PatchGenerationException("Initial state of '" + initial.getName() + "' has assembler errors");
			if (!currentDisassembleRes.hasValue())
				throw new PatchGenerationException("Failed to disassemble current state of '" + initial.getName() + "'");
			if (currentDisassembleRes.hasErr())
				throw new PatchGenerationException("Current state of '" + initial.getName() + "' has assembler errors");
			String initialDisassemble = initialDisassembleRes.get();
			String currentDisassemble = currentDisassembleRes.get();
			List<StringDiff.Diff> assemblerDiffs = StringDiff.diff(initialDisassemble, currentDisassemble);
			if (!assemblerDiffs.isEmpty())
				jvmAssemblerPatches.add(new JvmAssemblerPatch(initialPath, assemblerDiffs));
		};
		PatchConsumer<FilePathNode, FileInfo> fileConsumer = (filePath, initial, current) -> {
			if (initial.isTextFile() && current.isTextFile()) {
				String initialText = initial.asTextFile().getText();
				String currentText = current.asTextFile().getText();
				List<StringDiff.Diff> textDiffs = StringDiff.diff(initialText, currentText);
				if (!textDiffs.isEmpty())
					textFilePatches.add(new TextFilePatch(filePath, textDiffs));
			} else {
				// TODO: Support binary patches of non-text files
				logger.debug("Skipping file diff for '{}' as it is not a text file", initial.getName());
			}
		};

		try {
			WorkspaceResource resource = workspace.getPrimaryResource();
			ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
			resource.bundleStream().forEach(b -> {
				BundlePathNode bundlePath = resourcePath.child(b);
				Set<String> removedKeys = b.getRemovedKeys();
				for (String key : removedKeys) {
					if (b instanceof ClassBundle<?>) {
						ClassInfo stub = new StubClassInfo(key);
						ClassPathNode stubPath = bundlePath.child(stub.getPackageName()).child(stub);
						removals.add(new RemovePath(stubPath));
					} else {
						FileInfo stub = new StubFileInfo(key);
						FilePathNode stubPath = bundlePath.child(stub.getDirectoryName()).child(stub);
						removals.add(new RemovePath(stubPath));
					}
				}
			});
			visitDirtyItems(workspace, resource, resource.getJvmClassBundle(), classConsumer);
			for (var entry : resource.getVersionedJvmClassBundles().entrySet()) {
				visitDirtyItems(workspace, resource, entry.getValue(), classConsumer);
			}
			visitDirtyItems(workspace, resource, resource.getFileBundle(), fileConsumer);
		} catch (Throwable t) {
			throw new PatchGenerationException(t);
		}

		return new WorkspacePatch(workspace,
				Collections.unmodifiableList(removals),
				Collections.unmodifiableList(jvmAssemblerPatches),
				Collections.unmodifiableList(textFilePatches));
	}

	@SuppressWarnings({"unchecked", "DataFlowIssue"})
	private <I extends Info, P extends PathNode<?>> void visitDirtyItems(@Nonnull Workspace workspace,
	                                                                     @Nonnull WorkspaceResource resource,
	                                                                     @Nonnull Bundle<I> bundle,
	                                                                     @Nonnull PatchConsumer<P, I> consumer) throws Throwable {
		BundlePathNode bundlePath = PathNodes.bundlePath(workspace, resource, bundle);
		Set<String> dirtyKeys = bundle.getDirtyKeys();
		for (String dirtyKey : dirtyKeys) {
			Stack<I> history = bundle.getHistory(dirtyKey);
			I current = history.peek();
			I oldest = history.elementAt(0);
			int lastDirSeparator = dirtyKey.lastIndexOf('/');
			String directoryName = lastDirSeparator >= 0 ? dirtyKey.substring(0, lastDirSeparator) : null;
			DirectoryPathNode directoryPath = bundlePath.child(directoryName);
			if (current instanceof ClassInfo currentClass) {
				ClassPathNode classPath = directoryPath.child(currentClass);
				consumer.accept((P) classPath, oldest, current);
			} else if (current instanceof FileInfo currentFile) {
				FilePathNode filePath = directoryPath.child(currentFile);
				consumer.accept((P) filePath, oldest, current);
			}
		}
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ResourcePatchProviderConfig getServiceConfig() {
		return config;
	}

	@FunctionalInterface
	private interface PatchConsumer<P extends PathNode<?>, I extends Info> {
		void accept(P path, I initial, I current) throws Throwable;
	}
}
