# Assembler

The assembler module is responsible for compiling text representing the instructions and data of a method.

## The logical steps of the assembler

1. Tokenize some text input
2. Parse the tokens into groups
3. Map the groups into our AST format
4. Validate the AST is correct enough for us to transform it into bytecode
5. Transform the AST into bytecode
6. Validate the bytecode

### Step 1: Tokenizing

Handled entirely by [JASM](https://github.com/Nowilltolife/Jasm).

### Step 2: Parse the groups

Handled entirely by [JASM](https://github.com/Nowilltolife/Jasm). The tokens are transformed into logical groups which 
can be handled by a custom listener type in the next step.

### Step 3: Map to AST

JASM's groupings are tied to the bytecode format, which makes mapping them into our own AST model very easy.
Just a matter of implementing the right interfaces and plugging in values to our classes from the interfaces.

### Step 4: Validation of AST

We then have a series of validation passes on our AST model just to ensure that there are no errors that prevent us 
from transforming the AST into bytecode. If there are errors we will not be able to proceed to the next step.

### Step 5: Transformation

Now that we know our AST model does not contain errors preventing us from transforming we can just focus on actual
transformation on this step. All error handling is done in the prior step.

### Step 6: Validation of Bytecode

Now that we have the bytecode we can then use existing Objectweb ASM analysis capabilities to do more verbose validation of
the code. We could reinvent the wheel for our own AST model, but its much easier to use something that already exists.

Once this step is complete we have some bytecode of a method body that we know is valid. 