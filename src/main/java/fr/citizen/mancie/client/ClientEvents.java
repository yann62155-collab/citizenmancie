package fr.citizen.mancie.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.citizen.mancie.CitizenMancie;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final KeyMapping ROULETTE = new KeyMapping(
            "key.citizenmancie.roulette",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.citizenmancie");

    private ClientEvents() {}

    @EventBusSubscriber(modid = CitizenMancie.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void onKeys(RegisterKeyMappingsEvent event) {
            event.register(ROULETTE);
        }
    }

    @EventBusSubscriber(modid = CitizenMancie.MODID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            while (ROULETTE.consumeClick()) {
                // Joueur sans aucun sort : la touche U ne fait strictement rien.
                if (mc.screen != null || ClientSpellData.spells().isEmpty()) continue;
                mc.setScreen(new SpellWheelScreen());
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientSpellData.reset();
        }
    }
}
