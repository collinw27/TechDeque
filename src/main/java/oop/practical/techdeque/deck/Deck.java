package oop.practical.techdeque.deck;

import java.util.ArrayList;

public final class Deck
{
    private ArrayList<Card> cards;
    private ArrayList<Card> remainingCards;

    public Deck(ArrayList<Card> cardList)
    {
        cards = cardList;
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

    public Card drawTop()
    {
        if (isEmpty())
            return null;
        Card drawn = remainingCards.get(0);
        remainingCards.removeFirst();
        return drawn;
    }

    public ArrayList<Card> drawCards(int amount)
    {
        ArrayList<Card> output = new ArrayList<>();
        for (int i = 0; i < amount; i++)
        {
            if (!isEmpty())
                output.add(drawTop());
        }
        return output;
    }

    public Deck copy()
    {
        ArrayList<Card> newCards = new ArrayList<>(cards);
        return new Deck(newCards);
    }
}
