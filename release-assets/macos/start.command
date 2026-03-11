#!/bin/zsh
set -e
cd "$(dirname "$0")"

CONFIG_FILE="ModernMusicBot.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing $CONFIG_FILE"
  echo "Copy ModernMusicBot.properties.example to $CONFIG_FILE and set your bot token."
  read -r "reply?Press Enter to close..."
  exit 1
fi

JAVA_BIN=""
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
else
  JAVA_BIN="$(command -v java || true)"
fi

if [[ -z "$JAVA_BIN" ]]; then
  echo "Java not found. Install Java 25 and try again."
  read -r "reply?Press Enter to close..."
  exit 1
fi

"$JAVA_BIN" --enable-native-access=ALL-UNNAMED -jar DiscordModernMusicBot.jar "$CONFIG_FILE"
read -r "reply?Bot stopped. Press Enter to close..."