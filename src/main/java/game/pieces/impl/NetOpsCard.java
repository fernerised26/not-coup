package game.pieces.impl;

import game.pieces.Card;
import game.pieces.Roles;

public class NetOpsCard extends Card {

	public static Roles ROLE = Roles.NETOPS;
	
	private String name = "NetOps";
	
	public String toString() {
		return name;
	}
}
