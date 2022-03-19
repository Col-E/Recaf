package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;

/**
 * Utils for SSVM.
 *
 * @author xDark
 */
public class VirtualMachineUtil {

	/**
	 * @param vm
	 * 		VM instance.
	 *
	 * @return version of JDk the VM runs on.
	 */
	public static int getVersion(VirtualMachine vm) {
		return vm.getSymbols().java_lang_Object.getNode().version - 44;
	}
}
