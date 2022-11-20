package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Unchecked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This will attempt to assemble <i><b>EVERY</b></i> method in the classpath.
 * Only run this manually if you think you broke something and want over 8000 test cases to look at.
 */
@Disabled
public class AssembleEntireClasspathTests extends JasmUtils {
	private static int count;
	private static int line;

	@ParameterizedTest
	@MethodSource("lookup")
	public void test(String name) {
		AssemblerPipeline pipeline = PipelineTests.createPipeline();
		String debug = "?";
		try {
			ClassReader cr = new ClassReader(name);
			ClassNode node = new ClassNode();
			cr.accept(node, ClassReader.SKIP_FRAMES);
			for (MethodNode method : node.methods) {
				String location = node.name + "." + method.name + method.desc;
				debug = "// " + location;
				BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(method);
				transformer.visit();
				Unit unit = transformer.getUnit();
				count++;
				assertNotNull(unit, "Failed to create unit from: " + location);
				String code = unit.print(new PrintContext("."));
				line += 1 + StringUtil.count("\n", code);
				debug += "\n" + code;
				assertNotNull(code, "Failed to disassemble: " + location);
				// Run assembler pipeline
				//  - AST validation
				//  - Method generation
				assertTrue(PipelineTests.generate(node.name, true, pipeline, code));
			}
			// Sleep call is so if you try and stop it'll actually have a moment to acknowledge the request
			Thread.sleep(1);
		} catch (Exception ex) {
			fail(debug, ex);
		}
	}

	@AfterAll
	public static void onComplete() {
		System.out.println("Methods:    " + count);
		System.out.println("# of lines: " + line);
	}

	public static List<String> lookup() {
		return ModuleFinder.ofSystem()
				.findAll()
				.stream()
				.map(Unchecked.function(ModuleReference::open))
				.flatMap(Unchecked.function(ModuleReader::list))
				.filter(x -> x.endsWith(".class"))
				.map(x -> x.substring(0, x.length() - 6))
				.collect(Collectors.toList());
	}
}
