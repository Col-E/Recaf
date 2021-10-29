grammar Bytecode;

unit            : comment* definition code EOF ;

definition      : methodDef | fieldDef ;

fieldDef        : modifiers name singleDesc ;
methodDef       : modifiers name L_PAREN methodParams? R_PAREN singleDesc ;

code            : codeEntry* ;
codeEntry       : instruction
                | label
                | comment+
                ;

instruction : insn
            | insnInt
            | insnVar
            | insnLdc
            | insnInvoke
            | insnField
            | insnType
            | insnJump
            | insnDynamic
            | insnLookup
            | insnTable
            | insnIinc
            | insnNewArray
            | insnMultiA
            | insnLine
            ;
// AFAIK there is no good way to pull out an "opcode" rule that works for each instruction that filters
// out opcodes that don't match an instruction type. Of course we could do rules like "intOpcode", "ldcOpcode", etc
// but that still feels kinda messy. So in the parsing logic we'll just do "getChild(0)" to fetch the opcodes.
insn        : NOP
            | ACONST_NULL
            | ICONST_M1 | ICONST_0 | ICONST_1 | ICONST_2 | ICONST_3 | ICONST_4 | ICONST_5
            | LCONST_0 | LCONST_1
            | FCONST_0 | FCONST_1 | FCONST_2
            | DCONST_0 | DCONST_1
            | IALOAD | LALOAD | FALOAD | DALOAD | AALOAD | BALOAD | CALOAD | SALOAD
            | IASTORE | LASTORE | FASTORE | DASTORE | AASTORE | BASTORE | CASTORE | SASTORE
            | POP | POP2
            | DUP | DUP_X1 | DUP_X2 | DUP2 | DUP2_X1 | DUP2_X2
            | SWAP
            | IADD | LADD | FADD | DADD
            | ISUB | LSUB | FSUB | DSUB
            | IMUL | LMUL | FMUL | DMUL
            | IDIV | LDIV | FDIV | DDIV
            | IREM | LREM | FREM | DREM
            | INEG | LNEG | FNEG | DNEG
            | ISHL | LSHL | ISHR | LSHR | IUSHR | LUSHR
            | IAND | LAND | IOR | LOR | IXOR | LXOR
            | I2L | I2F | I2D | L2I | L2F | L2D | F2I | F2L | F2D | D2I | D2L | D2F | I2B | I2C | I2S
            | LCMP | FCMPL | FCMPG | DCMPL | DCMPG
            | IRETURN | LRETURN | FRETURN | DRETURN | ARETURN | RETURN
            | ARRAYLENGTH
            | ATHROW
            | MONITORENTER | MONITOREXIT
            ;
insnInt     : (BIPUSH | SIPUSH) intLiteral ;
insnNewArray: NEWARRAY (intLiteral | charLiteral) ;
insnInvoke  : (INVOKESTATIC | INVOKEVIRTUAL | INVOKESPECIAL | INVOKEINTERFACE) methodHandle;
insnField   : (GETSTATIC | GETFIELD | PUTSTATIC | GETFIELD) fieldHandle;
insnLdc     : LDC (intLiteral | hexLiteral | floatLiteral | stringLiteral | type) ;
insnVar     : (ILOAD | LLOAD | FLOAD | DLOAD | ALOAD | ISTORE | LSTORE | FSTORE | DSTORE | ASTORE | RET) varId ;
insnType    : (NEW | ANEWARRAY | CHECKCAST | INSTANCEOF) internalType ;
insnDynamic : INVOKEDYNAMIC name methodDesc dynamicHandle L_BRACKET dynamicArgs R_BRACKET ;
insnJump    : (IFEQ | IFNE | IFLT | IFGE | IFGT | IFLE | IF_ICMPEQ | IF_ICMPNE | IF_ICMPLT | IF_ICMPGE | IF_ICMPGT | IF_ICMPLE | IF_ACMPEQ | IF_ACMPNE | GOTO | JSR | IFNULL | IFNONNULL) name ;
insnIinc    : IINC varId intLiteral ;
insnMultiA  : MULTIANEWARRAY singleDesc intLiteral ;
insnLine    : LINE name intLiteral ;
insnLookup  : LOOKUPSWITCH switchMap switchDefault ;
insnTable   : TABLESWITCH switchRange switchOffsets switchDefault ;
switchMap     : (KW_MAPPING)? L_BRACKET switchMapList R_BRACKET ;
switchMapList : switchMapEntry (COMMA switchMapList)? ;
switchMapEntry: name EQUALS intLiteral ;
switchRange   : KW_RANGE? L_BRACKET intLiteral COLON intLiteral R_BRACKET  ;
switchOffsets : KW_RANGE? L_BRACKET switchOffsetsList R_BRACKET;
switchOffsetsList : name (COMMA switchOffsetsList)? ;
switchDefault : KW_DEFAULT? L_BRACKET name R_BRACKET ;
dynamicHandle : KW_HANDLE? L_BRACKET (methodHandle | fieldHandle) R_BRACKET ;
dynamicArgs   : KW_ARGS? L_BRACKET argumentList? R_BRACKET ;

