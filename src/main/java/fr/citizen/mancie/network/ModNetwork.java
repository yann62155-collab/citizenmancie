package fr.citizen.mancie.network;

import fr.citizen.mancie.client.ClientSpellData;
import fr.citizen.mancie.spell.ModAttachments;
import fr.citizen.mancie.spell.Spell;
import fr.citizen.mancie.spell.SpellCaster;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // optional() : les joueurs sans le mod (console via Geyser/ViaProxy) peuvent quand même se connecter.
        PayloadRegistrar r = event.registrar("1").optional();

        r.playToClient(SyncSpellsPayload.TYPE, SyncSpellsPayload.STREAM_CODEC,
                (p, ctx) -> ClientSpellData.setSpells(p.spells()));

        r.playToClient(CooldownPayload.TYPE, CooldownPayload.STREAM_CODEC,
                (p, ctx) -> ClientSpellData.setCooldown(p.spell(), p.ticks()));

        r.playToServer(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC,
                (p, ctx) -> {
                    if (ctx.player() instanceof ServerPlayer sp) SpellCaster.cast(sp, p.spell());
                });
    }

    public static void syncSpells(ServerPlayer player) {
        if (!player.connection.hasChannel(SyncSpellsPayload.TYPE)) return;
        List<String> ids = new ArrayList<>();
        for (Spell s : player.getData(ModAttachments.DATA).known()) ids.add(s.id());
        PacketDistributor.sendToPlayer(player, new SyncSpellsPayload(ids));
    }

    public static void sendCooldown(ServerPlayer player, Spell spell, int ticks) {
        if (!player.connection.hasChannel(CooldownPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new CooldownPayload(spell.id(), ticks));
    }
}
