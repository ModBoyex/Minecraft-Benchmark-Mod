package com.minecraftbenchmark;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;

public class MinecraftBenchmarkClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(BenchmarkManager::onClientTick);
		WorldRenderEvents.END_MAIN.register(context -> BenchmarkManager.frameLog());
		ServerLifecycleEvents.SERVER_STOPPED.register(BenchmarkManager::onServerStopped);

		// Warm up progress Bar
		HudRenderCallback.EVENT.register((guiGraphics, deltaTick) -> {
			if (BenchmarkManager.currentState != BenchmarkManager.BenchmarkState.WARMUP) return;

			Minecraft mc = Minecraft.getInstance();
			int screenWidth = mc.getWindow().getGuiScaledWidth();
			int screenHeight = mc.getWindow().getGuiScaledHeight();

			float progress = BenchmarkManager.getWarmupProgress();
			int barWidth = 200;
			int barHeight = 5;
			int x = (screenWidth - barWidth) / 2;
			int y = screenHeight - 40;

			// Background
			guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF555555);
			// Fill
			guiGraphics.fill(x, y, x + (int)(barWidth * progress), y + barHeight, 0xFF00AA00);
			// Label
			String label = "Warming up... " + (int)(progress * 100) + "%";
			guiGraphics.drawCenteredString(mc.font, label, screenWidth / 2, y - 12, 0xFFFFFFFF);
		});
	}
}