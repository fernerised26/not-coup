package game.systems;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import game.pieces.Deck;
import game.pieces.impl.DeckImpl;
import game.systems.web.PlayerController;
import game.systems.web.TableController;

@Component
public class Tabletop {
	
	public boolean roundActive = false;
	public Map<String, Player> playerMap = new LinkedHashMap<>(); //Linked to maintain a play order
	public JSONArray orderedPlayerNames;
	private Deck deck = new DeckImpl();
	
	@Autowired
	private PlayerController playerController;
	
	@Autowired
	private TableController tableController;
	
	public String addPlayer(String name) {
		synchronized(playerMap) {
			if(roundActive) {
				return null;
			}
			playerMap.put(name, new Player(name));
			String playerListHtml = convertSetToHtml(playerMap.keySet());
			tableController.notifyTableOfPlayerChange(playerListHtml);
			return playerListHtml;
		}
	}
	
	public void removePlayer(String name) {
		synchronized(playerMap) {
			playerMap.remove(name);
			String playerListHtml = convertSetToHtml(playerMap.keySet());
			tableController.notifyTableOfPlayerChange(playerListHtml);
		}
	}
	
	public boolean isPlayerPresent(String name) {
		return playerMap.containsKey(name);
	}
	
	public boolean startRound() throws IOException {
		synchronized(playerMap) {
			roundActive = true;
			deck.initialize();
			orderedPlayerNames = new JSONArray();
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				Player currPlayer = playerEntry.getValue();
				currPlayer.addCardsInit(deck.draw(2));
				currPlayer.addCoins(2);
				orderedPlayerNames.add(playerEntry.getKey());
			}
			
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				String currPlayerName = playerEntry.getKey();
				String maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
				playerController.contactPlayerInitTable(currPlayerName, maskedPlayerJson);
			}
			return roundActive;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String getMaskedPlayerMapAsJson(String targetPlayerName) {
		JSONObject playerMapJsonObj = new JSONObject();
		playerMapJsonObj.put("order", orderedPlayerNames);
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			if(playerEntry.getKey().equals(targetPlayerName)) {
				playerMapJsonObj.put(targetPlayerName, playerEntry.getValue().getSelf());
			} else {
				playerMapJsonObj.put(targetPlayerName, playerEntry.getValue().getMaskedSelf());
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
