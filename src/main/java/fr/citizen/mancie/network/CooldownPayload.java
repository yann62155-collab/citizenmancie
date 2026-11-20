package fr.citizen.mancie.network;

import fr.citizen.mancie.CitizenMancie;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur → client : un sort vient d'être lancé, recharge en ticks. */
public record CooldownPayload(String spell, int ticks) implements CustomPacketPayload {
    public static final Type<CooldownPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenMancie.MODID, "cooldown"));

    public static final StreamCodec<ByteBuf, CooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CooldownPayload::spell,
            ByteBufCodecs.VAR_INT, CooldownPayload::ticks,
            CooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
