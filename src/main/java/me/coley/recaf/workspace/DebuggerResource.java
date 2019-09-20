package me.coley.recaf.workspace;

import me.coley.recaf.debug.VMWrap;

/**
 * Importable debuger resource.
 *
 * @author Matt
 */
public class DebuggerResource extends DeferringResource {
	private final VMWrap vm;

	/**
	 * Constructs an debugger resource.
	 *
	 * @param vm
	 * 		Debug wrapper.
	 * @param backing
	 * 		Resource to defer to.
	 */
	public DebuggerResource(VMWrap vm, JavaResource backing) {
		super(ResourceKind.DEBUGGER);
		setBacking(backing);
		this.vm = vm;
	}

	/**
	 * @return Debug wrapper.
	 */
	public VMWrap getVm() {
		return vm;
	}
}
