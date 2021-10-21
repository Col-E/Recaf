grammar Bytecode;

unit            : comment* methodDefine body EOF ;

fieldDefine     : modifiers name singleDesc ;
methodDefine    : modifiers name methodDesc ;

body            : codeEntry* ;
codeEntry       : instruction
                | label
                | comment
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
insnInt     : (BIPUSH | SIPUSH) INTEGER_LITERAL ;
insnNewArray: NEWARRAY (INTEGER_LITERAL | CHARACTER_LITERAL) ;
insnInvoke  : (INVOKESTATIC | INVOKEVIRTUAL | INVOKESPECIAL | INVOKEINTERFACE) methodHandle;
insnField   : (GETSTATIC | GETFIELD | PUTSTATIC | GETFIELD) fieldHandle;
insnLdc     : LDC (INTEGER_LITERAL | HEX_LITERAL | FLOATING_PT_LITERAL | STRING_LITERAL | type) ;
insnVar     : (ILOAD | LLOAD | FLOAD | DLOAD | ALOAD | ISTORE | LSTORE | FSTORE | DSTORE | ASTORE | RET) varId ;
insnType    : (NEW | ANEWARRAY | CHECKCAST | INSTANCEOF) internalType ;
insnDynamic : INVOKEDYNAMIC name methodDesc dynamicHandle L_BRACKET dynamicArgs R_BRACKET ;
insnJump    : (IFEQ | IFNE | IFLT | IFGE | IFGT | IFLE | IF_ICMPEQ | IF_ICMPNE | IF_ICMPLT | IF_ICMPGE | IF_ICMPGT | IF_ICMPLE | IF_ACMPEQ | IF_ACMPNE | GOTO | JSR | IFNULL | IFNONNULL) name ;
insnIinc    : IINC varId INTEGER_LITERAL ;
insnMultiA  : MULTIANEWARRAY singleDesc INTEGER_LITERAL ;
insnLine    : LINE name INTEGER_LITERAL ;
insnLookup  : LOOKUPSWITCH switchMap switchDefault  ;
insnTable   : TABLESWITCH switchRange switchOffsets switchDefault  ;
switchMap     : (KW_MAPPING)? L_BRACKET switchMapList R_BRACKET ;
switchMapList : switchMapEntry (COMMA switchMapList)? ;
switchMapEntry: name EQUALS INTEGER_LITERAL ;
switchRange   : KW_RANGE? L_BRACKET INTEGER_LITERAL COLON INTEGER_LITERAL R_BRACKET  ;
switchOffsets : KW_RANGE? L_BRACKET switchOffsetsList R_BRACKET;
switchOffsetsList : name (COMMA switchOffsetsList)? ;
switchDefault : KW_DEFAULT? L_BRACKET name R_BRACKET ;
dynamicHandle : KW_HANDLE? L_BRACKET (methodHandle | fieldHandle) R_BRACKET ;
dynamicArgs   : KW_ARGS? L_BRACKET argument* R_BRACKET ;

KW_DEFAULT : 'Default' | 'default' | 'dflt' ;
KW_HANDLE  : 'Handle' | 'handle' ;
KW_ARGS    : 'Args' | 'args' ;
KW_RANGE   : 'Range' | 'range' ;
KW_MAPPING : 'Mapping' | 'mapping' | 'map' ;

methodHandle: type '.' name methodDesc ;
fieldHandle : type '.' name singleDesc ;

methodDesc      : L_PAREN multiDesc* R_PAREN singleDesc ;
multiDesc       : (singleDesc | PRIMS)+ ;
singleDesc      : TYPE_DESC | PRIM_DESC ;

type            : internalType | PRIM_DESC ;
internalType    : TYPE | NAME ;
name            : NAME ;

