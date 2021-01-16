package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class CaptainCard extends Card {

	public static Roles ROLE = Roles.CAPTAIN;
	
	private String name = "Captain";
	private String eliminatedName = "CaptainElim";

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
