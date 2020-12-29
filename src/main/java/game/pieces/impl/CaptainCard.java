package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class CaptainCard extends Card {

	public static Roles ROLE = Roles.CAPTAIN;
	
	private String name = "Captain";

	public String toString() {
		return name;
	}
}