argument    : (dynamicHandle | INTEGER_LITERAL | CHARACTER_LITERAL | HEX_LITERAL | FLOATING_PT_LITERAL | STRING_LITERAL | BOOLEAN_LITERAL) ;
varId       : name | INTEGER_LITERAL ;

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
MOD_PUBLIC       : 'public'        | 'PUBLIC' ;
MOD_PRIVATE      : 'private'       | 'PRIVATE' ;
MOD_PROTECTED    : 'protected'     | 'PROTECTED' ;
MOD_STATIC       : 'static'        | 'STATIC' ;
MOD_FINAL        : 'final'         | 'FINAL' ;
MOD_SYNCHRONIZED : 'synchronized'  | 'SYNCHRONIZED' ;
MOD_SUPER        : 'super'         | 'SUPER' ;
MOD_BRIDGE       : 'bridge'        | 'BRIDGE' ;
MOD_VOLATILE     : 'volatile'      | 'VOLATILE' ;
MOD_VARARGS      : 'varargs'       | 'VARARGS' ;
MOD_TRANSIENT    : 'transient'     | 'TRANSIENT' ;
MOD_NATIVE       : 'native'        | 'NATIVE' ;
MOD_INTERFACE    : 'interface'     | 'INTERFACE' ;
MOD_ABSTRACT     : 'abstract'      | 'ABSTRACT' ;
MOD_STRICTFP     : 'strictfp'      | 'STRICTFP' ;
MOD_SYNTHETIC    : 'synthetic'     | 'SYNTHETIC' ;
MOD_ANNOTATION   : 'annotation'    | 'ANNOTATION' ;
MOD_ENUM         : 'enum'          | 'ENUM' ;
MOD_MODULE       : 'module'        | 'MODULE' ;
MOD_MANDATED     : 'mandated'      | 'MANDATED' ;
LABEL            : NAME COLON ;
LINE             : 'LINE' ;
NOP              : 'NOP' ;
ACONST_NULL      : 'ACONST_NULL' ;
ICONST_M1        : 'ICONST_M1' ;
ICONST_0         : 'ICONST_0' ;
ICONST_1         : 'ICONST_1' ;
ICONST_2         : 'ICONST_2' ;
ICONST_3         : 'ICONST_3' ;
ICONST_4         : 'ICONST_4' ;
ICONST_5         : 'ICONST_5' ;
LCONST_0         : 'LCONST_0' ;
LCONST_1         : 'LCONST_1' ;
FCONST_0         : 'FCONST_0' ;
FCONST_1         : 'FCONST_1' ;
FCONST_2         : 'FCONST_2' ;
DCONST_0         : 'DCONST_0' ;
DCONST_1         : 'DCONST_1' ;
BIPUSH           : 'BIPUSH' ;
SIPUSH           : 'SIPUSH' ;
LDC              : 'LDC' ;
ILOAD            : 'ILOAD' ;
LLOAD            : 'LLOAD' ;
FLOAD            : 'FLOAD' ;
DLOAD            : 'DLOAD' ;
ALOAD            : 'ALOAD' ;
IALOAD           : 'IALOAD' ;
LALOAD           : 'LALOAD' ;
FALOAD           : 'FALOAD' ;
DALOAD           : 'DALOAD' ;
AALOAD           : 'AALOAD' ;
BALOAD           : 'BALOAD' ;
CALOAD           : 'CALOAD' ;
SALOAD           : 'SALOAD' ;
ISTORE           : 'ISTORE' ;
LSTORE           : 'LSTORE' ;
FSTORE           : 'FSTORE' ;
DSTORE           : 'DSTORE' ;
ASTORE           : 'ASTORE' ;
IASTORE          : 'IASTORE' ;
LASTORE          : 'LASTORE' ;
FASTORE          : 'FASTORE' ;
DASTORE          : 'DASTORE' ;
AASTORE          : 'AASTORE' ;
BASTORE          : 'BASTORE' ;
CASTORE          : 'CASTORE' ;
SASTORE          : 'SASTORE' ;
POP              : 'POP' ;
POP2             : 'POP2' ;
DUP              : 'DUP' ;
DUP_X1           : 'DUP_X1' ;
DUP_X2           : 'DUP_X2' ;
DUP2             : 'DUP2' ;
DUP2_X1          : 'DUP2_X1' ;
DUP2_X2          : 'DUP2_X2' ;
SWAP             : 'SWAP' ;
IADD             : 'IADD' ;
LADD             : 'LADD' ;
FADD             : 'FADD' ;
DADD             : 'DADD' ;
ISUB             : 'ISUB' ;
LSUB             : 'LSUB' ;
FSUB             : 'FSUB' ;
DSUB             : 'DSUB' ;
IMUL             : 'IMUL' ;
LMUL             : 'LMUL' ;
FMUL             : 'FMUL' ;
DMUL             : 'DMUL' ;
IDIV             : 'IDIV' ;
LDIV             : 'LDIV' ;
FDIV             : 'FDIV' ;
DDIV             : 'DDIV' ;
IREM             : 'IREM' ;
LREM             : 'LREM' ;
FREM             : 'FREM' ;
DREM             : 'DREM' ;
INEG             : 'INEG' ;
LNEG             : 'LNEG' ;
FNEG             : 'FNEG' ;
DNEG             : 'DNEG' ;
ISHL             : 'ISHL' ;
LSHL             : 'LSHL' ;
ISHR             : 'ISHR' ;
LSHR             : 'LSHR' ;
IUSHR            : 'IUSHR' ;
LUSHR            : 'LUSHR' ;
IAND             : 'IAND' ;
LAND             : 'LAND' ;
IOR              : 'IOR' ;
LOR              : 'LOR' ;
IXOR             : 'IXOR' ;
LXOR             : 'LXOR' ;
IINC             : 'IINC' ;
I2L              : 'I2L' ;
I2F              : 'I2F' ;
I2D              : 'I2D' ;
L2I              : 'L2I' ;
L2F              : 'L2F' ;
L2D              : 'L2D' ;
F2I              : 'F2I' ;
F2L              : 'F2L' ;
F2D              : 'F2D' ;
D2I              : 'D2I' ;
D2L              : 'D2L' ;
D2F              : 'D2F' ;
I2B              : 'I2B' ;
I2C              : 'I2C' ;
I2S              : 'I2S' ;
LCMP             : 'LCMP' ;
FCMPL            : 'FCMPL' ;
FCMPG            : 'FCMPG' ;
DCMPL            : 'DCMPL' ;
DCMPG            : 'DCMPG' ;
IFEQ             : 'IFEQ' ;
IFNE             : 'IFNE' ;
IFLT             : 'IFLT' ;
IFGE             : 'IFGE' ;
IFGT             : 'IFGT' ;
IFLE             : 'IFLE' ;
IF_ICMPEQ        : 'IF_ICMPEQ' ;
IF_ICMPNE        : 'IF_ICMPNE' ;
IF_ICMPLT        : 'IF_ICMPLT' ;
IF_ICMPGE        : 'IF_ICMPGE' ;
IF_ICMPGT        : 'IF_ICMPGT' ;
IF_ICMPLE        : 'IF_ICMPLE' ;
IF_ACMPEQ        : 'IF_ACMPEQ' ;
IF_ACMPNE        : 'IF_ACMPNE' ;
GOTO             : 'GOTO' ;
JSR              : 'JSR' ;
RET              : 'RET' ;
TABLESWITCH      : 'TABLESWITCH' ;
LOOKUPSWITCH     : 'LOOKUPSWITCH' ;
IRETURN          : 'IRETURN' ;
LRETURN          : 'LRETURN' ;
FRETURN          : 'FRETURN' ;
DRETURN          : 'DRETURN' ;
ARETURN          : 'ARETURN' ;
RETURN           : 'RETURN' ;
GETSTATIC        : 'GETSTATIC' ;
PUTSTATIC        : 'PUTSTATIC' ;
GETFIELD         : 'GETFIELD' ;
PUTFIELD         : 'PUTFIELD' ;
INVOKEVIRTUAL    : 'INVOKEVIRTUAL' ;
INVOKESPECIAL    : 'INVOKESPECIAL' ;
INVOKESTATIC     : 'INVOKESTATIC' ;
INVOKEINTERFACE  : 'INVOKEINTERFACE' ;
INVOKEDYNAMIC    : 'INVOKEDYNAMIC' ;
NEW              : 'NEW' ;
NEWARRAY         : 'NEWARRAY' ;
ANEWARRAY        : 'ANEWARRAY' ;
ARRAYLENGTH      : 'ARRAYLENGTH' ;
ATHROW           : 'ATHROW' ;
CHECKCAST        : 'CHECKCAST' ;
INSTANCEOF       : 'INSTANCEOF' ;
MONITORENTER     : 'MONITORENTER' ;
MONITOREXIT      : 'MONITOREXIT' ;
MULTIANEWARRAY   : 'MULTIANEWARRAY' ;
IFNULL           : 'IFNULL' ;
IFNONNULL        : 'IFNONNULL' ;

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