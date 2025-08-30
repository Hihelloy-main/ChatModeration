[![Maven CI](https://img.shields.io/github/actions/workflow/status/Hihelloy-main/ChatModeration/maven.yml?branch=master&style=flat-square)](https://github.com/Hihelloy-main/ChatModeration/actions)
[![GitHub release](https://img.shields.io/github/v/release/Hihelloy-main/ChatModeration?style=flat-square)](https://github.com/Hihelloy-main/ChatModeration/releases)
[![Github Downloads](https://img.shields.io/github/downloads/Hihelloy-main/ChatModeration/total.svg)](https://github.com/Hihelloy-main/ChatModeration/releases)
![SpigotMC Downloads](https://img.shields.io/spiget/downloads/128458?label=Spigot%20Downloads)

# ChatModerator - AI-Powered Minecraft Chat Moderation Plugin

A sophisticated Minecraft Spigot plugin that uses AI technology to automatically moderate chat messages, keeping your server safe and friendly.

## Features

- **AI-Powered Moderation**: Uses OpenAI's moderation API to detect inappropriate content
- **Universal Compatibility**: Works seamlessly on Spigot, Paper, and Folia servers with automatic detection
- **Configurable Word Filter**: Block specific words with a customizable blacklist
- **Flexible Configuration**: Adjust moderation sensitivity and behavior
- **Admin Tools**: Commands to manage the plugin and word lists
- **Permission System**: Bypass moderation for trusted players
- **Real-time Notifications**: Alert administrators of violations
- **Comprehensive Logging**: Track all moderation actions

## Installation

1. Download the plugin JAR file
2. Stop your server
3. Place it in your server's `plugins/` folder
4. Start your server to generate the configuration files
5. Configure your OpenAI API key in `plugins/ChatModerator/config.yml`
6. Restart your server or use `/chatmod reload`

## Configuration

### OpenAI API Setup

1. Get an API key from [OpenAI](https://platform.openai.com/api-keys)
2. Edit `plugins/ChatModerator/config.yml`
3. Replace `your-openai-api-key-here` with your actual API key

### Blocked Words

Add words to the blocked list in the configuration:

```yaml
moderation:
  blocked-words:
    - "badword1"
    - "inappropriate"
    - "spam"
```

### AI Moderation Thresholds

Adjust sensitivity for different categories:

```yaml
moderation:
  thresholds:
    hate: 0.3          # Lower = more sensitive
    harassment: 0.3
    sexual: 0.5
    violence: 0.3
```

## Commands

- `/chatmod reload` - Reload configuration
- `/chatmod status` - Show plugin status
- `/chatmod toggle` - Enable/disable moderation
- `/chatmod add-word <word>` - Add word to block list
- `/chatmod remove-word <word>` - Remove word from block list
- `/chatmod unmute <player>` - Unmute a player after they've said a blocked word
- `/chatmod aitest <word>` - Test your api key

## Permissions

- `chatmoderator.admin` - Access to all commands (default: op)
- `chatmoderator.bypass` - Bypass chat moderation (default: false)

## How It Works

1. **Message Interception**: The plugin listens for all chat messages
2. **Server Detection**: Automatically detects Folia and uses appropriate schedulers
3. **Word Filter Check**: First checks against your custom blocked words
4. **AI Analysis**: Sends message to OpenAI for content analysis
5. **Action Execution**: Blocks inappropriate messages and notifies relevant parties

## Requirements

- Minecraft Server 1.20.2+
- Spigot, Paper, Folia, or Luminol
- Java 17+
- OpenAI API key (for AI moderation)
- Internet connection

## Building from Source

```bash
git clone <repository-url>
cd chat-moderator
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Support

For issues or feature requests, please create an issue in the repository or contact the plugin developer.

## License

This project is licensed under the MIT License.
