package com.minecraftbenchmark;

import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;


import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ResultsScreen extends Screen {

    private final double avgFps;
    private final double onePercentLow;
    private final int frameCount;

    private float GUIscale = 1f;
    private int fontOffset = this.font.lineHeight - 10;

    protected ResultsScreen(double avgFps, double onePercentLow, int frameCount) {
        super(Component.literal("Benchmark Results"));
        this.avgFps = avgFps;
        this.onePercentLow = onePercentLow;
        this.frameCount = frameCount;
    }

    @Override
    protected void init() {
        addRenderableWidget(
                Button.builder(
                        Component.literal("Done"),
                        b -> Minecraft.getInstance().setScreen(new TitleScreen())
                ).bounds(this.width / 2 - 50, this.height - 40, 100, 20).build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GUIscale = Math.min(this.width / 480f, this.height / 270f);
        fontOffset = this.font.lineHeight - 10;

        // Draw background first
        g.fill(0, 0, this.width, this.height, 0xBB000000);

        int maxWidth = 110;
        int col1_label = 10;
        int col1_value = 100;
        int col2_label = 220;
        int col2_value = 320;

        // Results Tittle
        drawScaled(g, "Results: ", 5, 5, 1.5f);

        // Add Results
        drawScaled(g, "Avg FPS: ", col1_label, 20, 1f);
        drawScaledToFit(g, Double.toString(Math.round(avgFps*100f)/100.0), col1_value, 20, maxWidth);

        drawScaled(g, "FPS 1% Low: ", col1_label, 30, 1f);
        drawScaledToFit(g, Double.toString(Math.round(onePercentLow*100f)/100.0),  col1_value, 30, maxWidth);

        drawScaled(g, "Frame Count: ", col1_label, 40, 1f);
        drawScaledToFit(g, Double.toString(frameCount),  col1_value, 40, maxWidth);

        // System Specs
        drawScaled(g, "System Specs: ", 5, 55, 1.25f);

        drawScaled(g, "Game Version:",  col1_label, 70, 1f);
        drawScaledToFit(g, SharedConstants.getCurrentVersion().name(), col1_value, 70, maxWidth);

        drawScaled(g, "GPU Model:", col1_label, 80, 1f);
        drawScaledToFit(g, GL11.glGetString(GL11.GL_RENDERER), col1_value, 80, maxWidth);

        drawScaled(g, "GPU Driver:", col1_label, 90, 1f);
        drawScaledToFit(g, GL11.glGetString(GL11.GL_VERSION),  col1_value, 90, maxWidth);

        drawScaled(g, "CPU Model",  col1_label, 100, 1f);
        drawScaledToFit(g, com.mojang.blaze3d.platform.GLX._getCpuInfo(),  col1_value, 100, maxWidth);

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        drawScaled(g, "System Memory:", col1_label, 110, 1f);
        drawScaledToFit(g, osBean.getTotalMemorySize() / (1024 * 1024 * 1024) + " GB",  col1_value, 110, maxWidth);

        drawScaled(g, "Allocated Memory:", col1_label, 120, 1f);
        drawScaledToFit(g, Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024) + " GB", col1_value, 120, maxWidth);

        drawScaled(g, "OS Name:",  col1_label, 130, 1f);
        drawScaledToFit(g, System.getProperty("os.name"), col1_value, 130, maxWidth);

        drawScaled(g, "OS Version:", col1_label, 140, 1f);
        drawScaledToFit(g, System.getProperty("os.version"), col1_value, 140, maxWidth);

        drawScaled(g, "Java Version:", col1_label, 150, 1f);
        drawScaledToFit(g, System.getProperty("java.version"), col1_value, 150, maxWidth);


        // Settings
        drawScaled(g, "Game Settings:", col2_label-5, 5, 1.25f);

        drawScaled(g, "FOV:", col2_label, 25, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.fov().get().toString(), col2_value, 25, maxWidth);

        int width = Minecraft.getInstance().getWindow().getScreenWidth();
        int height = Minecraft.getInstance().getWindow().getScreenHeight();
        drawScaled(g, "Resolution:", col2_label, 35, 1f);
        drawScaledToFit(g, width + "x" + height, col2_value, 35, maxWidth);

        drawScaled(g, "Full Screen:", col2_label, 45, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.fullscreen().get().toString(), col2_value, 45, maxWidth);

        drawScaled(g, "Max Frame Rate:", col2_label, 55, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.framerateLimit().get().toString(),  col2_value, 55, maxWidth);

        drawScaled(g, "VSync:", col2_label, 65, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.enableVsync().get().toString(), col2_value, 65, maxWidth);

        drawScaled(g, "Render Distance:", col2_label, 75, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.renderDistance().get().toString(), col2_value, 75, maxWidth);

        drawScaled(g, "Simulation Distance:", col2_label, 85, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.simulationDistance().get().toString(), col2_value, 85, maxWidth);

        drawScaled(g, "Clouds:", col2_label, 95, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.cloudStatus().get().toString(), col2_value, 95, maxWidth);

        drawScaled(g, "Particles:",  col2_label, 105, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.particles().get().toString(), col2_value, 105, maxWidth);

        drawScaled(g, "Entity Shadows:", col2_label, 115, 1f);
        drawScaledToFit(g, Minecraft.getInstance().options.entityShadows().get().toString(), col2_value, 115, maxWidth);

//        drawFrameGraph(g, BenchmarkManager.getFrameTimes(), (480-160), (270-60), 150, 50);

        // Shader Settings
        //  Preset
        drawScaled(g, getShaderName(), col2_label, 125, 1f);
        // Check if Iris is loaded first via Fabric's mod detection
//        if (FabricLoader.getInstance().isModLoaded("iris")) {
//            Optional<NamespacedId> shaderpack = IrisApi.getInstance().getActiveShaderpack();
//            if (shaderpack.isPresent()) {
//                String name = shaderpack.get().toString(); // e.g. "ComplementaryUnbound_r5.7.1"
//            } else {
//                // "No shaders" / vanilla
//            }
//        }

        // Resource Packs

        // Mod List

        // Draw button on top
        super.render(g, mouseX, mouseY, delta);
    }

    private static String getShaderName() {
        if (!FabricLoader.getInstance().isModLoaded("iris"))
            return "None";
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = apiClass.getMethod("getInstance").invoke(null);
            boolean inUse = (boolean) instance.getClass()
                    .getMethod("isShaderPackInUse").invoke(instance);
            if (!inUse) return "None";
        } catch (Exception e) {
            return "Unknown";
        }

        try {
            Path irisConfig = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("iris.properties");

            if (!Files.exists(irisConfig)) return "Unknown";

            for (String line : Files.readAllLines(irisConfig)) {
                if (line.startsWith("shaderPack=")) {
                    String name = line.substring("shaderPack=".length()).trim();
                    return name.isEmpty() ? "None" : name;
                }
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "Unknown";
    }

    private void drawFrameGraph(GuiGraphics g, List<Long> frameTimes, int x, int y, int width, int height) {
        int graphScale = (int) GUIscale;

        // Background
        g.fill(x*graphScale, y*graphScale, (x + width)*graphScale, (y + height)*graphScale, 0xFF111111);

        if (frameTimes.isEmpty()) return;

        // Target lines (60fps = 16.6ms, 30fps = 33.3ms)
        long targetMs60 = 16_666_666L;
        long targetMs30 = 33_333_333L;
        long maxFrameTime = Collections.max(frameTimes);
        long graphMax = Math.max(maxFrameTime, targetMs30); // always show at least 30fps line

        int y60 = y + height - (int)((float) targetMs60 / graphMax * height);
        int y30 = y + height - (int)((float) targetMs30 / graphMax * height);

        g.fill(x*graphScale, y60*graphScale, (x + width)*graphScale, (y60 + 1)*graphScale, 0x8800FF00); // green = 60fps
        g.fill(x*graphScale, y30*graphScale, (x + width)*graphScale, (y30 + 1)*graphScale, 0x88FF4400); // red = 30fps

        // Draw bars, sampling if we have more frames than pixels
        int count = frameTimes.size();
        for (int i = 0; i < width; i++) {
            int sampleIndex = (int)((float) i / width * count);
            long ft = frameTimes.get(sampleIndex);

            int barHeight = (int)((float) ft / graphMax * height);
            barHeight = Math.min(barHeight, height);

            // Color shifts green -> yellow -> red based on frame time
            int color = ftToColor(ft, targetMs60, targetMs30);
            g.fill((x + i)*graphScale, (y + height - barHeight)*graphScale, (x + i + 1)*graphScale, (y + height)*graphScale, color);
        }
    }

    private int ftToColor(long ft, long good, long bad) {
        if (ft <= good) return 0xFF00CC00;
        if (ft >= bad)  return 0xFFFF2200;
        float t = (float)(ft - good) / (bad - good);
        int r = (int)(t * 255);
        int g = (int)((1 - t) * 200);
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private void drawScaled(GuiGraphics g, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x*GUIscale, (y-fontOffset)*GUIscale);
        g.pose().scale(GUIscale*scale);
        g.drawString(this.font, text, 0, 0, color, true);
        g.pose().popMatrix();
    }
    private void drawScaled(GuiGraphics g, String text, int x, int y, float scale) {
        drawScaled(g, text, x, y, scale, 0xFFFFFFFF);
    }

    private void drawScaledToFit(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        float scale = Math.min(1.0f, maxWidth / (float) this.font.width(text));
        drawScaled(g, text, x, y, scale, color);
    }
    private void drawScaledToFit(GuiGraphics g, String text, int x, int y, int maxWidth) {
        drawScaledToFit(g, text, x, y, maxWidth, 0xFFFFFFFF);
    }
}
