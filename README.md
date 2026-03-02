# PrimeCast Player

PrimeCast Player is a prototype IPTV application built with Jetpack Compose.

## Current module scope

- M3U playlist parser (`M3uParser`)
- Category detection for:
  - Live TV
  - Movies
  - Series
- Sample IPTV repository backed by a demo M3U playlist
- Main screen with:
  - category tabs
  - channel list
  - channel detail card
  - embedded stream preview using Media3 ExoPlayer
  - menu actions (placeholder flow with snackbar feedback)
- Unit tests for parser behavior

## Project structure

- `app/src/main/kotlin/com/example/primecastplayer/MainActivity.kt`
- `app/src/main/kotlin/com/example/primecastplayer/iptv/*`
- `app/src/test/java/com/example/primecastplayer/iptv/M3uParserTest.kt`
