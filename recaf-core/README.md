# Core

The core module contains logic necessary to use Recaf strictly as a library, 
and the basis to create UI presentations off of.

## Lifecycle of Recaf usage

1. A `Presentation` instance is instantiated, determining how to display Recaf. 
   This can be through a GUI as implemented in the `recaf-ui` module, 
   or an `EmptyPresentation` to display nothing at all for library and automated usage.
2. A `Controller` instance is instantiated taking in the `Presentation`.
3. A `Workspace` instance is created from some user input. 
   This updates the controller's available `Services` and passes data along to the `Presentation` to display the new workspace.
4. Content in the workspace is viewed and modified by the user.
5. The workspace is exported back to a file _(such as a `.jar`)_ and the user closes the application / process ends.

## Packages & features

- `android` outlines data types for android support.
- `code` outlines data types for classes/fields/methods and more. These types reside within `Resource` types.
- `compile` provides access to Java compiler implementations.
  - By default, only `javac` is available.
- `decompile` provides access to Java decompiler implementations.
  - `cfr` is a standard decompiler, from [benf.org](http://www.benf.org/other/cfr/)
  - `fallback` is a disassembling decompiler intended for usage when all others fail. 
  - `jadx` is a standard decompiler designed for Android but supports standard Java decompilation, from [skylot/jadx](https://github.com/skylot/jadx)
  - `procyon` is a standard decompiler, from [mstrobel/procyon](https://github.com/mstrobel/procyon)
  - `vine` is a standard decompiler forked from JetBrain's FernFlower, from [Vineflower/vineflower](https://github.com/Vineflower/vineflower)
- `graph` provides graphing capabilities such as inheritance structures of classes and interfaces.
- `io` provides various IO types for internal usage.
- `launch` provides data types populated by [pico-cli](https://picocli.info/) to handle parsing launch arguments.
- `mapping` provides remapping capabilities.
  - `gen` provides automated mapping generation capabilities.
  - `format` provides mapping format implementations which can be parsed and exported to.
- `parse` provides Java source code parsing capabilities via [JavaParser](https://github.com/javaparser/javaparser)
  - `jpimpl` defines custom declaration types used by JavaParser's type-solving logic to support bytecode backed solving.
- `plugin` outlines types intended to be implemented by plugins, providing consolidated access to all implementations.
  - `tools` defines tool outlines. Examples of implementations: `compile`, `decompile`, `mapping.format` packages.
- `presentation` outlines how front-ends for Recaf receive data on internal updates. See the `recaf-ui` module for an implementation.
- `search` provides various search capabilities for strings, numbers, declarations, and references.
- `ssvm` provides sand-boxed VM capabilities via [SSVM](https://github.com/xxDark/SSVM)
- `workspace` outlines the workspace structure.