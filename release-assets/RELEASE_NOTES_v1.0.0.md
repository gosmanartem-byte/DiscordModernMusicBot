# DiscordModernMusicBot v1.0.0

## Highlights

- Desktop control panel for starting, stopping, and saving bot settings without editing files manually.
- Localized bot status and interactive player panel with a control-panel language selector.
- Rich in-chat player panel with playback buttons, queue preview, volume, bass, voice status, and refresh support.
- Slash commands and prefix commands for playback, queue control, settings, and diagnostics.
- Persistent per-server settings stored in SQLite, including prefix, language, DJ role, default volume, and autoplay.
- DJ and admin permission controls for playback-sensitive commands.
- Expanded queue tools: remove, shuffle, clear, loop modes, seek, autoplay, and health/audio diagnostics.
- GitHub Actions CI build workflow for repeatable package validation on GitHub.

## Included Commands

- Playback: `/play`, `/pause`, `/resume`, `/skip`, `/stop`, `/player`
- Queue: `/queue`, `/remove`, `/shuffle`, `/clear`, `/loop`, `/seek`
- Sound: `/volume`, `/bass`, `!loud`, `!normal`
- Server settings: `/settings`, `/setprefix`, `/setlang`, `/setdj`, `/autoplay`
- Diagnostics: `/health`, `/debugaudio`

## Packaging

- macOS release includes native macOS launcher scripts.
- Linux releases are split into `linux-x86-64` and `linux-aarch64` archives.
- Windows release includes Windows launcher scripts.
- Fresh ZIPs for all targets were rebuilt from the current source state for this release.

## Notes

- Commands and command output remain in English by design.
- Localization currently applies to bot status text and the player UI.
- GitHub Actions is green for the repository after the workflow-scope credential fix.