package game.pieces;

import java.io.IOException;
import java.util.List;

public interface Deck {

	public void initialize() throws IOException;
	public void shuffle();
	public List<Card> draw(int numOfCards);
	public void add(List<Card> cardsToAdd);
}
