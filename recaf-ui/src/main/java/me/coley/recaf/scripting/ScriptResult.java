package me.coley.recaf.scripting;

import bsh.EvalError;
import bsh.ParseException;
import bsh.TargetError;

/**
 * Wrapper for results of {@link ScriptEngine} calls.
 *
 * @author Matt Coley
 */
public class ScriptResult {
	private final Exception exception;
	private final Object result;

	/**
	 * @param result
	 * 		Script result.
	 * @param exception
	 * 		Script failure reason.
	 */
	public ScriptResult(Object result, Exception exception) {
		this.result = result;
		this.exception = exception;
	}

	/**
	 * @return {@code true} when there was no failures reported.
	 */
	public boolean wasSuccess() {
		return getException() == null;
	}

	/**
	 * @return {@code true} when there is an {@link #getException() failure}.
	 */
	public boolean failed() {
		return getException() != null;
	}

	/**
	 * @return {@code true} when there is an {@link #getException() failure} and it was due to an {@link EvalError}.
	 *
	 * @see #getExceptionAsEval()
	 */
	public boolean wasScriptFailure() {
		return getException() instanceof EvalError;
	}

	/**
	 * @return {@code true} when there is an {@link #getException() failure} and it was due to an {@link ParseException}.
	 *
	 * @see #getExceptionAsParse()
	 */
	public boolean wasScriptParseFailure() {
		return getException() instanceof ParseException;
	}

	/**
	 * @return {@code true} when there is an {@link #getException() failure} and it was due to an {@link TargetError}.
	 *
	 * @see #getExceptionAsTarget()
	 */
	public boolean wasScriptTargetFailure() {
		return getException() instanceof TargetError;
	}

	/**
	 * @return Generic failure reason.
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Can only be used when {@link #wasScriptFailure()} is {@code true}.
	 *
	 * @return Failure reason due to BSH failing to parse the script.
	 */
	public EvalError getExceptionAsEval() {
		return (EvalError) exception;
	}

	/**
	 * Can only be used when {@link #wasScriptTargetFailure()} is {@code true}.
	 *
	 * @return Failure reason due to BSH failing to parse the script.
	 */
	public TargetError getExceptionAsTarget() {
		return (TargetError) exception;
	}

	/**
	 * Can only be used when {@link #wasScriptTargetFailure()} is {@code true}.
	 *
	 * @return Failure reason due to BSH failing to parse the script.
	 */
	public ParseException getExceptionAsParse() {
		return (ParseException) exception;
	}

	/**
	 * @return Script result.
	 */
	public Object getResult() {
		return result;
	}
}
