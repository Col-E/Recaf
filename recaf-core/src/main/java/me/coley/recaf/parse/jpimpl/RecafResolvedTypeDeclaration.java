package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.core.resolution.MethodUsageResolutionCapability;
import com.github.javaparser.symbolsolver.javaparsermodel.LambdaArgumentTypePlaceholder;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistTypeParameter;
import com.github.javaparser.symbolsolver.logic.FunctionalInterfaceLogic;
import com.github.javaparser.symbolsolver.logic.MethodResolutionCapability;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.utils.Pair;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.workspace.Workspace;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecafResolvedTypeDeclaration implements ResolvedReferenceTypeDeclaration, ResolvedValueDeclaration,
		MethodResolutionCapability, MethodUsageResolutionCapability {
	protected final WorkspaceTypeSolver typeSolver;
	protected final CommonClassInfo classInfo;

	public RecafResolvedTypeDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo classInfo) {
		this.typeSolver = typeSolver;
		this.classInfo = classInfo;
	}

	public static RecafResolvedTypeDeclaration from(WorkspaceTypeSolver typeSolver, CommonClassInfo info) {
		RecafResolvedTypeDeclaration dec;
		int acc = info.getAccess();
		if (AccessFlag.isEnum(acc))
			dec = new RecafResolvedEnumDeclaration(typeSolver, info);
		else if (AccessFlag.isInterface(acc))
			dec = new RecafResolvedInterfaceDeclaration(typeSolver, info);
		else
			dec = new RecafResolvedClassDeclaration(typeSolver, info);
		return dec;
	}

	public CommonClassInfo getClassInfo() {
		return classInfo;
	}

	@Override
	public boolean isJavaLangObject() {
		return classInfo.getName().equals("java/lang/Object");
	}

	@Override
	public boolean isJavaLangEnum() {
		return classInfo.getName().equals("java/lang/Enum");
	}

	@Override
	public ResolvedType getType() {
		return new RecafResolvedReferenceType(this);
	}

	@Override
	public ResolvedTypeDeclaration asType() {
		return this;
	}

	@Override
	public ResolvedClassDeclaration asClass() {
		return new RecafResolvedClassDeclaration(typeSolver, classInfo);
	}

	@Override
	public ResolvedInterfaceDeclaration asInterface() {
		return new RecafResolvedInterfaceDeclaration(typeSolver, classInfo);
	}

	@Override
	public ResolvedEnumDeclaration asEnum() {
		return new RecafResolvedEnumDeclaration(typeSolver, classInfo);
	}

	@Override
	public ResolvedAnnotationDeclaration asAnnotation() {
		return new RecafResolvedAnnotationDeclaration(typeSolver, classInfo);
	}

	@Override
	public Set<ResolvedReferenceTypeDeclaration> internalTypes() {
		// TODO: support for inner classes
		return Collections.emptySet();
	}

	protected Optional<ResolvedReferenceType> getSuperClass() {
		Workspace workspace = typeSolver.getWorkspace();
		CommonClassInfo parentInfo = workspace.getResources().getClass(classInfo.getSuperName());
		if (parentInfo != null) {
			RecafResolvedTypeDeclaration superDec = RecafResolvedTypeDeclaration.from(typeSolver, parentInfo);
			if (!superDec.isJavaLangObject())
				return Optional.of(new RecafResolvedReferenceType(superDec));
		}
		return Optional.empty();
	}

	protected List<ResolvedReferenceType> getInterfaces() {
		return getInterfacesImpl().stream()
				.map(RecafResolvedReferenceType::new)
				.collect(Collectors.toList());
	}

	private List<RecafResolvedTypeDeclaration> getInterfacesImpl() {
		Workspace workspace = typeSolver.getWorkspace();
		List<RecafResolvedTypeDeclaration> list = new ArrayList<>();
		for (String interfaceName : classInfo.getInterfaces()) {
			CommonClassInfo interfaceInfo = workspace.getResources().getClass(interfaceName);
			if (interfaceInfo != null)
				list.add(new RecafResolvedInterfaceDeclaration(typeSolver, interfaceInfo));
			else
				throw new ResolveLookupException("Cannot resolve interface '" + interfaceName + "' to workspace class");
		}
		return list;
	}

	protected List<ResolvedReferenceType> getAllSuperClasses() {
		Optional<ResolvedReferenceType> superClass = getSuperClass();
		if (superClass.isPresent()) {
			List<ResolvedReferenceType> list = new ArrayList<>();
			while (superClass.isPresent()) {
				ResolvedReferenceType referenceType = superClass.get();
				if (referenceType.isJavaLangObject())
					break;
				if (referenceType instanceof RecafResolvedReferenceType) {
					RecafResolvedReferenceType recafReferenceType = (RecafResolvedReferenceType) referenceType;
					list.add(recafReferenceType);
					superClass = recafReferenceType.getDeclaration().getSuperClass();
				}
			}
			return list;
		}
		return Collections.emptyList();
	}

	protected List<ResolvedReferenceType> getAllInterfaces() {
		Set<RecafResolvedTypeDeclaration> visited = new HashSet<>();
		List<RecafResolvedTypeDeclaration> unvisited = new ArrayList<>();
		unvisited.add(this);
		while (!unvisited.isEmpty()) {
			RecafResolvedTypeDeclaration current = unvisited.remove(0);
			List<RecafResolvedTypeDeclaration> notVisistedInterfaces = current
					.getInterfacesImpl()
					.stream()
					.filter(node -> !visited.contains(node) && unvisited.contains(node))
					.collect(Collectors.toList());
			unvisited.addAll(notVisistedInterfaces);
			visited.add(current);
		}
		visited.remove(this);
		return visited.stream()
				.map(RecafResolvedReferenceType::new)
				.collect(Collectors.toList());
	}

	@Override
	public List<ResolvedReferenceType> getAncestors(boolean acceptIncompleteList) {
		List<ResolvedReferenceType> ancestors = new ArrayList<>(getInterfaces());
		getSuperClass().ifPresent(ancestors::add);
		return ancestors;
	}

	@Override
	public List<ResolvedFieldDeclaration> getDeclaredFields() {
		return classInfo.getFields().stream()
				.map(f -> new RecafResolvedFieldDeclaration(typeSolver, classInfo, f))
				.collect(Collectors.toList());
	}

	@Override
	public List<ResolvedFieldDeclaration> getAllFields() {
		return Stream.concat(getDeclaredFields().stream(),
						getAllAncestors()
								.stream()
								.flatMap(t -> t.getDeclaredFields().stream()))
				.collect(Collectors.toList());
	}

	@Override
	public Set<ResolvedMethodDeclaration> getDeclaredMethods() {
		return classInfo.getMethods().stream()
				.filter(m -> m.getName().charAt(0) != '<')
				.map(m -> new RecafResolvedMethodDeclaration(typeSolver, classInfo, m))
				.collect(Collectors.toSet());
	}

	@Override
	public Set<MethodUsage> getAllMethods() {
		Set<MethodUsage> methods = new HashSet<>();

		Set<String> methodsSignatures = new HashSet<>();

		for (ResolvedMethodDeclaration methodDeclaration : getDeclaredMethods()) {
			MethodUsage methodUsage = createMethodUsage(methodDeclaration);
			methods.add(methodUsage);
			methodsSignatures.add(methodUsage.getSignature());
		}

		for (ResolvedReferenceType ancestor : getAllAncestors()) {
			List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParametersMap = ancestor.getTypeParametersMap();
			for (MethodUsage mu : ancestor.getDeclaredMethods()) {
				// replace type parameters to be able to filter away overridden generified methods
				MethodUsage methodUsage = mu;
				for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> p : typeParametersMap) {
					methodUsage = methodUsage.replaceTypeParameter(p.a, p.b);
				}
				String signature = methodUsage.getSignature();
				if (!methodsSignatures.contains(signature)) {
					methodsSignatures.add(signature);
					methods.add(mu);
				}
			}
		}

		return methods;
	}

	@Override
	public boolean isAssignableBy(ResolvedType type) {
		if (type.isNull())
			return true;
		if (type instanceof LambdaArgumentTypePlaceholder)
			return isFunctionalInterface();
		if (!type.isReferenceType())
			return false;
		String qualifiedName = type.asReferenceType().getQualifiedName();
		return isAssignableBy(qualifiedName);
	}


	@Override
	public boolean isAssignableBy(ResolvedReferenceTypeDeclaration other) {
		return isAssignableBy(other.getQualifiedName());
	}

	private boolean isAssignableBy(String qualifiedName) {
		return Stream.concat(
						Stream.of(getQualifiedName()),
						getAncestors(false).stream()
								.map(ResolvedReferenceType::getQualifiedName))
				.anyMatch(name -> name.equals(qualifiedName));
	}

	@Override
	public boolean hasDirectlyAnnotation(String qualifiedName) {
		// Need to visit the bytecode to determine this, and we don't use this, so it is not worth the effort.
		return false;
	}

	@Override
	public boolean isFunctionalInterface() {
		return FunctionalInterfaceLogic.getFunctionalMethod(this).isPresent();
	}

	@Override
	public boolean isInterface() {
		return AccessFlag.isInterface(classInfo.getAccess());
	}

	@Override
	public boolean isEnum() {
		return AccessFlag.isEnum(classInfo.getAccess());
	}

	@Override
	public boolean isAnnotation() {
		return AccessFlag.isAnnotation(classInfo.getAccess());
	}

	@Override
	public List<ResolvedConstructorDeclaration> getConstructors() {
		return classInfo.getMethods().stream()
				.filter(m -> m.getName().charAt(0) != '<' && m.getName().charAt(1) == 'i')
				.map(m -> new RecafResolvedConstructorDeclaration(typeSolver, classInfo, m))
				.collect(Collectors.toList());
	}

	protected AccessSpecifier accessSpecifier() {
		int acc = classInfo.getAccess();
		if (AccessFlag.isPublic(acc))
			return AccessSpecifier.PUBLIC;
		else if (AccessFlag.isProtected(acc))
			return AccessSpecifier.PROTECTED;
		else if (AccessFlag.isPrivate(acc))
			return AccessSpecifier.PRIVATE;
		return AccessSpecifier.PACKAGE_PRIVATE;
	}

	@Override
	public Optional<ResolvedReferenceTypeDeclaration> containerType() {
		// Can add the 'outer-class' attribute to CommonClassInfo in order to compute this if needed
		return Optional.empty();
	}

	@Override
	public String getPackageName() {
		String name = classInfo.getName().replace('/', '.');
		return name.substring(0, name.lastIndexOf('.'));
	}

	@Override
	public String getClassName() {
		return getQualifiedName();
	}

	@Override
	public String getQualifiedName() {
		return classInfo.getName().replace('/', '.');
	}

	@Override
	public String getName() {
		return classInfo.getName().substring(classInfo.getName().lastIndexOf('/') + 1);
	}

	@Override
	public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
		String signature = classInfo.getSignature();
		if (signature == null)
			return Collections.emptyList();
		else {
			// TODO: Cache and/or switch to ASM
			try {
				SignatureAttribute.ClassSignature classSignature =
						SignatureAttribute.toClassSignature(signature);
				return Arrays.stream(classSignature.getParameters())
						.map((tp) -> new JavassistTypeParameter(tp, this, typeSolver))
						.collect(Collectors.toList());
			} catch (Exception ex) {
				return Collections.emptyList();
			}
		}
	}

	@Override
	public SymbolReference<ResolvedMethodDeclaration> solveMethod(String name, List<ResolvedType> argumentsTypes, boolean staticOnly) {
		// TODO: Iteratively look backwards instead of requesting all methods of all ancestors immediately
		Set<ResolvedMethodDeclaration> methodSet =
				Stream.concat(getAllAncestors().stream()
										.filter(a -> a.getTypeDeclaration().isPresent())
										.map(a -> a.getTypeDeclaration().get()).flatMap(t -> t.getDeclaredMethods().stream()),
								getDeclaredMethods().stream())
						.filter(m -> m.getName().equals(name) && (!staticOnly || m.isStatic()))
						.filter(m -> m.declaringType().equals(this) || m.accessSpecifier() != AccessSpecifier.PRIVATE)
						.filter(m -> m.getNumberOfParams() == argumentsTypes.size())
						.collect(Collectors.toSet());
		// Only one result, that should be it
		if (methodSet.size() == 1)
			return SymbolReference.solved(methodSet.iterator().next());
		// Multiple results, need to compare arguments
		for (ResolvedMethodDeclaration methodDeclaration : methodSet) {
			int params = methodDeclaration.getNumberOfParams();
			boolean argMatch = true;
			for (int p = 0; p < params; p++) {
				// Get the argument type (and strip if of type arguments so that JavaParser doesn't complain)
				ResolvedType argumentType = argumentsTypes.get(p);
				/*
				if (argumentType instanceof ResolvedReferenceType) {
					ResolvedReferenceTypeDeclaration typeDeclaration =
							((ResolvedReferenceType) argumentType).getTypeDeclaration().get();
					argumentType = new ReferenceTypeImpl(typeDeclaration, Collections.emptyList(), typeSolver);
				}*/
				// Compare our method's parameter against the given argument type
				ResolvedParameterDeclaration param = methodDeclaration.getParam(p);
				ResolvedType paramType = param.getType();
				// TODO: Need to resolve lambda placeholders
				if (argumentType instanceof LambdaArgumentTypePlaceholder)
					continue;
				if (paramType instanceof ResolvedReferenceType) {
					ResolvedReferenceTypeDeclaration typeDeclaration =
							((ResolvedReferenceType) paramType).getTypeDeclaration().get();
					if (!typeDeclaration.isAssignableBy(argumentType)) {
						argMatch = false;
						break;
					}
				} else if (!param.getType().isAssignableBy(argumentType)) {
					argMatch = false;
					break;
				}
			}
			// This method's arguments matched
			if (argMatch)
				return SymbolReference.solved(methodDeclaration);
		}
		// No match
		return SymbolReference.unsolved(ResolvedMethodDeclaration.class);
	}

	@Override
	public Optional<MethodUsage> solveMethodAsUsage(String name, List<ResolvedType> argumentTypes, Context invocationContext, List<ResolvedType> typeParameters) {
		SymbolReference<ResolvedMethodDeclaration> solved = solveMethod(name, argumentTypes, false);
		if (solved.isSolved()) {
			ResolvedMethodDeclaration methodDeclaration = solved.getCorrespondingDeclaration();
			MethodUsage methodUsage = createMethodUsage(methodDeclaration);
			return Optional.of(methodUsage);
		} else
			return Optional.empty();
	}

	private MethodUsage createMethodUsage(ResolvedMethodDeclaration methodDeclaration) {
		// TODO: This seems like the wrong way to populate type parameters.
		//  Other impls don't have to do this. Where are they doing that?
		MethodUsage methodUsage = new MethodUsage(methodDeclaration);
		for (int i = 0; i < methodDeclaration.getTypeParameters().size(); i++) {
			ResolvedTypeParameterDeclaration parameterDeclaration = methodDeclaration.getTypeParameters().get(i);
			ResolvedReferenceTypeDeclaration t;
			if (parameterDeclaration.isClass() || parameterDeclaration.isInterface() || parameterDeclaration.isAnnotation())
				t = parameterDeclaration.asReferenceType();
			else
				t = parameterDeclaration.getLowerBound().asReferenceType().getTypeDeclaration().get();
			ResolvedType tpToReplace = new ReferenceTypeImpl(t, typeSolver);
			methodUsage = methodUsage.replaceTypeParameter(parameterDeclaration, tpToReplace);
		}
		return methodUsage;
	}

	// TODO: Implement 'SymbolResolutionCapability' when PR is merged
	// @Override
	public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name, TypeSolver typeSolver) {
		// TODO: Iteratively look backwards instead of requesting all fields of all ancestors immediately
		Optional<ResolvedFieldDeclaration> first = Stream.concat(getAllAncestors().stream()
								.flatMap(t -> t.getDeclaredFields().stream()),
						getDeclaredFields().stream())
				.filter(f -> f.getName().equals(name))
				.findFirst();
		if (first.isPresent())
			return SymbolReference.solved(first.get());
		return SymbolReference.unsolved(ResolvedValueDeclaration.class);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o.getClass() == RecafResolvedTypeDeclaration.class) {
			RecafResolvedTypeDeclaration that = (RecafResolvedTypeDeclaration) o;
			return classInfo.equals(that.classInfo);
		} else if (o instanceof ResolvedClassDeclaration) {
			return super.equals(o);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return classInfo.hashCode();
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
