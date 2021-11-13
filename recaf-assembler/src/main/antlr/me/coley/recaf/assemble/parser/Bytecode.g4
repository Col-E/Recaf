grammar Bytecode;

// Quick notes for those unfamiliar with the grammer format
//  - Format
//      - Its very similar to REGEX
//      - The lower-case names are parser rules
//      - The upper-case names are lexer rules
//  - How it works
//      - The lexer generates tokens from some input text
//          - Rules that match more text get preference
//          - If two rules match the same amount of text, the first rule in the file is used
//          - Therefor order is very important.
//      - The parser can piece together tokens and other rules to make new rules
//      - The parser must operate on tokenized items

unit            : comment* definition code EOF ;

definition      : methodDef | fieldDef ;

fieldDef        : modifiers? name singleDesc ;
methodDef       : modifiers? name L_PAREN methodParams? R_PAREN singleDesc ;

code            : codeEntry* ;
codeEntry       : instruction
                | label
                | comment+
                | tryCatch
                | throwEx
                | constVal
                | signature
                ;

instruction : insn
            | insnInt
            | insnVar
            | insnLdc
            | insnField
            | insnMethod
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
insnInt     : (BIPUSH | SIPUSH) (intLiteral | hexLiteral) ;
insnNewArray: NEWARRAY (intLiteral | charLiteral) ;
insnMethod  : (INVOKESTATIC | INVOKEVIRTUAL | INVOKESPECIAL | INVOKEINTERFACE) methodRef;
insnField   : (GETSTATIC | GETFIELD | PUTSTATIC | GETFIELD) fieldRef;
insnLdc     : LDC (intLiteral | hexLiteral | floatLiteral | stringLiteral | type) ;
insnVar     : (ILOAD | LLOAD | FLOAD | DLOAD | ALOAD | ISTORE | LSTORE | FSTORE | DSTORE | ASTORE | RET) varId ;
insnType    : (NEW | ANEWARRAY | CHECKCAST | INSTANCEOF) type ;
insnDynamic : INVOKEDYNAMIC name methodDesc dynamicHandle dynamicArgs? ;
insnJump    : (IFEQ | IFNE | IFLT | IFGE | IFGT | IFLE | IF_ICMPEQ | IF_ICMPNE | IF_ICMPLT | IF_ICMPGE | IF_ICMPGT | IF_ICMPLE | IF_ACMPEQ | IF_ACMPNE | GOTO | JSR | IFNULL | IFNONNULL) name ;
insnIinc    : IINC varId (intLiteral | hexLiteral) ;
insnMultiA  : MULTIANEWARRAY singleDesc intLiteral ;
insnLine    : LINE name intLiteral ;
insnLookup  : LOOKUPSWITCH switchMap switchDefault ;
insnTable   : TABLESWITCH switchRange switchOffsets switchDefault ;
switchMap     : (KW_MAPPING)? L_PAREN switchMapList R_PAREN ;
switchMapList : switchMapEntry (COMMA switchMapList)? ;
switchMapEntry: name EQUALS intLiteral ;
switchRange   : KW_RANGE? L_PAREN intLiteral COLON intLiteral R_PAREN ;
switchOffsets : KW_OFFSETS? L_PAREN switchOffsetsList R_PAREN;
switchOffsetsList : name (COMMA switchOffsetsList)? ;
switchDefault : KW_DEFAULT? L_PAREN name R_PAREN ;
dynamicHandle : KW_HANDLE? L_PAREN (methodHandle | fieldHandle) R_PAREN ;
dynamicArgs   : KW_ARGS? L_PAREN argumentList? R_PAREN ;

methodHandle: handleTag type '.' name methodDesc ;
methodRef   : type '.' name methodDesc ;
fieldHandle : handleTag type '.' name singleDesc ;
fieldRef    : type '.' name singleDesc ;