KW_DEFAULT : 'Default' | 'default' | 'dflt' ;
KW_HANDLE  : 'Handle' | 'handle' ;
KW_ARGS    : 'Args' | 'args' ;
KW_RANGE   : 'Range' | 'range' ;
KW_MAPPING : 'Mapping' | 'mapping' | 'map' ;

methodHandle: handleTag type '.' name methodDesc ;
fieldHandle : handleTag type '.' name singleDesc ;
handleTag   : H_GETFIELD | H_GETSTATIC | H_PUTFIELD | H_PUTSTATIC
            | H_INVOKEVIRTUAL | H_INVOKESTATIC | H_INVOKESPECIAL | H_NEWINVOKESPECIAL | H_INVOKEINTERFACE
            ;

methodParams    : methodParam (',' methodParams)? ;
methodParam     : type name ;

methodDesc      : L_PAREN multiDesc* R_PAREN singleDesc ;
multiDesc       : (singleDesc | PRIMS)+ ;
singleDesc      : TYPE_DESC | PRIM_DESC ;

boolLiteral     : BOOLEAN_LITERAL ;
charLiteral     : CHARACTER_LITERAL ;
intLiteral      : INTEGER_LITERAL ;
hexLiteral      : HEX_LITERAL ;
floatLiteral    : FLOATING_PT_LITERAL ;
stringLiteral   : STRING_LITERAL ;
type            : internalType | PRIM_DESC ;
internalType    : TYPE | NAME ;
name            : NAME ;

argumentList : argument (COMMA argumentList)? ;
argument     : (dynamicHandle | intLiteral | charLiteral | hexLiteral | floatLiteral | stringLiteral | boolLiteral | type) ;
varId        : name | INTEGER_LITERAL ;

label       : LABEL ;

comment     : LINE_COMMENT
            | MULTILINE_COMMENT
            ;

modifiers   : modifier (modifier)* ;
modifier    : MOD_PUBLIC
            | MOD_PRIVATE
            | MOD_PROTECTED
            | MOD_STATIC
            | MOD_FINAL
            | MOD_SYNCHRONIZED
            | MOD_SUPER
            | MOD_BRIDGE
            | MOD_VOLATILE
            | MOD_VARARGS
            | MOD_TRANSIENT
            | MOD_NATIVE
            | MOD_INTERFACE
            | MOD_ABSTRACT
            | MOD_STRICTFP
            | MOD_SYNTHETIC
            | MOD_ANNOTATION
            | MOD_ENUM
            | MOD_MODULE
            | MOD_MANDATED
            ;
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
LABEL            : NAME COLON ;
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


INTEGER_LITERAL     : '-'? DEC_DIGIT + LONG_TYPE_SUFFIX? ;
HEX_LITERAL         : '0' ('x' | 'X') HEX_DIGIT + LONG_TYPE_SUFFIX? ;
CHARACTER_LITERAL   : '\'' (ESCAPE_SEQUENCE | ~ ('\'' | '\\')) '\'' ;
STRING_LITERAL      : '"' (ESCAPE_SEQUENCE | ~ ('\\' | '"'))* '"' ;
FLOATING_PT_LITERAL
    : '-'? DEC_DIGIT + '.' DEC_DIGIT* FLOAT_TYPE_SUFFIX?
    | '-'? '.' DEC_DIGIT + FLOAT_TYPE_SUFFIX?
    | '-'? DEC_DIGIT + FLOAT_TYPE_SUFFIX?
    | '-'? DEC_DIGIT + FLOAT_TYPE_SUFFIX
    ;
BOOLEAN_LITERAL
    : 'TRUE' | 'true'
    | 'FALSE' | 'false'
    ;

TYPE_DESC       : L_BRACKET* THE_L CLASS_NAME+ SEMICOLON ;
PRIM_DESC       : L_BRACKET* PRIM ;
PRIM            : ('V' | 'Z' | 'C' | 'B' | 'S' | 'I' | 'F' | 'D' | 'J') ;
PRIMS           : PRIM PRIMS? ;
NAME            : LETTER_OR_DIGIT+ ;
TYPE            : CLASS_NAME ;

fragment CLASS_NAME : LETTER_OR_DIGIT+ (NAME_SEPARATOR CLASS_NAME)* ;

WHITESPACE          : (SPACE | CARRIAGE_RET | NEWLINE | TAB) -> skip ;
THE_L               : 'L'  ;
MULTILINE_COMMENT   : '/*' .*? '*/' ;
LINE_COMMENT        : '//' ~ ('\n' | '\r')* '\r'? '\n' ;
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
DOT                 : '.'  ;
fragment DEC_DIGIT         : ('0' .. '9') ;
fragment HEX_DIGIT         : ('0' .. '9' | 'a' .. 'f' | 'A' .. 'F') ;
fragment LONG_TYPE_SUFFIX  : ('l' | 'L') ;
fragment FLOAT_TYPE_SUFFIX : ('f' | 'F' | 'd' | 'D') ;
fragment ESCAPE_SEQUENCE   : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\') | UNICODE_ESCAPE ;
fragment UNICODE_ESCAPE    : '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT ;
fragment LETTER
	:	[a-zA-Z$_] // these are the "java letters" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierStart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;
fragment LETTER_OR_DIGIT
	:	[a-zA-Z0-9$_] // these are the "java letters or digits" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;