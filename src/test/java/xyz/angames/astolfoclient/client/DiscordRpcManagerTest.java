package xyz.angames.astolfoclient.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiscordRpcManagerTest {
    @Test
    void publishesAnAnimatedLogoByDefault() throws Exception {
        JsonObject activity = publishedActivity(new DiscordRpcManager());
        String image = activity.getAsJsonObject("assets").get("large_image").getAsString();
        assertTrue(URI.create(image).getPath().endsWith(".gif"),
                "RPC must send an actual animation, not a static PNG with a spinning-logo caption");
        assertFalse(activity.getAsJsonObject("assets").get("large_text").getAsString().contains("kręci"));
        assertFalse(activity.get("state").getAsString().contains("kręci"));
    }

    @Test
    void switchingRotationOffSelectsTheConfiguredStaticLogo() throws Exception {
        DiscordRpcManager manager = new DiscordRpcManager();
        set(manager, "imageKey", "https://example.org/my-logo.png");
        set(manager, "animatedImageKey", "https://example.org/my-rotation.gif");
        manager.setPresence("Playing", "Minecraft", true, true, true);
        assertEquals("https://example.org/my-rotation.gif", publishedActivity(manager)
                .getAsJsonObject("assets").get("large_image").getAsString());
        manager.setPresence("Playing", "Minecraft", true, true, false);
        assertEquals("https://example.org/my-logo.png", publishedActivity(manager)
                .getAsJsonObject("assets").get("large_image").getAsString());
    }

    @Test
    void hidingLogoAndElapsedTimeOmitsBothFromThePayload() throws Exception {
        DiscordRpcManager manager = new DiscordRpcManager();
        manager.setPresence("Playing", "Minecraft", false, false, true);
        JsonObject activity = publishedActivity(manager);
        assertFalse(activity.has("assets"));
        assertFalse(activity.has("timestamps"));
        assertEquals("Playing", activity.get("details").getAsString());
        assertEquals("Minecraft", activity.get("state").getAsString());
    }

    private static JsonObject publishedActivity(DiscordRpcManager manager) throws Exception {
        AtomicReference<byte[]> frame = new AtomicReference<>();
        Class<?> channelType = Class.forName(DiscordRpcManager.class.getName() + "$IPCChannel");
        Object channel = Proxy.newProxyInstance(channelType.getClassLoader(), new Class<?>[]{channelType},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("write")) frame.set((byte[]) arguments[0]);
                    return null;
                });
        set(manager, "ipcChannel", channel);
        set(manager, "ready", true);
        set(manager, "running", true);
        set(manager, "startTimestamp", 1_700_000_000L);
        var publish = DiscordRpcManager.class.getDeclaredMethod("publish");
        publish.setAccessible(true);
        publish.invoke(manager);
        assertNotNull(frame.get(), "RPC should publish a SET_ACTIVITY frame");
        ByteBuffer data = ByteBuffer.wrap(frame.get()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1, data.getInt());
        int length = data.getInt();
        assertEquals(data.remaining(), length);
        byte[] body = new byte[length];
        data.get(body);
        JsonObject payload = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("SET_ACTIVITY", payload.get("cmd").getAsString());
        return payload.getAsJsonObject("args").getAsJsonObject("activity");
    }

    private static void set(DiscordRpcManager manager, String name, Object value) throws Exception {
        var field = DiscordRpcManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(manager, value);
    }
}