handleTag   : H_GETFIELD | H_GETSTATIC | H_PUTFIELD | H_PUTSTATIC
            | H_INVOKEVIRTUAL | H_INVOKESTATIC | H_INVOKESPECIAL | H_NEWINVOKESPECIAL | H_INVOKEINTERFACE
            ;

methodParams    : methodParam (',' methodParams)? ;
methodParam     : paramType name ;
paramType       : type | singleDesc ;

methodSig       : L_PAREN multiSig* R_PAREN singleSig ;
methodDesc      : L_PAREN multiDesc* R_PAREN singleDesc ;
multiSig        : (singleSig | PRIMS)+ ;
multiDesc       : (singleDesc | PRIMS)+ ;
singleSig       : sigDesc | typeDesc | primDesc ;
singleDesc      : typeDesc | primDesc ;

name            : BASE_NAME | primDesc | keyword ;
type            : classWords | primDesc  ;
typeDesc        : L_BRACKET* type SEMICOLON ;
sigDesc         : L_BRACKET* sig ;
primDesc        : L_BRACKET* PRIM ;
sig             : classWords sigArg? SEMICOLON | primDesc ;
sigArg          : L_ANGLE sig* R_ANGLE ;

classWords      : word (NAME_SEPARATOR word)* ;
word            : L_BASE_NAME | T_BASE_NAME | BASE_NAME | keyword ;

boolLiteral     : BOOLEAN_LITERAL ;
charLiteral     : CHARACTER_LITERAL ;
intLiteral      : INTEGER_LITERAL ;
hexLiteral      : HEX_LITERAL ;
floatLiteral    : FLOATING_PT_LITERAL ;
stringLiteral   : STRING_LITERAL ;

argumentList : argument (COMMA argumentList)? ;
argument     : (dynamicHandle | intLiteral | charLiteral | hexLiteral | floatLiteral | stringLiteral | boolLiteral | type) ;
varId        : name | intLiteral ;

label       : name COLON ;

comment     : LINE_COMMENT
            | MULTILINE_COMMENT
            ;

