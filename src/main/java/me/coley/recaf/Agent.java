package me.coley.recaf;

import me.coley.recaf.util.RecafClassLoader;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Recaf loader.
 *
 * @author xxDark
 */
public final class Agent {
    /**
     * Start Recaf as a launch-argument Java agent.
     *
     * @param agentArgs
     * 		Agent arguments to pass to Recaf.
     * @param inst
     * 		Instrumentation instance.
     *
     * @throws Exception if any error occur.
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        agent(agentArgs, inst);
    }

    /**
     * Start Recaf as a dynamically attached Java agent.
     *
     * @param agentArgs
     * 		Agent arguments to pass to Recaf.
     * @param inst
     * 		Instrumentation instance.
     *
     * @throws Exception if any error occur.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        agent(agentArgs, inst);
    }

    private static void agent(String args, Instrumentation inst) throws Exception {
        // Can't use any API here.
        URL source = Agent.class.getProtectionDomain().getCodeSource().getLocation();
        URLClassLoader loader = new RecafClassLoader(new URL[]{source});
        Class<?> recaf = loader.loadClass("me.coley.recaf.Recaf");
        Method m = recaf.getDeclaredMethod("agent", String.class, Instrumentation.class);
        m.setAccessible(true);
        m.invoke(null,  args, inst);
    }
}
