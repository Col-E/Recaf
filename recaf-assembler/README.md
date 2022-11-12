# Assembler

The assembler module is responsible for compiling text representing the instructions and data of a method.

## The logical steps of the assembler

1. Tokenize some text input, collect the tokens into logical groups
2. Map the groups into our AST format
3. Validate the AST is correct enough for us to transform it into bytecode
4. Transform the AST into bytecode
5. Validate the bytecode

### Step 1: Tokenizing & Collecting tokens into groups

Handled entirely by [JASM](https://github.com/Nowilltolife/Jasm). 
The parsed tokens are transformed into logical groups which can be handled by a custom listener type in the next step.

### Step 2: Map to AST

JASM's groupings are tied to the bytecode format, which makes mapping them into our own AST model very easy.
Just a matter of implementing the right interfaces and plugging in values to our classes from the interfaces.

### Step 3: Validation of AST

We then have a series of validation passes on our AST model to ensure that there are no errors that prevent us 
from transforming the AST into bytecode. In addition to these validation passes, there is basic stack-analysis 
run on the AST which will emit additional errors and warnings depending on the severity of issues detected. 

While warnings can be ignored _(they should not be, unless the warning is shown due to a bug in the analyzer)_ 
errors cannot be and will need to be addressed. Once any errors from this step are resolved we can proceed to the next step.

### Step 4: Transformation

Now that we know our AST model does not contain errors preventing us from transforming we can just focus on actual
transformation on this step. All error handling is done in the prior step.

Once this step is complete we have some bytecode of a method body that we know is valid. 