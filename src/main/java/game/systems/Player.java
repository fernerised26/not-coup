package game.systems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;

import game.pieces.Card;
import game.pieces.Roles;

public class Player {

	public String name;
	public int coins = 0;
	public List<Card> cardsOwned = new ArrayList<>();
	private Set<Roles> roles = new HashSet<>();
	
	public Player(String name) {
		super();
		this.name = name;
	}
	
	public void addCards(List<Card> cardsToAdd) {
		cardsOwned.addAll(cardsToAdd);
	}
	
	public int addCoins(int numOfCoinsToAdd) {
		return coins += numOfCoinsToAdd;
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
	
	@Override
	public String toString() {
		return "{\"coins\":" + coins + ", \"cardsOwned\":" + JSONArray.toJSONString(cardsOwned) +"}";
	}
}
