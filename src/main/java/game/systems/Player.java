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

	private String name;
	private int coins = 0;
	private List<Card> cardsOwned = new ArrayList<>();
	private Set<Roles> roles = new HashSet<>();
	private JSONArray jsonHand;
	private JSONObject jsonSelf;
	private JSONArray maskedHand;
	private JSONObject maskedSelf;
	private String secret;
	private boolean isLost = false;
	private boolean isVoidLocked = false;
	private Player nextPlayer;
	private Player prevPlayer;
	
	@SuppressWarnings("unchecked")
	public Player(String name, String secret) {
		super();
		this.name = name;
		this.secret = secret;
		JSONArray tempMaskedHand = new JSONArray();
		tempMaskedHand.add(Card.FACEDOWN);
		tempMaskedHand.add(Card.FACEDOWN);
		maskedHand = tempMaskedHand; 
		
		JSONObject tempMaskedSelf = new JSONObject();
		tempMaskedSelf.put("coins", coins);
		tempMaskedSelf.put("cardsOwned", maskedHand);
		tempMaskedSelf.put("isLost", false);
		maskedSelf = tempMaskedSelf;
		
		jsonHand = new JSONArray();
		
		JSONObject tempSelfJson = new JSONObject();
		tempSelfJson.put("coins", coins);
		tempSelfJson.put("cardsOwned", jsonHand);
		tempSelfJson.put("isLost", false);
		tempSelfJson.put("isVoidLocked", false);
		jsonSelf = tempSelfJson;
	}
	
	public Player cloneForReset() {
		Player clone = new Player(name, secret);
		return clone;
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
	
	public Card eliminateCardInHand(int indexInHand) {
		Card cardToFlip = cardsOwned.get(indexInHand);
		cardToFlip.flipUp();
		cardToFlip.eliminate();
		updateHandsForReveal(indexInHand, cardToFlip.toString());
		return cardToFlip;
	}
	
	public Card getCardInHand(int indexInHand) {
		Card card = cardsOwned.get(indexInHand);
		return card;
	}
	
	@SuppressWarnings("unchecked")
	public Card revealCardInHand(int indexInHand) {
		Card cardToFlip = cardsOwned.get(indexInHand);
		cardToFlip.flipUp();
		updateHandsForReveal(indexInHand, cardToFlip.toString());
		return cardToFlip;
	}
	
	public void replaceCardInHand(Card replacementCard, int indexToReplace) {
		cardsOwned.set(indexToReplace, replacementCard);
		jsonHand.set(indexToReplace, replacementCard.toString());
		maskedHand.set(indexToReplace, Card.FACEDOWN);
	}
	
	public boolean isSecret(String presentedSecret) {
		return secret.equals(presentedSecret);
	}
	
	@SuppressWarnings("unchecked")
	private void updateJsonCoins() {
		maskedSelf.put("coins", coins);
		jsonSelf.put("coins", coins);
	}
	
	@SuppressWarnings("unchecked")
	private void updateHandsForReveal(int indexInHand, String flippedCardName) {
		jsonHand.set(indexInHand, flippedCardName);
		maskedHand.set(indexInHand, flippedCardName);
	}
	
	@SuppressWarnings("unchecked")
	public void updateJsonHandForExchange() {
		for(int i=0; i<cardsOwned.size(); i++) {
			Card currReplacementCard = cardsOwned.get(i);
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
		StringBuilder builder = new StringBuilder();
		builder.append("Player [name=").append(name)
			.append(", coins=").append(coins)
			.append(", cardsOwned=").append(cardsOwned)
			.append(", roles=").append(roles)
			.append(", jsonHand=").append(jsonHand.toJSONString())
			.append(", jsonSelf=").append(jsonSelf.toJSONString())
			.append(", maskedHand=").append(maskedHand.toJSONString())
			.append(", maskedSelf=").append(maskedSelf.toJSONString())
			.append(", secret=").append(secret)
			.append(", isLost=").append(isLost)
			.append(", nextPlayer=").append(nextPlayer.getName())
			.append(", prevPlayer=").append(prevPlayer.getName()).append("]");
		return builder.toString();
	}

	public String getName() {
		return name;
	}

	public int getCoins() {
		return coins;
	}

	public List<Card> getCardsOwned() {
		return cardsOwned;
	}

	public Set<Roles> getRoles() {
		return roles;
	}
	
	public JSONObject getSelf() {
		return jsonSelf;
	}
	
	public JSONObject getMaskedSelf() {
		return maskedSelf;
	}
	
	public boolean isLost() {
		return isLost;
	}
	
	public void eliminatePlayer() {
		isLost = true;
		maskedSelf.put("isLost", true);
		jsonSelf.put("isLost", true);
	}

	public Player getNextPlayer() {
		return nextPlayer;
	}

	public void setNextPlayer(Player nextPlayer) {
		this.nextPlayer = nextPlayer;
	}

	public Player getPrevPlayer() {
		return prevPlayer;
	}

	public void setPrevPlayer(Player prevPlayer) {
		this.prevPlayer = prevPlayer;
	}

	public boolean isVoidLocked() {
		return isVoidLocked;
	}

	public void setVoidLocked(boolean isVoidLocked) {
		this.isVoidLocked = isVoidLocked;
		jsonSelf.put("isVoidLocked", isVoidLocked);
	}
}
