package software.coley.recaf.ui.control.richtext.suggest.java.lookups;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.ui.control.richtext.suggest.java.ScopedVariable;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.ErroneousModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.Model;
import software.coley.sourcesolver.model.VariableModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lookup for variables visible in the local scope of a given position.
 *
 * @author Matt Coley
 */
public final class LocalScopeLookup {
	/**
	 * Collect variables visible at the given position.
	 *
	 * @param unit
	 * 		The compilation unit to search.
	 * @param position
	 * 		The position within the AST to find variables for.
	 *
	 * @return Variables visible at the given position, including method parameters and local variables.
	 * Variables are ordered by declaration order, with parameters appearing before local variables.
	 */
	@Nonnull
	public List<ScopedVariable> collectVisibleVariables(@Nonnull CompilationUnitModel unit, int position) {
		Model leaf = findDeepestModelAt(unit, position);

		// Need to operate from a method scope to find parameters, so find the containing method first.
		MethodModel containingMethod = leaf instanceof MethodModel method ? method : leaf.getParentOfType(MethodModel.class);
		if (containingMethod == null)
			return List.of();

		// Check parameters first since they are always visible in the method scope, even if declared after the position.
		Map<String, ScopedVariable> variables = new LinkedHashMap<>(); // Linked to preserve declaration order.
		for (VariableModel parameter : containingMethod.getParameters()) {
			if (parameter.getRange().begin() <= position)
				variables.putIfAbsent(parameter.getName(), new ScopedVariable(parameter.getName(), true));
		}

		// Check local variables in the scope, but skip parameters since they were already checked.
		Set<VariableModel> parameters = new HashSet<>(containingMethod.getParameters());
		Model scope = leaf;
		while (scope != null && scope != containingMethod.getParent()) {
			for (VariableModel variable : scope.getRecursiveChildrenOfType(VariableModel.class)) {
				if (parameters.contains(variable))
					continue;
				if (variable.getRange().end() <= position)
					variables.putIfAbsent(variable.getName(), new ScopedVariable(variable.getName(), false));
			}
			scope = scope.getParent();
		}

		return new ArrayList<>(variables.values());
	}

	/**
	 * Find a variable with the given name that is visible at the given position. This includes method parameters and local variables.
	 *
	 * @param unit
	 * 		The compilation unit to search.
	 * @param position
	 * 		The position within the AST to find the variable for.
	 * @param name
	 * 		The name of the variable to find.
	 *
	 * @return The variable with the given name that is visible at the given position, or {@code null} if no such variable exists.
	 * If multiple variables with the same name are visible, the one declared closest to the given position is returned.
	 */
	@Nullable
	public VariableModel findVisibleVariable(@Nonnull CompilationUnitModel unit, int position, @Nonnull String name) {
		Model leaf = findDeepestModelAt(unit, position);

		// Need to operate from a method scope to find parameters, so find the containing method first.
		MethodModel containingMethod = leaf instanceof MethodModel method ? method : leaf.getParentOfType(MethodModel.class);
		if (containingMethod == null)
			return null;

		// Check parameters first since they are always visible in the method scope, even if declared after the position.
		for (VariableModel parameter : containingMethod.getParameters())
			if (parameter.getRange().begin() <= position && parameter.getName().equals(name))
				return parameter;

		// Check local variables in the scope, but skip parameters since they were already checked.
		Set<VariableModel> parameters = new HashSet<>(containingMethod.getParameters());
		Model scope = leaf;
		while (scope != null && scope != containingMethod.getParent()) {
			for (VariableModel variable : scope.getRecursiveChildrenOfType(VariableModel.class)) {
				if (parameters.contains(variable))
					continue;
				if (variable.getRange().end() <= position && variable.getName().equals(name))
					return variable;
			}
			scope = scope.getParent();
		}

		return null;
	}

	/**
	 * @param model
	 * 		Model to start searching from.
	 * @param position
	 * 		Position to find the deepest model for.
	 *
	 * @return The deepest model in the AST that contains the given position.
	 * If the position is not contained in any child models, the given model is returned.
	 */
	@Nonnull
	public Model findDeepestModelAt(@Nonnull Model model, int position) {
		Model current = model;
		while (true) {
			Model child = current.getChildAtPosition(position);
			if (child == null || child instanceof ErroneousModel)
				return current;
			current = child;
		}
	}
}
