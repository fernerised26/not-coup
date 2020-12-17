package game.pieces;

public abstract class Card {

	public static String FACEDOWN = "FACEDOWN";
	public boolean isFaceUp = false;
	public String name;
	
	public void flipUp() {
		isFaceUp = true;
	};
	
	public void flipDown() {
		isFaceUp = false;
	}
}
