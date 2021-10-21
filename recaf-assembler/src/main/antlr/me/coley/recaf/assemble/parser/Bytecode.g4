grammar Bytecode;

unit            : comment* methodDefine body? ;

fieldDefine     : modifiers name singleDesc ;
methodDefine    : modifiers name methodDesc ;

body            : codeEntry* ;
codeEntry       : comment
                | label
                ;



methodDesc      : L_PAREN multiDesc* R_PAREN singleDesc ;
multiDesc       : (singleDesc | PRIMS)+ ;
singleDesc      : TYPE_DESC | PRIM_DESC ;

type            : internalType | PRIM_DESC ;
internalType    : TYPE ;
name            : NAME ;

label       : name COLON ;

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
INTEGER_LITERAL     : '-'? ('0' | '1' .. '9' '0' .. '9'*) LONG_TYPE_SUFFIX? ;
HEX_LITERAL         : '0' ('x' | 'X') HEX_DIGIT + LONG_TYPE_SUFFIX? ;
CHARACTER_LITERAL   : '\'' (ESCAPE_SEQUENCE | ~ ('\'' | '\\')) '\'' ;
STRING_LITERAL      : '"' (ESCAPE_SEQUENCE | ~ ('\\' | '"'))* '"' ;
FLOATING_PT_LITERAL
    : '-'? ('0' .. '9') + '.' ('0' .. '9')* FLOAT_TYPE_SUFFIX?
    | '-'? '.' ('0' .. '9') + FLOAT_TYPE_SUFFIX?
    | '-'? ('0' .. '9') + FLOAT_TYPE_SUFFIX?
    | '-'? ('0' .. '9') + FLOAT_TYPE_SUFFIX
    ;
BOOLEAN_LITERAL
    : 'TRUE' | 'true'
    | 'FALSE' | 'false'
    ;
fragment HEX_DIGIT         : ('0' .. '9' | 'a' .. 'f' | 'A' .. 'F')  ;
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