package me.coley.recaf.parse.source;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import me.coley.recaf.workspace.Workspace;

import java.io.*;

import static com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory.toTypeDeclaration;

/**
 * Type resolver that uses a Recaf workspace as a classpath.
 *
 * @author Matt
 */
public class WorkspaceTypeResolver implements TypeSolver {
	private final TypeSolver childSolver = new ReflectionTypeSolver();
	private final ClassPool classPool = new ClassPool(false);
	private Workspace workspace;
	private TypeSolver parent;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public WorkspaceTypeResolver(Workspace workspace) {
		this.workspace = workspace;
		classPool.appendSystemPath();
		for (String name : workspace.getClassNames())
			classPool.insertClassPath(new ByteArrayClassPath(name.replace("/", "."), workspace.getRawClass(name)));
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
			String internal = name.replace(".", "/");
			if(workspace.hasClass(internal)) {
				InputStream is = new ByteArrayInputStream(workspace.getRawClass(internal));
				ResolvedReferenceTypeDeclaration dec = toTypeDeclaration(classPool.makeClass(is), getRoot());
				return SymbolReference.solved(dec);
			}
		} catch(IOException ex) {
			throw new IllegalStateException("Failed to resolve type: " + name, ex);
		}
		return childSolver.tryToSolveType(name);
	}
}
