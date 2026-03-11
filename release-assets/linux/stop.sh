#!/usr/bin/env bash
set -euo pipefail

PIDS="$(pgrep -f 'DiscordModernMusicBot.jar' || true)"
if [[ -z "$PIDS" ]]; then
  echo "DiscordModernMusicBot is not running."
  exit 0
fi

echo "Stopping DiscordModernMusicBot..."
kill $PIDS || true
sleep 1

REMAINING="$(pgrep -f 'DiscordModernMusicBot.jar' || true)"
if [[ -n "$REMAINING" ]]; then
  kill -9 $REMAINING || true
fi

echo "DiscordModernMusicBot stopped."