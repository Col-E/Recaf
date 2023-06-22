package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

import static software.coley.recaf.services.source.AstResolveResult.declared;
import static software.coley.recaf.services.source.AstResolveResult.reference;

/**
 * Helper that can link text at some given offset within a source <i>(given as a {@link J.CompilationUnit})</i>
 * to content in a workspace.
 *
 * @author Matt Coley
 */
public class AstContextHelper {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull data from.
	 */
	public AstContextHelper(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * @param unit
	 * 		Compilation unit to look at.
	 * @param offset
	 * 		Offset in the source <i>(Assuming from the {@link String} the unit originates from)</i> resolve.
	 * @param backingText
	 * 		Optional backing text that is the origin of the tree.
	 * 		Can be used for detecting dropped tokens.
	 *
	 * @return Resolved content at the given offset, or {@code null} if no resolution could be made.
	 */
	@Nullable
	public AstResolveResult resolve(@Nonnull J.CompilationUnit unit, int offset, @Nullable String backingText) {
		List<Tree> astPath = AstUtils.getAstPathAtOffset(offset, unit, backingText);

		// If no AST path was found, we have no clue.
		if (astPath.isEmpty())
			return null;

		// Iterate over path, checking if we can resolve some reference to a type/member/package.
		// First items in the path are the most specific, thus yielding the 'best' results.
		AstResolveResult firstResult = null;
		for (Tree ast : astPath) {
			AstResolveResult local = resolve(unit, ast);
			if (local != null) {
				// Track what the first result is (detailing what the most specific match is)
				if (firstResult == null)
					firstResult = local;

				// Some tree types cannot be used to determine if the referenced result is for
				// a declaration or a reference. In these cases we will use the next tree in the
				// ast path to differentiate, assuming it is not ambiguous. Handling of the parent
				// will be done in the next for-loop iteration.
				if (!isAmbiguousForDeclarationVsReference(ast))
					// The local result is not ambiguous.
					//
					// If the type matches the same as the first result, yield the first result
					// with the declaration-state adapted to the state of the local result.
					//
					// If the type does not match, we cannot assume that the parent
					// is just the declaration wrapper of the ambiguous ast element, and thus
					// we will treat the original result as a reference.
					if (firstResult.path().equals(local.path()))
						return firstResult.matchDeclarationState(local);
					else
						return firstResult.asReference();
			}
		}

		// Use fallback of first result found, but as a reference.
		// Can't easily tell if it is a reference or declaration, so we will use reference as the default assumption.
		return firstResult == null ? null : firstResult.asReference();
	}

	/**
	 * <b>Note:</b> To external callers, you will likely want to use {@link #isAmbiguousForDeclarationVsReference(Tree)}
	 * to check if the passed {@code ast} value will yield a trustworthy value for {@link AstResolveResult#isDeclaration()}.
	 *
	 * @param unit
	 * 		Compilation unit to look at.
	 * @param ast
	 * 		AST element in the unit to resolve.
	 *
	 * @return Resolved content for the given ast element, or {@code null} if no resolution could be made.
	 */
	@Nullable
	public AstResolveResult resolve(@Nonnull J.CompilationUnit unit, @Nonnull Tree ast) {
		if (ast instanceof J.Identifier identifierAst) {
			JavaType identifierType = identifierAst.getFieldType();
			if (identifierType == null)
				identifierType = identifierAst.getType();
			if (identifierType instanceof JavaType.Method methodType) {
				ClassMemberPathNode resolved = resolveMethod(methodType);
				if (resolved != null)
					return reference(resolved);
			} else if (identifierType instanceof JavaType.Variable fieldType) {
				PathNode<?> resolved = resolveField(fieldType);
				if (resolved != null)
					return declared(resolved);
			} else if (identifierType instanceof JavaType.FullyQualified qualified) {
				ClassPathNode resolved = resolveClass(qualified);
				if (resolved != null)
					return reference(resolved);
			} else if (identifierType instanceof JavaType.Array || identifierType instanceof JavaType.MultiCatch) {
				ClassPathNode resolved = resolveClass(identifierType);
				if (resolved != null)
					return reference(resolved);
			}
		} else if (ast instanceof J.ClassDeclaration declarationAst) {
			JavaType.FullyQualified declarationType = declarationAst.getType();
			ClassPathNode resolved = resolveClass(declarationType);
			if (resolved != null)
				return declared(resolved);
		} else if (ast instanceof J.MethodDeclaration declarationAst) {
			JavaType.Method methodType = declarationAst.getMethodType();
			ClassMemberPathNode resolved = resolveMethod(methodType);
			if (resolved != null)
				return declared(resolved);
		} else if (ast instanceof J.MethodInvocation invocationAst) {
			// TODO: Handle generics
			//   DummyEnum.class.getEnumConstants() --> ()[LDummyEnum; but we want the base type ()[LEnum;
			JavaType.Method methodType = invocationAst.getMethodType();
			ClassMemberPathNode resolved = resolveMethod(methodType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.FieldAccess fieldAst) {
			JavaType.Variable fieldType = fieldAst.getName().getFieldType();
			PathNode<?> resolved = resolveField(fieldType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.NewClass newAst) {
			JavaType classType = newAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
			if (newAst.getClazz() != null) {
				resolved = resolveClass(newAst.getClazz().getType());
				if (resolved != null)
					return reference(resolved);
			}
		} else if (ast instanceof J.NewArray arrayAst) {
			JavaType classType = arrayAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.ArrayType arrayAst) {
			JavaType classType = arrayAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.ArrayAccess arrayAst) {
			JavaType classType = arrayAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.InstanceOf instanceOfAst) {
			JavaType classType = instanceOfAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.MultiCatch instanceOfAst) {
			JavaType classType = instanceOfAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.TypeCast castAst) {
			JavaType classType = castAst.getType();
			ClassPathNode resolved = resolveClass(classType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.VariableDeclarations.NamedVariable variableAst) {
			JavaType.Variable variableType = variableAst.getVariableType();
			PathNode<?> resolved = resolveField(variableType);
			if (resolved != null)
				return declared(resolved);
			ClassPathNode resolvedClass = resolveClass(variableType);
			if (resolvedClass != null)
				return reference(resolvedClass);
		} else if (ast instanceof J.VariableDeclarations variableAst) {
			JavaType.Variable variableType = variableAst.getVariables().get(0).getVariableType();
			PathNode<?> resolved = resolveField(variableType);
			if (resolved != null)
				return declared(resolved);
		} else if (ast instanceof J.EnumValue enumAst) {
			JavaType enumType = enumAst.getName().getType();
			ClassPathNode enumOwnerPath = resolveClass(enumType);
			if (enumOwnerPath != null) {
				ClassInfo value = enumOwnerPath.getValue();
				for (FieldMember field : value.getFields())
					if (field.getName().equals(enumAst.getName().getSimpleName()) &&
							field.getDescriptor().equals("L" + value.getName() + ";"))
						return declared(enumOwnerPath.child(field));
			}
		} else if (ast instanceof J.MemberReference referenceAst) {
			JavaType.Method methodType = referenceAst.getMethodType();
			ClassMemberPathNode resolved = resolveMethod(methodType);
			if (resolved != null)
				return reference(resolved);
		} else if (ast instanceof J.Package packageAst) {
			String packageName = packageAst.getPackageName().replace('.', '/');
			DirectoryPathNode packagePath = workspace.findPackage(packageName);
			if (packagePath != null)
				return declared(packagePath);
		} else if (ast instanceof J.Import importAst) {
			String className = importAst.getTypeName().replace('.', '/');
			ClassPathNode classPath = workspace.findClass(className);
			if (classPath != null) {
				// Attempt to resolve static import reference
				if (importAst.isStatic()) {
					String name = importAst.getQualid().getSimpleName();
					if (!name.equals("*")) {
						ClassMember member = classPath.getValue().fieldAndMethodStream()
								.filter(m -> name.equals(m.getName()))
								.findFirst().orElse(null);
						if (member != null)
							return reference(classPath.child(member));
					}
				}
				return reference(classPath);
			}
		} else if (ast instanceof J.Block blockAst && blockAst.isStatic()) {
			// Edge case for static initializer
			ClassPathNode declaringPath = resolveClass(unit.getClasses().get(0).getType());
			if (declaringPath != null) {
				ClassInfo declaringClass = declaringPath.getValue();
				for (MethodMember method : declaringClass.getMethods()) {
					if (method.getName().equals("<clinit>"))
						return declared(declaringPath.child(method));
				}
			}
		}
		return null;
	}

	/**
	 * @param classType
	 * 		OpenRewrite type for general types.
	 *
	 * @return Path to resolved class.
	 */
	@Nullable
	public ClassPathNode resolveClass(@Nullable JavaType classType) {
		if (classType != null) {
			// Edge case handling because OpenRR handles string as a primitive
			if (classType instanceof JavaType.Primitive primitive) {
				if (primitive == JavaType.Primitive.String)
					return workspace.findJvmClass("java/lang/String");
				else
					return null;
			} else if (classType instanceof JavaType.Array array) {
				// We want to resolve the class, so grab the element type.
				classType = array.getElemType();
			}
			try {
				String declarationName = AstUtils.toInternal(classType);
				return workspace.findClass(declarationName);
			} catch (UnsupportedOperationException ignored) {
				// passed type was not a class type
			}
		}
		return null;
	}

	/**
	 * @param classType
	 * 		OpenRewrite type for class types.
	 *
	 * @return Path to resolved class.
	 */
	@Nullable
	public ClassPathNode resolveClass(@Nullable JavaType.FullyQualified classType) {
		if (classType != null) {
			String declarationName = AstUtils.toInternal(classType);
			return workspace.findClass(declarationName);
		}
		return null;
	}

	/**
	 * @param fieldType
	 * 		OpenRewrite type for field types.
	 *
	 * @return Path to resolved field.
	 */
	@Nullable
	public PathNode<?> resolveField(@Nullable JavaType.Variable fieldType) {
		if (fieldType != null) {
			// A variable's owner for fields is the declaring class which is a fully qualified type.
			// Local variable's owner are methods, so we only check for fully qualified owner types.
			JavaType ownerType = fieldType.getOwner();
			if (ownerType instanceof JavaType.FullyQualified qualifiedOwner) {
				String owner = AstUtils.toInternal(qualifiedOwner);
				String name = fieldType.getName();
				String desc = AstUtils.toDesc(fieldType);
				ClassPathNode classPath = workspace.findClass(owner);
				if (classPath != null) {
					// Edge case, OpenRewrite will treat 'this' variable in *some* cases as a field.
					if (name.equals("this") && Type.getType(desc).getInternalName().equals(owner))
						return classPath;

					// Iterate fields, find matching name/desc.
					for (FieldMember field : classPath.getValue().getFields())
						if (field.getName().equals(name) && (desc.contains("<unknown>") || field.getDescriptor().equals(desc)))
							return classPath.child(field);
				}
			}
		}
		return null;
	}

	/**
	 * @param methodType
	 * 		OpenRewrite type for method types.
	 *
	 * @return Path to resolved field.
	 */
	@Nullable
	public ClassMemberPathNode resolveMethod(@Nullable JavaType.Method methodType) {
		if (methodType != null) {
			String owner = AstUtils.toInternal(methodType.getDeclaringType());
			ClassPathNode classPath = workspace.findClass(owner);
			if (classPath != null) {
				String name = methodType.getName();
				String desc = AstUtils.toDesc(methodType);
				if (name.equals("<constructor>")) {
					// OpenRewrite does not use bytecode level semantics for constructors
					name = "<init>";
					desc = StringUtil.cutOffAtFirst(desc, ")") + ")V";
				}
				for (MethodMember method : classPath.getValue().getMethods())
					if (method.getName().equals(name) && method.getDescriptor().equals(desc))
						return classPath.child(method);
			}
		}
		return null;
	}

	/**
	 * @param ast
	 * 		Element to check.
	 *
	 * @return {@code true} if we cannot immediately determine if the element represents a declaration or reference.
	 */
	public static boolean isAmbiguousForDeclarationVsReference(@Nullable Tree ast) {
		return ast instanceof J.Identifier;
	}
}
