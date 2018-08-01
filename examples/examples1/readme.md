From time to time in java applications researching time some of you may want to change already compiled java application (patch jar file). 
So, for patching jar files you can use Recaf tool. 

### How to use

In a first step, you need to download already compiled application from release. In this case I tested recaf-1.2.3.jar with Java 8. 

### Java bytecode, example 1, Hello 
To execute all application, which has written in java language, you need a Java virtual machine (JVM). 
Java bytecode is the instruction set of the JVM. 

So, let's see simple java application in bytecode view.

Here is a simple Java application which just printing **secret_KEY**

```java
  package test;
  public class test {
    public static void main(String[] args) {
      String my_costom_variable = "secret_KEY";
      System.out.println(my_costom_variable);
    }
  }
```
[img]
So, using Recaf application we can see this java application bytecode.
[img]
In 4-th step need to right-click in "main" name and choose *instructions* options.

```java
0 : LABEL A
1 : LINE 4 (0 : LABEL A)
2 : LOC "secret_KEY"
3 : ASTORE 1 (my_custom_variable:String)
4 : LABEL B
5 : LINE 5 (4 : LABEL B)
6 : GETSTATIC System.out PrintStream
7 : ALOAD 1 (my_custom_variable:String)
8 : IWOKEVIRTUAL PrintStream.println(String)void
9 : LABEL C
10: LINE 6 (9 : LABEL C)
11: RETURN
12: LABEL D
```
So,
```
0-1 lines is a like "entry point" of main function,
2-3 init of my_custom_variable 
6-7 init System.out with my_custom_variable parameter
8 call print function
9 exit of application
```
***JVM has a lot of instructions which you can get from this page https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings


