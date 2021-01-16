package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class DecoyCard extends Card {

	public static Roles ROLE = Roles.DECOY;

	private String name = "Decoy";
	private String eliminatedName = "DecoyElim";

	public String toString() {
		if(isEliminated()) {
			return eliminatedName;
		} else {
			return name;
		}
	}
	
	@Override
	public Roles getRole() {
		return ROLE;
	}
}
