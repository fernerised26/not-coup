package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class HitmanCard extends Card {

	public static Roles ROLE = Roles.HITMAN;
	
	private String name = "Hitman";
	private String eliminatedName = "HitmanElim";

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
