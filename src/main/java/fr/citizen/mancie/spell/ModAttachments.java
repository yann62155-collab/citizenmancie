package fr.citizen.mancie.spell;

import fr.citizen.mancie.CitizenMancie;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CitizenMancie.MODID);

    public static final Supplier<AttachmentType<MancieData>> DATA = ATTACHMENTS.register("data",
            () -> AttachmentType.builder(() -> MancieData.EMPTY)
                    .serialize(MancieData.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
