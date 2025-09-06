[![Maven CI](https://img.shields.io/github/actions/workflow/status/Hihelloy-main/ChatModeration/maven.yml?branch=master&style=flat-square)](https://github.com/Hihelloy-main/ChatModeration/actions)
[![GitHub release](https://img.shields.io/github/v/release/Hihelloy-main/ChatModeration?style=flat-square)](https://github.com/Hihelloy-main/ChatModeration/releases)
[![Github Downloads](https://img.shields.io/github/downloads/Hihelloy-main/ChatModeration/total.svg)](https://github.com/Hihelloy-main/ChatModeration/releases)
![SpigotMC Downloads](https://img.shields.io/spiget/downloads/128458?label=Spigot%20Downloads)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/chatmoderator?label=Modrinth%20Downloads)

# ChatModerator - AI-Powered Minecraft Chat Moderation Plugin

A sophisticated Minecraft plugin that automatically moderates chat messages using AI, keeping your server safe and friendly. ChatModerator now supports **both OpenAI and Gemini AI providers** for content analysis, giving you flexibility and reliability.

## Features

* **AI-Powered Moderation**: Analyze chat messages with OpenAI or Gemini APIs to detect inappropriate content.
* **Universal Compatibility**: Works seamlessly on Spigot, Paper, Folia, and Luminol servers with automatic detection.
* **Configurable Word Filter**: Block specific words with a customizable blacklist.
* **Flexible AI Thresholds**: Adjust sensitivity for different categories like hate speech, harassment, sexual content, and violence.
* **Admin Tools**: Manage the plugin, word lists, and moderation behavior via commands.
* **Permission System**: Allow trusted players to bypass moderation.
* **Real-time Notifications**: Alert administrators when violations occur.
* **Comprehensive Logging**: Track all moderation actions and AI decisions.
* **AI Test Command**: Quickly test your API key with sample messages.
* **Folia/Luminol Scheduler Support**: Ensures tasks run correctly on threaded server implementations.

## Installation

1. Download the plugin JAR file.
2. Stop your server.
3. Place the JAR in your server's `plugins/` folder.
4. Start your server to generate configuration files.
5. Configure your OpenAI or Gemini API key in `plugins/ChatModerator/config.yml`.
6. Restart your server or run `/chatmod reload`.

## Configuration

### AI Provider Setup

* **OpenAI**: Get an API key from OpenAI and replace `your-openai-api-key-here` in the config.
* **Gemini**: Get an API key from Gemini and replace `your-gemini-api-key-here` in the config.

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

* `/chatmod reload` - Reload configuration
* `/chatmod status` - Show plugin status
* `/chatmod toggle` - Enable/disable moderation
* `/chatmod add-word <word>` - Add word to block list
* `/chatmod remove-word <word>` - Remove word from block list
* `/chatmod unmute <player>` - Unmute a player after a blocked word
* `/chatmod aitest <message>` - Test your API key with a sample message

## Permissions

* `chatmoderator.admin` - Access to all commands (default: op)
* `chatmoderator.bypass` - Bypass chat moderation (default: false)

## How It Works

1. **Message Interception**: Listens to all chat messages.
2. **Server Detection**: Detects Folia/Luminol and uses appropriate schedulers.
3. **Word Filter Check**: Checks messages against your custom blocked words.
4. **AI Analysis**: Sends message to OpenAI or Gemini for content analysis.
5. **Action Execution**: Blocks inappropriate messages, notifies admins, and logs actions.

## Requirements

* Minecraft Server 1.20+
* Spigot, Paper, Folia, or Luminol
* Java 17+
* OpenAI or Gemini API key (optional for AI moderation)
* Internet connection

## Building from Source

```bash
git clone <repository-url>
cd chat-moderator
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Support

For issues or feature requests, create an issue in the repository or contact the plugin developer.

## License

This project is licensed under the MIT License.

## Downloads
Spigot: https://www.spigotmc.org/resources/chatmoderator.128458/

Modrinth: https://modrinth.com/plugin/chatmoderator

Github: https://github.com/Hihelloy-main/ChatModerator

