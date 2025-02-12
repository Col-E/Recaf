# Contributing to Recaf

The following is a series of guidelines for contributing to Recaf. They're not _"rules"_ per say, rather they're more like goals to strive towards. Regardless of how closely you adhere to the following guidelines I really appreciate you taking the time to contribute, it means a lot :+1:

**Table of Contents**

- [What if I am not a programmer?](#what-if-i-am-not-a-programmer)
- [What should I know before I get started?](#what-should-i-know-before-getting-started)
- [Is there a todo list?](#is-there-a-to-do-list)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Coding Guidelines](#coding-guidelines)
- [Pull Requests](#pull-requests)

**TLDR?**

- Follow the code style.
- Document and comment your code.
- Make sure the tests pass after making changes.
- Translations and feature ideas are appreciated too.

**Questions?**

You can DM `invokecoley` on discord, or join the [Recaf discord](https://discord.gg/Bya5HaA).

## What if I am not a programmer?

[There is plenty to contribute that isn't based in code.](https://www.youtube.com/watch?v=GAqfMNB-YBU&t=603)

For example, you can contribute ideas, add translations, or write documentation:

- [Documentation source](https://github.com/Col-E/recaf-site)
- [Documentation site](https://recaf.coley.software/)

## What should I know before getting started?

It depends on what changes you are making. For instance, changing the user-interface requires very minimal or no reverse-engineering prior knowledge. If you do need JVM reversal knowledge to work on a feature you can check the [primer guide](PRIMER.md) which points to several good resources and outlines key details. For a general understanding of how Recaf works and how to begin creating contributions you can read our [getting started](https://recaf.coley.software/dev/getting-started.html) page as well.

## Is there a to-do list?

Unfortunately the to-do list is scattered around a few places. We're working on eventually consolidating everything into one place.

## Reporting Bugs

When creating an issue select the `Bug report` button.
This will provide a template that you can fill in the details for your bug.
Please include as much information as possible.
This can include:

- Clear and descriptive title
- Log files
- Steps to reproduce the bug
- An explanation of what you _\*expected\*_ to happen
- The file being analyzed _(Do not share anything you do not own the rights to)_

## Suggesting Features

When creating an issue select the `Feature request` button.
This will provide a template that you can fill in the details for your feature idea.
Be as descriptive as possible with your idea.

**Note**: Not all ideas may be within Recaf's scope. In these cases the feature should be implemented as a script or plugin.

## Coding Guidelines

**Style**: IDE code formatting rules can be found in the [`/setup` directory](setup/).

**Commits**: Try and keep commits small and focused on one thing at a time.

## Pull Requests

When creating a pull request please consider the following when filling in the template:

- Clear and descriptive title
- A clear description of what changes are included in the pull

Github's PR system will validate that your changes compile and pass the unit tests as well.