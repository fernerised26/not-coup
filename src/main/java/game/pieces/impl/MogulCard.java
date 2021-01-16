package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class MogulCard extends Card {

	public static Roles ROLE = Roles.MOGUL;
	
	private String name = "Mogul";
	private String eliminatedName = "MogulElim";

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
