# MewdekoMC (Paper)

Companion plugin for [Mewdeko](https://mewdeko.tech) that bridges your Paper server with your Discord bot. Chat, deaths, advancements, and join/leave events flow to Discord, and messages or commands can come back the other way over a persistent WebSocket connection.

## Requirements

- Paper 1.21+ (or a fork that ships the Paper API)
- Java 21+
- A running Mewdeko bot with the Minecraft integration enabled

## Installation

1. Drop `MewdekoMC-<version>.jar` into your server's `plugins/` folder.
2. Start the server once to generate `plugins/MewdekoMC/config.yml`.
3. Fill in `api-url` and `api-key` from the Mewdeko dashboard, then restart or run `/mewdeko reload`.

## Configuration

`plugins/MewdekoMC/config.yml`

```yaml
# WebSocket URL from the Mewdeko dashboard
api-url: ""

# API key from the Mewdeko dashboard
api-key: ""

# How often to push server status (seconds)
status-interval: 30

# Bridge in-game chat to Discord
chat-bridge: true

# Send join and leave notifications
join-leave-events: true

# Send death messages
death-messages: true

# Send advancement completions
advancement-messages: true

# Stream console output to Discord (disabled by default)
console-streaming: false
```

## Commands

All subcommands require the `mewdeko.admin` permission.

| Command | Description |
|---|---|
| `/mewdeko` | Show connection status |
| `/mewdeko status` | Show connection status |
| `/mewdeko reload` | Reload config from disk |
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

```bash
./gradlew shadowJar
```

Output: `build/libs/MewdekoMC-<version>.jar`

## License

MIT
