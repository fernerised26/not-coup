package game.pieces;

public abstract class Card {

	public static String FACEDOWN = "FACEDOWN";
	private boolean isFaceUp = false;
	private boolean isEliminated = false;
	
	public void flipUp() {
		isFaceUp = true;
	};
	
	public void flipDown() {
		isFaceUp = false;
	}

	public boolean isFaceUp() {
		return isFaceUp;
	}
	
	public void eliminate() {
		isEliminated = true;
	}
	
	public boolean isEliminated() {
		return isEliminated;
	}
	
	public abstract Roles getRole();
}
