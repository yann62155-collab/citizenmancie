package fr.citizen.mancie.spell;

import fr.citizen.mancie.network.ModNetwork;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lancement des sorts, toujours côté serveur.
 * Aucun sort ne pose de feu sur les blocs (pas de risque pour les villes en bois).
 */
public final class SpellCaster {
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.05f), 1.2f);
    private static final Map<UUID, Map<String, Integer>> COOLDOWNS = new HashMap<>();

    private SpellCaster() {}

    public static void cast(ServerPlayer p, String spellId) {
        Spell s = Spell.byId(spellId);
        if (s == null || p.isSpectator() || !p.isAlive()) return;

        MancieData data = p.getData(ModAttachments.DATA);
        if (!data.has(s)) {
            bar(p, Component.literal("Tu ne maîtrises pas ce sort.").withColor(0xAA3333));
            return;
        }

        int now = p.getServer().getTickCount();
        Map<String, Integer> cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Integer end = cds.get(s.id());
        if (end != null && end > now) {
            int sec = (int) Math.ceil((end - now) / 20.0);
            bar(p, Component.literal(s.displayName() + " se recharge… " + sec + " s").withColor(0x888888));
            return;
        }

        if (s.healthCost() > 0 && !p.isCreative() && p.getHealth() <= s.healthCost() + 1) {
            bar(p, Component.literal("Ton sang est trop faible pour ce sort.").withColor(0xB0141E));
            return;
        }

        boolean ok = switch (s) {
            case TRAIT_DE_FEU -> traitDeFeu(p);
            case EMBRASEMENT -> embrasement(p);
            case PEAU_DE_BRAISE -> peauDeBraise(p);
            case LUMIERE_GUERISSEUSE -> lumiereGuerisseuse(p);
            case EBLOUISSEMENT -> eblouissement(p);
            case REVELATION -> revelation(p);
            case DRAIN_DE_SANG -> drainDeSang(p);
            case LAME_DE_SANG -> lameDeSang(p);
            case PACTE_DE_SANG -> pacteDeSang(p);
        };
        if (!ok) return;

        if (s.healthCost() > 0 && !p.isCreative()) {
            p.setHealth(Math.max(1f, p.getHealth() - s.healthCost()));
            level(p).sendParticles(BLOOD, p.getX(), p.getY() + 1, p.getZ(), 15, 0.3, 0.5, 0.3, 0);
        }

        cds.put(s.id(), now + s.cooldownTicks());
        ModNetwork.sendCooldown(p, s, s.cooldownTicks());
        bar(p, Component.literal(s.mancie().symbol() + " " + s.displayName()).withColor(s.mancie().rgb()));
    }

    public static void clear(UUID id) {
        COOLDOWNS.remove(id);
    }

    // ------------------------------------------------------------------ Pyromancie

    private static boolean traitDeFeu(ServerPlayer p) {
        Hit hit = raycast(p, 20);
        line(p, hit.end(), ParticleTypes.FLAME);
        sound(p, SoundEvents.BLAZE_SHOOT, 1.2f);
        if (hit.target() != null) {
            damage(p, hit.target(), 5f);
            hit.target().setRemainingFireTicks(100);
            level(p).sendParticles(ParticleTypes.LAVA, hit.end().x, hit.end().y, hit.end().z, 6, 0.2, 0.2, 0.2, 0);
        }
        return true;
    }

    private static boolean embrasement(ServerPlayer p) {
        ServerLevel lvl = level(p);
        for (LivingEntity e : around(p, 5)) {
            damage(p, e, 2f);
            e.setRemainingFireTicks(80);
        }
        ring(p, 5, ParticleTypes.FLAME);
        lvl.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 0.5, p.getZ(), 20, 2.5, 0.3, 2.5, 0);
        sound(p, SoundEvents.FIRECHARGE_USE, 0.8f);
        return true;
    }

    private static boolean peauDeBraise(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 900, 0));
        level(p).sendParticles(ParticleTypes.SMALL_FLAME, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        sound(p, SoundEvents.BLAZE_AMBIENT, 1f);
        return true;
    }

    // ------------------------------------------------------------------ Luminomancie

    private static boolean lumiereGuerisseuse(ServerPlayer p) {
        p.heal(6f);
        List<Player> allies = p.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(5),
                o -> o != p && o.isAlive() && !o.isSpectator());
        for (Player o : allies) {
            o.heal(4f);
            level(p).sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + 2, o.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        level(p).sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0.03);
        sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
        return true;
    }

    private static boolean eblouissement(ServerPlayer p) {
        for (LivingEntity e : around(p, 6)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
        ring(p, 6, ParticleTypes.END_ROD);
        level(p).sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getEyeY(), p.getZ(), 40, 0.5, 0.5, 0.5, 0.2);
        sound(p, SoundEvents.BEACON_ACTIVATE, 1.6f);
        return true;
    }

    private static boolean revelation(ServerPlayer p) {
        for (LivingEntity e : around(p, 24)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        level(p).sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1, p.getZ(), 40, 1.5, 1, 1.5, 0.02);
        sound(p, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Hématomancie

    private static boolean drainDeSang(ServerPlayer p) {
        Hit hit = raycast(p, 12);
        if (hit.target() == null) {
            bar(p, Component.literal("Aucune cible à drainer.").withColor(0x888888));
            return false;
        }
        line(p, hit.end(), BLOOD);
        damage(p, hit.target(), 4f);
        p.heal(4f);
        sound(p, SoundEvents.WARDEN_HEARTBEAT, 1.2f);
        return true;
    }

    private static boolean lameDeSang(ServerPlayer p) {
        Hit hit = raycast(p, 16);
        if (hit.target() == null) {
            bar(p, Component.literal("Aucune cible pour la lame.").withColor(0x888888));
            return false;
        }
        line(p, hit.end(), BLOOD);
        damage(p, hit.target(), 8f);
        level(p).sendParticles(ParticleTypes.DAMAGE_INDICATOR, hit.end().x, hit.end().y, hit.end().z, 6, 0.2, 0.2, 0.2, 0.1);
        sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
        return true;
    }

    private static boolean pacteDeSang(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
        level(p).sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1, p.getZ(), 50, 0.6, 0.9, 0.6, 0.02);
        sound(p, SoundEvents.WARDEN_HEARTBEAT, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ Outils

    /**
     * Dégâts d'un sort. Sur un joueur, on utilise une source « magie » sans attaquant :
     * le sort blesse donc les joueurs même si le PvP est désactivé sur le serveur.
     */
    private static void damage(ServerPlayer caster, LivingEntity target, float amount) {
        DamageSource src = target instanceof Player
                ? caster.damageSources().magic()
                : caster.damageSources().indirectMagic(caster, caster);
        target.hurt(src, amount);
        if (target instanceof ServerPlayer victim && victim != caster) {
            victim.displayClientMessage(Component.literal("Tu es frappé par la mancie de " + caster.getGameProfile().getName() + " !")
                    .withColor(0xB0141E), true);
        }
    }

    private record Hit(Vec3 end, LivingEntity target) {}

    private static Hit raycast(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 end = start.add(p.getLookAngle().scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        AABB box = p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(p, start, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != p,
                start.distanceToSqr(end));
        if (ent != null && ent.getEntity() instanceof LivingEntity le) {
            return new Hit(ent.getLocation(), le);
        }
        return new Hit(end, null);
    }

    private static List<LivingEntity> around(ServerPlayer p, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && e.distanceToSqr(p) <= r * r);
    }

    private static void line(ServerPlayer p, Vec3 end, ParticleOptions particle) {
        Vec3 start = p.getEyePosition().add(0, -0.2, 0);
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.01) return;
        Vec3 step = dir.normalize().scale(0.4);
        Vec3 pos = start;
        for (double d = 0; d < len; d += 0.4) {
            level(p).sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
            pos = pos.add(step);
        }
    }

    private static void ring(ServerPlayer p, double r, ParticleOptions particle) {
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            level(p).sendParticles(particle, p.getX() + Math.cos(a) * r, p.getY() + 0.3, p.getZ() + Math.sin(a) * r,
                    1, 0, 0.05, 0, 0);
        }
    }

    private static void sound(ServerPlayer p, SoundEvent ev, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ev, SoundSource.PLAYERS, 1f, pitch);
    }

    private static void bar(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }

    private static ServerLevel level(ServerPlayer p) {
        return p.serverLevel();
    }
}
