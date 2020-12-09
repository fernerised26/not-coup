package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class CaptainCard extends Card {

	public static Roles ROLE = Roles.CAPTAIN;
	
	public String name = "Captain";
	
	@Override
	public String toString() {
		return "Captain";
	}
}
