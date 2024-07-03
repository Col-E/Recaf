package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableMap;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicMapConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.RestoreAwareConfigContainer;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.json.GsonProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Config for {@link SnippetManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SnippetManagerConfig extends BasicConfigContainer implements ServiceConfig, RestoreAwareConfigContainer {
	private final SnippetMap snippets = new SnippetMap();

	@Inject
	public SnippetManagerConfig(@Nonnull GsonProvider gsonProvider) {
		super(ConfigGroups.SERVICE_UI, SnippetManager.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicMapConfigValue<>("snippet-map", SnippetMap.class, String.class, Snippet.class, snippets, true));
	}

	@Override
	public void onNoRestore() {
		snippets.put("println", new Snippet("println", "A simple single-string println() call", """
				// System.out.println("Hello");
				getstatic java/lang/System.out Ljava/io/PrintStream;
				ldc "Hello"
				invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				"""));
		snippets.put("println-fmt", new Snippet("println formatted", "A formatted string println() call", """
				// String name = "bob";
				// System.out.printf("hello %s\\n", name);
				start:
				    ldc "bob"
				    astore name
				printf:
				    getstatic java/lang/System.out Ljava/io/PrintStream;
				    ldc "hello %s\\u000A"
				    iconst_1
				    anewarray Ljava/lang/Object;
				    dup
				    iconst_0
				    aload name
				    aastore
				    invokevirtual java/io/PrintStream.printf (Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintStream;
				    pop
				end:
				"""));
		snippets.put("fori", new Snippet("for-i", "for-loop between [0, 9]", """
				// for (int i = 0; i < 10; i++) someMethod();
				init:
				    // int i = 0
				    iconst_0
				    istore i
				check:
				    // if (i >= 10) break
				    iload i
				    bipush 10
				    if_icmpge exit
				contents:
				    // Inside the for-loop
				    invokestatic com/example/MyClass.someMethod ()V
				inc:
				    // i++ and continue; portion of for-loop
				    iinc i 1
				    goto check
				exit:
				// End of for-loop
				"""));
		snippets.put("while", new Snippet("while", "A simple while loop", """
				// int i = 100;
				// while (i >= 0) {
				//   someMethod();
				//   i--;
				// }
				init:
				    // int i = 100;
				    bipush 100
				    istore i
				check:
				    // if (i < 0) break;
				    iload i
				    iflt exit
				contents:
				    // Inside the while loop
				    invokestatic com/example/MyClass.someMethod ()V
				    iinc i -1
				    goto check
				exit:
				// End of while-loop
				"""));
		snippets.put("if-else", new Snippet("if ... else ...", "A simple if else statement", """
				// if (b)
				//   whenTrue();
				// else
				//   whenFalse();
				start:
				    iload someBoolean
				    ifeq isFalse
				    invokestatic com/example/MyClass.whenTrue ()V
				    goto end
				isFalse:
				    invokestatic com/example/MyClass.whenFalse ()V
				end:
				"""));
	}

	/**
	 * @return Map of recorded snippets.
	 */
	@Nonnull
	public SnippetMap getSnippets() {
		return snippets;
	}

	/**
	 * Map type to hold snippets.
	 */
	public static class SnippetMap extends ObservableMap<String, Snippet, Map<String, Snippet>> {
		public SnippetMap() {
			super(HashMap::new);
		}
	}
}
