package game.systems;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import game.pieces.Card;
import game.pieces.Deck;
import game.pieces.impl.DeckImpl;

public class Tabletop {
	
	public boolean roundActive = false;
	public Map<String, Player> playerMap = new LinkedHashMap<>(); //Linked to maintain a play order
	private Deck deck = new DeckImpl();
	
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
	
	public boolean startRound() throws IOException {
		synchronized(playerMap) {
			roundActive = true;
			deck.initialize();
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				Player currPlayer = playerEntry.getValue();
				currPlayer.addCards(deck.draw(2));
				currPlayer.addCoins(2);
			}
			return roundActive;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String getMaskedPlayerMapAsJson(String targetPlayerName) {
		JSONObject playerMapJsonObj = new JSONObject();
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			if(playerEntry.getKey().equals(targetPlayerName)) {
				playerMapJsonObj.put(targetPlayerName, playerEntry.getValue());
			} else {
				Player currPlayer = playerEntry.getValue();
				JSONObject maskedPlayer = new JSONObject();
				maskedPlayer.put("coins", currPlayer.coins);
				
				JSONArray currPlayerHand = new JSONArray();
				for(Card card : currPlayer.cardsOwned) {
					if(card.isFaceUp) {
						currPlayerHand.add(card.name);
					} else {
						currPlayerHand.add(Card.FACEDOWN);
					}
				}
				maskedPlayer.put("cardsOwned", currPlayerHand);
			}
		}
		return playerMapJsonObj.toJSONString();
	}
	
	private String convertSetToHtml(Set<String> playerSet) {
		StringBuilder sb = new StringBuilder();
		for(String player : playerSet) {
			sb.append("<tr><td>").append(player).append("</td></tr>");
		}
		return sb.toString();
	}
}
