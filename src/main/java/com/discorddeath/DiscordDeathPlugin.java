package com.discorddeath;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import com.google.common.base.Strings;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.api.events.ActorDeath;

import static net.runelite.http.api.RuneLiteAPI.GSON;

import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@PluginDescriptor(
	name = "Discord Death Logger"
)
public class DiscordDeathPlugin extends Plugin
{
	@Inject
	private DiscordDeathConfig config;

	@Inject
	private Client client;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private DrawManager drawManager;

	private List<String> friendNames;


	@Override
	protected void startUp()
	{

	}

	@Override
	protected void shutDown()
	{
	}

	@Provides
	DiscordDeathConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DiscordDeathConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equalsIgnoreCase(DiscordDeathConfig.GROUP))
		{
			String string = config.friendNames();
			friendNames = string != null ? Text.fromCSV(string) : Collections.emptyList();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		Actor actor = actorDeath.getActor();
		if (actor instanceof Player)
		{
			Player player = (Player) actor;
			WebhookBody webhookBody = new WebhookBody();
			StringBuilder stringBuilder = new StringBuilder();
			if (player == client.getLocalPlayer())
			{
				stringBuilder.append(actor.getName());
//				final ChatLineBuffer chatLineBuffer = client.getChatLineMap().get(ChatMessageType.ENGINE);
//				final MessageNode[] lines = chatLineBuffer.getLines();
//				final MessageNode line = lines[0];

				if(actor.getInteracting() != null) {
					stringBuilder.append(" was clapped by "); // replace this with possible death messages
					stringBuilder.append(actor.getInteracting().getName());
				}
				else {
					//add message if player is not interacting with anything.
					stringBuilder.append(actor.getName() + " has died...what an idiot.");
				}

				webhookBody.setContent(stringBuilder.toString());
				sendWebhook(webhookBody);
			}
			// Add screenshots if friend death screenshots is enabled
			else if ((player.isFriendsChatMember() || player.isFriend()) &&
					player.getCanvasTilePoly() != null && config.screenshotFriendDeath())
			{
				stringBuilder.append(player.getName() + " has died.");
				webhookBody.setContent(stringBuilder.toString());

				// If a specific name is specified, only send when that player has died, otherwise if no player is
				// specified, always send the picture.
				if(!friendNames.isEmpty())
				{
					for (String name: friendNames)
					{
						if(name.equalsIgnoreCase(player.getName()))
						{
							sendWebhook(webhookBody);
						}
					}
				}
				else
				{
					sendWebhook(webhookBody);
				}
			}
		}
	}

	private void sendWebhook(WebhookBody webhookBody)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

		HttpUrl url = HttpUrl.parse(configUrl);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", GSON.toJson(webhookBody));

		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = (BufferedImage) image;
			byte[] imageBytes;
			try
			{
				imageBytes = convertImageToByteArray(bufferedImage);
			}
			catch (IOException e)
			{
				log.warn("Error converting image to byte array", e);
				return;
			}

			requestBodyBuilder.addFormDataPart("file", "image.png",
					RequestBody.create(MediaType.parse("image/png"), imageBytes));
			buildRequestAndSend(url, requestBodyBuilder);
		});
	}

	private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
	{
		RequestBody requestBody = requestBodyBuilder.build();
		Request request = new Request.Builder()
				.url(url)
				.post(requestBody)
				.build();
		sendRequest(request);
	}

	private void sendRequest(Request request)
	{
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Error submitting webhook", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				response.close();
			}
		});
	}

	private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

}
