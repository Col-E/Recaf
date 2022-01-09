grammar Bytecode;

import BytecodeTokens;

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

unit        : comment* definition code EOF ;
// Sections of a unit
definition  : methodDef | fieldDef ;
fieldDef    : modifiers? name desc ;
methodDef   : modifiers? name L_PAREN methodParams? R_PAREN desc ;
methodParams: methodParam (',' methodParams)? ;
methodParam : desc name ;
code        : codeEntry* ;
// Sections of code / attributes
codeEntry   : instruction
            | label
            | comment+
            | tryCatch
            | throwEx
            | constVal
            | signature
            | annotation
            | expr
            | unmatched
            ;
comment     : LINE_COMMENT
            | MULTILINE_COMMENT
            ;
expr        : EXPR exprEntry ;
exprEntry   : L_BRACE (exprEntry)* R_BRACE
            | TRY L_BRACE (exprEntry)* R_BRACE (CATCH L_PAREN exprLine+ R_PAREN L_BRACE (exprEntry)* R_BRACE)?
            | exprLine ;
exprLine    : (name | literal | comment | L_PAREN | R_PAREN | L_ANGLE | R_ANGLE | MODULO | PLUS | MINUS | EQUALS | STAR | XOR | PIPE | AND | NOT | NAME_SEPARATOR | SEMICOLON | COLON | COMMA | DOT)+ ;
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
insnNewArray: NEWARRAY (intLiteral | name) ;
insnMethod  : (INVOKESTATIC | INVOKEVIRTUAL | INVOKESPECIAL | INVOKEINTERFACE) methodRef;
insnField   : (GETSTATIC | GETFIELD | PUTSTATIC | PUTFIELD) fieldRef;
insnLdc     : LDC (intLiteral | hexLiteral | floatLiteral | greedyStringLiteral | type | desc) ;
insnVar     : (ILOAD | LLOAD | FLOAD | DLOAD | ALOAD | ISTORE | LSTORE | FSTORE | DSTORE | ASTORE | RET) name ;
insnType    : (NEW | ANEWARRAY | CHECKCAST | INSTANCEOF) (type | desc) ;
insnDynamic : INVOKEDYNAMIC name methodDesc dynamicHandle dynamicArgs? ;
insnJump    : (IFEQ | IFNE | IFLT | IFGE | IFGT | IFLE | IF_ICMPEQ | IF_ICMPNE | IF_ICMPLT | IF_ICMPGE | IF_ICMPGT | IF_ICMPLE | IF_ACMPEQ | IF_ACMPNE | GOTO | JSR | IFNULL | IFNONNULL) name ;
insnIinc    : IINC name (intLiteral | hexLiteral) ;
insnMultiA  : MULTIANEWARRAY desc intLiteral ;
insnLine    : LINE name intLiteral ;
insnLookup  : LOOKUPSWITCH switchMap switchDefault ;
insnTable   : TABLESWITCH switchRange switchOffsets switchDefault ;
tryCatch    : TRY name name CATCH L_PAREN catchType R_PAREN name ;
catchType   : type | STAR ;
throwEx     : THROWS type ;
constVal    : VALUE (intLiteral | hexLiteral | floatLiteral | stringLiteral) ;
signature   : SIGNATURE sigArg? (methodSig | sig) ;
label       : name COLON ;
// Switch instruction pieces
switchMap     : (KW_MAPPING)? L_PAREN switchMapList? R_PAREN ;
switchMapList : switchMapEntry (COMMA switchMapList)? ;
switchMapEntry: name EQUALS intLiteral ;
switchRange   : KW_RANGE? L_PAREN intLiteral COLON intLiteral R_PAREN ;
switchOffsets : KW_OFFSETS? L_PAREN switchOffsetsList R_PAREN;
switchOffsetsList : name (COMMA switchOffsetsList)? ;
switchDefault : KW_DEFAULT? L_PAREN name R_PAREN ;
// Handle types
dynamicHandle : KW_HANDLE? L_PAREN (methodHandle | fieldHandle) R_PAREN ;
dynamicArgs   : KW_ARGS? L_PAREN argumentList? R_PAREN ;
argumentList    : argument (COMMA argumentList)? ;
argument        : dynamicHandle
                | intLiteral
                | charLiteral
                | hexLiteral
                | floatLiteral
                | stringLiteral
                | boolLiteral
                | type
                | desc
                | methodDesc
                ;
