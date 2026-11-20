package fr.citizen.mancie;

import com.mojang.logging.LogUtils;
import fr.citizen.mancie.network.ModNetwork;
import fr.citizen.mancie.spell.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CitizenMancie.MODID)
public class CitizenMancie {
    public static final String MODID = "citizenmancie";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CitizenMancie(IEventBus modBus) {
        ModAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(ModNetwork::register);
        LOGGER.info("[Citizen Mancie] Pyromancie, Luminomancie et Hématomancie chargées.");
    }
}
