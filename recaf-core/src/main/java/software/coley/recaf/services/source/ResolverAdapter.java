package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.BasicLocalVariable;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.LocalVariablePathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.sourcesolver.model.AnnotationExpressionModel;
import software.coley.sourcesolver.model.AssignmentExpressionModel;
import software.coley.sourcesolver.model.ClassModel;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.ErroneousModel;
import software.coley.sourcesolver.model.MethodBodyModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.Model;
import software.coley.sourcesolver.model.ModifiersModel;
import software.coley.sourcesolver.model.TypeModel;
import software.coley.sourcesolver.model.VariableModel;
import software.coley.sourcesolver.resolve.BasicResolver;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.ClassMemberPair;
import software.coley.sourcesolver.resolve.entry.DescribableEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;
import software.coley.sourcesolver.resolve.result.ClassResolution;
import software.coley.sourcesolver.resolve.result.DescribableResolution;
import software.coley.sourcesolver.resolve.result.FieldResolution;
import software.coley.sourcesolver.resolve.result.MethodResolution;
import software.coley.sourcesolver.resolve.result.MultiClassResolution;
import software.coley.sourcesolver.resolve.result.MultiMemberResolution;
import software.coley.sourcesolver.resolve.result.PackageResolution;
import software.coley.sourcesolver.resolve.result.Resolution;
import software.coley.sourcesolver.resolve.result.Resolutions;
import software.coley.sourcesolver.resolve.result.VariableResolution;

import java.util.List;

/**
 * Adapts {@link Resolution} values into our {@link AstResolveResult}.
 *
 * @author Matt Coley
 */
public class ResolverAdapter extends BasicResolver {
	private final Workspace workspace;
	private ClassPathNode classContextPath;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from in order to adapt {@link Resolution} values into our {@link AstResolveResult} model.
	 * @param unit
	 * 		Root element model.
	 * @param pool
	 * 		Pool to access class metadata.
	 */
	public ResolverAdapter(@Nonnull Workspace workspace, @Nonnull CompilationUnitModel unit, @Nonnull EntryPool pool) {
		super(unit, pool);
		this.workspace = workspace;
	}

	/**
	 * @return Backing entry pool used for source resolution.
	 */
	@Nonnull
	public EntryPool getEntryPool() {
		return getPool();
	}

	/**
	 * Marks the declared class in the compilation unit as being resolved to the given class.
	 *
	 * @param cls
	 * 		Class that represents the code outlined by the compilation unit.
	 */
	public void setClassContext(@Nonnull ClassInfo cls) {
		ClassModel model = getUnit().getDeclaredClasses().getFirst();
		ClassEntry entry = getPool().getClass(cls.getName());
		if (model != null && entry != null)
			setDeclaredClass(model, entry);
	}

	/**
	 * Marks the declared class in the compilation unit as being resolved to the given class path.
	 *
	 * @param path
	 * 		Path to the class that represents the code outlined by the compilation unit.
	 */
	public void setClassContext(@Nonnull ClassPathNode path) {
		classContextPath = path;
		setClassContext(path.getValue());
	}

	/**
	 * @param position
	 * 		Absolute position in the source code of the item we want to resolve.
	 *
	 * @return Our mapped resolution result which points to a path in the workspace for the resolved content.
	 */
	@Nullable
	public AstResolveResult resolveThenAdapt(int position) {
		// Find the deepest model at position.
		Model model = getUnit();
		while (true) {
			it:
			{
				for (Model child : model.getChildren()) {
					if (child.getRange().isWithin(position) && !(child instanceof ErroneousModel)) {
						model = child;
						break it;
					}
				}
				break;
			}
		}

		// Resolve the content at the given position then adapt it.
		Resolution resolution = resolveAt(position, model);
		return adapt(resolution, model);
	}

