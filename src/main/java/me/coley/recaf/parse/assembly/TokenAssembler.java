package me.coley.recaf.parse.assembly;

import me.coley.recaf.parse.assembly.exception.AssemblyParseException;
import me.coley.recaf.parse.assembly.util.MatchResult;
import me.coley.recaf.parse.assembly.util.RegexToken;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.Collections;
import java.util.List;

/**
 * Assembler that will used a tokenizer to fetch required arguments from the input assembly text.
 *
 * @param <T>
 * 		Type of instruction to assemble.
 *
 * @author Matt
 */
public abstract class TokenAssembler<T extends AbstractInsnNode> extends AbstractAssembler<T> {
	private RegexToken token;

	public TokenAssembler(int opcode) {
		super(opcode);
	}

	/**
	 * @return Tokenizer to use.
	 */
	public RegexToken token() {
		if(token == null)
			token = createToken();
		return token;
	}

	/**
	 * @return Tokenizer to use.
	 */
	public abstract RegexToken createToken();

	protected T fail(String text, String details, String token) {
		String clazz = this.getClass().getSimpleName() + "InsnNode";
		StringBuilder sb = new StringBuilder(clazz + " parse failure: " + text);
		if(details != null)
			sb.append('\n').append(details).append('\n');
		sb.append("Failed on token: ").append(token);
		throw new AssemblyParseException(sb.toString());
	}

	@Override
	public List<String> suggest(String text) {
		MatchResult result = token().matches(text);
		if(result.isSuccess())
			return Collections.emptyList();
		return result.getFailedToken().suggest();
	}
}
