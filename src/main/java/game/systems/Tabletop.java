package game.systems;

import java.util.ArrayList;
import java.util.List;

import game.pieces.Deck;
import game.pieces.impl.DeckImpl;

public class Tabletop {

	List<Player> playerList = new ArrayList<>();
	Deck deck = new DeckImpl();
	
	public void addPlayer(String name) {
		playerList.add(new Player(name));
	}
	
}
