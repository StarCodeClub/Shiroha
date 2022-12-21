# 这是 `StarCraft` 的内部项目
## Tuinty 魔改版

<div align=center>
    <!-- ./blob/master/tuinity-logo.webp -->
    <img src="./tuinity-logo.webp" width="512">
    <br /><br />
    <p>Fork of <a href="https://github.com/PaperMC/Paper">Paper</a> aimed at improving server performance at high playercounts.</p>
    <img alt="Java CI" src="https://github.com/Spottedleaf/Tuinity/workflows/Java%20CI/badge.svg">
    <a href="https://irc.spi.gt/iris/?channels=tuinity">
        <img alt="IRC" src="https://img.shields.io/badge/irc-%23tuinity-%23DF8826">
    </a>
    <a href="https://discord.gg/tuinity">
        <img alt="Discord" src="https://img.shields.io/badge/discord-discord.gg%2Ftuinity-%237289da">
    </a>
</div>

## How To (Server Admins)
Tuinity uses the same paperclip jar system that Paper uses.

You can download the latest build of Tuinity by going [here](https://ci.codemc.io/job/Spottedleaf/job/Tuinity/).

You can also [build it yourself](https://github.com/Spottedleaf/Tuinity#building).

## How To (Plugin developers)
In order to use Tuinity as a dependency you must [build it yourself](https://github.com/Spottedleaf/Tuinity#building).
Each time you want to update your dependency you must re-build Tuinity.

Tuinity-API maven dependency:
```xml
<dependency>
    <groupId>com.tuinity</groupId>
    <artifactId>tuinity-api</artifactId>
    <version>1.16.5-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
 </dependency>
 ```

Tuinity-Server maven dependency:
```xml
<dependency>
    <groupId>com.tuinity</groupId>
    <artifactId>tuinity</artifactId>
    <version>1.16.5-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

There is no repository required since the artifacts should be locally installed
via building Tuinity.

## Building

Requirements:
- You need `git` installed, with a configured user name and email. 
   On Windows you need to run from git bash.
- You need `maven` installed.
- You need `jdk` 8+ installed to compile (and `jre` 8+ to run).
- Anything else that `paper` requires to build.

If all you want is a paperclip server jar, just run `./tuinity jar`.

Otherwise, to setup the `Tuinity-API` and `Tuinity-Server` repo, just run the following command
in your project root `./tuinity patch` additionally, after you run `./tuinity patch` you can run `./tuinity build` to build the 
respective api and server jars.

`./tuinity patch` should initialize the repo such that you can now start modifying and creating
patches. The folder `Tuinity-API` is the api repo and the `Tuinity-Server` folder
is the server repo and will contain the source files you will modify.

#### Creating a patch
Patches are effectively just commits in either `Tuinity-API` or `Tuinity-Server`.
To create one, just add a commit to either repo and run `./tuinity rb`, and a
patch will be placed in the patches folder. Modifying commits will also modify its
corresponding patch file.

## License
The PATCHES-LICENSE file describes the license for api & server patches,
found in `./patches` and its subdirectories except when noted otherwise.

Everything else is licensed under the MIT license, except when note otherwise.
See https://github.com/starlis/empirecraft and https://github.com/electronicboy/byof
for the license of material used/modified by this project.

### Note

The fork is based off of aikar's EMC framework found [here](https://github.com/starlis/empirecraft).
