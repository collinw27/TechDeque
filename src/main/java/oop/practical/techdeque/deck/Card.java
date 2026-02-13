package oop.practical.techdeque.deck;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;

// Important: rank is stored from 1-5, not 0-4

public record Card
(Type type, Integer rank)
{
    public enum Type {
        GRASS(0),
        FIRE(1),
        WATER(2);

        // Types are internally declared with an index to allow
        // rock-paper-scissors comparison

        private final int index;

        private Type(int index)
        {
            this.index = index;
        }

        public int getIndex() { return index;}

        @Override
        public String toString()
        {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    private static ArrayList<String> rankStrings = new ArrayList(Arrays.asList(
        "I", "II", "III", "IV", "V"
    ));

    // Methods that verify the validity of Card formatting are implemented here
    // This is maybe more abstracted than necessary, but it feels more correct than
    // allowing the GameManager to enforce its own constraints on Card state

    public static Type parseType(String str) throws IllegalArgumentException
    {
        try
        {
            return Type.valueOf(str.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Invalid card type: " + str);
        }
    }

    public static Integer parseRank(String str)
    {
        Integer index = rankStrings.indexOf(str);
        if (index >= 0)
            return index + 1;
        throw new IllegalArgumentException("Invalid card rank: " + str);
    }

    public static boolean isValidRank(Integer rank)
    {
        return (rank >= 1 && rank <= rankStrings.size());
    }

    @Override @NonNull
    public String toString()
    {
        return type.toString() + "-" + rankStrings.get(rank - 1);
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
