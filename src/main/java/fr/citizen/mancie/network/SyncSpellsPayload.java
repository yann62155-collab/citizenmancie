package fr.citizen.mancie.network;

import fr.citizen.mancie.CitizenMancie;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Serveur → client : liste des sorts connus. */
public record SyncSpellsPayload(List<String> spells) implements CustomPacketPayload {
    public static final Type<SyncSpellsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenMancie.MODID, "sync_spells"));

    public static final StreamCodec<ByteBuf, SyncSpellsPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
                    .map(SyncSpellsPayload::new, SyncSpellsPayload::spells);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
