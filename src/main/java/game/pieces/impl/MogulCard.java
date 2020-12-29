package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class MogulCard extends Card {

	public static Roles ROLE = Roles.MOGUL;
	
	private String name = "Mogul";

	public String toString() {
		return name;
	}
}
