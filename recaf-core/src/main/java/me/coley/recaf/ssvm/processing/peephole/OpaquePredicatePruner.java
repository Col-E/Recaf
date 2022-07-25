package me.coley.recaf.ssvm.processing.peephole;

public class OpaquePredicatePruner   {
	// TODO: If a flow path's arguments are all constants, just delete the other flow-path's contents
	//       since it indicates an opaque predicate. This should be done in a separate processor.
	//       This one should be updated to not take the never-true path.
}