tryCatch    : TRY name name CATCH L_PAREN catchType R_PAREN name ;
catchType   : type | STAR ;
throwEx     : THROWS type ;
constVal    : VALUE (intLiteral | hexLiteral | floatLiteral | stringLiteral) ;
signature   : SIGNATURE (methodSig | singleSig) ;

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
// This exists so that the "name" rule can properly take in keyword tokens.
// This prevents name shadowing keywords from breaking the parser.
keyword     : MOD_PUBLIC
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
            | LINE
            | NOP
            | ACONST_NULL
            | ICONST_M1
            | ICONST_0
            | ICONST_1
            | ICONST_2
            | ICONST_3
            | ICONST_4
            | ICONST_5
            | LCONST_0
            | LCONST_1
            | FCONST_0
            | FCONST_1
            | FCONST_2
            | DCONST_0
            | DCONST_1
            | BIPUSH
            | SIPUSH
            | LDC
            | ILOAD
            | LLOAD
            | FLOAD
            | DLOAD
            | ALOAD
            | IALOAD
            | LALOAD
            | FALOAD
            | DALOAD
            | AALOAD
            | BALOAD
            | CALOAD
            | SALOAD
            | ISTORE
            | LSTORE
            | FSTORE
            | DSTORE
            | ASTORE
            | IASTORE
            | LASTORE
            | FASTORE
            | DASTORE
            | AASTORE
            | BASTORE
            | CASTORE
            | SASTORE
            | POP
            | POP2
            | DUP
            | DUP_X1
            | DUP_X2
            | DUP2
            | DUP2_X1
            | DUP2_X2
            | SWAP
            | IADD
            | LADD
            | FADD
            | DADD
            | ISUB
            | LSUB
            | FSUB
            | DSUB
            | IMUL
            | LMUL
            | FMUL
            | DMUL
            | IDIV
            | LDIV
            | FDIV
            | DDIV
            | IREM
            | LREM
            | FREM
            | DREM
            | INEG
            | LNEG
            | FNEG
            | DNEG
            | ISHL
            | LSHL
            | ISHR
            | LSHR
            | IUSHR
            | LUSHR
            | IAND
            | LAND
            | IOR
            | LOR
            | IXOR
            | LXOR
            | IINC
            | I2L
            | I2F
            | I2D
            | L2I
            | L2F
            | L2D
            | F2I
            | F2L
            | F2D
            | D2I
            | D2L
            | D2F
            | I2B
            | I2C
            | I2S
            | LCMP
            | FCMPL
            | FCMPG
            | DCMPL
            | DCMPG
            | IFEQ
            | IFNE
            | IFLT
            | IFGE
            | IFGT
            | IFLE
            | IF_ICMPEQ
            | IF_ICMPNE
            | IF_ICMPLT
            | IF_ICMPGE
            | IF_ICMPGT
            | IF_ICMPLE
            | IF_ACMPEQ
            | IF_ACMPNE
            | GOTO
            | JSR
            | RET
            | TABLESWITCH
            | LOOKUPSWITCH
            | IRETURN
            | LRETURN
            | FRETURN
            | DRETURN
            | ARETURN
            | RETURN
            | GETSTATIC
            | PUTSTATIC
            | GETFIELD
            | PUTFIELD
            | INVOKEVIRTUAL
            | INVOKESPECIAL
            | INVOKESTATIC
            | INVOKEINTERFACE
            | INVOKEDYNAMIC
            | NEW
            | NEWARRAY
            | ANEWARRAY
            | ARRAYLENGTH
            | ATHROW
            | CHECKCAST
            | INSTANCEOF
            | MONITORENTER
            | MONITOREXIT
            | MULTIANEWARRAY
            | IFNULL
            | IFNONNULL
            | H_GETFIELD
            | H_GETSTATIC
            | H_PUTFIELD
            | H_PUTSTATIC
            | H_INVOKEVIRTUAL
            | H_INVOKESTATIC
            | H_INVOKESPECIAL
            | H_NEWINVOKESPECIAL
            | H_INVOKEINTERFACE
            | KW_DEFAULT
            | KW_HANDLE
            | KW_ARGS
            | KW_RANGE
            | KW_MAPPING
            | KW_OFFSETS
            | TRY
            | CATCH
            | THROWS
            | VALUE
            | SIGNATURE
            | THE_L
            | THE_T
            ;

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

TRY       : 'try'          | 'TRY' ;
CATCH     : 'catch'        | 'CATCH' ;
THROWS    : 'throws'       | 'THROWS' ;
VALUE     : 'const-value'  | 'CONST-VALUE'  ;
SIGNATURE : 'signature'    | 'SIGNATURE' ;

INTEGER_LITERAL     : '-'? DEC_DIGIT + LONG_TYPE_SUFFIX? ;
HEX_LITERAL         : '0' ('x' | 'X') HEX_DIGIT + LONG_TYPE_SUFFIX? ;
CHARACTER_LITERAL   : '\'' (ESCAPE_SEQUENCE | ~ ('\'' | '\\')) '\'' ;
STRING_LITERAL      : '"' (~ [\r\n] | '""')* '"'
                    | '"' (ESCAPE_SEQUENCE | ~ ('\\' | '"'))* '"'
                    ;
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

PRIM            : ('V' | 'Z' | 'C' | 'B' | 'S' | 'I' | 'F' | 'D' | 'J') ;
PRIMS           : PRIM PRIMS? ;
T_BASE_NAME    : THE_T BASE_NAME ;
L_BASE_NAME    : THE_L BASE_NAME ;
BASE_NAME      : (UNICODE_ESCAPE | LETTER_OR_DIGIT)+ ;

COMMENT_PRFIX       : NAME_SEPARATOR NAME_SEPARATOR NAME_SEPARATOR*;
WHITESPACE          : (SPACE | CARRIAGE_RET | NEWLINE | TAB) -> skip ;
STAR                : '*'  ;
THE_L               : 'L'  ;
THE_T               : 'T'  ;
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