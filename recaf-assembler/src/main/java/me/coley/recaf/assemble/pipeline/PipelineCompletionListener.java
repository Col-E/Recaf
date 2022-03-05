package me.coley.recaf.assemble.pipeline;

/**
 * Listener for responding to completion of the pipeline.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface PipelineCompletionListener {
	/**
	 * Called when the pipeline completes all steps and the output is generated <i>(and validated if desired)</i>.
	 *
	 * @param object
	 *        {@link org.objectweb.asm.tree.FieldNode} or {@link org.objectweb.asm.tree.MethodNode}.
	 */
	void onCompletedOutput(Object object);
}
