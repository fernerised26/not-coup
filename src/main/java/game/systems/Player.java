package game.systems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import game.pieces.Card;
import game.pieces.Roles;

public class Player {

	public String name;
	public int coins = 0;
	public List<Card> cardsOwned = new ArrayList<>();
	
	private Set<Roles> roles = new HashSet<>();
	private JSONArray jsonHand;
	private JSONObject jsonSelf;
	private JSONArray maskedHand;
	private JSONObject maskedSelf;
	
	@SuppressWarnings("unchecked")
	public Player(String name) {
		super();
		this.name = name;
		JSONArray tempMaskedHand = new JSONArray();
		tempMaskedHand.add(Card.FACEDOWN);
		tempMaskedHand.add(Card.FACEDOWN);
		maskedHand = tempMaskedHand; 
		
		JSONObject tempMaskedSelf = new JSONObject();
		tempMaskedSelf.put("coins", coins);
		tempMaskedSelf.put("cardsOwned", maskedHand);
		maskedSelf = tempMaskedSelf;
		
		jsonHand = new JSONArray();
		
		JSONObject tempSelfJson = new JSONObject();
		tempSelfJson.put("coins", coins);
		tempSelfJson.put("cardsOwned", jsonHand);
		jsonSelf = tempSelfJson;
	}
	
	public void addCardsInit(List<Card> cardsToAdd) {
		for(Card card : cardsToAdd) {
			cardsOwned.add(card);
			jsonHand.add(card.toString());
		}
	}
	
	public int addCoins(int numOfCoinsToAdd) {
		coins += numOfCoinsToAdd;
		updateJsonCoins();
		return coins;
	}
	
	public void eliminateCardInHand(int indexInHand) {
		Card cardToFlip = cardsOwned.get(indexInHand);
		cardToFlip.flipUp();
		updateMaskedHandForElimination(indexInHand, cardToFlip.toString());
	}
	
	public String revealCardInHand(int indexInHand) {
		Card cardToFlip = cardsOwned.get(indexInHand);
		return cardToFlip.toString();
	}
	
	public void addCardsViaExchange(List<Card> replacementHand) {
		cardsOwned = replacementHand;
		updateJsonHandForExchange(replacementHand);
	}

	public JSONObject getSelf() {
		return jsonSelf;
	}
	
	public JSONObject getMaskedSelf() {
		return maskedSelf;
	}
	
	@SuppressWarnings("unchecked")
	private void updateJsonCoins() {
		maskedSelf.put("coins", coins);
		jsonSelf.put("coins", coins);
	}
	
	@SuppressWarnings("unchecked")
	private void updateMaskedHandForElimination(int indexInHand, String flippedCardName) {
		maskedHand.set(indexInHand, flippedCardName);
	}
	
	@SuppressWarnings("unchecked")
	private void updateJsonHandForExchange(List<Card> replacementHand) {
		for(int i=0; i<replacementHand.size(); i++) {
			Card currReplacementCard = replacementHand.get(i);
			jsonHand.set(i, currReplacementCard.toString());
			if(currReplacementCard.isFaceUp()) {
				maskedHand.set(i, currReplacementCard.toString());
			} else {
				maskedHand.set(i, Card.FACEDOWN);
			}
		}
	}
	
	@Override
	public String toString() {
		return "Player [name=" + name + ", coins=" + coins + ", cardsOwned=" + cardsOwned + ", roles=" + roles + ", jsonHand=" + jsonHand.toJSONString() + ", maskedHand=" + maskedHand.toJSONString() + "]";
	}
}
