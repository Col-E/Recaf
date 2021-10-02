# Recaf 3.X - Dev branch [![Discord](https://img.shields.io/discord/443258489146572810.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/Bya5HaA) [![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](CONTRIBUTING.md)

![preview](docs/main.png)

This is the 3rd redesign branch. This will not be ready for general usage for quite a while. 
However, we would more than appreciate if it if you tried it anyways and gave us feedback.

You can do so on the discord server, or in a new issue.

## Modules

### Core

The core module is only what Recaf needs to run as a library. Workspace stuff, and any additional utilities provided. No UI or any of those dependencies. 
The only UI aspects included are skeleton interfaces that the UI module will implement.

### UI

The UI module will include the JavaFX GUI and the headless CLI. They should look very similar to how 2.X has them since the 3.X redesign is backend-oriented.

### Launcher

The launcher will be what is provided in each release. It ideally will only rarely have to be modified. Responsibilities include:

1. Downloading new Recaf components (Core/UI, probably fatJar'd together)
2. Running the latest locally installed version with the proper arguments and classpath _(No more classpath injection shenanigans)_
3. Tracking the entire changelog so users can see exactly what happened between what version they have, and the latest.

## Usage

To build, run `gradlew clean shadowJar`, which will generate `recaf-ui\build\libs\recaf-$VERSION$-SNAPSHOT-J8-jar-with-dependencies.jar`.

To run from inside an IDE, the main class in the `UI` module is `me.coley.recaf.RecafUI`.

## Development

See the [Contributing guide](CONTRIBUTING.md).