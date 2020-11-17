package game.systems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import game.pieces.Card;
import game.pieces.Roles;

public class Player {

	public String name;
	public int coins = 0;
	private List<Card> cardsOwned = new ArrayList<>();
	private Set<Roles> roles = new HashSet<>();
	
	public Player(String name) {
		super();
		this.name = name;
	}

	public void income() {
		coins++;
	}
	
	public void foreignAid() {
		coins+=2;
	}
	
	public void coup() {
		coins-=7;
		//TODO interrupt system
	}
	
	public void tax() {
		if(roles.contains(Roles.DUKE)) {
			coins+=3;
		}
	}
	
	public void assassinate() {
		if(roles.contains(Roles.ASSASSIN)) {
			//TODO
		}
	}
}
