package fr.citizen.mancie.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.citizen.mancie.network.ModNetwork;
import fr.citizen.mancie.spell.Mancie;
import fr.citizen.mancie.spell.MancieData;
import fr.citizen.mancie.spell.ModAttachments;
import fr.citizen.mancie.spell.Spell;
import fr.citizen.mancie.spell.SpellCaster;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Arrays;
import java.util.List;

/**
 * Staff (permission 2 / OP) : /mancie donner|ecole|retirer|vider|voir|liste
 * Joueurs : /sorts (voir ses sorts), /sort <id> (lancer sans la roulette, utile pour la console)
 */
public final class MancieCommands {
    private static final int GOLD = 0xC9A44C;

    private static final SuggestionProvider<CommandSourceStack> ALL_SPELLS = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Spell.values()).map(Spell::id), b);

    private static final SuggestionProvider<CommandSourceStack> MANCIES = (c, b) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Mancie.values()).map(Mancie::id), b);

    private static final SuggestionProvider<CommandSourceStack> OWN_SPELLS = (c, b) -> {
        if (c.getSource().getEntity() instanceof ServerPlayer p) {
            return SharedSuggestionProvider.suggest(p.getData(ModAttachments.DATA).spells(), b);
        }
        return b.buildFuture();
    };

    private MancieCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("mancie")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("donner")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .then(Commands.argument("sort", StringArgumentType.word()).suggests(ALL_SPELLS)
                                        .executes(MancieCommands::give))))
                .then(Commands.literal("ecole")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .then(Commands.argument("mancie", StringArgumentType.word()).suggests(MANCIES)
                                        .executes(MancieCommands::giveSchool))))
                .then(Commands.literal("retirer")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .then(Commands.argument("sort", StringArgumentType.word()).suggests(ALL_SPELLS)
                                        .executes(MancieCommands::remove))))
                .then(Commands.literal("vider")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .executes(MancieCommands::clear)))
                .then(Commands.literal("voir")
                        .then(Commands.argument("joueur", EntityArgument.player())
                                .executes(c -> show(c, EntityArgument.getPlayer(c, "joueur")))))
                .then(Commands.literal("liste").executes(MancieCommands::listAll)));

        d.register(Commands.literal("sorts")
                .executes(c -> show(c, c.getSource().getPlayerOrException())));

        d.register(Commands.literal("sort")
                .then(Commands.argument("sort", StringArgumentType.word()).suggests(OWN_SPELLS)
                        .executes(c -> {
                            SpellCaster.cast(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "sort"));
                            return 1;
                        })));
    }

    // ------------------------------------------------------------------

    private static int give(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "joueur");
        Spell s = Spell.byId(StringArgumentType.getString(c, "sort"));
        if (s == null) return fail(c, "Sort inconnu. Fais /mancie liste.");

        MancieData data = target.getData(ModAttachments.DATA);
        if (data.has(s)) return fail(c, target.getGameProfile().getName() + " connaît déjà " + s.displayName() + ".");

        target.setData(ModAttachments.DATA, data.with(s));
        ModNetwork.syncSpells(target);
        announce(target, s);
        ok(c, "Sort " + s.displayName() + " (" + s.mancie().displayName() + ") enseigné à " + target.getGameProfile().getName() + ".");
        return 1;
    }

    private static int giveSchool(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "joueur");
        Mancie m = Mancie.byId(StringArgumentType.getString(c, "mancie"));
        if (m == null) return fail(c, "Mancie inconnue : pyromancie, luminomancie ou hematomancie.");

        MancieData data = target.getData(ModAttachments.DATA);
        int added = 0;
        for (Spell s : Spell.ofMancie(m)) {
            if (!data.has(s)) {
                data = data.with(s);
                announce(target, s);
                added++;
            }
        }
        target.setData(ModAttachments.DATA, data);
        ModNetwork.syncSpells(target);
        ok(c, added + " sort(s) de " + m.displayName() + " enseigné(s) à " + target.getGameProfile().getName() + ".");
        return added;
    }

    private static int remove(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "joueur");
        Spell s = Spell.byId(StringArgumentType.getString(c, "sort"));
        if (s == null) return fail(c, "Sort inconnu.");

        MancieData data = target.getData(ModAttachments.DATA);
        if (!data.has(s)) return fail(c, target.getGameProfile().getName() + " ne connaît pas ce sort.");

        target.setData(ModAttachments.DATA, data.without(s));
        ModNetwork.syncSpells(target);
        target.sendSystemMessage(Component.literal("✦ Le sort " + s.displayName() + " t'a été retiré.").withColor(0xAA3333));
        ok(c, "Sort " + s.displayName() + " retiré à " + target.getGameProfile().getName() + ".");
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "joueur");
        target.setData(ModAttachments.DATA, MancieData.EMPTY);
        SpellCaster.clear(target.getUUID());
        ModNetwork.syncSpells(target);
        target.sendSystemMessage(Component.literal("✦ Tu as perdu tous tes sorts.").withColor(0xAA3333));
        ok(c, "Tous les sorts de " + target.getGameProfile().getName() + " ont été retirés.");
        return 1;
    }

    private static int show(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        List<Spell> known = target.getData(ModAttachments.DATA).known();
        CommandSourceStack src = c.getSource();
        src.sendSystemMessage(Component.literal("── Grimoire de " + target.getGameProfile().getName() + " ──").withColor(GOLD));
        if (known.isEmpty()) {
            src.sendSystemMessage(Component.literal("Aucun sort. Seul le staff peut enseigner la mancie.").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        for (Mancie m : Mancie.values()) {
            for (Spell s : known) {
                if (s.mancie() != m) continue;
                src.sendSystemMessage(spellLine(s));
            }
        }
        return known.size();
    }

    private static int listAll(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        src.sendSystemMessage(Component.literal("── Sorts de mancie ──").withColor(GOLD));
        for (Spell s : Spell.values()) {
            src.sendSystemMessage(spellLine(s).append(Component.literal("  [" + s.id() + "]").withStyle(ChatFormatting.DARK_GRAY)));
        }
        return Spell.values().length;
    }

    // ------------------------------------------------------------------

    private static MutableComponent spellLine(Spell s) {
        return Component.literal(" " + s.mancie().symbol() + " ").withColor(s.mancie().rgb())
                .append(Component.literal(s.displayName()).withColor(s.mancie().rgb()))
                .append(Component.literal(" — " + s.description()).withStyle(ChatFormatting.GRAY));
    }

    private static void announce(ServerPlayer target, Spell s) {
        target.sendSystemMessage(Component.literal("✦ Le staff t'a enseigné un sort de " + s.mancie().displayName() + " : ")
                .withColor(GOLD)
                .append(Component.literal(s.displayName()).withColor(s.mancie().rgb()))
                .append(Component.literal(" (maintiens U pour la roulette)").withStyle(ChatFormatting.GRAY)));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    private static void ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg).withColor(GOLD), true);
    }

    private static int fail(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
