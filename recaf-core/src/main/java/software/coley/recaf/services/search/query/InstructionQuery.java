package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.InstructionPathNode;
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.util.BlwUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Instruction text search implementation.
 *
 * @author Matt Coley
 */
public class InstructionQuery implements JvmClassQuery {
	private final List<StringPredicate> predicates;

	/**
	 * @param predicates
	 * 		List of predicates, where each entry matches a single line of disassembled instruction text.
	 */
	public InstructionQuery(@Nonnull List<StringPredicate> predicates) {
		this.predicates = predicates;
	}

	@Nonnull
	@Override
	public JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate) {
		return (resultSink, classPath, classInfo) -> {
			ClassNode node = new ClassNode();
			classInfo.getClassReader().accept(node, ClassReader.SKIP_FRAMES);
			List<String> matched = new ArrayList<>(predicates.size());
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				ClassMemberPathNode memberPath = classPath.child(method.name, method.desc);
				if (memberPath == null)
					continue;
				matched.clear();
				for (int i = 0; i < method.instructions.size() - predicates.size(); i++) {
					for (int j = 0; j < predicates.size(); j++) {
						int line = i + j;

						// This utility call maps instructions to BLW ones, and passes them to JASM
						// so the format should match what you see in the assembler, barring labels
						// and other debug info.
						String disassembled = BlwUtil.toString(method.instructions.get(line));
						if (!predicates.get(j).match(disassembled)) {
							matched.clear();
							break;
						} else {
							matched.add(disassembled);
						}
					}

					// Add result if we matched all predicates.
					if (matched.size() == predicates.size()) {
						InstructionPathNode path = memberPath.childInsn(method.instructions.get(i), i);
						resultSink.accept(path, String.join("\n", matched));
						matched.clear();
					}
				}
			}
		};
	}
}
