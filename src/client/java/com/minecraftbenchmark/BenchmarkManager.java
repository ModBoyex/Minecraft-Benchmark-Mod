package com.minecraftbenchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.client.Minecraft.getInstance;

public class BenchmarkManager {
    public static final String BENCHMARK_WORLD_NAME = "Vanilla Benchmark World";
    public static final String MOD_ID = "minecraft-benchmark";

    // Time Spent
//    private static final int warmupTimeSec = 45;
//    private static final int benchmarkTimeSec = 125;
    private static final int warmupTimeSec = 5;
    private static final int benchmarkTimeSec = 5;

    // State Machine Ticks
    private static int warmupTicksRemaining = 0;
    private static int benchmarkTicsRemaining = 0;

    // Frame Rate Ticks
    private static long lastFrameTime = 0L;
    private static long totalFrameTime = 0L;
    private static int frameCount = 0;
    private static final List<Long> frameTimes = new ArrayList<Long>();

    enum BenchmarkState {
        IDLE,
        WARMUP,
        RUNNING,
        SHOW_RESULTS
    }
    public static BenchmarkState currentState = BenchmarkState.IDLE;
    public static volatile boolean worldDeleted = false;

    private static final List<List<Double>> path = List.of(
            List.of(151.5, 123.0, 496.5, 0.0, 0.0, 0.0),
            List.of(148.5, 120.0, 524.5, 0.0, 0.0, 72.0),
            List.of(152.5, 120.0, 540.5, 0.0, 0.0, 114.0),
            List.of(152.5, 121.0, 550.5, -30.0, -5.0, 140.0),
            List.of(162.5, 119.0, 557.5, -60.0, 30.0, 171.0),
            List.of(190.5, 107.0, 575.5, -57.5, 12.0, 261.0),
            List.of(194.5, 108.0, 584.5, -10.0, 3.0, 286.0),
            List.of(198.5, 107.0, 607.5, 0.0, -10.0, 346.0),
            List.of(238.5, 78.0, 674.5, -25.0, 5.0, 557.0),
            List.of(264.5, 63.0, 703.5, 10.0, 25.0, 664.0),
            List.of(255.5, 45.0, 747.5, 13.0, -10.0, 787.0),
            List.of(247.5, 63.0, 779.5, 15.0, -25.0, 882.0),
            List.of(235.5, 68.0, 805.5, 3.0, -25.0, 956.0),
            List.of(233.5, 85.0, 844.5, 5.0, -30.0, 1065.0),
            List.of(217.5, 120.0, 884.5, 1.0, -25.0, 1206.0),
            List.of(203.5, 141.0, 914.5, 20.0, 10.0, 1306.0),
            List.of(192.5, 135.0, 944.5, 40.0, 20.0, 1388.0),
            List.of(184.5, 124.0, 972.5, 65.0, 10.0, 1467.0),
            List.of(169.5, 115.0, 979.5, 80.0, 10.0, 1515.0),
            List.of(80.5, 98.0, 987.5, 90.0, 20.0, 1747.0),
            List.of(50.5, 90.0, 987.5, 70.0, -10.0, 1826.0),
            List.of(19.5, 98.0, 997.5, 90.0, -10.0, 1911.0),
            List.of(-53.5, 111.0, 995.5, 90.0, 10.0, 2100.0),
            List.of(-61.5, 111.0, 995.5, 80.0, -25.0, 2120.0),
            List.of(-76.5, 119.0, 998.5, 100.0, 3.0, 2164.0),
            List.of(-91.5, 118.0, 996.5, 70.0, -2.0, 2203.0),
            List.of(-112.5, 120.0, 1004.5, 55.0, -10.0, 2260.0),
            List.of(-142.5, 125.0, 1031.5, 85.0, -5.0, 2363.0),
            List.of(-156.5, 128.0, 1032.5, 90.0, 10.0, 2400.0));
    private static int currentKeyFrameIndex = 0;

    // Rotation state for frame-level interpolation
    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static float lastTickYaw = 0f;
    private static float lastTickPitch = 0f;
    private static long tickStartTime = 0L;
    private static final long NANOS_PER_TICK = 50_000_000L; // 50ms = 1 tick @ 20 ticks/sec

    public static void requestStart() {
        warmupTicksRemaining = 0;
        currentState = BenchmarkState.WARMUP;
    }

