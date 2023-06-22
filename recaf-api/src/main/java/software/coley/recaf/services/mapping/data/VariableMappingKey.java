package software.coley.recaf.services.mapping.data;

import java.util.Objects;

/**
 * Mapping key for variable.
 *
 * @author xDark
 */
public class VariableMappingKey extends AbstractMappingKey {
	private final String owner;
	private final String methodName;
	private final String methodDesc;
	private final String variableName;
	private final String variableDesc;

	/**
	 * @param owner
	 * 		Class name.
	 * @param methodName
	 * 		Method name.
	 * @param methodDesc
	 * 		Method descriptor.
	 * @param variableName
	 * 		Variable name.
	 * @param variableDesc
	 * 		Variable descriptor.
	 */
	public VariableMappingKey(String owner, String methodName, String methodDesc, String variableName, String variableDesc) {
		this.owner = owner;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.variableName = variableName;
		this.variableDesc = variableDesc;
	}

	@Override
	protected String toText() {
		String owner = this.owner;
		String methodName = this.methodName;
		String methodDesc = Objects.toString(this.methodDesc);
		String variableName = this.variableName;
		String variableDesc = this.variableDesc;
		StringBuilder builder = new StringBuilder(owner.length() + methodName.length() +
				methodDesc.length() + variableName.length() + 5);
		builder.append(owner).append('\t');
		builder.append(methodName).append('\t');
		builder.append(methodDesc).append('\t');
		builder.append(variableName);
		if (variableDesc != null) {
			builder.append('\t').append(variableDesc);
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof VariableMappingKey)) return false;

		VariableMappingKey that = (VariableMappingKey) o;

		return owner.equals(that.owner) && methodName.equals(that.methodName)
				&& Objects.equals(methodDesc, that.methodDesc)
				&& variableName.equals(that.variableName)
				&& Objects.equals(variableDesc, that.variableDesc);
	}


	@Override
	public int hashCode() {
		int result = owner.hashCode();
		result = 31 * result + methodName.hashCode();
		result = 31 * result + Objects.hashCode(methodDesc);
		result = 31 * result + variableName.hashCode();
		result = 31 * result + Objects.hashCode(variableDesc);
		return result;
	}
}
