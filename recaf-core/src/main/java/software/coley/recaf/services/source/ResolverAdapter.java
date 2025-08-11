package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.sourcesolver.model.AnnotationExpressionModel;
import software.coley.sourcesolver.model.AssignmentExpressionModel;
import software.coley.sourcesolver.model.ClassModel;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.ErroneousModel;
import software.coley.sourcesolver.model.MethodBodyModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.Model;
import software.coley.sourcesolver.model.TypeModel;
import software.coley.sourcesolver.model.VariableModel;
import software.coley.sourcesolver.resolve.BasicResolver;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.ClassMemberPair;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;
import software.coley.sourcesolver.resolve.result.ClassResolution;
import software.coley.sourcesolver.resolve.result.FieldResolution;
import software.coley.sourcesolver.resolve.result.MethodResolution;
import software.coley.sourcesolver.resolve.result.MultiClassResolution;
import software.coley.sourcesolver.resolve.result.MultiMemberResolution;
import software.coley.sourcesolver.resolve.result.PackageResolution;
import software.coley.sourcesolver.resolve.result.Resolution;
import software.coley.sourcesolver.resolve.result.Resolutions;

import java.util.List;

/**
 * Adapts {@link Resolution} values into our {@link AstResolveResult}.
 *
 * @author Matt Coley
 */
public class ResolverAdapter extends BasicResolver {
	private final Workspace workspace;

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
			ClassPathNode path = workspace.findClass(name);
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
		} else if (resolution instanceof FieldResolution fieldResolution) {
			String ownerName = fieldResolution.getOwnerEntry().getName();
			ClassPathNode ownerPath = workspace.findClass(ownerName);
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
			ClassPathNode ownerPath = workspace.findClass(ownerName);
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
				ClassPathNode path = workspace.findClass(firstClassName);
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
}
