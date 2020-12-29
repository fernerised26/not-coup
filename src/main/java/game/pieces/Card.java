package game.pieces;

public abstract class Card {

	public static String FACEDOWN = "FACEDOWN";
	private boolean isFaceUp = false;
	
	public void flipUp() {
		isFaceUp = true;
	};
	
	public void flipDown() {
		isFaceUp = false;
	}

	public boolean isFaceUp() {
		return isFaceUp;
	}
}
