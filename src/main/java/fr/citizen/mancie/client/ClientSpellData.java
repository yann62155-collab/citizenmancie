package fr.citizen.mancie.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copie côté client des sorts connus (aucune classe client Minecraft ici). */
public final class ClientSpellData {
    private static volatile List<String> spells = List.of();
    private static final Map<String, Long> COOLDOWN_END_MS = new HashMap<>();

    private ClientSpellData() {}

    public static void setSpells(List<String> list) {
        spells = List.copyOf(list);
    }

    public static List<String> spells() {
        return spells;
    }

    public static synchronized void setCooldown(String spell, int ticks) {
        COOLDOWN_END_MS.put(spell, System.currentTimeMillis() + ticks * 50L);
    }

    /** Secondes restantes avant de pouvoir relancer (0 = prêt). */
    public static synchronized int remainingSeconds(String spell) {
        Long end = COOLDOWN_END_MS.get(spell);
        if (end == null) return 0;
        long left = end - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0);
    }

    public static synchronized void reset() {
        spells = List.of();
        COOLDOWN_END_MS.clear();
    }
}
