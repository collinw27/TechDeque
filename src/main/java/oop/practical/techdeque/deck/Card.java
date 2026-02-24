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
    Optional<Specialty> specialty;

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

    private enum SpecialtyType
    {
        SHIELD(1),
        SPEAR(3),
        ULTIMATE(5);

        public final int rank;

        SpecialtyType(int rank)
        {
            this.rank = rank;
        }
    }

    public enum Specialty
    {
        GrassShield(Type.Grass, SpecialtyType.SHIELD),
        WaterShield(Type.Water, SpecialtyType.SHIELD),
        FireShield(Type.Fire, SpecialtyType.SHIELD),
        GrassSpear(Type.Grass, SpecialtyType.SPEAR),
        WaterSpear(Type.Water, SpecialtyType.SPEAR),
        FireSpear(Type.Fire, SpecialtyType.SPEAR),
        Solarbeam(Type.Grass, SpecialtyType.ULTIMATE),
        Tempest(Type.Water, SpecialtyType.ULTIMATE),
        Inferno(Type.Fire, SpecialtyType.ULTIMATE);

        public final Type type;
        public final SpecialtyType specialtyType;

        Specialty(Type type, SpecialtyType specialtyType)
        {
            this.type = type;
            this.specialtyType = specialtyType;
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
        this.specialty = Optional.empty();
    }

    public Card(Specialty specialty)
    {
        this.type = specialty.type;
        this.specialty = Optional.of(specialty);
        this.rank = specialty.specialtyType.rank;
    }

    public int rank() { return rank; }
    public Type type() { return type; }

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

    public static Optional<Specialty> parseSpecialty(String str)
    {
        try
        {
            return Optional.of(Specialty.valueOf(str));
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
        if (specialty.isEmpty())
            return type.toString() + "-" + rankStrings.get(rank - 1);
        else
            return specialty.get().name();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Card c)
            return (type.equals(c.type) && rank == c.rank == specialty.equals(c.specialty));
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
