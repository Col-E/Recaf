lexer grammar BytecodeTokens;

KW_DEFAULT  : 'Default' | 'default' | 'dflt' ;
KW_HANDLE   : 'Handle' | 'handle' ;
KW_ARGS     : 'Args' | 'args' ;
KW_RANGE    : 'Range' | 'range' ;
KW_MAPPING  : 'Mapping' | 'mapping' | 'map' ;
KW_OFFSETS  : 'Offsets' | 'offsets' ;

MOD_PUBLIC       : 'public'          | 'PUBLIC' ;
MOD_PRIVATE      : 'private'         | 'PRIVATE' ;
MOD_PROTECTED    : 'protected'       | 'PROTECTED' ;
MOD_STATIC       : 'static'          | 'STATIC' ;
MOD_FINAL        : 'final'           | 'FINAL' ;
MOD_SYNCHRONIZED : 'synchronized'    | 'SYNCHRONIZED' ;
MOD_SUPER        : 'super'           | 'SUPER' ;
MOD_BRIDGE       : 'bridge'          | 'BRIDGE' ;
MOD_VOLATILE     : 'volatile'        | 'VOLATILE' ;
MOD_VARARGS      : 'varargs'         | 'VARARGS' ;
MOD_TRANSIENT    : 'transient'       | 'TRANSIENT' ;
MOD_NATIVE       : 'native'          | 'NATIVE' ;
MOD_INTERFACE    : 'interface'       | 'INTERFACE' ;
MOD_ABSTRACT     : 'abstract'        | 'ABSTRACT' ;
MOD_STRICTFP     : 'strictfp'        | 'STRICTFP' ;
MOD_SYNTHETIC    : 'synthetic'       | 'SYNTHETIC' ;
MOD_ANNOTATION   : 'annotation'      | 'ANNOTATION' ;
MOD_ENUM         : 'enum'            | 'ENUM' ;
MOD_MODULE       : 'module'          | 'MODULE' ;
MOD_MANDATED     : 'mandated'        | 'MANDATED' ;
LINE             : 'line'            | 'LINE' ;
NOP              : 'nop'             | 'NOP' ;
ACONST_NULL      : 'aconst_null'     | 'ACONST_NULL' ;
ICONST_M1        : 'iconst_M1'       | 'ICONST_M1' ;
ICONST_0         : 'iconst_0'        | 'ICONST_0' ;
ICONST_1         : 'iconst_1'        | 'ICONST_1' ;
ICONST_2         : 'iconst_2'        | 'ICONST_2' ;
ICONST_3         : 'iconst_3'        | 'ICONST_3' ;
ICONST_4         : 'iconst_4'        | 'ICONST_4' ;
ICONST_5         : 'iconst_5'        | 'ICONST_5' ;
LCONST_0         : 'lconst_0'        | 'LCONST_0' ;
LCONST_1         : 'lconst_1'        | 'LCONST_1' ;
FCONST_0         : 'fconst_0'        | 'FCONST_0' ;
FCONST_1         : 'fconst_1'        | 'FCONST_1' ;
FCONST_2         : 'fconst_2'        | 'FCONST_2' ;
DCONST_0         : 'dconst_0'        | 'DCONST_0' ;
DCONST_1         : 'dconst_1'        | 'DCONST_1' ;
BIPUSH           : 'bipush'          | 'BIPUSH' ;
SIPUSH           : 'sipush'          | 'SIPUSH' ;
LDC              : 'ldc'             | 'LDC' ;
ILOAD            : 'iload'           | 'ILOAD' ;
LLOAD            : 'lload'           | 'LLOAD' ;
FLOAD            : 'fload'           | 'FLOAD' ;
DLOAD            : 'dload'           | 'DLOAD' ;
ALOAD            : 'aload'           | 'ALOAD' ;
IALOAD           : 'iaload'          | 'IALOAD' ;
LALOAD           : 'laload'          | 'LALOAD' ;
FALOAD           : 'faload'          | 'FALOAD' ;
DALOAD           : 'daload'          | 'DALOAD' ;
AALOAD           : 'aaload'          | 'AALOAD' ;
BALOAD           : 'baload'          | 'BALOAD' ;
CALOAD           : 'caload'          | 'CALOAD' ;
SALOAD           : 'saload'          | 'SALOAD' ;
ISTORE           : 'istore'          | 'ISTORE' ;
LSTORE           : 'lstore'          | 'LSTORE' ;
FSTORE           : 'fstore'          | 'FSTORE' ;
DSTORE           : 'dstore'          | 'DSTORE' ;
ASTORE           : 'astore'          | 'ASTORE' ;
IASTORE          : 'iastore'         | 'IASTORE' ;
LASTORE          : 'lastore'         | 'LASTORE' ;
FASTORE          : 'fastore'         | 'FASTORE' ;
DASTORE          : 'dastore'         | 'DASTORE' ;
AASTORE          : 'aastore'         | 'AASTORE' ;
BASTORE          : 'bastore'         | 'BASTORE' ;
CASTORE          : 'castore'         | 'CASTORE' ;
SASTORE          : 'sastore'         | 'SASTORE' ;
POP              : 'pop'             | 'POP' ;
POP2             : 'pop2'            | 'POP2' ;
DUP              : 'dup'             | 'DUP' ;
DUP_X1           : 'dup_x1'          | 'DUP_X1' ;
DUP_X2           : 'dup_x2'          | 'DUP_X2' ;
DUP2             : 'dup2'            | 'DUP2' ;
DUP2_X1          : 'dup2_x1'         | 'DUP2_X1' ;
DUP2_X2          : 'dup2_x2'         | 'DUP2_X2' ;
SWAP             : 'swap'            | 'SWAP' ;
IADD             : 'iadd'            | 'IADD' ;
LADD             : 'ladd'            | 'LADD' ;
FADD             : 'fadd'            | 'FADD' ;
DADD             : 'dadd'            | 'DADD' ;
ISUB             : 'isub'            | 'ISUB' ;
LSUB             : 'lsub'            | 'LSUB' ;
FSUB             : 'fsub'            | 'FSUB' ;
DSUB             : 'dsub'            | 'DSUB' ;
IMUL             : 'imul'            | 'IMUL' ;
LMUL             : 'lmul'            | 'LMUL' ;
FMUL             : 'fmul'            | 'FMUL' ;
DMUL             : 'dmul'            | 'DMUL' ;
IDIV             : 'idiv'            | 'IDIV' ;
LDIV             : 'ldiv'            | 'LDIV' ;
FDIV             : 'fdiv'            | 'FDIV' ;
DDIV             : 'ddiv'            | 'DDIV' ;
IREM             : 'irem'            | 'IREM' ;
LREM             : 'lrem'            | 'LREM' ;
FREM             : 'frem'            | 'FREM' ;
DREM             : 'drem'            | 'DREM' ;
INEG             : 'ineg'            | 'INEG' ;
LNEG             : 'lneg'            | 'LNEG' ;
FNEG             : 'fneg'            | 'FNEG' ;
DNEG             : 'dneg'            | 'DNEG' ;
ISHL             : 'ishl'            | 'ISHL' ;
LSHL             : 'lshl'            | 'LSHL' ;
ISHR             : 'ishr'            | 'ISHR' ;
LSHR             : 'lshr'            | 'LSHR' ;
IUSHR            : 'iushr'           | 'IUSHR' ;
LUSHR            : 'lushr'           | 'LUSHR' ;
IAND             : 'iand'            | 'IAND' ;
LAND             : 'land'            | 'LAND' ;
IOR              : 'ior'             | 'IOR' ;
LOR              : 'lor'             | 'LOR' ;
IXOR             : 'ixor'            | 'IXOR' ;
LXOR             : 'lxor'            | 'LXOR' ;
IINC             : 'iinc'            | 'IINC' ;
I2L              : 'i2l'             | 'I2L' ;
I2F              : 'i2f'             | 'I2F' ;
I2D              : 'i2d'             | 'I2D' ;
L2I              : 'l2i'             | 'L2I' ;
L2F              : 'l2f'             | 'L2F' ;
L2D              : 'l2d'             | 'L2D' ;
F2I              : 'f2i'             | 'F2I' ;
F2L              : 'f2l'             | 'F2L' ;
F2D              : 'f2d'             | 'F2D' ;
D2I              : 'd2i'             | 'D2I' ;
D2L              : 'd2l'             | 'D2L' ;
D2F              : 'd2f'             | 'D2F' ;
I2B              : 'i2b'             | 'I2B' ;
I2C              : 'i2c'             | 'I2C' ;
I2S              : 'i2s'             | 'I2S' ;
LCMP             : 'lcmp '           | 'LCMP' ;
FCMPL            : 'fcmpl'           | 'FCMPL' ;
FCMPG            : 'fcmpg'           | 'FCMPG' ;
DCMPL            : 'dcmpl'           | 'DCMPL' ;
DCMPG            : 'dcmpg'           | 'DCMPG' ;
IFEQ             : 'ifeq'            | 'IFEQ' ;
IFNE             : 'ifne'            | 'IFNE' ;
IFLT             : 'iflt'            | 'IFLT' ;
IFGE             : 'ifge'            | 'IFGE' ;
IFGT             : 'ifgt'            | 'IFGT' ;
IFLE             : 'ifle'            | 'IFLE' ;
IF_ICMPEQ        : 'if_icmpeq'       | 'IF_ICMPEQ' ;
IF_ICMPNE        : 'if_icmpne'       | 'IF_ICMPNE' ;
IF_ICMPLT        : 'if_icmplt'       | 'IF_ICMPLT' ;
IF_ICMPGE        : 'if_icmpge'       | 'IF_ICMPGE' ;
IF_ICMPGT        : 'if_icmpgt'       | 'IF_ICMPGT' ;
IF_ICMPLE        : 'if_icmple'       | 'IF_ICMPLE' ;
IF_ACMPEQ        : 'if_acmpeq'       | 'IF_ACMPEQ' ;
IF_ACMPNE        : 'if_acmpne'       | 'IF_ACMPNE' ;
GOTO             : 'goto'            | 'GOTO' ;
JSR              : 'jsr'             | 'JSR' ;
RET              : 'ret'             | 'RET' ;
TABLESWITCH      : 'tableswitch'     | 'TABLESWITCH' ;
LOOKUPSWITCH     : 'lookupswitch'    | 'LOOKUPSWITCH' ;
IRETURN          : 'ireturn'         | 'IRETURN' ;
LRETURN          : 'lreturn'         | 'LRETURN' ;
FRETURN          : 'freturn'         | 'FRETURN' ;
DRETURN          : 'dreturn'         | 'DRETURN' ;
ARETURN          : 'areturn'         | 'ARETURN' ;
RETURN           : 'return'          | 'RETURN' ;
GETSTATIC        : 'getstatic'       | 'GETSTATIC' ;
PUTSTATIC        : 'putstatic'       | 'PUTSTATIC' ;
GETFIELD         : 'getfield'        | 'GETFIELD' ;
PUTFIELD         : 'putfield'        | 'PUTFIELD' ;
INVOKEVIRTUAL    : 'invokevirtual'   | 'INVOKEVIRTUAL' ;
INVOKESPECIAL    : 'invokespecial'   | 'INVOKESPECIAL' ;
INVOKESTATIC     : 'invokestatic'    | 'INVOKESTATIC' ;
INVOKEINTERFACE  : 'invokeinterface' | 'INVOKEINTERFACE' ;
INVOKEDYNAMIC    : 'invokedynamic'   | 'INVOKEDYNAMIC' ;
NEW              : 'new'             | 'NEW' ;
NEWARRAY         : 'newarray'        | 'NEWARRAY' ;
ANEWARRAY        : 'anewarray'       | 'ANEWARRAY' ;
ARRAYLENGTH      : 'arraylength'     | 'ARRAYLENGTH' ;
ATHROW           : 'athrow'          | 'ATHROW' ;
CHECKCAST        : 'checkcast'       | 'CHECKCAST' ;
INSTANCEOF       : 'instanceof'      | 'INSTANCEOF' ;
MONITORENTER     : 'monitorenter'    | 'MONITORENTER' ;
MONITOREXIT      : 'monitorexit'     | 'MONITOREXIT' ;
MULTIANEWARRAY   : 'multianewarray'  | 'MULTIANEWARRAY' ;
IFNULL           : 'ifnull'          | 'IFNULL' ;
IFNONNULL        : 'ifnonnull'       | 'IFNONNULL' ;

