package oop.practical.techdeque.deck;

// This class is used for editing decks while not in combat
// This is because a deck shouldn't be edited while it's in use
// Once editing is finished, buildDeck() generates a combat-usable Deck

import java.util.*;

public class EditableDeck
{
    private ArrayList<Card> cards = new ArrayList<>();

    public EditableDeck() {}
    public EditableDeck(ArrayList<Card> cards)
    {
        this.cards = cards;
    }

    public int addCard(Card card, int quantity, boolean validate)
    {
        // Performs a rudimentary validation that only checks max of this card

        int cardTotal = (int)cards.stream().filter(c -> c.equals(card)).count() + quantity;
        if (validate)
        {
            if (card.rank() <= 2 && cardTotal > 3)
                throw new IllegalArgumentException("Too many copies of %s (%s > 3).".formatted(card, cardTotal));
            else if (card.rank() > 2 && card.rank() <= 4 && cardTotal > 2)
                throw new IllegalArgumentException("Too many copies of %s (%s > 2).".formatted(card, cardTotal));
            else if (card.rank() == 5 && cardTotal > 1)
                throw new IllegalArgumentException("Too many copies of %s (%s > 1).".formatted(card, cardTotal));
        }

        // If validated, add the cards

        for (int i = 0; i < quantity; i++)
        {
            cardTotal++;
            cards.add(card);
        }
        return quantity;
    }

    public int removeCard(Card card, int quantity)
    {
        for (int i = 0; i < quantity; i++)
        {
            if (!cards.remove(card))
                throw new IllegalArgumentException("Attempted to remove nonexistent card.");
        }
        return quantity;
    }

    public int clear()
    {
        int totalRemoved = cards.size();
        cards.clear();
        return totalRemoved;
    }

    public Deck buildDeck()
    {
        return new Deck(new ArrayList<>(cards));
    }

    public EditableDeck duplicate()
    {
        return new EditableDeck(new ArrayList<>(cards));
    }

    public int getSize()
    {
        return cards.size();
    }

    public void sort()
    {
        cards.sort(Comparator.comparing(Card::toString));
    }

    // It seems more reasonable to perform deck validation within the deck itself,
    // because otherwise another class would need to access the internal dat
    // of the deck object

    public void validate()
    {
        // Validate deck size

        if (cards.size() < 10)
            throw new IllegalArgumentException("Deck must have at least 10 cards.");

        // Select deck type & validate card totals

        Card.Type deckType = null;
        Map<Card, Integer> cardTotals = new HashMap<>();
        for (Card card : cards)
        {
            cardTotals.putIfAbsent(card, 0);
            cardTotals.put(card, cardTotals.get(card) + 1);
            if (card.rank() == 5)
                deckType = card.type();
        }
        if (deckType == null)
            throw new IllegalArgumentException("Deck must have a type.");
        for (Card card : cardTotals.keySet())
        {
            if (card.rank() <= 2 && cardTotals.get(card) > 3)
                throw new IllegalArgumentException("Too many copies of %s (%s > 3).".formatted(card, cardTotals.get(card)));
            else if (card.rank() > 2 && card.rank() <= 4 && cardTotals.get(card) > 2)
                throw new IllegalArgumentException("Too many copies of %s (%s > 2).".formatted(card, cardTotals.get(card)));
            else if (card.rank() == 5 && cardTotals.get(card) > 1)
                throw new IllegalArgumentException("Too many copies of %s (%s > 1).".formatted(card, cardTotals.get(card)));
            else if (card.rank() == 5 && card.type() != deckType)
                throw new IllegalArgumentException("Rank V card must be of type %s.".formatted(deckType.name()));
        }

        // Ensure half of the deck is of the correct type
        // Also ensure half is only basic cards

        Card.Type finalDeckType = deckType;
        int typedCards = (int) cards.stream().filter(c -> c.type() == finalDeckType).count();
        int basicCards = (int) cards.stream().filter(c -> (c.specialty().isEmpty())).count();
        int halfDeckSize = (int) Math.ceil(cards.size() / 2.0);
        if (typedCards < halfDeckSize)
            throw new IllegalArgumentException("Deck has too few cards of type %s (%s < %s).".formatted(
                deckType.name(), typedCards, halfDeckSize
            ));
        if (basicCards < halfDeckSize)
            throw new IllegalArgumentException("Deck has too few basic cards (%s < %s).".formatted(
                basicCards, halfDeckSize
            ));
    }

    public List<String> getStringList()
    {
        return cards.stream().map(Card::toString).toList();
    }

    @Override
    public String toString()
    {
        return cards.stream().map(Card::toString).toList().toString();
    }
}
