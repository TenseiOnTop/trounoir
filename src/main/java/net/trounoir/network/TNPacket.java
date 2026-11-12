package net.trounoir.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.trounoir.TrouNoir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Message unique du mod : un type et une charge JSON compressée.
 * Les deux sens l'utilisent ; le côté de réception décide du traitement.
 */
public final class TNPacket {
    public final String type;
    private final byte[] data;

    public TNPacket(String type, JsonObject json) {
        this(type, compress(json.toString()));
    }

    private TNPacket(String type, byte[] data) {
        this.type = type;
        this.data = data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(type);
        buf.writeByteArray(data);
    }

    public static TNPacket decode(FriendlyByteBuf buf) {
        String type = buf.readUtf();
        byte[] data = buf.readByteArray();
        return new TNPacket(type, data);
    }

    public void handle(CustomPayloadEvent.Context context) {
        JsonObject json;
        try {
            json = JsonParser.parseString(decompress(data)).getAsJsonObject();
        } catch (RuntimeException e) {
            TrouNoir.LOGGER.warn("[Trou-Noir] Paquet '{}' illisible", type);
            return;
        }
        if (context.isServerSide()) {
            ServerPlayer sender = context.getSender();
            if (sender != null) ServerPackets.handle(sender, type, json);
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> net.trounoir.client.ClientPackets.handle(type, json));
        }
    }

    private static byte[] compress(String text) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decompress(byte[] data) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            // Borne de sécurité : aucun message légitime n'approche cette taille.
            byte[] out = gzip.readNBytes(8 * 1024 * 1024);
            return new String(out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
