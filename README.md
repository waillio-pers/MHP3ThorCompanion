# MHP3rd Companion

A fan-made offline companion for *Monster Hunter Portable 3rd* on Android. It was originally designed for the Ayn Thor and also adapts to regular Android phones, landscape devices, and larger tablets.

Project: [github.com/waillio-pers/MHP3ThorCompanion](https://github.com/waillio-pers/MHP3ThorCompanion)

## Features

- Large and Small Monster reference, including hitzones and rewards
- Item sources and crafting usage
- Gathering maps
- Weapon trees, filters, and sorting
- Skills and Jewels
- Quests, Training, and Yukumo Farm information
- Palico Expeditions
- Adjustable UI scale and adaptive layouts
- Works offline

## Screens and compatibility

The Ayn Thor is the original target. The interface adapts to portrait and landscape phones and larger screens, including tablets. Android layouts and device configurations vary, so this is not a claim of exhaustive compatibility with every device.

## Installation

Download **MHP3rd-Companion-1.0.0.apk** from [GitHub Releases](https://github.com/waillio-pers/MHP3ThorCompanion/releases). Android may ask you to allow installation from the app or browser used to open the APK. Only grant that permission to a source you trust; there is no need to disable Android's general security protections.

## Build from source

You need JDK 21, Android SDK Platform 36, and the Android SDK build tools required by the project. JDK 21 is also needed by the current Android SDK 36-backed JVM test suite. After cloning the repository, run:

```sh
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`. A debug build does not require release-signing credentials. For maintainers preparing a signed release, see [docs/RELEASE-SIGNING.md](docs/RELEASE-SIGNING.md).

To run the current Python integrity tests, install Python 3 and the dependencies in `requirements-test.txt`, then run `python -m pytest` from the repository root.

## Data and credits

The included data was assembled and cross-checked against several Monster Hunter community references and uses vocabulary from the Team Maverick ONE English translation. The public source and attribution notes are in [data/SOURCES.md](data/SOURCES.md). The accepted production dataset is included; the internal research archive used to prepare it is not.

## License

The [MIT License](LICENSE) applies to original project source code only. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the scope and third-party material notice.

## Disclaimer

This is an unofficial fan-made project. It is not affiliated with or endorsed by CAPCOM. Monster Hunter and related names, artwork, trademarks, and game assets belong to their respective owners.
