audiobook-android
===

![Travis (.org)](https://img.shields.io/travis/NYPL-Simplified/audiobook-android.svg?style=flat-square)

### Compilation

```
$ ./gradlew clean assembleDebug test publishToMavenLocal
```

### Project Structure

The project is divided into separate modules. Programmers wishing to use the API will primarily be
concerned with the [Core API](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.api),
but will also need to add [providers](#providers) to the classpaths of their projects in order
to actually do useful work. The API is designed to make it easy to develop an event-driven user
interface, but this project also includes a ready-made [player UI](#player_ui) that can be embedded
into applications. Additionally, audio engine providers that do not, by themselves, handle downloads
require callers to provide a _download provider_. Normally, this code would be provided directly
by applications (as applications tend to have centralized code to handle downloads), but a
[simple implementation](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.downloads)
is available to ease integration.

|Module|Description|
|------|-----------|
| [org.librarysimplified.audiobook.api](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.api) | Core API
| [org.librarysimplified.audiobook.downloads](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.downloads) | A generic download provider for non-encrypted audio books
| [org.librarysimplified.audiobook.manifest.nypl](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.manifest.nypl) | NYPL manifest parser
| [org.librarysimplified.audiobook.mocking](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.mocking) | A mock implementation of the API for unit testing
| [org.librarysimplified.audiobook.open_access](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.open_access) | ExoPlayer-based audio player provider for non-encrypted audio books
| [org.librarysimplified.audiobook.rbdigital](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.rbdigital) | Functionality specific to RBDigital audio books
| [org.librarysimplified.audiobook.tests.device](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.tests.device) | Unit tests that execute on real or emulated devices
| [org.librarysimplified.audiobook.tests.sandbox](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.tests.sandbox) | A sandbox for quickly testing changes during development
| [org.librarysimplified.audiobook.tests](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.tests) | Unit tests that can execute without needing a real or emulated device
| [org.librarysimplified.audiobook.views](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.views) | UI components

### Changelog

The project currently uses [com.io7m.changelog](https://www.io7m.com/software/changelog/)
to manage release changelogs.

### Usage

1. Download (or synthesize) an [audio book manifest](#manifest_parsers). [Hadrien Gardeur](https://github.com/HadrienGardeur/audiobook-manifest/) publishes many example manifests in formats supported by the API.
2. Ask the API to [parse the manifest](#using_manifest_parsers).
3. Ask the API to [create an audio engine](#using_audio_engines) from the parsed manifest.
4. Make calls to the resulting [audio book](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerAudioBookType.kt) to download and play individual parts of the book.

See the provided [example project](https://github.com/NYPL-Simplified/audiobook-demo-android) for a
complete example that is capable of downloading and playing audio books.

### Dependencies

At a minimum, applications will need the Core API, one or more [manifest parser](#manifest_parsers)
implementations, and one or more [audio engine](#audio_engines) implementations. Use the following
Gradle dependencies to get a manifest parser that can parse the NYPL manifest format, and an audio
engine that can play non-encrypted audio books:

```
ext {
  nypl_audiobook_api_version = "1.0.0"
}

dependencies {
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.manifest.nypl:${nypl_audiobook_api_version}"
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.api:${nypl_audiobook_api_version}"
  implementation "org.librarysimplified.audiobook:org.librarysimplified.audiobook.open_access:${nypl_audiobook_api_version}"
}
```

### Versioning

The API is expected to follow [semantic versioning](https://semver.org/).

### Providers

The API uses a _service provider_ model in order to provide strong _modularity_ and to decouple
consumers of the API from specific implementations of the API. To this end, the API uses
[ServiceLoader](https://docs.oracle.com/javase/10/docs/api/java/util/ServiceLoader.html)
internally in order to allow new implementations of both [manifest parsers](#manifest_parsers) and
[audio engines](#audio_engines) to be registered and made available to client applications without
requiring any changes to the application code.

### Manifest Parsers <a id="manifest_parsers"/>

#### Overview

An audio book is typically delivered to the client via a _manifest_. A manifest is normally a
JSON description of the audio book that includes links to audio files, and other metadata. It is the
responsibility of a _manifest parser_ to turn a JSON AST into a typed manifest data structure
defined in the Core API.

#### Using Manifest Parsers <a id="using_manifest_parsers"/>

Programmers should make calls to the [PlayerManifests](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerManifests.kt)
class, passing in an input stream representing the raw bytes of a manifest. The methods return a
`PlayerResult` value providing either the parsed manifest or an exception indicating why parsing
failed. The `PlayerManifests` class asks each registered [manifest parser](#creating_manifest_parsers)
whether or not it can parse the given raw data and picks the first one that claims that it can.
Programmers are not intended to have to use instances of the [PlayerManifestParserType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerManifestParserType.kt)
directly.

#### Creating Manifest Parsers <a id="creating_manifest_parsers"/>

Programmers will generally not need to create new manifest parsers, but will instead use one or
more of the [provided implementations](https://github.com/NYPL-Simplified/audiobook-android/tree/develop/org.librarysimplified.audiobook.manifest.nypl).
However, applications needing to use a new and unsupported manifest format will need to
provide and register new manifest parser implementations.

In order to add a new manifest parser, it's necessary to define a new class that implements
the [PlayerManifestParserType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerManifestParserType.kt)
and defines a public, no-argument constructor. It's then necessary to register this class so that
`ServiceLoader` can find it by creating a resource file at
`META-INF/services/org.librarysimplified.audiobook.api.PlayerManifestParserType` containing the fully
qualified name of the new class. The standard [PlayerManifestParserNYPL](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest.nypl/src/main/java/org/librarysimplified/audiobook/manifest/nypl/PlayerManifestParserNYPL.kt)
class and its associated [service file](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.manifest.nypl/src/main/resources/META-INF/services/org.librarysimplified.audiobook.api.PlayerManifestParserType)
serve as minimal examples for new parser implementations. When a `jar` (or `aar`) file is placed on
the classpath containing both the class and the service file, `ServiceLoader` will find the
implementation automatically when the user asks for parser implementations.

Parsers are responsible for examining the given JSON AST and telling the caller whether or not they
think that they are capable of parsing the AST into a useful structure. For example,
[audio engine providers](#audio_engines) that require DRM might check the AST to see if the
required DRM metadata structures are present. The Core API will ask each parser implementation in
turn if the implementation can parse the given JSON, and the first implementation to respond in the
affirmative will be used. Implementations should take care to be honest; an implementation that
always claimed to be able to parse the given JSON would prevent other (possibly more suitable)
implementations from being considered.

### Audio Engines <a id="audio_engines"/>

#### Overview

An _audio engine_ is a component that actually downloads and plays a given audio book.

#### Using Audio Engines <a id="using_audio_engines"/>

Given a parsed [manifest](#using_manifest_parsers), programmers should make calls to the methods
defined on the [PlayerAudioEngines](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerAudioEngines.kt)
class. Similarly to the `PlayerManifests` class, the `PlayerAudioEngines` class will ask each
registered [audio engine implementation](#creating_audio_engines) in turn if it is capable of
supporting the book described by the given manifest. Please consult the documentation for that
class for information on how to filter and/or prefer particular implementations. The
(somewhat arbitrary) default behaviour is to select all implementations that claim to be able to
support the given book, and then select the implementation that advertises the highest version number.

#### Creating Audio Engines <a id="creating_audio_engines"/>

Implementations must implement the [PlayerAudioEngineProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerAudioEngineProviderType.kt)
interface and register themselves in the same manner as [manifest parsers](#creating_manifest_parsers).

Creating a new audio engine provider is a fairly involved process. The provided
[ExoPlayer-based implementation](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.open_access/src/main/java/org/librarysimplified/audiobook/open_access/ExoEngineProvider.kt)
may serve as an example for new implementations.

In order to reduce duplication of code between audio engines, the downloading of books is
abstracted out into a [PlayerDownloadProviderType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.api/src/main/java/org/librarysimplified/audiobook/api/PlayerDownloadProviderType.kt)
interface that audio engine implementations can call in order to perform the work of actually
downloading books. Implementations of this interface are actually provided by the calling programmer
as this kind of code is generally provided by the application using the audio engine.

### Player UI <a id="player_ui"/>

#### Overview

The API comes with a set of Android views and fragments that can be embedded into an application
to provide a simple user interface for the player API.

#### Using the UI

1. Declare an `Activity` that implements the [PlayerFragmentListenerType](https://github.com/NYPL-Simplified/audiobook-android/blob/develop/org.librarysimplified.audiobook.views/src/main/java/org/librarysimplified/audiobook/views/PlayerFragmentListenerType.kt).
2. Load a `PlayerFragment` instance into the activity.

Please consult the provided [example project](https://github.com/NYPL-Simplified/audiobook-demo-android)
and the documentation comments on the `PlayerFragmentListenerType` for details.

### Testing <a id="testing"/>

#### Overview

The project contains numerous unit tests, many of which are designed
to run _both_ locally and on real or emulated devices. The reason for
this is that, during development, it's desirable to be able to run the
tests locally to quickly experiment with changes; running the entire
suite on the local machine takes just a few seconds. However, prior
to deployment, it's both desirable and necessary to run those same
tests on a real device in order to shake out platform-specific bugs.
Running tests on a real device is slow; it typically takes minutes
to run the entire test suite and it would therefore make development
rather painful if this was the only way to run the tests.

In order to implement this, the project implements tests that must
run locally *and* on devices as abstract classes ("contracts")
in `src/main/java` in the `org.librarysimplified.audiobook.tests`
module. It then defines a set of classes that extend
the abstract test classes in `src/test/java` in the
`org.librarysimplified.audiobook.tests` module, and a set of classes that
extend the abstract test classes in `src/androidTest/java` in the
`org.librarysimplified.audiobook.tests.device` module. The latter are _instrumented
device tests_ and will run on real or emulated devices. The former
classes will run the tests locally.

Some tests will _only_ run on real devices because they have
hard dependencies on the Android API. These tests do not have any
corresponding abstract base classes.

#### Espresso

The test suite contains tests that will exercise user interface code
with [Espresso](https://developer.android.com/training/testing/espresso).
Unfortunately, Espresso appears to be rather fragile, and the following
points _must_ be observed if the test suite is to run correctly:

1. Device animations must be switched off. This can be achieved manually
   by changing all of the _animation scale_ settings in the device's
   developer options menu to 0. Alternatively, the following `adb`
   invocations achieve the same thing:

```
  adb shell settings put global window_animation_scale 0 &
  adb shell settings put global transition_animation_scale 0 &
  adb shell settings put global animator_duration_scale 0 &
```

2. The device must not be locked/sleeping during the test execution.
   This is not mentioned in the Espresso documentation. If the device
   is locked, many activities will not correctly go into the `RESUMED`
   state and the test code will not execute properly.


