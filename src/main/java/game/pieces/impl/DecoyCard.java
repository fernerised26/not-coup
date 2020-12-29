package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class DecoyCard extends Card {

	public static Roles ROLE = Roles.DECOY;

	private String name = "Decoy";
	
	public String toString() {
		return name;
	}
}
