package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import software.coley.recaf.services.mapping.Mappings;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static software.coley.recaf.services.source.AstUtils.toInternal;

/**
 * {@link JavaVisitor} to remap type and member references from a {@link Mappings} instance in a {@link J.CompilationUnit}.
 * <br>
 * A fair amount of this logic is lifted from exiting OpenRewrite classes but with modifications.
 * We do not intend on maintaining an AST after doing mapping operations, so we do not consistently maintain AST node's resolved types
 * like the original OpenRewrite operations do.
 * <ul>
 *     <li>{@link ChangePackage}</li>
 *     <li>{@link ChangeType}</li>
 *     <li>{@link ChangeMethodName}</li>
 *     <li>{@link ChangeFieldName}</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class AstMappingVisitor extends JavaIsoVisitor<ExecutionContext> {
	private final Mappings mappings;
	private JavaType.FullyQualified currentType;

	/**
	 * @param mappings
	 * 		Mappings to apply.
	 */
	public AstMappingVisitor(@Nonnull Mappings mappings) {
		this.mappings = mappings;
	}

	// TODO: Support for mapping fields & methods
	//  - Currently this is set up to ONLY handle CLASSES.
	//  - Adding additional mapping will likely cause some of the assumptions in the existing logic to break
	//    so I expect more changes will need to be made to support this.

	@Nonnull
	@Override
	public J.CompilationUnit visitCompilationUnit(@Nonnull J.CompilationUnit cu, @Nonnull ExecutionContext ctx) {
		List<J.ClassDeclaration> classes = cu.getClasses();
		if (classes.isEmpty())
			throw new IllegalStateException("Unit must have at least one class!");
		currentType = classes.get(0).getType();
		return super.visitCompilationUnit(cu, ctx);
	}

	@Nonnull
	@Override
	public J.Package visitPackage(@Nonnull J.Package pkg, @Nonnull ExecutionContext ctx) {
		String internalType = toInternal(currentType);
		String mappedType = mappings.getMappedClassName(internalType);
		if (mappedType != null) {
			int endIndex = mappedType.lastIndexOf('/');
			if (endIndex > 0) {
				String mappedPackage = mappedType.substring(0, endIndex).replace('/', '.');
				pkg = JavaTemplate.builder(mappedPackage)
						.contextSensitive()
						.build()
						.apply(getCursor(), pkg.getCoordinates().replace());
			}
		}
		return pkg;
	}

	@Nonnull
	@Override
	public J.Import visitImport(@Nonnull J.Import impoort, @Nonnull ExecutionContext ctx) {
		J.FieldAccess qualid = impoort.getQualid();
		String internalType = impoort.getTypeName().replace('.', '/');
		String mappedType = mappings.getMappedClassName(internalType);
		if (mappedType != null) {
			String suffix = impoort.isStatic() ? "." + qualid.getName().getSimpleName() : "";
			mappedType = mappedType.replace('/', '.');
			int lastDot = mappedType.lastIndexOf('.');
			String simpleMappedName = mappedType.substring(lastDot + 1);
			String packageName = mappedType.substring(0, lastDot);
			// This is really cringe, but it works.
			Markers markers = new Markers(UUID.randomUUID(), Collections.emptyList());
			Space space = Space.format("");
			J.Identifier packageIdentifier = new J.Identifier(UUID.randomUUID(), space, markers, Collections.emptyList(), packageName, null, null);
			J.Identifier nameIdentifier = new J.Identifier(UUID.randomUUID(), space, markers, Collections.emptyList(), simpleMappedName + suffix, null, null);
			impoort = impoort.withQualid(qualid.withName(nameIdentifier)
					.withTarget(packageIdentifier)
					.withType(visitType(qualid.getType(), ctx)));
		}

		// We sadly cannot *easily* update static imports because their type does NOT get resolved.
		// We know their owner type because that is the import, but the descriptor is missing.
		// Later if it becomes an issue, we can do a lookup in the workspace (which would need to be provided)
		// and loop over members of the imported type, finding a match that way.
		return impoort;
	}

	@Nonnull
	@Override
	public J.ArrayType visitArrayType(@Nonnull J.ArrayType array, @Nonnull ExecutionContext ctx) {
		try {
			array = array.withElementType(visitIdentifier((J.Identifier) array.getElementType(), ctx));
			return super.visitArrayType(array, ctx);
		} catch (Throwable t) {
			return super.visitArrayType(array, ctx);
		}
	}

	@Nonnull
	@Override
	public J.Identifier visitIdentifier(@Nonnull J.Identifier identifier, @Nonnull ExecutionContext ctx) {
		J.Identifier visitedIdentifier = super.visitIdentifier(identifier, ctx);
		JavaType initialType = identifier.getType();
		JavaType visitedType = visitedIdentifier.getType();
		if (visitedType == null || initialType == null)
			return visitedIdentifier;

		// If the parent visit operation resulted in the identifier's type changing we need to update the identifier
		// to reflect the new class type.
		if (initialType != visitedType) {
			// Get the parent context, so we can make sense of what this identifier is used for.
			Cursor cursorParent = getCursor();
			do {
				cursorParent = cursorParent.getParent();
			} while (cursorParent != null &&
					(cursorParent.getValue() instanceof JContainer ||
							cursorParent.getValue() instanceof JLeftPadded ||
							cursorParent.getValue() instanceof JRightPadded));

			// Handle if valid parent found.
			if (cursorParent != null) {
				Object parentValue = cursorParent.getValue();
				if (parentValue instanceof J.Annotation ||
						parentValue instanceof J.VariableDeclarations ||
						parentValue instanceof J.ClassDeclaration ||
						parentValue instanceof J.MethodDeclaration ||
						parentValue instanceof J.MemberReference ||
						parentValue instanceof J.ControlParentheses ||
						parentValue instanceof J.ParameterizedType ||
						parentValue instanceof J.NewClass ||
						parentValue instanceof J.NewArray) {
					// In these cases, the identifier should always be a reference to the class type, and not a general name.
					// For instance:
					//  @NAME
					//  NAME variable = ...
					//  class NAME { ... }
					//  NAME getFoo() { ... }
					//  foo = (NAME) bar;
					//  Supplier<NAME> supplier = NAME::new
					//  new NAME
					//  new NAME[]
					JavaType.FullyQualified visitedQualified = (JavaType.FullyQualified) visitedType;
					visitedIdentifier = visitedIdentifier.withSimpleName(visitedQualified.getClassName());
				} else if (parentValue instanceof J.MethodInvocation || parentValue instanceof J.FieldAccess) {
					// This will handle the reference context.
					// For static references, we want to rename the identifier.
					// There's no 'isStatic' call, so we assume if the identifier is the class's simple name we ought to map it.
					if (visitedType instanceof JavaType.FullyQualified visitedQualified) {
						JavaType.FullyQualified initialQualified = (JavaType.FullyQualified) initialType;

						// TODO: Handle fully qualified context like 'com.example.MyClass.myStaticMethod()'
						if (visitedIdentifier.getSimpleName().equals(initialQualified.getClassName()))
							visitedIdentifier = visitedIdentifier.withSimpleName(visitedQualified.getClassName());
					} else if (visitedType instanceof JavaType.Array) {
						// In this case, the identifier seems to always be the reference name.
						// We can skip doing anything here.
					} else if (visitedType instanceof JavaType.Method) {
						// In this case, the identifier seems to always be the calling context.
						// We can skip doing anything here.
					} else {
						throw new UnsupportedOperationException("Calling context on reference unknown: " + visitedType.getClass().getName());
					}
				}
			}
		}

		return visitedIdentifier;
	}

	@Nonnull
	@Override
	public J.VariableDeclarations visitVariableDeclarations(@Nonnull J.VariableDeclarations multiVariable, @Nonnull ExecutionContext ctx) {
		J.VariableDeclarations visitedVariables = super.visitVariableDeclarations(multiVariable, ctx);

		// Handle field mapping. There is no difference in visitor for local vars and fields, so we
		// will look at the visitor stack to see where we are. If the closest parent is a class and not an element
		// that can define variables like a method/lambda, then it should be a field.
		boolean isField = false;
		Cursor cursor = getCursor();
		while (cursor.getParent() != null) {
			Object value = cursor.getValue();
			if (value instanceof J.ClassDeclaration) {
				isField = true;
				break;
			} else if (value instanceof J.MethodDeclaration) {
				break;
			} else if (value instanceof J.Lambda) {
				break;
			}
			cursor = cursor.getParent();
		}
		if (isField) {
			String owner = AstUtils.toInternal(currentType);
			List<J.VariableDeclarations.NamedVariable> variableList = visitedVariables.getVariables();
			for (int i = 0; i < variableList.size(); i++) {
				J.VariableDeclarations.NamedVariable variable = variableList.get(i);
				JavaType type = variable.getType();
				if (type != null) {
					String name = variable.getSimpleName();
					String desc = AstUtils.toDesc(type);
					String mappedFieldName = mappings.getMappedFieldName(owner, name, desc);
					if (mappedFieldName != null) {
						variableList.set(i, variable.withName(variable.getName().withSimpleName(mappedFieldName)));
					}
				}
			}
			visitedVariables = visitedVariables.withVariables(variableList);
		}

		return visitedVariables;
	}

	@Nonnull
	@Override
	public J.EnumValue visitEnumValue(@Nonnull J.EnumValue _enum, @Nonnull ExecutionContext ctx) {
		J.EnumValue visitedValue = super.visitEnumValue(_enum, ctx);

		// Handle field mapping for declared enum values
		String owner = AstUtils.toInternal(currentType);
		String name = visitedValue.getName().getSimpleName();
		String desc = "L" + owner + ";";
		String mappedFieldName = mappings.getMappedFieldName(owner, name, desc);
		if (mappedFieldName != null)
			visitedValue = visitedValue.withName(visitedValue.getName().withSimpleName(mappedFieldName));

		return visitedValue;
	}

	@Nonnull
	@Override
	public J.FieldAccess visitFieldAccess(@Nonnull J.FieldAccess fieldAccess, @Nonnull ExecutionContext ctx) {
		J.FieldAccess visitedFieldRef = super.visitFieldAccess(fieldAccess, ctx);

		// Handle field mapping for instances where there is context like 'this.field' or 'method().field'
		JavaType exprType = fieldAccess.getTarget().getType();
		JavaType fieldType = fieldAccess.getName().getType();
		if (exprType != null && fieldType != null) {
			String owner = AstUtils.toInternal(exprType);
			String desc = AstUtils.toDesc(fieldType);
			String name = fieldAccess.getName().getSimpleName();
			String mappedFieldName = mappings.getMappedFieldName(owner, name, desc);
			if (mappedFieldName != null)
				visitedFieldRef = visitedFieldRef.withName(visitedFieldRef.getName().withSimpleName(mappedFieldName));
		}

		return visitedFieldRef;
	}

	@Nonnull
	@Override
	public J.Assignment visitAssignment(@Nonnull J.Assignment assignment, @Nonnull ExecutionContext ctx) {
		J.Assignment visitedAssignment = super.visitAssignment(assignment, ctx);

		// Handle updating the variable, which may be a field reference.
		Expression initialVariable = assignment.getVariable();
		Expression visitedVariable = visitedAssignment.getVariable();
		if (Objects.equals(initialVariable.toString(), visitedVariable.toString()) &&
				initialVariable instanceof J.Identifier initialIdentifier &&
				visitedVariable instanceof J.Identifier visitedIdentifier) {
			// Variable was not renamed. We only need to address non-context driven variables.
			// Things like 'this.field' are handled due to the API handling this as field-access.
			// But 'field' by itself is handled as a generic identifier, which we need to fix here to use the new name.
			JavaType.Variable fieldType = initialIdentifier.getFieldType();
			if (fieldType != null && fieldType.getOwner() != null) {
				String owner = AstUtils.toInternal(fieldType.getOwner());
				String name = fieldType.getName();
				String desc = AstUtils.toDesc(fieldType.getType());
				String mappedFieldName = mappings.getMappedFieldName(owner, name, desc);
				if (mappedFieldName != null)
					visitedAssignment = visitedAssignment.withVariable(visitedIdentifier.withSimpleName(mappedFieldName));
			}
		}

		// Handle updating the assignment value, which may be a field reference as well.
		Expression initialAssigned = assignment.getAssignment();
		Expression visitedAssigned = visitedAssignment.getAssignment();
		if (Objects.equals(initialAssigned.toString(), visitedAssigned.toString()) &&
				initialAssigned instanceof J.Identifier initialIdentifier &&
				visitedAssigned instanceof J.Identifier visitedIdentifier) {
			// The assigned value was not modified during visit.
			// Similar to the reasons above, we only need to update it when the value is a non-context driven field reference.
			// This means for 'x = y' we need to update 'y'. But if it were 'x = this.y' we don't have to.
			JavaType.Variable fieldType = initialIdentifier.getFieldType();
			if (fieldType != null && fieldType.getOwner() != null) {
				String owner = AstUtils.toInternal(fieldType.getOwner());
				String name = fieldType.getName();
				String desc = AstUtils.toDesc(fieldType.getType());
				String mappedFieldName = mappings.getMappedFieldName(owner, name, desc);
				if (mappedFieldName != null)
					visitedAssignment = visitedAssignment.withAssignment(visitedIdentifier.withSimpleName(mappedFieldName));
			}
		}

		return visitedAssignment;
	}

	@Nonnull
	@Override
	public J.MethodDeclaration visitMethodDeclaration(@Nonnull J.MethodDeclaration method, @Nonnull ExecutionContext ctx) {
		J.MethodDeclaration visitedMethod = super.visitMethodDeclaration(method, ctx);

		// Edge case for mapping type names in constructors
		if (visitedMethod.getSimpleName().equals(currentType.getClassName())) {
			J.Identifier name = method.getName();
			JavaType visitedNameType = visitType(currentType, ctx);
			JavaType.Method type = method.getMethodType();
			JavaType.Method visitedMethodType = (JavaType.Method) visitType(type, ctx);
			if (visitedNameType instanceof JavaType.FullyQualified qualified) {
				visitedMethod = visitedMethod
						.withName(name.withSimpleName(qualified.getClassName()))
						.withMethodType(visitedMethodType);
			}
		} else if (method.getMethodType() != null) {
			// Handle method mappings
			String owner = AstUtils.toInternal(currentType);
			String name = method.getSimpleName();
			String desc = AstUtils.toDesc(method.getMethodType());

			String mappedMethodName = mappings.getMappedMethodName(owner, name, desc);
			if (mappedMethodName != null)
				visitedMethod = visitedMethod
						.withName(method.getName().withSimpleName(mappedMethodName));
		}

		return visitedMethod;
	}

	@Nonnull
	@Override
	public J.MethodInvocation visitMethodInvocation(@Nonnull J.MethodInvocation method, @Nonnull ExecutionContext ctx) {
		J.MethodInvocation visitedMethod = super.visitMethodInvocation(method, ctx);

		// Handle method mappings
		JavaType.Method methodType = visitedMethod.getMethodType();
		if (methodType != null && visitedMethod.getType() != null) {
			String owner = AstUtils.toInternal(methodType.getDeclaringType());
			String name = visitedMethod.getSimpleName();
			String desc = AstUtils.toDesc(methodType);

			String mappedMethodName = mappings.getMappedMethodName(owner, name, desc);
			if (mappedMethodName != null)
				visitedMethod = visitedMethod.withName(visitedMethod.getName().withSimpleName(mappedMethodName));
		}

		// Handle updating the call context value, which may be a field reference as well.
		Expression initialContext = method.getSelect();
		Expression visitedContext = visitedMethod.getSelect();
		if (initialContext != null && visitedContext != null &&
				Objects.equals(initialContext.toString(), visitedContext.toString()) &&
				initialContext instanceof J.Identifier initialIdentifier &&
				visitedContext instanceof J.Identifier visitedIdentifier) {
			// The assigned value was not modified during visit.
			// Similar to the reasons above, we only need to update it when the value is a non-context driven field reference.
			// This means for 'x = y' we need to update 'y'. But if it were 'x = this.y' we don't have to.
			JavaType.Variable fieldType = initialIdentifier.getFieldType();
			if (fieldType != null && fieldType.getOwner() != null) {
				String owner = AstUtils.toInternal(fieldType.getOwner());
				String name = fieldType.getName();
				String desc = AstUtils.toDesc(fieldType.getType());
				String mappedFieldName = mappings.getMappedFieldName(owner, name, desc);
				if (mappedFieldName != null)
					visitedMethod = visitedMethod.withSelect(visitedIdentifier.withSimpleName(mappedFieldName));
			}
		}

		return visitedMethod;
	}

	@Nonnull
	@Override
	public J.MemberReference visitMemberReference(@Nonnull J.MemberReference memberRef, @Nonnull ExecutionContext context) {
		J.MemberReference m = super.visitMemberReference(memberRef, context);

		// Handle method mappings
		JavaType.Method methodType = m.getMethodType();
		if (methodType != null && m.getType() != null) {
			J.Identifier reference = m.getReference();

			String owner = AstUtils.toInternal(methodType.getDeclaringType());
			String name = reference.getSimpleName();
			String desc = AstUtils.toDesc(methodType);

			String mappedMethodName = mappings.getMappedMethodName(owner, name, desc);
			if (mappedMethodName != null)
				m = m.withReference(reference.withSimpleName(mappedMethodName));
		}

		return m;
	}

	/**
	 * Rather than have dozens of visitors for all possible edge cases what we do is change the type of values here.
	 * Then in {@link #visitIdentifier(J.Identifier, ExecutionContext)} we swap out the name when the type is changed.
	 *
	 * @param javaType
	 * 		Initial type.
	 * @param ctx
	 * 		Visitor execution context.
	 *
	 * @return Modified type, or input to retain the same.
	 */
	@Nullable
	@Override
	public JavaType visitType(@Nullable JavaType javaType, @Nonnull ExecutionContext ctx) {
		if (javaType instanceof JavaType.FullyQualified qualifiedType) {
			// Update class references
			String internalTypeName = qualifiedType.getFullyQualifiedName().replace('.', '/');
			String mappedClassName = mappings.getMappedClassName(internalTypeName);
			if (mappedClassName != null)
				javaType = qualifiedType.withFullyQualifiedName(mappedClassName.replace('/', '.'));
		} else if (javaType instanceof JavaType.Array array) {
			// Update array references
			JavaType visitedElementType = visitType(array.getElemType(), ctx);
			if (visitedElementType != null && visitedElementType != array.getElemType())
				javaType = array.withElemType(visitedElementType);
		} else if (javaType instanceof JavaType.Method method) {
			// Update method types
			JavaType visitedReturnType = visitType(method.getReturnType(), ctx);
			if (visitedReturnType != null && visitedReturnType != method.getReturnType())
				method = method.withReturnType(visitedReturnType);

			// Update arguments
			boolean dirty = false;
			List<JavaType> parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.size(); i++) {
				JavaType parameterType = parameterTypes.get(i);
				JavaType visited = visitType(parameterType, ctx);
				if (visited != parameterType) {
					parameterTypes.set(i, visited);
					dirty = true;
				}
			}
			if (dirty)
				method = method.withParameterTypes(parameterTypes);

			// Update thrown types
			dirty = false;
			List<JavaType.FullyQualified> thrownExceptions = method.getThrownExceptions();
			for (int i = 0; i < thrownExceptions.size(); i++) {
				JavaType.FullyQualified thrownType = thrownExceptions.get(i);
				JavaType.FullyQualified visited = (JavaType.FullyQualified) visitType(thrownType, ctx);
				if (visited != thrownType) {
					thrownExceptions.set(i, visited);
					dirty = true;
				}
			}
			if (dirty)
				method = method.withParameterTypes(parameterTypes);

			// Update annotations
			dirty = false;
			List<JavaType.FullyQualified> annotations = method.getAnnotations();
			for (int i = 0; i < annotations.size(); i++) {
				JavaType.FullyQualified annotationType = annotations.get(i);
				JavaType.FullyQualified visited = (JavaType.FullyQualified) visitType(annotationType, ctx);
				if (visited != annotationType) {
					annotations.set(i, visited);
					dirty = true;
				}
			}
			if (dirty)
				method = method.withParameterTypes(parameterTypes);

			// Update reference
			javaType = method;
		} else if (javaType instanceof JavaType.Variable variableType) {
			// Update variable type
			JavaType type = variableType.getType();
			JavaType visited = visitType(type, ctx);
			if (visited != null && visited != type)
				variableType = variableType.withType(visited);
			javaType = variableType;
		}
		return super.visitType(javaType, ctx);
	}
}
