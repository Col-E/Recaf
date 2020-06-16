# Recaf [![Discord](https://img.shields.io/discord/443258489146572810.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/Bya5HaA) [![Build Status](https://cloud.drone.io/api/badges/Col-E/Recaf/status.svg)](https://cloud.drone.io/Col-E/Recaf) ![downloads](https://img.shields.io/github/downloads/Col-E/Recaf/total.svg) [![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](CONTRIBUTING.md)

![screenshot of recaf](docs/screenshots/main.png)

An easy to use modern Java bytecode editor that abstracts away the complexities of Java programs. 
Recaf will automatically handle generation of stack frames and constant pool entries for you.

* _[Usage & Documentation](https://col-e.github.io/Recaf/documentation.html)_

### Download

See the [releases](https://github.com/Col-E/Recaf/releases) page for the latest build.

## Preface

If you're just getting started with reverse-engineering in Java, read [PRIMER.md](PRIMER.md). Then check the documentaiton pages.

## Contributing 

Even if you're not a developer you can still contribute to Recaf. Reporting bugs and suggesting ideas is very helpful! 
Check out the [contribution guide here](CONTRIBUTING.md) for more information.

## Setting up the project

Clone the repository via `git clone https://github.com/Col-E/Recaf.git`

Open the project in an IDE or generate the build with maven.

**IDE**:
  1. Import the project from the `pom.xml`
      * [IntelliJ](https://www.jetbrains.com/help/idea/maven-support.html#maven_import_project_start)
      * [Eclipse](https://stackoverflow.com/a/36242422)
  2. Create a run configuration with the main class `me.coley.recaf.Recaf`
  
**Without IDE**:
  1. Execute `build`
      * Follow the prompt in the script to build the project.
  2. Run the generated build: `java -jar target/recaf-{version}-jar-with-dependencies.jar`

For additional information, join the [Discord server _(https://discord.gg/Bya5HaA)_](https://discord.gg/Bya5HaA) and check the [blog post on getting started](https://coley.software/recaf-getting-started-primer/).
