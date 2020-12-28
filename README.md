# Recaf 3.X - Dev branch [![Discord](https://img.shields.io/discord/443258489146572810.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/Bya5HaA) [![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](CONTRIBUTING.md)

This is the 3rd redesign branch.

## General idea

The problem with 2.X is that it is still too tightly coupled with the GUI/CLI. Attempting to base internals off of commands was also dumb.

So instead this version will design Recaf to work initially as just a library. No GUI or CLI will be needed for core operations.

There will be the following modules:

1. Core
2. UI
3. Launcher

## Core

The core module is only what Recaf needs to run as a library. Workspace stuff, and any additional utilities provided. No UI or any of those dependencies. 
The only UI aspects included are skeleton interfaces that the UI module will implement.

## UI

The UI module will include the JavaFX GUI and the headless CLI. They should look very similar to how 2.X has them since the 3.X redesign is backend-oriented.

## Launcher

The launcher will be what is provided in each release. It ideally will only rarely have to be modified. Responsibilities include:

1. Downloading new Recaf components (Core/UI, probably fatJar'd together)
2. Running the latest locally installed version with the proper arguments and classpath _(No more classpath injection shenanigans)_
3. Tracking the entire changelog so users can see exactly what happened between what version they have, and the latest.