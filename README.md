# MHP3rd Companion

Offline companion for MH Portable 3rd. Designed around Ayn Thor second screen, but adaptive layouts are added for phones and such. UI and features mostly tested on Ayn Thor second screen, while phone UI been tested less thoroughly. Its not a dual screen mod, it never do anything to you device or your emulators. Just a companion app with data about the game.

🛑**THIS APP HAS BEEN VIBECODED!**🤖

If its a deal breaker to you - you should know. Though, I was making original 1.0.0 version for roughly two months, while playing MHP3rd on a main screen, so there is been a lot of testing. 

Project: [github.com/waillio-pers/MHP3ThorCompanion](https://github.com/waillio-pers/MHP3ThorCompanion)

## Features

- All monsters from the game been added, Large and Small. Rewards, hitzones, recommended element and such.
- All items is added, I believe with most complete source information - percentages, amount of drops and so on. Data about sources been cross-referenced from different sources, to increase accuracy. All sources are present in "About" section in settings inside the app or in Sources.md. Also each item has all possible way to use it (in crafting or trading)
- Maps with interactive nodes. Each node are calibrated manually to increase accuracy with complete breakdown what you can get from low rang and high rang version of such node. Since its hard to click individual node on a map sometimes, there will show up a modal pop-up, allowing you to choose which node you were meant to press.
- Complete weapons trees. Less UI polished part of an app for my liking, but I cant figure out a better way to do it. However, its usable and understandable. You can get a crafting path to selected weapon, can sort by attack power and rarity, filter by element, decorations slots and such. Better to be used in landscape mode, but I made adjustments for narrow screens (it will be ugly, sorry).
- Complete skills and according decorations list. No need to click a ton of buttons inside interface to figure out what this or that skill do, hooray! With extended description.
- There is quests and trainings sections. Quests are not clickable, since there will be TOO much to show. However, each quests internally contain all information about possible rewards and such - so you can see where you can get Commendation or anything else. 
- All and all, almost everything you have in game is present. Only two missing pieces: armor and felyne skills from drinks. Armor Im not adding to plan, since, well, you can see it in game, nothing is hidden (as far as I know). But felyne skills I might add later, since descriptions are too vague for my liking.
- You can adjust UI scale in settings from 0.8 to 1.1 scale. All pieces of UI **should** be adaptive. Reach out if anything looks ugly on your device (except weapons section, thats Im aware of already)

Huge thanks for Team Maverick ONE for transalting this wonderful game. That made it accesible to me when I was a kid and has been improved since even more. And all english naming in this app corresponds to their translation.

**❗Okay, from this point goes AI generated part of a Readme.**

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
