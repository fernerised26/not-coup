package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class DukeCard extends Card {

	public static Roles ROLE = Roles.DUKE;
	
	public String name = "Duke";
	
	@Override
	public String toString() {
		return "Duke";
	}
}
