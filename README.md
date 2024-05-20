# Git4s

[![Build Status](https://github.com/geirolz/git4s/actions/workflows/cicd.yml/badge.svg)](https://github.com/geirolz/git4s/actions)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/0c050a5f75354469b166cd0717bb1072)](https://app.codacy.com/gh/geirolz/git4s/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/0c050a5f75354469b166cd0717bb1072)](https://app.codacy.com/gh/geirolz/git4s/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Sonatype Nexus (Releases)](https://img.shields.io/nexus/r/com.github.geirolz/git4s_3?server=https%3A%2F%2Foss.sonatype.org)](https://mvnrepository.com/artifact/com.github.geirolz/git4s)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://api.mergify.com/v1/badges/geirolz/git4s&style=flat)](https://mergify.io)
[![GitHub license](https://img.shields.io/github/license/geirolz/git4s)](https://github.com/geirolz/git4s/blob/main/LICENSE)

Functional and typesafe library to use git commands in Scala, based on cats, cats-effect and fs2.

```sbt
libraryDependencies += "com.github.geirolz" %% "git4s" % "0.0.2"
```

## Features
**general**
- [x] git help
- [x] git init

**installation**
- [x] git version
- [x] install git [brew | choco | apt-get]
- [x] uninstall git [brew | choco | apt-get]
- [x] reinstall git [brew | choco | apt-get]

**config**
- [x] git config --get [local | global]
- [x] git config --unset [local | global]
- [x] git config [local | global]

**tag**
- [x] git tag

**repository**
- [x] git status
- [x] git clone
- [x] git add
- [x] git commit
- [x] git push
- [x] git pull
- [x] git fetch
- [x] git checkout
- [x] git branch [create]
- [x] git reset [to commit | to HEAD~n]
- [x] git clean 
- [ ] git diff [WIP]

## Usage

You can create a Git4s instance using the `apply` method.

Logging is done using the `CmdLogger` type class implicitly passed to each method. You can provide your own
implementation of `CmdLogger` to log the command output as you like, or use the default one provided by the library.
By default, the library uses the `Noop` logger which doesn't log anything since usually these logs are useful just
for debugging purpose.

Example:

```scala
import cats.effect.IO
import git4s.Git4s
import git4s.data.GitVersion
import git4s.logging.*

given logger: CmdLogger[IO]
= CmdLogger.console[IO](LogFilter.all)
val result: IO[GitVersion] = Git4s[IO].version
// result: IO[GitVersion] = FlatMap(
//   ioe = Uncancelable(
//     body = cats.effect.IO$$$Lambda/0x0000000303605958@b5734aa,
//     event = cats.effect.tracing.TracingEvent$StackTrace
//   ),
//   f = fs2.Stream$CompileOps$$Lambda/0x0000000303605d20@2d0e0e97,
//   event = cats.effect.tracing.TracingEvent$StackTrace
// )
```

## Contributing

We welcome contributions from the open-source community to make Git4s even better. If you have any bug reports,
feature requests, or suggestions, please submit them via GitHub issues. Pull requests are also welcome.

Before contributing, please read
our [Contribution Guidelines](https://github.com/geirolz/cats-git/blob/main/CONTRIBUTING.md) to understand the
development process and coding conventions.

Please remember te following:

- Run `sbt scalafmtAll` before submitting a PR.
- Run `sbt gen-doc` to update the documentation.

## License

Git4s is released under the [Apache License 2.0](https://github.com/geirolz/git4s/blob/main/LICENSE).
Feel free to use it in your open-source or commercial projects.
