package me.coley.recaf.bytecode.analysis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

import me.coley.recaf.Input;
import me.coley.recaf.Logging;

// TODO: Rather than true/false, have detailed fail-cases for users to pinpoint what went wrong.
public class Verify {

	/**
	 * @param method
	 *            The MethodNode to check.
	 * @return Check if this method has passed verification.
	 */
	public static boolean isValid(MethodNode method) {
		try {
			Printer printer = new Textifier();
			TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(printer);
			CheckMethodAdapter check = new CheckMethodAdapter(method.access, method.name, method.desc, traceMethodVisitor,
					new HashMap<>());
			method.accept(check);
			return true;
		} catch (IllegalArgumentException ex) {
			// Thrown by CheckMethodAdapter
		} catch (Exception ex) {
			// Unknown origin
			Logging.error(ex);
		}
		return false;
	}

	/**
	 * @param clazz
	 *            The ClassNode to check.
	 * @return Check if this class has passed verification.
	 */
	public static boolean isValid(ClassNode clazz) {
		StringWriter sw = new StringWriter();
		try {
			PrintWriter printer = new PrintWriter(sw);
			TraceClassVisitor trace = new TraceClassVisitor(printer);
			CheckClassAdapter check = new CheckClassAdapter(trace);
			clazz.accept(check);
			return true;
		} catch (IllegalArgumentException ex) {
			// Thrown by CheckMethodAdapter
		} catch (Exception ex) {
			// Unknown origin
			Logging.error(ex);
		}
		return false;
	}

	/**
	 * @return Check if the current Input instance is valid.
	 */
	public static boolean isValid() {
		try {
			Input in = Input.get();
			for (String name : in.getModifiedClasses()) {
				if (!isValid(in.getClass(name))) {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
