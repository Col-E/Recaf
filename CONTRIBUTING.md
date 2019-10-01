# Contributing to Recaf

The following is a series of guidelines for contributing to Recaf. They're not _"rules"_ per say, rather they're more like goals to strive towards. Regardless of how closely you adhere to the following guidelines I really appreciate you taking the time to contribute, it means a lot :+1:


**Table of Contents**

 * [What should I know before I get started?](#what-should-i-know-before-getting-started)
 * [Is there a todo list?](#is-there-a-todo-list)
 * [Reporting Bugs](#reporting-bugs)
 * [Suggesting Features](#suggesting-features)
 * [Coding Guidelines](#coding-guidelines)
 * [Pull Requests](#pull-requests)
 
**TLDR?**

Follow the style of the rest of the code. Comment your code where it makes sense. Make sure the unit tests pass before submitting a pull request.

## What should I know before getting started?

It depends on what changes you are making. For instance, changing the user-interface requires no reverse-engineering prior knowledge. 

When contributing features / fixes to components that revolve around the class file, see the reccomended reading document: [PRIMER.md](PRIMER.md)

## Is there a todo list?

There's multiple places where _"TODO"_ items may be:

 * [Issues](https://github.com/Col-E/Recaf/issues)
 * [Project board](https://github.com/Col-E/Recaf/projects)

Additionally you can check for `// TODO:` messages in the source code. Not everything is given its own issue or project goal. Most IDE's have a feature to show all of these messages. These are typically smaller scale items than what appear on the issues/project board.

## Reporting Bugs


When creating an issue select the `Bug report` button. This will provide a template that you can fill in the details for your bug. Please include as much information as possible. This can include:

 * Clear and descriptive title
 * Log files
 * Steps to reproduce the bug 
 * An explaination of what you _\*expected\*_ to happen
 * The file being analyzed _(Do not share anything you do not own the rights to)_ 

## Suggesting Features

When creating an issue select the `Feature request` button. This will provide a template that you can fill in the details for your feature idea. Be as descriptive as possible with your idea. 

## Coding Guidelines

Recaf uses Checkstyle to enforce a modified varient of the [Google Java guidelines](https://google.github.io/styleguide/javaguide.html). The default formatting of IntelliJ or Eclipse should work just fine. Don't auto-format entire classes at a time though. Only format code that you are modifying. This keeps the commits small and localized to the changes you're creating, making it easier for others to understand the intent behind each commit.

Aside from format enforcement there are also some code-complexity checks that prevent methods from being too large or having too much control flow. This is intended to cut down giant methods into smaller and more understandable chunks.

## Pull Requests

Before making a pull request make sure that your changes successfully compile and pass the unit tests. You can do so by running the following maven command: `mvn clean test`

When creating a pull request please consider the following when filling in the template:

 * Clear and descriptive title
 * A clear description of what changes are included in the pull