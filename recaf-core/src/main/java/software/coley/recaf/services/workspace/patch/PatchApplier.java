package software.coley.recaf.services.workspace.patch;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.error.Error;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.assembler.JvmAssemblerPipeline;
import software.coley.recaf.services.workspace.patch.model.JvmAssemblerPatch;
import software.coley.recaf.services.workspace.patch.model.RemovePath;
import software.coley.recaf.services.workspace.patch.model.TextFilePatch;
import software.coley.recaf.services.workspace.patch.model.WorkspacePatch;
import software.coley.recaf.util.StringDiff;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service to apply {@link WorkspacePatch}s.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PatchApplier implements Service {
	public static final String SERVICE_ID = "resource-patch-applier";
	private static final Logger logger = Logging.get(PatchApplier.class);
	private final AssemblerPipelineManager assemblerPipelineManager;
	private final ResourcePatchApplierConfig config;

	@Inject
	public PatchApplier(@Nonnull AssemblerPipelineManager assemblerPipelineManager,
	                    @Nonnull ResourcePatchApplierConfig config) {
		this.assemblerPipelineManager = assemblerPipelineManager;
		this.config = config;
	}

	/**
	 * Applies the given patch to the workspace it's associated with.
	 *
	 * @param patch
	 * 		Patch to apply.
	 * @param feedback
	 * 		Optional feedback for receiving errors.
	 * 		When any error is observed the patching process is abandoned.
	 *
	 * @return {@code true} When the patch was successful.
	 * {@code false} when the patch was abandoned.
	 */
	public boolean apply(@Nonnull WorkspacePatch patch, @Nullable PatchFeedback feedback) {
		List<Runnable> tasks = new ArrayList<>();
		ErrorDelegate errorConsumerDelegate = new ErrorDelegate(feedback == null ? null : feedback::onAssemblerErrorsObserved);

		for (RemovePath removal : patch.removals()) {
			PathNode<?> path = removal.path();
			Info toRemove = path.getValueOfType(Info.class);
			Bundle<?> containingBundle = path.getValueOfType(Bundle.class);
			if (containingBundle == null) {
				if (feedback != null) feedback.onIncompletePathObserved(path);
				return false;
			}
			if (toRemove == null) {
				if (feedback != null) feedback.onIncompletePathObserved(path);
				return false;
			}
			String entryName = toRemove.getName();
			tasks.add(() -> {
				if (containingBundle.remove(entryName) == null)
					logger.warn("Could not apply removal for path '{}' - not found in the workspace", entryName);
			});
		}

		JvmAssemblerPipeline jvmAssemblerPipeline = assemblerPipelineManager.getJvmAssemblerPipeline();
		for (JvmAssemblerPatch jvmAssemblerPatch : patch.jvmAssemblerPatches()) {
			// Skip if any errors have been seen.
			if (errorConsumerDelegate.hasSeenErrors())
				return false;

			ClassPathNode path = jvmAssemblerPatch.path().withCurrentWorkspaceContent();
			JvmClassInfo jvmClass = path.getValue().asJvmClass();
			JvmClassBundle jvmBundle = path.getValueOfType(JvmClassBundle.class);
			if (jvmBundle == null) {
				if (feedback != null) feedback.onIncompletePathObserved(path);
				return false;
			}

			// Apply patch
			List<StringDiff.Diff> diffs = jvmAssemblerPatch.assemblerDiffs();
			jvmAssemblerPipeline.disassemble(path).ifOk(disassemble -> {
				// Apply diffs to disassembled class.
				String patchedAssembly = StringDiff.Diff.apply(disassemble, diffs);

				// Reassemble the class and update the workspace.
				// And parse / assemble step failure
				jvmAssemblerPipeline.tokenize(patchedAssembly, "<patch>")
						.flatMap(jvmAssemblerPipeline::roughParse)
						.flatMap(jvmAssemblerPipeline::concreteParse)
						.flatMap(concreteAst -> jvmAssemblerPipeline.assemble(concreteAst, path))
						.ifOk(patchResult -> {
							JavaClassRepresentation representation = patchResult.representation();
							if (representation != null) {
								tasks.add(() -> {
									JvmClassInfo patchedClass = jvmClass.toJvmClassBuilder()
											.adaptFrom(representation.classFile())
											.build();
									jvmBundle.put(patchedClass);
								});
							}
						}).ifErr(errorConsumerDelegate::errors);
			}).ifErr(errors -> {
				// Disassemble failure
				if (feedback != null) feedback.onAssemblerErrorsObserved(errors);
			});
		}

		for (TextFilePatch filePatch : patch.textFilePatches()) {
			// Skip if any errors have been seen.
			if (errorConsumerDelegate.hasSeenErrors())
				return false;

			FilePathNode path = filePatch.path().withCurrentWorkspaceContent();
			TextFileInfo textFile = path.getValue().asTextFile();
			FileBundle fileBundle = path.getValueOfType(FileBundle.class);
			if (fileBundle == null) {
				if (feedback != null) feedback.onIncompletePathObserved(path);
				return false;
			}
			String patchedText = StringDiff.Diff.apply(textFile.getText(), filePatch.textDiffs());
			tasks.add(() -> {
				TextFileInfo patchedTextFile = textFile.toTextBuilder()
						.withRawContent(patchedText.getBytes(textFile.getCharset()))
						.build();
				fileBundle.put(patchedTextFile);
			});
		}

		// If no errors have been seen apply all patches.
		if (!errorConsumerDelegate.hasSeenErrors()) {
			for (Runnable task : tasks) {
				try {
					task.run();
				} catch (Throwable t) {
					// Most likely caused by listeners and not the patch itself.
					// Log the error and continue.
					logger.error("Error applying patch task", t);
				}
			}
		}
		return true;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ResourcePatchApplierConfig getServiceConfig() {
		return config;
	}

	private static class ErrorDelegate {
		private final Consumer<List<Error>> errorConsumer;
		private boolean seenErrors;

		private ErrorDelegate(@Nullable Consumer<List<Error>> errorConsumer) {this.errorConsumer = errorConsumer;}

		public void errors(List<Error> errors) {
			seenErrors = true;
			if (errorConsumer != null) errorConsumer.accept(errors);
		}

		public boolean hasSeenErrors() {
			return seenErrors;
		}
	}
}