methodHandle: handleTag type DOT name methodDesc ;
methodRef   : type DOT name methodDesc ;
fieldHandle : handleTag type DOT name desc ;
fieldRef    : type DOT name desc ;
handleTag   : H_GETFIELD | H_GETSTATIC | H_PUTFIELD | H_PUTSTATIC
            | H_INVOKEVIRTUAL | H_INVOKESTATIC | H_INVOKESPECIAL | H_NEWINVOKESPECIAL | H_INVOKEINTERFACE
            ;
// Annotations
annotation  : (VISIBLE_ANNOTATION | INVISIBLE_ANNOTATION | VISIBLE_TYPE_ANNOTATION | INVISIBLE_TYPE_ANNOTATION)
                type L_PAREN annoArgs R_PAREN ;
annoArgs    : annoArg (COMMA annoArgs)? ;
annoArg     : name EQUALS intLiteral
            | name EQUALS charLiteral
            | name EQUALS hexLiteral
            | name EQUALS floatLiteral
            | name EQUALS stringLiteral
            | name EQUALS boolLiteral
            | name EQUALS type
            | name EQUALS desc
            | name EQUALS methodDesc
            | name EQUALS annotation
            | name EQUALS annoList
            | name EQUALS annoEnum
            ;
annoList    : L_BRACKET intLiteral (COMMA intLiteral)+ R_BRACKET
            | L_BRACKET charLiteral (COMMA charLiteral)+ R_BRACKET
            | L_BRACKET hexLiteral (COMMA hexLiteral)+ R_BRACKET
            | L_BRACKET floatLiteral (COMMA floatLiteral)+ R_BRACKET
            | L_BRACKET stringLiteral (COMMA stringLiteral)+ R_BRACKET
            | L_BRACKET type (COMMA type)+ R_BRACKET
            | L_BRACKET desc (COMMA desc)+ R_BRACKET
            | L_BRACKET annoEnum (COMMA annoEnum)+ R_BRACKET
            | L_BRACKET methodDesc (COMMA methodDesc)+ R_BRACKET
            | L_BRACKET annotation (COMMA annotation)+ R_BRACKET
            ;
annoEnum    : type DOT name ;
// Signature parsing
methodSig       : L_PAREN sig* R_PAREN sig ;
sig             : L_BRACKET* type sigArg? SEMICOLON
                | L_BRACKET* name
                ;
sigArg          : L_ANGLE (sigDef | sig | STAR)* R_ANGLE ;
sigDef          : name COLON sig ;
// Basic elements for most common operations and other literals
//  - Descriptor correctness is not enforced here, that's done in the parser code
methodDesc      : L_PAREN desc* R_PAREN desc ;
desc            : L_BRACKET* type SEMICOLON?
                | L_BRACKET* name
                ;
type            : name (NAME_SEPARATOR name)*
                | L_BRACKET desc // array types
                ;
name            : (BASE_NAME | CTOR | STATIC_CTOR | keyword) ;
literal         : boolLiteral | charLiteral | intLiteral | hexLiteral | floatLiteral | stringLiteral ;
boolLiteral     : BOOLEAN_LITERAL ;
charLiteral     : CHARACTER_LITERAL ;
intLiteral      : INTEGER_LITERAL ;
hexLiteral      : HEX_LITERAL ;
floatLiteral    : FLOATING_PT_LITERAL ;
stringLiteral   : STRING_LITERAL ;
// This is a less intelligent string match, but makes it easy for users to do things like ""Hello World"" if they want
// to actually match "Hello World" with quotes
greedyStringLiteral : GREEDY_STRING_LITERAL | stringLiteral ;

// Keywords
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
            | CTOR
            | STATIC_CTOR
            | BOOLEAN_LITERAL
            | VISIBLE_ANNOTATION
            | INVISIBLE_ANNOTATION
            | VISIBLE_TYPE_ANNOTATION
            | INVISIBLE_TYPE_ANNOTATION
            | EXPR
            ;
unmatched   : . ;