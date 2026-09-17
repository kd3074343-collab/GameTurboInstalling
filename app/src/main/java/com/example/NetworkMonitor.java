package com.example;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkMonitor {

    public interface PingCallback {
        void onPingComplete(long latencyMs, String quality, String advice);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String getNetworkType(Context context) {
        if (context == null) return "Disconnected";
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return "Unknown";

            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return "Disconnected";

            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            if (caps == null) return "Disconnected";

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi (High Speed)";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Cellular (Mobile Data)";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "Ethernet (LAN)";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                return "Bluetooth Tethering";
            }
            return "Connected (Other)";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static void runPingTest(PingCallback callback) {
        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            long latencyMs = -1;

            // Test socket handshake to public DNS (8.8.8.8) on port 53
            try (Socket socket = new Socket()) {
                SocketAddress socketAddress = new InetSocketAddress("8.8.8.8", 53);
                socket.connect(socketAddress, 3000); // 3 sec timeout
                latencyMs = System.currentTimeMillis() - startTime;
                success = true;
            } catch (IOException e) {
                // Fallback attempt to 1.1.1.1
                try (Socket socket2 = new Socket()) {
                    long retryStart = System.currentTimeMillis();
                    SocketAddress socketAddress2 = new InetSocketAddress("1.1.1.1", 53);
                    socket2.connect(socketAddress2, 3000);
                    latencyMs = System.currentTimeMillis() - retryStart;
                    success = true;
                } catch (IOException ignored) {
                    success = false;
                }
            }

            final long finalLatency = latencyMs;
            final boolean finalSuccess = success;

            mainHandler.post(() -> {
                if (callback == null) return;
                if (!finalSuccess || finalLatency < 0) {
                    callback.onPingComplete(-1, "OFFLINE / UNREACHABLE", "Check your router connection or mobile signal.");
                } else if (finalLatency < 40) {
                    callback.onPingComplete(finalLatency, "EXCELLENT (Ultra Low Latency)", "Ideal for competitive online battle royales & FPS games.");
                } else if (finalLatency <= 70) {
                    callback.onPingComplete(finalLatency, "GOOD (Stable Gaming)", "Low jitter, optimal response times for multiplayer.");
                } else if (finalLatency <= 110) {
                    callback.onPingComplete(finalLatency, "FAIR (Occasional Jitter)", "Playable, but close background streaming or switch to 5GHz Wi-Fi.");
                } else {
                    callback.onPingComplete(finalLatency, "HIGH LATENCY (Lag Potential)", "Switch closer to your router or disable background cloud sync.");
                }
            });
        });
    }
}
