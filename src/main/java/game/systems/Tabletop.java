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
				
				System.out.println("dealing to "+currPlayer.name);
				
				List<Card> cardsDrawn = deck.draw(2);
				System.out.println("cards drawn: "+cardsDrawn);
				currPlayer.addCardsInit(cardsDrawn);
				currPlayer.addCoins(2);
				orderedPlayerNames.add(playerEntry.getKey());
				
				System.out.println("finished dealing "+ currPlayer);
			}
			
//			tableController.notifyTableOfPlayerOrder(orderedPlayerNames.toJSONString());
			System.out.println(playerMap);
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				String currPlayerName = playerEntry.getKey();
				String maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
				playerController.contactPlayerInitTable(currPlayerName, orderedPlayerNames.toJSONString(), maskedPlayerJson);
			}
			return roundActive;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String getMaskedPlayerMapAsJson(String targetPlayerName) {
		JSONObject playerMapJsonObj = new JSONObject();
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			String currPlayerName = playerEntry.getKey();
			if(currPlayerName.equals(targetPlayerName)) {
				playerMapJsonObj.put(currPlayerName, playerEntry.getValue().getSelf());
			} else {
				playerMapJsonObj.put(currPlayerName, playerEntry.getValue().getMaskedSelf());
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
