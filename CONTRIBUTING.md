# Contributing to Recaf

The following is a series of guidelines for contributing to Recaf. 
They're not _"rules"_ per say, rather they're more like goals to strive towards. 
Regardless of how closely you adhere to the following guidelines I really appreciate you taking the time to contribute, it means a lot :+1:

**Table of Contents**

 * [What if I am not a programmer?](#what-if-i-am-not-a-programmer)
 * [What should I know before I get started?](#what-should-i-know-before-getting-started)
 * [Is there a todo list?](#is-there-a-todo-list)
 * [Reporting Bugs](#reporting-bugs)
 * [Suggesting Features](#suggesting-features)
 * [Coding Guidelines](#coding-guidelines)
 * [Pull Requests](#pull-requests)
 * [Commit messages](#commit-messages)
 
**TLDR?**

Follow the style of the rest of the code. 
Comment your code where it makes sense. 
Make sure the unit tests pass before submitting a pull request. Follow the [commit message rules](#commit-messages).

## What if I am not a programmer?

[There is plenty to contribute that isn't based in code.](https://www.youtube.com/watch?v=GAqfMNB-YBU&t=603)

Check out the [non-technical board](https://github.com/Col-E/Recaf/projects/3) for further directions and ways to contribute.

## What should I know before getting started?

It depends on what changes you are making. 
For instance, changing the user-interface requires no reverse-engineering prior knowledge. 

When contributing features / fixes to components that revolve around the class file, see the recommended reading document: [PRIMER.md](PRIMER.md)

## Is there a todo list?

There's multiple places where _"TODO"_ items may be:

 * [Issues](https://github.com/Col-E/Recaf/issues)
 * [Project board](https://github.com/Col-E/Recaf/projects)

Additionally you can check for `// TODO:` messages in the source code. 
Not everything is given its own issue or project goal. 
Most IDE's have a feature to show all of these messages. 
These are typically smaller scale items than what appear on the issues/project board.

## Reporting Bugs

When creating an issue select the `Bug report` button. 
This will provide a template that you can fill in the details for your bug. 
Please include as much information as possible. 
This can include:

 * Clear and descriptive title
 * Log files
 * Steps to reproduce the bug 
 * An explanation of what you _\*expected\*_ to happen
 * The file being analyzed _(Do not share anything you do not own the rights to)_ 

## Suggesting Features

When creating an issue select the `Feature request` button. 
This will provide a template that you can fill in the details for your feature idea. 
Be as descriptive as possible with your idea. 

**Note**: Not all ideas may be within Recaf's scope. In this case it should be implemented using the plugin api. 
You can check out the template plugin project along with plugin documentation here: [Recaf-plugin-workspace](https://github.com/Col-E/Recaf-plugin-workspace)

## Coding Guidelines

Recaf uses Checkstyle to enforce a modified varient of the [Google Java guidelines](https://google.github.io/styleguide/javaguide.html). 
The default formatting of IntelliJ or Eclipse should work just fine. 
Don't auto-format entire classes at a time though. 
Only format code that you are modifying. 
This keeps the commits small and localized to the changes you're creating, making it easier for others to understand the intent behind each commit.

Aside from format enforcement there are also some code-complexity checks that prevent methods from being too large or having too much control flow. 
This is intended to cut down giant methods into smaller and more understandable chunks.

## Pull Requests

Before making a pull request make sure that your changes successfully compile and pass the unit tests. 
You can do so by running the following maven command: `mvn clean test`

When creating a pull request please consider the following when filling in the template:

 * Clear and descriptive title
 * A clear description of what changes are included in the pull

## Commit messages
This project follows the [Semantic Versioning](https://semver.org/) specification, which is completely automated through the Continuous Integration and [semantic-release](https://github.com/semantic-release/semantic-release). 
To make this possible, it is crucial to use the [Angular Commit Message Conventions](https://github.com/angular/angular.js/blob/master/DEVELOPERS.md#-git-commit-guidelines) in all of your commits, to allow the system to categorize your changes and take appropriate actions.

### Examples
* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing
  semi-colons, etc)
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing or correcting existing tests
* **chore**: Changes to the build process or auxiliary tools and libraries such as documentation
  generation