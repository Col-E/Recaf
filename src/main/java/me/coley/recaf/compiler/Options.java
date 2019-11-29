package me.coley.recaf.compiler;

/**
 * Compiler options.
 *
 * @author Matt
 */
public class Options {
	/**
	 * Include variable data in compilation.
	 */
	public boolean variables;
	/**
	 * Include line numbers in compilation.
	 */
	public boolean lineNumbers;
	/**
	 * Include source file name in compilation.
	 */
	public boolean sourceName;
	/**
	 * Class version to target.
	 */
	private TargetVersion target = TargetVersion.V8;

	/**
	 * @return Class version to target.
	 */
	public TargetVersion getTarget() {
		return target;
	}

	/**
	 * @param target
	 * 		Targeted version.
	 */
	public void setTarget(TargetVersion target) {
		this.target = target;
	}

	/**
	 * @return Javac argument format.
	 */
	public String toOption() {
		// generate options
		String options = getOptions();
		if(options.length() > 0) {
			return "-g:" + options;
		}
		// default to none
		return "-g:none";
	}

	private String getOptions() {
		StringBuilder s = new StringBuilder();
		if(variables)
			s.append("vars,");
		if(lineNumbers)
			s.append("lines,");
		if(sourceName)
			s.append("source");
		// substr off dangling comma
		String value = s.toString();
		if(value.endsWith(",")) {
			value = s.substring(0, value.length() - 1);
		}
		return value;
	}
}