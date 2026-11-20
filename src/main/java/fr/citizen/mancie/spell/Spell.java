package fr.citizen.mancie.spell;

import java.util.ArrayList;
import java.util.List;

/**
 * Tous les sorts du mod. L'identifiant (id) est celui utilisé dans les commandes
 * et dans la sauvegarde du joueur : ne pas le renommer une fois en jeu.
 */
public enum Spell {
    // --- Pyromancie ---
    TRAIT_DE_FEU("trait_de_feu", Mancie.PYROMANCIE, "Trait de feu",
            "Jet de flammes à 20 blocs : 5 dégâts et brûlure 5 s.", 60, 0),
    EMBRASEMENT("embrasement", Mancie.PYROMANCIE, "Embrasement",
            "Enflamme toutes les créatures à 5 blocs autour de toi.", 300, 0),
    PEAU_DE_BRAISE("peau_de_braise", Mancie.PYROMANCIE, "Peau de braise",
            "Résistance au feu pendant 45 s.", 900, 0),

    // --- Luminomancie ---
    LUMIERE_GUERISSEUSE("lumiere_guerisseuse", Mancie.LUMINOMANCIE, "Lumière guérisseuse",
            "Te soigne de 3 cœurs et les joueurs proches de 2 cœurs.", 400, 0),
    EBLOUISSEMENT("eblouissement", Mancie.LUMINOMANCIE, "Éblouissement",
            "Aveugle les créatures à 6 blocs pendant 5 s.", 400, 0),
    REVELATION("revelation", Mancie.LUMINOMANCIE, "Révélation",
            "Fait briller les créatures à 24 blocs et te donne la vision nocturne.", 600, 0),

    // --- Hématomancie ---
    DRAIN_DE_SANG("drain_de_sang", Mancie.HEMATOMANCIE, "Drain de sang",
            "Vole 2 cœurs à la cible visée (12 blocs).", 160, 0),
    LAME_DE_SANG("lame_de_sang", Mancie.HEMATOMANCIE, "Lame de sang",
            "Coûte 1,5 cœur : lame sanglante de 8 dégâts (16 blocs).", 100, 3),
    PACTE_DE_SANG("pacte_de_sang", Mancie.HEMATOMANCIE, "Pacte de sang",
            "Coûte 3 cœurs : Force II et Vitesse pendant 20 s.", 1200, 6);

    private final String id;
    private final Mancie mancie;
    private final String displayName;
    private final String description;
    private final int cooldownTicks;
    private final float healthCost;

    Spell(String id, Mancie mancie, String displayName, String description, int cooldownTicks, float healthCost) {
        this.id = id;
        this.mancie = mancie;
        this.displayName = displayName;
        this.description = description;
        this.cooldownTicks = cooldownTicks;
        this.healthCost = healthCost;
    }

    public String id() { return id; }
    public Mancie mancie() { return mancie; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public int cooldownTicks() { return cooldownTicks; }
    public float healthCost() { return healthCost; }

    public static Spell byId(String id) {
        if (id == null) return null;
        for (Spell s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    public static List<Spell> ofMancie(Mancie m) {
        List<Spell> list = new ArrayList<>();
        for (Spell s : values()) if (s.mancie == m) list.add(s);
        return list;
    }
}
