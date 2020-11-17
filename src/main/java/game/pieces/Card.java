package game.pieces;

public abstract class Card {

	public boolean isFaceUp = false;
	
	public void flipUp() {
		isFaceUp = true;
	};
	
	public void flipDown() {
		isFaceUp = false;
	}
}
