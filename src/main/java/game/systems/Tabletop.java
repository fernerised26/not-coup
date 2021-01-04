package game.systems;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import game.pieces.Card;
import game.pieces.Deck;
import game.pieces.impl.DeckImpl;
import game.systems.web.PlayerController;
import game.systems.web.TableController;

@Component
public class Tabletop {
	
	private boolean roundActive = false;
	private Map<String, Player> playerMap = new LinkedHashMap<>(); //Linked to maintain a play order
	private JSONArray orderedPlayerNames;
	private Deck deck = new DeckImpl();
	private Player currActivePlayer = null;
	
	@Autowired
	private PlayerController playerController;
	
	@Autowired
	private TableController tableController;
	
	public String addPlayer(String name, String secret) {
		synchronized(playerMap) {
			if(roundActive) {
				return null;
			}
			playerMap.put(name, new Player(name, secret));
			String playerListHtml = convertSetToHtml(playerMap.keySet());
			tableController.notifyTableOfPlayerChange(playerListHtml);
			return playerListHtml;
		}
	}
	
	public void removePlayer(String name) {
		synchronized(playerMap) {
			if(roundActive) {
				playerMap.remove(name);
				String playerListHtml = convertSetToHtml(playerMap.keySet());
				tableController.notifyTableOfPlayerChange(playerListHtml);
			}
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
				
//				System.out.println("dealing to "+currPlayer.name);
				
				List<Card> cardsDrawn = deck.draw(2);
//				System.out.println("cards drawn: "+cardsDrawn);
				currPlayer.addCardsInit(cardsDrawn);
				currPlayer.addCoins(2);
				orderedPlayerNames.add(playerEntry.getKey());
				
//				System.out.println("finished dealing "+ currPlayer);
			}
			
//			System.out.println(playerMap);
			currActivePlayer = playerMap.get(orderedPlayerNames.get(0));
			JSONObject returnObj = new JSONObject();
			returnObj.put("activePlayer", currActivePlayer.getName());
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				String currPlayerName = playerEntry.getKey();
				JSONObject maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
				returnObj.put("boardState", maskedPlayerJson);
				playerController.contactPlayerInitTable(currPlayerName, orderedPlayerNames.toJSONString(), returnObj.toJSONString());
			}			
			return roundActive;
		}
	}
	
	public JSONObject getMaskedPlayerMapAsJson(String targetPlayerName) {
		JSONObject playerMapJsonObj = new JSONObject();
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			String currPlayerName = playerEntry.getKey();
			if(currPlayerName.equals(targetPlayerName)) {
				playerMapJsonObj.put(currPlayerName, playerEntry.getValue().getSelf());
			} else {
				playerMapJsonObj.put(currPlayerName, playerEntry.getValue().getMaskedSelf());
			}
		}
		return playerMapJsonObj;
	}
	
	public boolean isRoundActive() {
		return roundActive;
	}

	public Map<String, Player> getPlayerMap() {
		return playerMap;
	}
	
	public Player getCurrActivePlayer() {
		return currActivePlayer;
	}
	
	public boolean isActivePlayer(String secret, String playerName) {
		return (currActivePlayer.getName().equals(playerName)
				&& currActivePlayer.isSecret(secret));
	}
	
	public boolean isSecretCorrect(String secret, String playerName) {
		return playerMap.get(playerName).isSecret(secret);
	}
	
	public void handlePayday() {
		currActivePlayer.addCoins(1);
		advanceActivePlayer();
		
		JSONObject returnObj = new JSONObject();
		returnObj.put("activePlayer", currActivePlayer.getName());
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			String currPlayerName = playerEntry.getKey();
			JSONObject maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
			returnObj.put("boardState", maskedPlayerJson);
			playerController.contactPlayerUpdateTable(currPlayerName, returnObj.toJSONString());
		}
	}
	
	private void advanceActivePlayer() {
		System.out.println("Advancing player");
		System.out.println(playerMap);
		System.out.println(orderedPlayerNames);
		int currActiveIndex = orderedPlayerNames.indexOf(currActivePlayer.getName());
		System.out.println("currActiveIndex: "+currActiveIndex);
		int newActiveIndex = (currActiveIndex + 1) == orderedPlayerNames.size() ? 0 : (currActiveIndex + 1);
		System.out.println("newActiveIndex: "+newActiveIndex);
		System.out.println("active name 1: " + currActivePlayer.getName());
		currActivePlayer = playerMap.get(orderedPlayerNames.get(newActiveIndex));
		System.out.println("active name 1: " + currActivePlayer.getName());
	}

	private String convertSetToHtml(Set<String> playerSet) {
		StringBuilder sb = new StringBuilder();
		for(String player : playerSet) {
			sb.append("<tr><td>").append(player).append("</td></tr>");
		}
		return sb.toString();
	}
}
