package me.coley.recaf.util;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.ui.controls.text.selection.ClassSelection;
import me.coley.recaf.ui.controls.text.selection.MemberSelection;
import org.fxmisc.richtext.model.TwoDimensional;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * JavaParser utilities.
 *
 * @author Matt
 */
public class JavaParserUtil {
	private static final NodeList<Type> NO_TYPE_ARGS = null;
	private static Method GET_SOLVER;

	/**
	 * Check if the specified compliation unit is considered parsed.
	 *
	 * @param unit
	 *      the compliation unit
	 * @return
	 *      {@code false} if unparseable
	 */
	public static boolean isCompilationUnitParseable(CompilationUnit unit) {
		return unit != null && unit.getParsed() == Node.Parsedness.PARSED;
	}

	/**
	 * Fetch type of selection from the given position.
	 *
	 * @param code
	 * 		Source to analyze.
	 * @param solver
	 * 		Parser symbol resolver.
	 * @param pos
	 * 		Position in source.
	 *
	 * @return Type of selection.
	 */
	public static Object getSelection(SourceCode code, SymbolResolver solver, TwoDimensional.Position pos) {
		// Get declaration at point
		Node node = getSelectedNode(code, pos);
		if(node == null)
			return null;
		// Resolve node to some declaration type and display context menu
		Object selection = checkForDeclaredSelection(solver, node);
		if (selection == null)
			selection = checkReferencedSelection(node);
		return selection;
	}

	/**
	 * @param code
	 * 		Code wrapper.
	 * @param pos
	 * 		Position of caret.
	 *
	 * @return Node of supported type at position.
	 */
	private static Node getSelectedNode(SourceCode code, TwoDimensional.Position pos) {
		// Abort if no analyzed code to parse
		if (code == null)
			return null;
		// Get node at row/column
		Node node = code.getVerboseNodeAt(pos.getMajor() + 1, pos.getMinor());
		// Go up a level until node type is supported
		while(node != null) {
			if(node instanceof Resolvable || node instanceof InitializerDeclaration)
				break;
			Optional<Node> parent = node.getParentNode();
			if(!parent.isPresent())
				break;
			node = parent.get();
		}
		return node;
	}

	/**
	 * Fetch type of selection from the given node.
	 *
	 * @param solver
	 * 		Parser symbol resolver.
	 * @param node
	 * 		Node type to check.
	 *
	 * @return Type of selection.
	 */
	private static Object checkForDeclaredSelection(SymbolResolver solver, Node node) {
		try {
			CompilationUnit unit = node.findCompilationUnit().orElseThrow(AssertionError::new);
			if (solver != null && !unit.containsData(Node.SYMBOL_RESOLVER_KEY)) {
				unit.setData(Node.SYMBOL_RESOLVER_KEY, solver);
			}

			if(node instanceof TypeDeclaration) {
				ResolvedReferenceTypeDeclaration dec = ((TypeDeclaration) node).resolve();
				String name = toInternal(dec);
				return new ClassSelection(name, true);
			} else if(node instanceof FieldDeclaration || (node instanceof VariableDeclarator &&
					node.getParentNode().get() instanceof FieldDeclaration)) {
				// Check if we need to fetch the parent instead
				if(node instanceof VariableDeclarator)
					node = node.getParentNode().get();
				ResolvedFieldDeclaration dec = ((FieldDeclaration) node).resolve();
				String owner = getOwner(dec);
				String name = dec.getName();
				String desc = getDescriptor(dec.getType());
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof MethodDeclaration) {
				ResolvedMethodDeclaration dec = ((MethodDeclaration) node).resolve();
				String owner = getOwner(dec);
				String name = dec.getName();
				String desc = getDescriptor(dec);
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof ConstructorDeclaration) {
				ResolvedConstructorDeclaration dec = ((ConstructorDeclaration) node).resolve();
				String owner = toInternal(dec.declaringType());
				String name = "<init>";
				String desc = getDescriptor(dec);
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof InitializerDeclaration) {
				InitializerDeclaration dec = (InitializerDeclaration) node;
				if(!dec.getParentNode().isPresent())
					return null; // sanity check, but it should ALWAYS be present and a type declaration
				String owner = toInternal(((TypeDeclaration) dec.getParentNode().get()).resolve());
				String name = "<clinit>";
				String desc = "()V";
				return new MemberSelection(owner, name, desc, true);
			} else if (node instanceof EnumConstantDeclaration) {
				EnumConstantDeclaration dec = (EnumConstantDeclaration) node;
				String owner = toInternal(((TypeDeclaration) dec.getParentNode().get()).resolve());
				String name = dec.getNameAsString();
				String desc = "L" + owner + ";";
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof NameExpr) {
				// Ok so this one is a bit tricky. There are different cases we want to handle.
				NameExpr nameExpr = (NameExpr) node;
				// This "type" is used as a fallback. This is for cases like:
				//  - MyType.func()
				//  - MyType.constant
				// Where we want to resolve the type "MyType" and NOT the whole declared member.
				// This works because in these cases "MyType" is not a reference or declaration, only a name.
				ResolvedType type = solver.calculateType(nameExpr);
				String internal = toInternal(type);
				// Check if we want to resolve the member, and not the selected value's type.
				// - Check by seeing if the resolved member name is the selected name.
				try {
					ResolvedValueDeclaration dec = nameExpr.resolve();
					if (nameExpr.getName().asString().equals(dec.getName())) {
						String owner = dec.isField() ? getOwner(dec.asField()) : getOwner(dec.asMethod());
						String name = dec.getName();
						String desc = getDescriptor(dec.getType());
						return new MemberSelection(owner, name, desc, false);
					}
				} catch(Exception ex) {
					// TODO: Enum constant references in self class map to defining type, not the field ref
					//
					// Failed, but its ok. We'll just return the type of this name.
					// - Method arguments names will have their type resolved
				}
				return new ClassSelection(internal, false);
			}
		} catch(UnsolvedSymbolException ex) {
			Log.error("Failed to resolve: " + ex.toString());
		}
		return null;
	}

