package com.minecraftbenchmark.mixin.client;

import com.minecraftbenchmark.BenchmarkManager;
import com.minecraftbenchmark.MinecraftBenchmarkClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static com.minecraftbenchmark.BenchmarkManager.BENCHMARK_WORLD_NAME;
import static com.minecraftbenchmark.BenchmarkManager.MOD_ID;

@Mixin(TitleScreen.class)
public abstract class TitleScreenButtonMixin extends Screen {
	protected TitleScreenButtonMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void minecraftBenchmark$addButton(CallbackInfo ci) {
		this.addRenderableWidget(Button.builder(Component.literal("Benchmark"), button -> {
			Minecraft minecraft = Minecraft.getInstance();

			try {
				minecraftBenchmark$ensureBenchmarkWorldExists(minecraft);
				minecraft.createWorldOpenFlows().openWorld(BENCHMARK_WORLD_NAME, () -> {});
			} catch (IOException e) {
				System.err.println("[minecraft-benchmark] Failed to prepare benchmark world: " + e.getMessage());
				e.printStackTrace();
			}
			BenchmarkManager.requestStart();
		}).bounds(this.width / 2 - 185, this.height / 4 + 96, 80, 20).build());
	}

	private static void minecraftBenchmark$ensureBenchmarkWorldExists(Minecraft minecraft) throws IOException {
		Path savesDir = minecraft.gameDirectory.toPath().resolve("saves");
		Path targetWorldDir = savesDir.resolve(BENCHMARK_WORLD_NAME);

		if (Files.exists(targetWorldDir.resolve("level.dat"))) {
			return;
		}

		Optional<Path> sourceWorldDirOpt = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.flatMap(container -> container.findPath(BENCHMARK_WORLD_NAME));

		if (sourceWorldDirOpt.isEmpty()) {
			throw new IOException("Resource world folder not found: " + BENCHMARK_WORLD_NAME);
		}

		Path sourceWorldDir = sourceWorldDirOpt.get();
		Files.createDirectories(targetWorldDir);

		try (var stream = Files.walk(sourceWorldDir)) {
			for (Path source : (Iterable<Path>) stream::iterator) {
				Path relative = sourceWorldDir.relativize(source);
				Path destination = targetWorldDir.resolve(relative.toString());

				if (Files.isDirectory(source)) {
					Files.createDirectories(destination);
				} else {
					Files.createDirectories(destination.getParent());
					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}
}
