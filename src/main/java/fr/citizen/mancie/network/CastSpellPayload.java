package fr.citizen.mancie.network;

import fr.citizen.mancie.CitizenMancie;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client → serveur : le joueur lance un sort depuis la roulette. Le serveur vérifie tout. */
public record CastSpellPayload(String spell) implements CustomPacketPayload {
    public static final Type<CastSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenMancie.MODID, "cast"));

    public static final StreamCodec<ByteBuf, CastSpellPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(CastSpellPayload::new, CastSpellPayload::spell);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
