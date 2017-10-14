package me.coley.recaf.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Icons {
    // Class access
    public static final Icon CL_CLASS;
    public static final Icon CL_INTERFACE;
    public static final Icon CL_ENUM;
    public static final Icon CL_ANNOTATION;
    // Field access
    public static final Icon FL_PUBLIC;
    public static final Icon FL_PROTECTED;
    public static final Icon FL_PRIVATE;
    public static final Icon FL_DEFAULT;
    // Method access
    public static final Icon ML_PUBLIC;
    public static final Icon ML_PROTECTED;
    public static final Icon ML_PRIVATE;
    public static final Icon ML_DEFAULT;
    // General modifiers
    public static final Icon MOD_STATIC;
    public static final Icon MOD_VOLATILE;
    public static final Icon MOD_TRANSIENT;
    public static final Icon MOD_SYNTHETIC;
    public static final Icon MOD_NATIVE;
    public static final Icon MOD_ABSTRACT;
    public static final Icon MOD_FINAL;
    // Misc
    public static final Icon MISC_PACKAGE;
    public static final Icon MISC_RESULT;

    static {
        //
        CL_CLASS = load("class.png");
        CL_INTERFACE = load("interface.png");
        CL_ENUM = load("enum.png");
        CL_ANNOTATION = load("annotation.png");
        //
        FL_PUBLIC = load("field_public.png");
        FL_PROTECTED = load("field_protected.png");
        FL_PRIVATE = load("field_private.png");
        FL_DEFAULT = load("field_default.png");
        //
        ML_PUBLIC = load("method_public.png");
        ML_PROTECTED = load("method_protected.png");
        ML_PRIVATE = load("method_private.png");
        ML_DEFAULT = load("method_default.png");
        //
        MOD_STATIC = load("static.png");
        MOD_VOLATILE = load("volatile.png");
        MOD_TRANSIENT = load("transient.png");
        MOD_SYNTHETIC = load("synthetic.png");
        MOD_NATIVE = load("native.png");
        MOD_ABSTRACT = load("abstract.png");
        MOD_FINAL = load("final.png");
        //
        MISC_PACKAGE = load("package.png");
        MISC_RESULT = load("result.png");
    }

    private static Icon load(String url) {
        // TODO: Why does File.separator force non-relative path names but this
        // works fine?
        String prefix = "/resources/";
        String file = prefix + url;
        return new ImageIcon(Icons.class.getResource(file));
    }
}
