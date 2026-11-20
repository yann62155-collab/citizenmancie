package fr.citizen.mancie.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** Sorts appris par un joueur (sauvegardés dans les données du joueur, conservés à la mort). */
public record MancieData(List<String> spells) {
    public static final MancieData EMPTY = new MancieData(List.of());

    public static final Codec<MancieData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("spells").forGetter(MancieData::spells)
    ).apply(i, MancieData::new));

    public boolean has(Spell s) {
        return spells.contains(s.id());
    }

    public MancieData with(Spell s) {
        if (has(s)) return this;
        List<String> l = new ArrayList<>(spells);
        l.add(s.id());
        return new MancieData(List.copyOf(l));
    }

    public MancieData without(Spell s) {
        List<String> l = new ArrayList<>(spells);
        l.remove(s.id());
        return new MancieData(List.copyOf(l));
    }

    /** Sorts valides, dans l'ordre d'apprentissage (ignore d'éventuels anciens ids). */
    public List<Spell> known() {
        List<Spell> l = new ArrayList<>();
        for (String id : spells) {
            Spell s = Spell.byId(id);
            if (s != null) l.add(s);
        }
        return l;
    }
}