H_GETFIELD         : 'h_getfield'         | 'H_GETFIELD' ;
H_GETSTATIC        : 'h_getstatic'        | 'H_GETSTATIC' ;
H_PUTFIELD         : 'h_putfield'         | 'H_PUTFIELD' ;
H_PUTSTATIC        : 'h_putstatic'        | 'H_PUTSTATIC' ;
H_INVOKEVIRTUAL    : 'h_invokevirtual'    | 'H_INVOKEVIRTUAL' ;
H_INVOKESTATIC     : 'h_invokestatic'     | 'H_INVOKESTATIC' ;
H_INVOKESPECIAL    : 'h_invokespecial'    | 'H_INVOKESPECIAL' ;
H_NEWINVOKESPECIAL : 'h_newinvokespecial' | 'H_NEWINVOKESPECIAL' ;
H_INVOKEINTERFACE  : 'h_invokeinterface'  | 'H_INVOKEINTERFACE' ;

TRY        : 'try'          | 'TRY' ;
CATCH      : 'catch'        | 'CATCH' ;
THROWS     : 'throws'       | 'THROWS' ;
VALUE      : 'value'        | 'VALUE'  ;
SIGNATURE  : 'signature'    | 'SIGNATURE' ;

VISIBLE_ANNOTATION        : 'visible_annotation'          | 'VISIBLE_ANNOTATION' ;
INVISIBLE_ANNOTATION      : 'invisible_annotation'        | 'INVISIBLE_ANNOTATION' ;
VISIBLE_TYPE_ANNOTATION   : 'visible_type_annotation'     | 'VISIBLE_TYPE_ANNOTATION' ;
INVISIBLE_TYPE_ANNOTATION : 'invisible_type_annotation'   | 'INVISIBLE_TYPE_ANNOTATION' ;


