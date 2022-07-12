package me.coley.recaf.parse.jpimpl;

public class ResolveLookupException extends IllegalStateException {
	public ResolveLookupException(String message) {
		super(message);
	}

	public ResolveLookupException(String message, Throwable cause) {
		super(message, cause);
	}
}
