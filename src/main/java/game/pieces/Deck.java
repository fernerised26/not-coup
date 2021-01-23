package game.pieces;

import java.io.IOException;
import java.util.List;

public interface Deck {

	public void initialize() throws IOException;
	public void shuffle();
	public List<Card> draw(int numOfCards);
	public Card drawOne();
	public void add(List<Card> cardsToAdd);
	public void add(Card cardToAdd);
	public int getDeckSize();
}