	/**
	 * @param resolution
	 * 		Resolution to adapt.
	 * @param target
	 * 		Target model that was the item being resolved.
	 *
	 * @return Our mapped resolution result which points to a path in the workspace for the resolved content.
	 */
	@Nullable
	public AstResolveResult adapt(@Nonnull Resolution resolution, @Nonnull Model target) {
		if (resolution.isUnknown())
			return null;
		else if (resolution instanceof ClassResolution classResolution) {
			String name = classResolution.getClassEntry().getName();
			ClassPathNode path = findClass(name);
			if (path == null)
				return null;

			// If the target *is the class* then it is a declaration.
			if (target instanceof ClassModel && target.resolve(this).matches(resolution))
				return AstResolveResult.declared(path);

			// If the target is within a method body, it is always a reference.
			// Same for:
			//  - Contents of annotation expressions like "@MyAnno(foo = bar)"
			//  - Contents of assignments in places like fields
			//  - Contents of type names
			//    - The class name is just a name model, not a type model so
			if (target.getParentOfType(MethodBodyModel.class) != null)
				return AstResolveResult.reference(path);
			if (target.getParentOfType(AnnotationExpressionModel.class) != null)
				return AstResolveResult.reference(path);
			if (target.getParentOfType(AssignmentExpressionModel.class) != null)
				return AstResolveResult.reference(path);
			if (target.getParentOfType(TypeModel.class) != null)
				return AstResolveResult.reference(path);

			ClassModel parentClassDeclaration = target.getParentOfType(ClassModel.class);
			if (parentClassDeclaration != null && parentClassDeclaration.resolve(this).matches(resolution))
				return AstResolveResult.declared(path);
			return AstResolveResult.reference(path);
		} else if (resolution instanceof VariableResolution variableResolution) {
			LocalVariablePathNode path = findVariable(variableResolution, target);
			if (path == null)
				return null;

			// Determine if it's a declaration or reference.
			//  - The target model must be the variable declaration model.
			//  - Its name/type must match the resolved variable.
			if (target instanceof VariableModel variable
					&& variable.getName().equals(variableResolution.getName())
					&& variableResolution.getResolvedType().getDescriptor().equals(descriptorOf(variable)))
				return AstResolveResult.declared(path);

			return AstResolveResult.reference(path);
		} else if (resolution instanceof FieldResolution fieldResolution) {
			String ownerName = fieldResolution.getOwnerEntry().getName();
			ClassPathNode ownerPath = findClass(ownerName);
			if (ownerPath == null)
				return null;
			FieldEntry fieldEntry = fieldResolution.getFieldEntry();
			ClassMemberPathNode fieldPath = ownerPath.child(fieldEntry.getName(), fieldEntry.getDescriptor());
			if (fieldPath == null)
				return null;

			// Determine if it's a declaration or reference.
			//  - Check if any declared class's fields have the target model in their range
			for (ClassModel declaredClass : getUnit().getRecursiveChildrenOfType(ClassModel.class))
				for (VariableModel field : declaredClass.getFields())
					if (field.getRange().isWithin(target.getRange().begin()))
						return AstResolveResult.declared(fieldPath);

			return AstResolveResult.reference(fieldPath);
		} else if (resolution instanceof MethodResolution methodResolution) {
			String ownerName = methodResolution.getOwnerEntry().getName();
			ClassPathNode ownerPath = findClass(ownerName);
			if (ownerPath == null)
				return null;
			MethodEntry methodEntry = methodResolution.getMethodEntry();
			ClassMemberPathNode methodPath = ownerPath.child(methodEntry.getName(), methodEntry.getDescriptor());
			if (methodPath == null)
				return null;

			// The model we resolved is a declaration if:
			//  - It is a 'MethodModel' that resolves to the same method
			//  - The declaring class must define a method of the same name/type
			for (ClassModel declaredClass : getUnit().getRecursiveChildrenOfType(ClassModel.class))
				if (target instanceof MethodModel targetMethod
						&& targetMethod.resolve(this).equals(methodResolution)
						&& declaredClass.resolve(this) instanceof ClassResolution declaredClassResolution
						&& declaredClassResolution.matches(methodResolution.getOwnerResolution())
						&& methodResolution.matches(declaredClassResolution.getDeclaredMemberResolution(methodEntry))) {
					return AstResolveResult.declared(methodPath);
				} else if (target instanceof ModifiersModel
						&& target.getParent() instanceof MethodModel parentMethod
						&& parentMethod.isStaticInitializer()) {
					return AstResolveResult.declared(methodPath);
				}
			return AstResolveResult.reference(methodPath);
		} else if (resolution instanceof MultiMemberResolution multiMemberResolution) {
			// Used in static star import contexts such as 'Math.*' or single method static imports such as 'Math.min'.
			// For stars, yield the class. For single members, yield the member.
			List<ClassMemberPair> memberEntries = multiMemberResolution.getMemberEntries();
			ClassMemberPair firstMember = memberEntries.getFirst();
			String firstClassName = firstMember.ownerEntry().getName();
			if (memberEntries.size() == 1) {
				// Single member
				return adapt(Resolutions.ofMember(firstMember), target);
			} else if (memberEntries.size() > 1) {
				// Multiple members
				ClassPathNode path = findClass(firstClassName);
				if (path != null)
					return AstResolveResult.reference(path);
			}
		} else if (resolution instanceof MultiClassResolution multiClassResolution) {
			// Used in start import contexts such as 'java.util.*' so yield the package they're all residing within.
			String firstClassName = multiClassResolution.getClassEntries().getFirst().getName();
			int slashIndex = firstClassName.lastIndexOf('/');
			if (slashIndex > 0) {
				String packageName = firstClassName.substring(0, slashIndex);
				DirectoryPathNode path = workspace.findPackage(packageName);
				if (path != null)
					return AstResolveResult.reference(path);
			}
		} else if (resolution instanceof PackageResolution packageResolution) {
			String packageName = packageResolution.getPackageName();
			if (packageName != null) {
				DirectoryPathNode path = workspace.findPackage(packageName);
				if (path != null)
					return AstResolveResult.reference(path);
			}
		}
		// TODO: To support operating on method parameters we need to update source-solver
		//  to have a resolution model on parameters. Then we may also want to have a generic
		//  local variable solver for similar capabilities. This would let us create mappings
		//  in the UI for variables which would be nice.

		return null;
	}

