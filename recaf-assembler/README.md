# Assembler

The assembler module is responsible for compiling text representing the instructions and data of a method.

## The logical steps of the assembler

1. Tokenize some text input
2. Parse the tokens into AST types
3. Validate the AST is correct enough for us to transform it into bytecode
4. Transform the AST into bytecode
5. Validate the bytecode

### Step 1: Tokenizing

Handled entirely by ANTLR. The language is defined by [`Bytecode.g4`](src/antlr/me/coley/recaf/assemble/parser/Bytecode.g4)

### Step 2: Parse the tokens into AST types

Handled mostly by ANTLR, but we make some wrapper code which is easier to manage. ANTLR generates a visitor api which
is named based on the language. So the generated visitor API class is `BytecodeVisitor` which we extend to transform
their AST types into our own.

### Step 3: Validation of AST

We then have a series of validation passes on our AST model just to ensure that there are no errors that prevent us 
from transforming the AST into bytecode. If there are errors we will not be able to proceed to the next step.

### Step 4: Transformation

Now that we know our AST model does not contain errors preventing us from transforming we can just focus on actual
transformation on this step. All error handling is done in the prior step.

### Step 5: Validation of Bytecode

Now that we have the bytecode we can then use existing Objectweb ASM analysis capabilities to do more verbose validation of
the code. We could reinvent the wheel for our own AST model, but its much easier to use something that already exists.

Once this step is complete we have some bytecode of a method body that we know is valid. 