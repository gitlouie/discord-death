package com.discorddeath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("DiscordDeathConfig")
public interface DiscordDeathConfig extends Config
{
	String GROUP = "DiscordDeathConfig";

	@ConfigItem(
			keyName = "webhook",
			name = "Webhook URL",
			description = "The Discord Webhook URL to send messages to",
			position = 1
	)
	String webhook();

	@ConfigItem(
			keyName = "friendDeath",
			name = "Screenshot Friend Deaths",
			description = "Configures whether or not screenshots are automatically taken when friends or friends chat members die.",
			position = 2
	)
	default boolean screenshotFriendDeath()
	{
		return false;
	}

	@ConfigItem(
			keyName = "friendNames",
			name = "Name of Friends to Screenshot",
			description = "Name of friends to take screenshots of when they die.",
			position = 3
	)
	String friendNames();
}
