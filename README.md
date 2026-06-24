# MewdekoMC (NeoForge)

Companion mod for [Mewdeko](https://mewdeko.tech) that bridges your NeoForge server with your Discord bot. Chat, deaths, advancements, and join/leave events flow to Discord, and messages or commands can come back the other way over a persistent WebSocket connection.

## Requirements

- NeoForge 21.1.x (Minecraft 1.21.1)
- Java 21+
- A running Mewdeko bot with the Minecraft integration enabled

## Installation

1. Drop `mewdeko-mc-forge-<version>-all.jar` into your server's `mods/` folder.
2. Start the server once to generate `config/mewdeko-server.toml`.
3. Fill in `api_url` and `api_key` from the Mewdeko dashboard, then restart the server.

## Configuration

`config/mewdeko-server.toml` (generated on first run)

```toml
# WebSocket URL from the Mewdeko dashboard
api_url = ""

# API key from the Mewdeko dashboard
api_key = ""

# How often to push server status (seconds)
status_interval = 30

# Bridge in-game chat to Discord
chat_bridge = true

# Send join and leave notifications
join_leave_events = true

# Send death messages
death_messages = true

# Send advancement completions
advancement_messages = true

# Stream console output to Discord (disabled by default)
console_streaming = false
```

## Commands

All subcommands require operator level 2.

| Command | Description |
|---|---|
| `/mewdeko` | Show connection status |
| `/mewdeko status` | Show connection status |
| `/mewdeko reconnect` | Drop and re-establish the bot connection |

## Events sent to Discord

| Event | Payload type |
|---|---|
| Player chat | `chat` |
| Player join | `player_join` |
| Player leave | `player_leave` |
| Player death | `death` |
| Advancement earned | `advancement` |
| Server status | `server_status` (periodic + on join/leave) |

## Events received from Discord

| Type | Effect |
|---|---|
| `chat` | Broadcasts a Discord message to all players |
| `command` | Runs a command as the console |
| `broadcast` | Broadcasts a plain message to all players |

## Building

Requires Java 21. If your system Java is newer, the wrapper will handle it via `gradle.properties`.

```bash
./gradlew jarJar
```

Output: `build/libs/mewdeko-mc-forge-<version>-all.jar` (includes OkHttp and Kotlin stdlib)

## License

MIT
