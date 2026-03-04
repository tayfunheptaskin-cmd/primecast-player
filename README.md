# PrimeCast Player

PrimeCast Player is a prototype IPTV application built with Jetpack Compose.

## Current module scope

- M3U playlist parser (`M3uParser`)
- Remote M3U loading from URL (`CachedIptvRepository`)
- Local cache fallback when network fetch fails
- Category detection for:
  - Live TV
  - Movies
  - Series
- Built-in sample playlist fallback for first run or hard failures
- Main screen with:
  - category tabs
  - channel list
  - channel detail card
  - embedded stream preview using Media3 ExoPlayer
  - playlist URL dialog (Manage Playlists / top bar action)
  - refresh action to force reload from network
- Unit tests for parser behavior

## Project structure

- `app/src/main/kotlin/com/example/primecastplayer/MainActivity.kt`
- `app/src/main/kotlin/com/example/primecastplayer/iptv/*`
- `app/src/test/java/com/example/primecastplayer/iptv/M3uParserTest.kt`
