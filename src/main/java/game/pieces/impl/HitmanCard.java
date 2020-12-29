package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class HitmanCard extends Card {

	public static Roles ROLE = Roles.HITMAN;
	
	private String name = "Hitman";
	
	public String toString() {
		return name;
	}
}
