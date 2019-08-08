package me.coley.recaf.workspace;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import java.io.*;

import static com.github.javaparser.symbolsolver.javassistmodel.JavassistFactory.toTypeDeclaration;

public class WorkspaceTypeResolver implements TypeSolver {
	private final ClassPool classPool = new ClassPool(false);
	private Workspace workspace;
	private TypeSolver parent;

	public WorkspaceTypeResolver(Workspace workspace) {
		this.workspace = workspace;
		classPool.appendSystemPath();
		for (String name : workspace.getClassNames())
			classPool.insertClassPath(new ByteArrayClassPath(name, workspace.getRawClass(name)));
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
			if(workspace.hasClass(name)) {
				InputStream is = new ByteArrayInputStream(workspace.getRawClass(name));
				ResolvedReferenceTypeDeclaration dec = toTypeDeclaration(classPool.makeClass(is), getRoot());
				return SymbolReference.solved(dec);
			}
		} catch(IOException ex) {
			throw new IllegalStateException("Failed to resolve type: " + name, ex);
		}
		return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
	}
}