	/**
	 * Fetch type of selection from the given node.
	 *
	 * @param node
	 * 		Node type to check.
	 *
	 * @return Type of selection.
	 */
	private static Object checkReferencedSelection(Node node) {
		if (node instanceof Resolvable<?>) {
			Object resolved = null;
			try {
				if (node instanceof ReferenceType) {
					ResolvedType dec = ((ReferenceType) node).resolve();
					String name = toInternal(dec);
					return new ClassSelection(name, false);
				}
				Resolvable<?> r = (Resolvable<?>) node;
				resolved = r.resolve();
			} catch (Throwable ex) {
				return null;
			}
			if (resolved instanceof ResolvedReferenceType) {
				ResolvedReferenceType type = (ResolvedReferenceType) resolved;
				return new ClassSelection(toInternal(type), false);
			} else if (resolved instanceof ResolvedReferenceTypeDeclaration) {
				ResolvedReferenceTypeDeclaration type = (ResolvedReferenceTypeDeclaration) resolved;
				return new ClassSelection(toInternal(type), false);
			} else if (resolved instanceof ResolvedConstructorDeclaration) {
				ResolvedConstructorDeclaration type = (ResolvedConstructorDeclaration) resolved;
				return new ClassSelection(toInternal(type.declaringType()), false);
			} else if (resolved instanceof ResolvedFieldDeclaration) {
				ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
				String owner = getOwner(type);
				String name = type.getName();
				String desc = getDescriptor(type);
				return new MemberSelection(owner, name, desc, false);
			} else if (resolved instanceof ResolvedMethodDeclaration) {
				ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
				String owner = getOwner(type);
				String name = type.getName();
				String desc = getDescriptor(type);
				return new MemberSelection(owner, name, desc, false);
			} else if (resolved instanceof ResolvedEnumConstantDeclaration) {
				ResolvedEnumConstantDeclaration type = (ResolvedEnumConstantDeclaration) resolved;
				String owner = toInternal(type.getType());
				String name = type.getName();
				String desc = getDescriptor(type);
				return new MemberSelection(owner, name, desc, false);
			}
		}
		return null;
	}

