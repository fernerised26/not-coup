package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class NetOpsCard extends Card {

	public static Roles ROLE = Roles.NETOPS;
	
	private String name = "NetOps";
	private String eliminatedName = "NetOpsElim";

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
