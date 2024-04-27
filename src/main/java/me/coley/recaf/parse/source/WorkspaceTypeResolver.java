package me.coley.recaf.parse.source;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import javassist.*;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.*;

import static com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory.toTypeDeclaration;

/**
 * Type resolver that uses a Recaf workspace as a classpath.
 *
 * @author Matt
 */
public class WorkspaceTypeResolver implements TypeSolver {
	private final TypeSolver childSolver = new ReflectionTypeSolver(false);
	private final ClassPool classPool = new ClassPool(false);
	private Workspace workspace;
	private TypeSolver parent;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public WorkspaceTypeResolver(Workspace workspace) {
		this.workspace = workspace;
		classPool.appendClassPath(new WorkspaceClassPath(workspace));
		classPool.appendSystemPath();
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
		try {
			// The default resolve seems to infinite loop on Object, but this doesn't.
			// IDK, JavaParser is weird.
			if (name.equals("java.lang.Object"))
				return SymbolReference.solved(new ReflectionClassDeclaration(Object.class, getRoot()));
			// JavaParser has no understanding of the difference between
			// a package separator and an inner class separator...
			// I mean, its designed to mimic source-level constructs but this is still disappointing...
			// I would like to not have to have a loop like this here for performance reasons.
			String internal = name.replace('.','/');
			do {
				if (workspace.hasClass(internal)) {
					InputStream is = new ByteArrayInputStream(workspace.getRawClass(internal));
					ResolvedReferenceTypeDeclaration dec = toTypeDeclaration(classPool.makeClass(is), getRoot());
					return SymbolReference.solved(dec);
				} else {
					internal = StringUtil.replaceLast(internal, "/", "$");
				}
			} while (internal.indexOf('/') > 0);
		} catch(IOException ex) {
			throw new IllegalStateException("Failed to resolve type: " + name, ex);
		}
		return childSolver.tryToSolveType(name);
	}
}