    public static void onClientTick(Minecraft client) {
        switch (currentState) {
            case IDLE:
                break;
            case WARMUP:
                warmupTicksRemaining++;
                if (client.player != null) {
                    client.player.setPos(151.5, 123, 496.5);
                    client.player.setXRot(0);
                    client.player.setYRot(0);
                }

                if (warmupTicksRemaining > (warmupTimeSec * 20) && client.player != null) {
                    if (getInstance().options.inactivityFpsLimit().get().toString().equals("AFK"))
                        getInstance().player.displayClientMessage(
                                Component.literal("You currently have (Reduce FPS When) set to (AFK)\n Please change to (MINIMIZED)"), false);

                    startBenchmark(client);
                }
                break;
            case RUNNING:
                // Reset frame interpolation timer at START of tick
                tickStartTime = System.nanoTime();
                benchmarkTicsRemaining++;

                // Move Player
                List<Double> a = path.get(currentKeyFrameIndex);
                List<Double> b = path.get(currentKeyFrameIndex + 1);
                List<Double> prev = path.get(Math.max(0, currentKeyFrameIndex - 1));
                List<Double> next = path.get(Math.min(path.size()-1, currentKeyFrameIndex + 2));

                double progress = (benchmarkTicsRemaining - a.get(5)) / (double) (b.get(5) - a.get(5));
                double t = blend(progress, 0); // 0.0 = pure linear, 1.0 = full smoothstep


                double x = lerp(a.get(0), b.get(0), t);
                double y = lerp(a.get(1), b.get(1), t);
                double z = lerp(a.get(2), b.get(2), t);

                // Store last tick rotation before updating target
                lastTickYaw = targetYaw;
                lastTickPitch = targetPitch;

                targetYaw = (float) lerp(a.get(3), b.get(3), progress);
                targetPitch = (float) lerp(a.get(4), b.get(4), progress);

                client.player.setPos(x, y, z);

                if (benchmarkTicsRemaining >= b.get(5) && currentKeyFrameIndex < path.size() - 2)
                    currentKeyFrameIndex++;


                // Check if over
                if (benchmarkTicsRemaining >= benchmarkTimeSec * 20)
                    currentState = BenchmarkState.SHOW_RESULTS;

                break;
            case SHOW_RESULTS:
                if (client.player != null && frameCount > 0 && !frameTimes.isEmpty()) {
                    double avgFrameTimeMs = (totalFrameTime / (double) frameCount) / 1_000_000.0;
                    double avgFps = 1000.0 / avgFrameTimeMs;
                    double onePercentLow = calcPercentLow(frameTimes, 0.01);
                    ResultsScreen results = new ResultsScreen(avgFps, onePercentLow, frameCount);

                    client.clearClientLevel(results);
                    client.setScreen(results);
                }
                currentState = BenchmarkState.IDLE;
                break;
        }
    }

    public static void frameLog() {
        if (currentState != BenchmarkState.RUNNING)
            return;

        long now = System.nanoTime();

        if (lastFrameTime != 0L) {
            long delta = now - lastFrameTime;
            totalFrameTime += delta;
            frameCount++;
            frameTimes.add(delta);
        }

        lastFrameTime = now;

        // Frame-level rotation interpolation
        long elapsedNanos = now - tickStartTime;
        double frameAlpha = Math.min(1.0, (double) elapsedNanos / NANOS_PER_TICK);

        float interpolatedYaw = (float) lerp(lastTickYaw, targetYaw, frameAlpha);
        float interpolatedPitch = (float) lerp(lastTickPitch, targetPitch, frameAlpha);

        Minecraft client = getInstance();
        if (client.player != null) {
            client.player.setYRot(interpolatedYaw);
            client.player.setXRot(interpolatedPitch);
        }
    }

    private static void startBenchmark(Minecraft client) {
        System.out.println("Benchmark starting!");
        if (currentState == BenchmarkState.RUNNING)
            return;

        // Disable auto-saving for the benchmark world
        MinecraftServer server = client.getSingleplayerServer();
        server.execute(() -> server.setAutoSave(false));

        worldDeleted = false;
        currentKeyFrameIndex = 0;
        benchmarkTicsRemaining = 0;

        lastFrameTime = 0L;
        totalFrameTime = 0L;
        frameCount = 0;

        frameTimes.clear();

        // Reset rotation state
        tickStartTime = System.nanoTime();
        lastTickYaw = 0f;
        lastTickPitch = 0f;
        targetYaw = 0f;
        targetPitch = 0f;

        currentState = BenchmarkState.RUNNING;
    }

    public static void onServerStopped(MinecraftServer server) {
        if (currentState != BenchmarkState.IDLE) return; // not benchmark ending

        Path worldToDelete = server.getServerDirectory()
                .resolve("saves")
                .resolve(BENCHMARK_WORLD_NAME);

        if (Files.exists(worldToDelete.resolve("level.dat"))) {
            try {
                FileUtils.deleteDirectory(worldToDelete.toFile());
                worldDeleted = true;
            } catch (IOException e) {
                System.err.println("[minecraft-benchmark] Failed to delete benchmark world: " + e.getMessage());
                worldDeleted = true; // unblock Done button regardless
            }
        }
    }

    private static double calcPercentLow(List<Long> frameTimes, double percentile) {
        if (frameTimes.isEmpty()) return 0.0;

        List<Long> sorted = new ArrayList<>(frameTimes);
        Collections.sort(sorted);

        int index = (int)(sorted.size() * (1.0 - percentile));
        long frameTime = sorted.get(index);
        return 1_000_000_000.0 / frameTime;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double blend(double t, double smoothAmount) {
        double smooth = t * t * (3 - 2 * t); // smoothstep
        return lerp(t, smooth, smoothAmount);
    }

    public static float getWarmupProgress() {
        return Math.min(1.0f, warmupTicksRemaining / (float)(warmupTimeSec * 20));
    }

    public static List<Long> getFrameTimes() {
        return Collections.unmodifiableList(frameTimes);
    }
}
