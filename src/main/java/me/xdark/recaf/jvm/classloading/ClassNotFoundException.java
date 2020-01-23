package me.xdark.recaf.jvm.classloading;

import me.xdark.recaf.jvm.VMException;

public class ClassNotFoundException extends VMException {

	public ClassNotFoundException() { }

	public ClassNotFoundException(String message) {
		super(message);
	}

	public ClassNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClassNotFoundException(Throwable cause) {
		super(cause);
	}

	public ClassNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
