#!/bin/zsh
set -e
JAR_NAME_PATTERN="modern-bot-1.0.0.*jar-with-dependencies.jar"
PIDS=$(pgrep -f "$JAR_NAME_PATTERN" || true)
if [[ -z "$PIDS" ]]; then
  echo "ModernMusicBot is not running."
  read -r "reply?Press Enter to close..."
  exit 0
fi

echo "Stopping ModernMusicBot..."
kill $PIDS || true
sleep 1

REMAINING=$(pgrep -f "$JAR_NAME_PATTERN" || true)
if [[ -n "$REMAINING" ]]; then
  echo "Force stopping remaining process(es): $REMAINING"
  kill -9 $REMAINING || true
fi

echo "ModernMusicBot stopped."
read -r "reply?Press Enter to close..."
