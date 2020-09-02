package me.coley.recaf.plugin.api;

import com.sun.tools.attach.VirtualMachine;

/**
 * Plugin that can intercept VMs Recaf attaches to.
 *
 * @author xxDark
 */
public interface AttachPlugin extends BasePlugin {

    /**
     * Called whether Recaf's agent is about to be loaded
     * into target VM.
     *
     * @param virtualMachine Target JVM.
     */
    void onAgentLoad(VirtualMachine virtualMachine);
}
