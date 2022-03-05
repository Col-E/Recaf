package me.coley.recaf.assemble.compiler;

import javassist.CtClass;
import javassist.bytecode.Bytecode;
import javassist.compiler.CodeGen;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.Lex;
import javassist.compiler.Parser;
import javassist.compiler.SymbolTable;
import javassist.compiler.TokenId;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.Declarator;
import javassist.compiler.ast.Stmnt;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.transformer.VariableInfo;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;

/**
 * An extension of Javassist's {@link Javac} that exposes some internal structures
 * needed to properly inject local variable information.
 *
 * @author Matt Coley
 */
class JavassistExpressionJavac extends Javac {
	private static final Field fGen;
	private static final Field fSTable;
	private static final Field fBytecode;
	private final ClassSupplier classSupplier;
	private final Variables variables;
	private final CtClass declaringClass;
	private final JvstCodeGen gen;
	private final Expression expression;
	private SymbolTable lastCompiledSymbols;

	/**
	 * @param declaringClass
	 * 		Type that contains the declared method body/expression.
	 * @param classSupplier
	 * 		Class information supplier.
	 * @param variables
	 * 		Variable name cache of the declared method.
	 * @param isStatic
	 * 		Flag for compiler.
	 */
	public JavassistExpressionJavac(CtClass declaringClass, ClassSupplier classSupplier,
									Variables variables, Expression expression, boolean isStatic) {
		super(declaringClass);
		this.declaringClass = declaringClass;
		this.classSupplier = classSupplier;
		this.variables = variables;
		this.expression = expression;
		gen = hookCodeGen();
		gen.inStaticMethod = isStatic;
	}

	@Override
	public void compileStmnt(String src) throws CompileError {
		Parser p = new Parser(new Lex(src));
		lastCompiledSymbols = new SymbolTable();
		if (variables != null) {
			// Variables collected by AST, should include 'this' for non-static methods at a minimum.
			// We need to fill this table manually...
			for (VariableInfo variableInfo : variables) {
				Type type = variableInfo.getLastUsedType();
				String name = variableInfo.getName();
				String desc = type.getDescriptor();
				int index = lastCompiledSymbols.size();
				gen.recordVariable(desc, name, index, lastCompiledSymbols);
				gen.setMaxLocals(index + (Types.isWide(type) ? 2 : 1));
			}
		} else if (!gen.inStaticMethod) {
			// Reserve 'this' as a variable/keyword
			gen.recordVariable(declaringClass, "this", lastCompiledSymbols);
		}

		while (p.hasMore()) {
			Stmnt statement = p.parseStatement(lastCompiledSymbols);
			// Generate bytecode
			if (statement != null)
				statement.accept(getGen());
			// Record variables defined in the statement
			if (variables != null) {
				recordNewVariables(statement);
			}
		}
	}

	/**
	 * Visits the given AST tree nodes and patches any variable declarator to use correct variable indices.
	 *
	 * @param tree
	 * 		Node to visit.
	 *
	 * @see #recordDeclaredVar(Declarator)
	 */
	private void recordNewVariables(ASTree tree) {
		// Check left for declarator, otherwise keep digging deeper
		if (tree instanceof Declarator) {
			recordDeclaredVar((Declarator) tree);
		} else {
			if (tree.getLeft() != null) {
				recordNewVariables(tree.getLeft());
			}
			if (tree.getRight() != null) {
				recordNewVariables(tree.getRight());
			}
		}
	}

	/**
	 * Records declared variables in to the {@link #variables} tracker.
	 * Also updates the generated AST of the variable declarator to use the correct local variable index.
	 *
	 * @param dec
	 * 		Declarator to patch.
	 */
	private void recordDeclaredVar(Declarator dec) {
		if (variables == null)
			throw new IllegalStateException("To patch declarator, variable index lookups are required!");
		String name = dec.getLeft().toString();
		// Skip if we already know about it
		int index = variables.getIndex(name);
		if (index >= 0) {
			return;
		}
		// Check if the variable is initialized
		index = dec.getLocalVar();
		if (index < 0) {
			// Not initialized
			return;
		} else {
			// Check that the initialized slot isn't already registered.
			// If it is, make sure we agree on the variable index
			int tmpIndex = variables.getIndex(name);
			if (tmpIndex >= 0 && index != tmpIndex) {
				throw new IllegalStateException("Variable mismatch");
			}
		}
		// Get the var type
		String desc = dec.getClassName();
		boolean isPrim = desc == null;
		if (isPrim) {
			// Javassist declarators do not have class names for primitives
			switch (dec.getType()) {
				case TokenId.BOOLEAN:
				case TokenId.BYTE:
				case TokenId.SHORT:
				case TokenId.INT:
					desc = "I";
					break;
				case TokenId.CHAR:
					desc = "C";
					break;
				case TokenId.FLOAT:
					desc = "F";
					break;
				case TokenId.DOUBLE:
					desc = "D";
					break;
				case TokenId.LONG:
					desc = "J";
					break;
				default:
					throw new IllegalArgumentException("Unknown primitive type for expression defined var");
			}
		}
		// Get var type and register usage
		Type type;
		if (isPrim) {
			type = Type.getType(desc);
		} else {
			String className = classSupplier.resolveFromImported(declaringClass, desc);
			type = Type.getObjectType(className);
		}
		try {
			variables.addVariableUsage(index, name, type, expression);
		} catch (MethodCompileException e) {
			// This occurs if the passed index is a reserved slot.
			throw new IllegalStateException(e);
		}
		dec.setClassName(type.getClassName());
		setMaxLocals(index);
	}

	/**
	 * @return Modified code gen to pull information from Recaf.
	 */
	private JvstCodeGen hookCodeGen() {
		try {
			JvstCodeGen internalCodeGen = new JavassistCodeGen(classSupplier, getBytecode(), declaringClass,
					declaringClass.getClassPool());
			fGen.set(this, internalCodeGen);
			return internalCodeGen;
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @return Code generator.
	 */
	public JvstCodeGen getGen() {
		return gen;
	}

	/**
	 * @return Generated bytecode.
	 */
	public Bytecode getGeneratedBytecode() {
		try {
			return (Bytecode) fBytecode.get(getGen());
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @return Symbol table used in AST analysis.
	 */
	public SymbolTable getRootSTable() {
		try {
			return (SymbolTable) fSTable.get(this);
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @return Symbol table containing new symbols from the passed body/expression.
	 */
	public SymbolTable getLastCompiledSymbols() {
		return lastCompiledSymbols;
	}

	static {
		try {
			fGen = Javac.class.getDeclaredField("gen");
			fGen.setAccessible(true);
			fSTable = Javac.class.getDeclaredField("stable");
			fSTable.setAccessible(true);
			fBytecode = CodeGen.class.getDeclaredField("bytecode");
			fBytecode.setAccessible(true);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
