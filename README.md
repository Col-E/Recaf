# Recaf [![Build Status](https://travis-ci.org/Col-E/Recaf.svg?branch=master)](https://travis-ci.org/Col-E/Recaf)
An easy to use modern Java bytecode editor based on Objectweb's ASM. No more hassling with the constant pool or stack-frames required.

* _[Usage](https://col-e.github.io/Recaf/usage.html)_
* _[Features](https://col-e.github.io/Recaf/features.html)_

### Download

See the [releases](https://github.com/Col-E/Recaf/releases) page for the latest build. Builds may become outdated at any time. It's recommended you build using maven. The usage guide covers how to do this.

### Libraries used:
* [ASM](http://asm.ow2.org/) - _Class editing abilities_
* [CFR](http://www.benf.org/other/cfr/) - _Decompilation_
* [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) - _Highlighting of decompilaton_
* [minimal-json](https://github.com/ralfstx/minimal-json) - _Json reading/writing for config storage_
* [InMemoryJavaCompiler](https://github.com/trung/InMemoryJavaCompiler) - _Recompiling via the decompiler_
* [picocli](http://picocli.info/) - _Command line argument parsing_

### Screenshots

![Screenshot](docs/screenshots/main.png)

For more screenshots check the [screenshots directory](docs/screenshots).