	/**
	 * @param type
	 * 		Resolved field declaration.
	 *
	 * @return Descriptor of the resolved field. May be {@code null}.
	 */
	public static String getDescriptor(ResolvedFieldDeclaration type) {
		String desc = null;
		try {
			desc =	getDescriptor(type.getType());
		} catch(UnsolvedSymbolException ex) {
			if (type instanceof JavaParserFieldDeclaration) {
				desc = getDescriptor(((JavaParserFieldDeclaration) type).getWrappedNode().getCommonType());
			}
		} catch(UnsupportedOperationException e) { /* Ignored */ }
		return desc;
	}

	/**
	 * @param type
	 * 		Resolved method declaration.
	 *
	 * @return Descriptor of the resolved method.
	 */
	public static String getDescriptor(ResolvedMethodDeclaration type) {
		Optional<MethodDeclaration> ast = type.toAst();
		String desc = null;
		if (ast.isPresent()) {
			desc = getDescriptor(ast.get());
		} /* else if (type instanceof JavassistMethodDeclaration){
			CtMethod method = Reflect.get(type, "ctMethod");
			if (method != null)
				desc = method.getMethodInfo().getDescriptor();
		} else if (type instanceof ReflectionMethodDeclaration) {
			ReflectionMethodDeclaration ref = (ReflectionMethodDeclaration) type;
			Method method = Reflect.get(ref, "method");
			desc = org.objectweb.asm.Type.getType(method).getDescriptor();
		} */ else {
			StringBuilder sbDesc = new StringBuilder("(");
			// Append the method parameters for the descriptor
			int p = type.getNumberOfParams();
			for (int i = 0; i < p; i++) {
				ResolvedParameterDeclaration param = type.getParam(i);
				String pDesc = null;
				if (param.isType()) {
					pDesc = "L" + param.asType().getQualifiedName().replace('.', '/') + ";";
				} else {
					ResolvedType pType = param.getType();
					pDesc = typeToDesc(pType);
				}
				if (pDesc == null)
					return null;
				sbDesc.append(pDesc);
			}
			// Append the return type for the descriptor
			ResolvedType typeRet = type.getReturnType();
			String retDesc = typeToDesc(typeRet);
			if (retDesc == null) {
				return null;
			}
			sbDesc.append(")");
			sbDesc.append(retDesc);
			return sbDesc.toString();
		}
		return desc;
	}

