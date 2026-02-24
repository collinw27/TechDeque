package oop.practical.techdeque.deck;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

// Important: rank is stored from 1-5, not 0-4

public final class Card
{
    Type type;
    int rank;
    Optional<SpecialtyCard> specialtyCard;

    public enum Type
    {
        Grass(0),
        Fire(1),
        Water(2);

        // Types are internally declared with an index to allow
        // rock-paper-scissors comparison

        private final int index;

        Type(int index)
        {
            this.index = index;
        }

        public int getIndex() { return index;}

        @Override
        public String toString()
        {
            return name();
        }
    }

    public enum Specialty
    {
        SHIELD(1),
        SPEAR(3),
        ULTIMATE(5);

        public final int rank;

        Specialty(int rank)
        {
            this.rank = rank;
        }
    }

    public enum SpecialtyCard
    {
        GrassShield(Type.Grass, Specialty.SHIELD),
        WaterShield(Type.Water, Specialty.SHIELD),
        FireShield(Type.Fire, Specialty.SHIELD),
        GrassSpear(Type.Grass, Specialty.SPEAR),
        WaterSpear(Type.Water, Specialty.SPEAR),
        FireSpear(Type.Fire, Specialty.SPEAR),
        Solarbeam(Type.Grass, Specialty.ULTIMATE),
        Tempest(Type.Water, Specialty.ULTIMATE),
        Inferno(Type.Fire, Specialty.ULTIMATE);

        public final Type type;
        public final Specialty specialty;

        SpecialtyCard(Type type, Specialty specialty)
        {
            this.type = type;
            this.specialty = specialty;
        }
    }

    private static final ArrayList<String> rankStrings = new ArrayList(Arrays.asList(
        "I", "II", "III", "IV", "V"
    ));

    // Two different constructors for normal vs. specialty
    // This prevents a redundant rank argument for specialty cards

    public Card(Type type, int rank)
    {
        this.type = type;
        this.rank = rank;
        this.specialtyCard = Optional.empty();
    }

    public Card(SpecialtyCard specialtyCard)
    {
        this.type = specialtyCard.type;
        this.specialtyCard = Optional.of(specialtyCard);
        this.rank = specialtyCard.specialty.rank;
    }

    public int rank() { return rank; }
    public Type type() { return type; }
    public Optional<Specialty> specialty()
    {
        return (specialtyCard.isPresent()) ? Optional.of(specialtyCard.get().specialty) : Optional.empty();
    }

    // Methods that verify the validity of Card formatting are implemented here
    // This is maybe more abstracted than necessary, but it feels more correct than
    // allowing the GameManager to enforce its own constraints on Card state

    public static Optional<Type> parseType(String str) throws IllegalArgumentException
    {
        try
        {
            str = str.isEmpty() ? "" : (str.substring(0, 1).toUpperCase()) + str.substring(1);
            return Optional.of(Type.valueOf(str));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parseRank(String str)
    {
        int index = rankStrings.indexOf(str);
        return (index >= 0) ? Optional.of(index + 1) : Optional.empty();
    }

    public static Optional<SpecialtyCard> parseSpecialty(String str)
    {
        try
        {
            return Optional.of(SpecialtyCard.valueOf(str));
        }
        catch (IllegalArgumentException e)
        {
            return Optional.empty();
        }
    }

    public static boolean isValidRank(Integer rank)
    {
        return (rank >= 1 && rank <= rankStrings.size());
    }

    @Override @NonNull
    public String toString()
    {
        if (specialtyCard.isEmpty())
            return type.toString() + "-" + rankStrings.get(rank - 1);
        else
            return specialtyCard.get().name();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Card c)
            return (type.equals(c.type) && rank == c.rank == specialtyCard.equals(c.specialtyCard));
        return false;
    }

    // Returns a positive number if type(this) > type(other)

    public int compareType(Card other)
    {
        int differential = ((type.getIndex() - other.type.getIndex()) % 3 + 3) % 3;
        return switch (differential)
        {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> -1;
            default -> throw new UnsupportedOperationException();
        };
    }

    public int compareRank(Card other)
    {
        return rank - other.rank;
    }
}