	/**
	 * @param resolution
	 * 		Variable resolution to adapt.
	 * @param target
	 * 		Target model that was the item being resolved.
	 *
	 * @return Path to the variable, or {@code null} when the enclosing method cannot be adapted.
	 */
	@Nullable
	private LocalVariablePathNode findVariable(@Nonnull VariableResolution resolution, @Nonnull Model target) {
		// Must be in a method.
		MethodModel method = target instanceof MethodModel targetMethod ? targetMethod : target.getParentOfType(MethodModel.class);
		if (method == null)
			return null;

		// The method must be resolvable so we can know its owner/descriptor.
		Resolution methodResolution = method.resolve(this);
		if (!(methodResolution instanceof MethodResolution resolvedMethod))
			return null;

		// Find owner class in the workspace.
		String ownerName = resolvedMethod.getOwnerEntry().getName();
		ClassPathNode ownerPath = findClass(ownerName);
		if (ownerPath == null)
			return null;

		// Build path down to the method.
		MethodEntry methodEntry = resolvedMethod.getMethodEntry();
		ClassMemberPathNode methodPath = ownerPath.child(methodEntry.getName(), methodEntry.getDescriptor());
		if (methodPath == null)
			return null;
		if (!(methodPath.getValue() instanceof MethodMember methodMember))
			return null;

		// Find the variable in the method's local variables.
		String name = resolution.getName();
		String descriptor = resolution.getResolvedType().getDescriptor();
		LocalVariable matchedVariable = null;
		for (LocalVariable localVariable : methodMember.getLocalVariables()) {
			if (localVariable.getName().equals(name)) {
				// The name matches, so now lets check the descriptor.
				// This ideally would just be a single 'equals' check, but some decompilers don't emit the same type
				// as what is in the table. For instance consider: List<String> foo = new ArrayList<>()
				//  - CFR emits: ArrayList<String> foo = new ArrayList<>()
				//  - Procyon/Fernflower emit: List<String> foo = new ArrayList<>() as expected
				if (!localVariable.getDescriptor().equals(descriptor)
						&& !Types.isPrimitive(descriptor)
						&& !Types.isArray(descriptor)) {
					// Check if the type is a child of the expected type.
					String mappingType = Type.getType(descriptor).getInternalName();
					String varType = Type.getType(localVariable.getDescriptor()).getInternalName();
					ClassEntry mappingEntry = getPool().getClass(mappingType);
					ClassEntry varEntry = getPool().getClass(varType);
					if (mappingEntry != null && varEntry != null && varEntry.isAssignableFrom(mappingEntry)) {
						matchedVariable = localVariable;
						break;
					}
					continue;
				}

				matchedVariable = localVariable;
				break;
			}
		}

		// Synthetic paths are still useful for type-aware context actions. Rename stays disabled for index < 0.
		if (matchedVariable == null)
			matchedVariable = new BasicLocalVariable(-1, name, descriptor, null);
		return methodPath.childVariable(matchedVariable);
	}

