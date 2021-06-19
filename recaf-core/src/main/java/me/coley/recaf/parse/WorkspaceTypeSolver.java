package me.coley.recaf.parse;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import javassist.ClassPool;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory.toTypeDeclaration;

/**
 * Type resolver that uses a Recaf workspace as a classpath.
 *
 * @author Matt Coley
 */
public class WorkspaceTypeSolver implements TypeSolver, WorkspaceListener, ResourceClassListener {
	private static final Logger logger = Logging.get(WorkspaceTypeSolver.class);
	private final Map<String, ResolvedReferenceTypeDeclaration> resolveCache = new HashMap<>();
	private final Set<String> failedResolves = new HashSet<>();
	private final ClassPool classPool = new ClassPool(false);
	private final TypeSolver childSolver = new ReflectionTypeSolver(false);
	private final Workspace workspace;
	private TypeSolver parent;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public WorkspaceTypeSolver(Workspace workspace) {
		this.workspace = workspace;
		// Use the workspace path first, then the system path last
		classPool.appendClassPath(new WorkspaceClassPath(workspace));
		classPool.appendSystemPath();
		// Listener will ensure cache does not de-sync with changes to library states, class updates, etc
		workspace.addListener(this);
	}

	@Override
	public TypeSolver getParent() {
		return this.parent;
	}

	@Override
	public void setParent(TypeSolver parent) {
		this.parent = parent;
	}

	@Override
	public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
		// Fast fail if the name failed to resolve last time
		if (failedResolves.contains(name)){
			return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
		}
		try {
			// JavaParser has no understanding of the difference between
			// a package separator and an inner class separator...
			// I mean, its designed to mimic source-level constructs but this is still disappointing...
			// I would like to not have to have a loop like this here for performance reasons.
			String internal = name.replace('.', '/');
			do {
				ResolvedReferenceTypeDeclaration declaration = findDeclaration(internal);
				if (declaration != null) {
					return SymbolReference.solved(declaration);
				}
				// Not found, try replacing the last package separator with an inner class separator.
				internal = StringUtil.replaceLast(internal, "/", "$");
			} while (internal.indexOf('/') > 0);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to resolve type: " + name, ex);
		}
		SymbolReference<ResolvedReferenceTypeDeclaration> parentSolved = childSolver.tryToSolveType(name);
		if (!parentSolved.isSolved()) {
			failedResolves.add(name);
		}
		return parentSolved;
	}

	private ResolvedReferenceTypeDeclaration findDeclaration(String internal) throws IOException {
		// Check if its been cached
		ResolvedReferenceTypeDeclaration value = resolveCache.get(internal);
		if (value != null)
			return value;
		// Pull from workspace, which should also be able to defer to runtime classes
		ClassInfo info = workspace.getResources().getClass(internal);
		if (info != null) {
			ResolvedReferenceTypeDeclaration dec = toDeclaration(info);
			resolveCache.put(internal, dec);
			return dec;
		}
		// Nothing found
		return null;
	}

	private ResolvedReferenceTypeDeclaration toDeclaration(ClassInfo info) throws IOException {
		InputStream is = new ByteArrayInputStream(info.getValue());
		return toTypeDeclaration(classPool.makeClass(is), getRoot());
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		// Remove failed resolves if the class is defined in the library
		library.getClasses().keySet().forEach(failedResolves::remove);
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		// Classes are removed, so we shouldn't be able to resolve them
		library.getClasses().keySet().forEach(resolveCache::remove);
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		// Remove failed resolves if the class is defined in the library
		failedResolves.remove(newValue.getName());
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		// Class is removed, so we shouldn't be able to resolve it
		resolveCache.remove(oldValue.getName());
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		// Replace the class declaration behind the reference in case there are updates to field or method definitions
		try {
			resolveCache.put(oldValue.getName(), toDeclaration(newValue));
		} catch (IOException ex) {
			logger.error("Failed name to type resolve cache entry for: " + oldValue.getName(), ex);
		}
	}

	/**
	 * @return Javassist class pool used pull class references from.
	 */
	public ClassPool getClassPool() {
		return classPool;
	}

	/**
	 * @return Workspace to pull class references from.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}
}
