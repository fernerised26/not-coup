package game.systems;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import game.pieces.Deck;
import game.pieces.impl.DeckImpl;

public class Tabletop {

	Map<String, Player> playerMap = new HashMap<>();
	
	Deck deck = new DeckImpl();
	
	public String addPlayer(String name) {
		synchronized(playerMap) {
			playerMap.put(name, new Player(name));
			return convertSetToHtml(playerMap.keySet());
		}
	}
	
	public String removePlayer(String name) {
		synchronized(playerMap) {
			playerMap.remove(name);
			return convertSetToHtml(playerMap.keySet());
		}
	}
	
	public boolean isPlayerPresent(String name) {
		return playerMap.containsKey(name);
	}
	
	private String convertSetToHtml(Set<String> playerSet) {
		StringBuilder sb = new StringBuilder();
		for(String player : playerSet) {
			sb.append("<tr><td>").append(player).append("</td></tr>");
		}
		return sb.toString();
	}
}