	/**
	 * @param variable
	 * 		Field or variable to get the descriptor of.
	 *
	 * @return Descriptor of the variable's type, or {@code null} if it cannot be resolved to a describable entry.
	 */
	@Nullable
	public String descriptorOf(@Nonnull VariableModel variable) {
		Resolution resolution = variable.getType().getKind() == TypeModel.Kind.VAR ?
				variable.resolve(this) : variable.getType().resolve(this);
		return descriptorOf(resolution);
	}

	/**
	 * @param method
	 * 		Method to get the descriptor of.
	 *
	 * @return Descriptor of the method, or {@code null} if it cannot be resolved to a method entry.
	 */
	@Nullable
	public String descriptorOf(@Nonnull MethodModel method) {
		Resolution resolution = method.resolve(this);
		if (resolution instanceof MethodResolution methodResolution)
			return methodResolution.getMethodEntry().getDescriptor();

		StringBuilder descriptor = new StringBuilder("(");
		for (VariableModel parameter : method.getParameters()) {
			String parameterDescriptor = descriptorOf(parameter.getType().resolve(this));
			if (parameterDescriptor == null)
				return null;
			descriptor.append(parameterDescriptor);
		}
		DescribableEntry returnType = method.getReturnType().resolve(this) instanceof DescribableResolution returnResolution ?
				returnResolution.getDescribableEntry() : null;
		if (returnType == null)
			return null;
		return descriptor.append(')').append(returnType.getDescriptor()).toString();
	}

	/**
	 * @param name Name of the class to find.
	 * @return Path to the class, or {@code null} if it cannot be found in the workspace.
	 */
	@Nullable
	private ClassPathNode findClass(@Nonnull String name) {
		// Try to find the class in the context of the current compilation unit first, then fall back to searching the workspace.
		ClassPathNode contextualPath = findContextualClass(name);
		if (contextualPath != null)
			return contextualPath;
		return workspace.findClass(name);
	}

	/**
	 * @param name Name of the class to find.
	 * @return Path to the class, or {@code null} if it cannot be found in the context of the current compilation unit.
	 */
	@Nullable
	private ClassPathNode findContextualClass(@Nonnull String name) {
		if (classContextPath == null)
			return null;

		// First check if the class context is the class we're looking for.
		if (classContextPath.getValue().getName().equals(name))
			return classContextPath;

		// Next check the same bundle as the context.
		BundlePathNode bundlePath = classContextPath.getParent().getParent();
		ClassInfo classInfo = (ClassInfo) bundlePath.getValue().get(name);
		if (classInfo != null)
			return bundlePath.child(classInfo);

		// Then check the same resource as the context, across its other class bundles.
		WorkspaceResource resource = classContextPath.getValueOfType(WorkspaceResource.class);
		if (resource != null) {
			for (ClassBundle<? extends ClassInfo> bundle : resource.classBundleStream().toList()) {
				if (bundle == bundlePath.getValue()) // We already checked this bundle, so skip it.
					continue;
				classInfo = bundle.get(name);
				if (classInfo != null)
					return PathNodes.classPath(workspace, resource, bundle, classInfo);
			}
		}
		return null;
	}

	@Nullable
	private static String descriptorOf(@Nonnull Resolution resolution) {
		if (resolution instanceof DescribableResolution describableResolution)
			return describableResolution.getDescribableEntry().getDescriptor();
		if (resolution instanceof VariableResolution variableResolution)
			return variableResolution.getResolvedType().getDescriptor();
		return null;
	}
}
