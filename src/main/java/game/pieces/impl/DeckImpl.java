package game.pieces.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.pieces.Card;
import game.pieces.Deck;
import game.pieces.Roles;

public class DeckImpl implements Deck {

	private Random rand = new Random();
	private List<Card> deck = new ArrayList<>();

	@Override
	public void initialize() throws IOException {
//		System.out.println("Deck contents1: "+deck);
		deck.clear();
		Roles[] roles = Roles.values();
		for (int roleIndex = 0; roleIndex < roles.length; roleIndex++) {
			for (int i = 0; i < 3; i++) {
				deck.add(CardCreator.createCard(roles[roleIndex]));
			}
		}
//		System.out.println("Deck contents2: "+deck);
		shuffle();
//		System.out.println("Deck contents3: "+deck);
	}

	@Override
	public void shuffle() {
		for (int i = deck.size(); i > 1; i--) {
			int randomPoint = rand.nextInt(i);
			Card tmp = deck.get(i - 1);
			deck.set(i - 1, deck.get(randomPoint));
			deck.set(randomPoint, tmp);
		}

	}

	@Override
	public List<Card> draw(int numOfCards) {
		List<Card> returnList = new ArrayList<>();
		
		for(int i=0; i<numOfCards; i++) {
			Card removedCard = deck.remove(deck.size()-1);
			returnList.add(removedCard);
		}
		
		return returnList;
	}
	
	@Override
	public Card drawOne() {
		Card removedCard = deck.remove(deck.size()-1);
		return removedCard;
	}

	
	@Override
	public void add(List<Card> cardsToAdd) {
		for(Card card : cardsToAdd) {
			card.flipDown();
			deck.add(card);
		}
	}
	
	@Override
	public void add(Card cardToAdd) {
		cardToAdd.flipDown();
		deck.add(cardToAdd);
	}
}