INTEGER_LITERAL       : '-'? DEC_DIGIT + LONG_TYPE_SUFFIX? ;
HEX_LITERAL           : '0' ('x' | 'X') HEX_DIGIT + LONG_TYPE_SUFFIX? ;
CHARACTER_LITERAL     : '\'' (ESCAPE_SEQUENCE | ~ ('\'' | '\\')) '\'' ;
STRING_LITERAL        : '"' (ESCAPE_SEQUENCE | ~ ('\\' | '"'))* '"' ;
GREEDY_STRING_LITERAL : '"' ~ ('\r' | '\n')* '"' ;
FLOATING_PT_LITERAL
    : '-'? DEC_DIGIT + DOT DEC_DIGIT* FLOAT_TYPE_SUFFIX?
    | '-'? DOT DEC_DIGIT + FLOAT_TYPE_SUFFIX?
    | '-'? DEC_DIGIT + FLOAT_TYPE_SUFFIX?
    | '-'? DEC_DIGIT + FLOAT_TYPE_SUFFIX
    ;
BOOLEAN_LITERAL
    : 'TRUE' | 'true'
    | 'FALSE' | 'false'
    ;

CTOR           : '<init>' ;
STATIC_CTOR    : '<clinit>' ;

COMMENT_PRFIX       : NAME_SEPARATOR NAME_SEPARATOR NAME_SEPARATOR*;
WHITESPACE          : (SPACE | NEWLINES | TAB) -> skip ;
NEWLINES            : CARRIAGE_RET | NEWLINE ;
STAR                : '*'  ;
MULTILINE_COMMENT   : '/*' .*? '*/' ;
LINE_COMMENT        : COMMENT_PRFIX ~ ('\n' | '\r')* ('\r'? '\n' | EOF) ;
NAME_SEPARATOR      : '/'  ;
SEMICOLON           : ';'  ;
COLON               : ':'  ;
SPACE               : ' '  ;
COMMA               : ','  ;
EQUALS              : '='  ;
CARRIAGE_RET        : '\r' ;
NEWLINE             : '\n' ;
TAB                 : '\t' ;
L_BRACE             : '{'  ;
R_BRACE             : '}'  ;
L_BRACKET           : '['  ;
R_BRACKET           : ']'  ;
L_PAREN             : '('  ;
R_PAREN             : ')'  ;
L_ANGLE             : '<'  ;
R_ANGLE             : '>'  ;
DOT                 : '.'  ;

BASE_NAME             : (UNICODE_ESCAPE | LETTER_OR_DIGIT | (LETTER LETTER_OR_DIGIT+))+ ;

fragment DEC_DIGIT         : ('0' .. '9') ;
fragment HEX_DIGIT         : ('0' .. '9' | 'a' .. 'f' | 'A' .. 'F') ;
fragment LONG_TYPE_SUFFIX  : ('l' | 'L') ;
fragment FLOAT_TYPE_SUFFIX : ('f' | 'F' | 'd' | 'D') ;
fragment ESCAPE_SEQUENCE   : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\') | UNICODE_ESCAPE ;
UNICODE_ESCAPE    : '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT ;
LETTER
	:	[a-zA-Z$_] // these are the "java letters" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierStart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;
LETTER_OR_DIGIT
	:	[a-zA-Z0-9$_] // these are the "java letters or digits" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;