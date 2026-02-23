package oop.practical.techdeque.deck;

// This class is used for editing decks while not in combat
// This is because a deck shouldn't be edited while it's in use

import java.util.ArrayList;
import java.util.List;

public class EditableDeck
{
    private ArrayList<Card> cards = new ArrayList<>();

    public int addCard(Card card, int quantity)
    {
        for (int i = 0; i < quantity; i++)
            cards.add(card);
        return quantity;
    }

    public int removeCard(Card card, int quantity)
    {
        int totalRemoved = 0;
        while (totalRemoved < quantity && cards.remove(card))
            totalRemoved++;
        return totalRemoved;
    }

    public int clear()
    {
        int totalRemoved = cards.size();
        cards.clear();
        return totalRemoved;
    }

    public Deck buildDeck()
    {
        return new Deck(cards);
    }

    public int getSize()
    {
        return cards.size();
    }

    public String getViewString()
    {
        List<String> cardStream = cards.stream().map(Card::toString).toList();
        return String.join("\n", cardStream);
    }

    @Override
    public String toString()
    {
        return cards.toString();
    }
}