	/**
	 * @param type
	 * 		Resolved constructor declaration.
	 *
	 * @return Descriptor of the resolved constructor.
	 */
	public static String getDescriptor(ResolvedConstructorDeclaration type) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the constructor parameters for the descriptor
		int p = type.getNumberOfParams();
		for(int i = 0; i < p; i++) {
			ResolvedParameterDeclaration param = type.getParam(i);
			sbDesc.append(typeToDesc(param.getType()));
		}
		sbDesc.append(")V");
		return sbDesc.toString();
	}

	/**
	 * @param md
	 *            JavaParser method declaration.
	 * @return Internal descriptor from declaration, or {@code null} if any parsing
	 *         failures occured.
	 */
	public static String getDescriptor(MethodDeclaration md) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the method parameters for the descriptor
		NodeList<Parameter> params = md.getParameters();
		for (Parameter param : params) {
			Type pType = param.getType();
			String pDesc = getDescriptor(pType);
			if (pDesc == null)
				return null;
			if (param.isVarArgs())
				pDesc = "[" + pDesc;
			sbDesc.append(pDesc);
		}
		// Append the return type for the descriptor
		Type typeRet = md.getType();
		String retDesc = getDescriptor(typeRet);
		if (retDesc == null)
			return null;
		sbDesc.append(")");
		sbDesc.append(retDesc);
		return sbDesc.toString();
	}

	/**
	 * @param dec
	 * 		Resolved value declaration.
	 *
	 * @return Internal descriptor of the value's type.
	 */
	public static String getDescriptor(ResolvedValueDeclaration dec) {
		return getDescriptor(dec.getType());
	}

	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	public static String getDescriptor(ResolvedType type) {
		if (type.isArray())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return type.isPrimitive() ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	public static String getDescriptor(Type type) {
		if (type.isArrayType())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return isPrim(type) ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * Converts the resolved type to an internal representation.
	 * If the type is an array the component type's internal name is returned.
	 * Primitives return their boxed names.
	 *
	 * @param type
	 * 		JavaParser resolved type.
	 *
	 * @return Internalized representation.
	 */
	public static String toInternal(ResolvedType type) {
		if(type.isVoid() || type.isPrimitive())
			return type.asPrimitive().getBoxTypeQName().replace(".", "/");
		if(type.isArray())
			return toInternal(type.asArrayType().getComponentType());
		if(type.isReference()) {
			if(type.asReferenceType().getTypeDeclaration() != null)
				return toInternal(type.asReferenceType().getTypeDeclaration().get());
			else
				return type.asReferenceType().getQualifiedName().replace(".", "/");
		}
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
	}

	/**
	 * Converts the resolved type to an internal representation.
	 *
	 * @param type
	 * 		JavaParser resolved declaration type.
	 *
	 * @return Internalized representation.
	 */
	public static String toInternal(ResolvedTypeDeclaration type) {
		if(type.isType()) {
			String qualified = type.getQualifiedName();
			String baseName = type.getClassName();
			// If the class is an inner class, the patterns are like so:
			// - Qualified: com.example.Outer.Inner
			// - Base: Outer.inner
			// From this, we can easily fix up name to use proper internals, like:
			// - com/example/Outer$Inner
			if (baseName.contains("."))
				baseName = baseName.replace('.', '$');
			String prefix = qualified.substring(0, qualified.length() - baseName.length());
			return (prefix + baseName).replace('.', '/');
		}
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
	}

	/**
	 * @param dec
	 * 		Resolved field declaration.
	 *
	 * @return Internal name of the field's owner.
	 */
	public static String getOwner(ResolvedFieldDeclaration dec) {
		return toInternal(dec.declaringType());
	}

	/**
	 * @param dec
	 * 		Resolved method declaration.
	 *
	 * @return Internal name of the method's owner.
	 */
	public static String getOwner(ResolvedMethodDeclaration dec) {
		return toInternal(dec.declaringType());
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private static String typeToDesc(ResolvedType type) {
		String qualified = null;
		if(type instanceof ResolvedTypeVariable)
			qualified = ((ResolvedTypeVariable) type).qualifiedName();
		else if(type instanceof ResolvedTypeParameterDeclaration)
			qualified = type.asTypeParameter().getQualifiedName();
		else if(type.isPrimitive())
			return primTypeToDesc(type.asPrimitive());
		else if(type.isVoid())
			return "V";
		else
			qualified = toInternal(type);
		if(qualified == null)
			return null;
		// Substring out generics
		if(qualified.contains("<") && qualified.contains(">"))
			qualified = qualified.substring(0, qualified.indexOf('<'));
		StringBuilder sbDesc = new StringBuilder();
		for(int i = 0; i < type.arrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(qualified.replace('.', '/'));
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private static String typeToDesc(Type type) {
		String key = null;
		if (type instanceof ClassOrInterfaceType) {
			ClassOrInterfaceType clsType = (ClassOrInterfaceType) type;
			clsType.setTypeArguments(NO_TYPE_ARGS);
			try {
				key = toInternal(clsType.resolve().asReferenceType());
			} catch(UnsolvedSymbolException ex) {
				Log.warn("JavaParser failed to resolve type '{}'", ex.getName());
			} catch(UnsupportedOperationException ex) {
				// Ok, so it may be "unsupported" however it may not technically be unresolvable.
				// For instance, generic types like "<T>" are "unsupported" but do get resolved
				// to their appropriate generic type parameter. JavaParser throws that away though.
				SymbolResolver solver = getSymbolResolver(type);
				if (solver != null) {
					Object resolved = solver.toResolvedType(type, Object.class);
					if (resolved instanceof ResolvedTypeVariable) {
						ResolvedTypeParameterDeclaration typeParam = ((ResolvedTypeVariable)resolved).asTypeParameter();
						if (typeParam.hasLowerBound())
							key = toInternal(typeParam.getLowerBound());
						else
							key = "java/lang/Object";
					}
				} else {
					Log.warn("Unsupported resolve operation for '{}'", ex.getMessage());
				}
			}
		}
		if (key == null)
			key = type.asString();
		if (key.contains("<"))
			key = key.substring(0, key.indexOf("<"));
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(key.replace('.', '/'));
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type.
	 * @return {@code true} if the type denotes a primitive or void type.
	 */
	private static boolean isPrim(Type type) {
		// void is not a primitive, but lets just pretend it is.
		return type.isVoidType() || type.isPrimitiveType();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(ResolvedType type) {
		return primTypeToDesc(type.describe(), type.arrayLevel());
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(Type type) {
		return primTypeToDesc(type.asString(), type.getArrayLevel());
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(String type, int arrayLevel) {
		String desc = null;
		switch (type) {
			case "boolean":
				desc = "Z";
				break;
			case "char":
				desc = "C";
				break;
			case "int":
				desc = "I";
				break;
			case "long":
				desc = "J";
				break;
			case "short":
				desc = "S";
				break;
			case "byte":
				desc = "B";
				break;
			case "double":
				desc = "D";
				break;
			case "float":
				desc = "F";
				break;
			case "void":
				desc = "V";
				break;
			default:
				throw new RuntimeException("Unknown primitive type field '" + type + "'");
		}
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < arrayLevel; i++)
			sbDesc.append("[");
		sbDesc.append(desc);
		return sbDesc.toString();
	}

	// ==================================================================================== //

	/**
	 * @param node
	 * 		Node to resolve.
	 *
	 * @return If the node is a class, {@code {name}}.<br>If the node is a member, {@code {owner,
	 * name, desc}}.
	 */
	public static String[] resolveReference(Node node) {
		if(!(node instanceof Resolvable))
			return null;
		// Resolve node to some declaration type
		Resolvable<?> r = (Resolvable<?>) node;
		Object resolved = null;
		try {
			resolved = r.resolve();
		} catch(Exception ex) {
			return null;
		}
		if (resolved instanceof ResolvedMethodDeclaration) {
			ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
			ResolvedTypeDeclaration declaring = type.declaringType();
			String owner = declaring.getQualifiedName().replace('.', '/');
			String name = type.getName();
			String desc = getDescriptor(type);
			return new String[] { owner, name, desc };
		} else if (resolved instanceof ResolvedFieldDeclaration) {
			ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
			ResolvedTypeDeclaration declaring = type.declaringType();
			String owner = declaring.getQualifiedName().replace('.', '/');
			String name = type.getName();
			String desc = getDescriptor(type);
			return new String[] { owner, name, desc };
		} else if (resolved instanceof ResolvedTypeDeclaration) {
			ResolvedTypeDeclaration owner = (ResolvedTypeDeclaration) resolved;
			String ownerInternal = owner.getQualifiedName().replace('.', '/');
			return new String[] { ownerInternal };
		}
		return null;
	}

	// ==================================================================================== //


	/**
	 * Finds the member by the given name and descriptor and returns its range.
	 *
	 * @param unit
	 * 		AST tree.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 *
	 * @return Range of member if the member exists.
	 */
	public static Optional<Range> getMemberRange(CompilationUnit unit, String name, String desc) {
		Optional<Range> range = Optional.empty();
		if(desc.contains("(")) {
			// Methods
			Optional<MethodDeclaration> opt = unit.findFirst(MethodDeclaration.class,
					(MethodDeclaration md) ->
							name.equals(md.getName().asString()) && desc.equals(getDescriptor(md)));
			if(opt.isPresent())
				range = opt.get().getName().getRange();
		} else {
			// Fields
			Optional<FieldDeclaration> opt = unit.findFirst(FieldDeclaration.class, (FieldDeclaration fd) -> {
				VariableDeclarator vd = fd.getVariable(0);
				return name.equals(vd.getName().asString()) &&
						desc.equals(getDescriptor(vd.getType()));
			});
			if(opt.isPresent())
				range = opt.get().getVariable(0).getName().getRange();
		}
		return range;
	}

	private static SymbolResolver getSymbolResolver(Node node) {
		if (GET_SOLVER == null)
			return null;
		try {
			return (SymbolResolver) GET_SOLVER.invoke(node);
		} catch(Throwable t) {
			return null;
		}
	}

	static {
		try {
			GET_SOLVER = Node.class.getDeclaredMethod("getSymbolResolver");
			GET_SOLVER.setAccessible(true);
		} catch(Throwable t) {
			Log.warn("Failed to get symbol-solver method");
		}
	}
}
