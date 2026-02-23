package oop.practical.techdeque.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Deck
{
    private ArrayList<Card> cards;
    private ArrayList<Card> remainingCards;

    public Deck(ArrayList<Card> cardList)
    {
        cards = new ArrayList<>(cardList);
        remainingCards = new ArrayList<>(cards);
    }

    @Override
    public String toString()
    {
        return remainingCards.toString();
    }

    public boolean isEmpty()
    {
        return remainingCards.isEmpty();
    }

    public void reshuffle()
    {
        remainingCards = new ArrayList<>(cards);
    }

    public Optional<Card> drawTop()
    {
        if (isEmpty())
            return Optional.empty();
        Card drawn = remainingCards.get(0);
        remainingCards.removeFirst();
        return Optional.of(drawn);
    }

    public ArrayList<Card> drawCards(int amount)
    {
        ArrayList<Card> output = new ArrayList<>();
        for (int i = 0; i < amount; i++)
        {
            if (!isEmpty())
                output.add(drawTop().get());
        }
        return output;
    }

    public Deck copy()
    {
        ArrayList<Card> newCards = new ArrayList<>(cards);
        return new Deck(newCards);
    }
}
