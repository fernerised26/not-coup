package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class AssassinCard extends Card {

	public static Roles ROLE = Roles.ASSASSIN;
	
	public String name = "Assassin";
	
	@Override
	public String toString() {
		return "Assassin";
	}
}
