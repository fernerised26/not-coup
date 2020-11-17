package game.pieces.impl;

import java.io.IOException;

import game.pieces.Card;
import game.pieces.Roles;

public class CardCreator {

	public static Card createCard(Roles role) throws IOException {
		switch (role) {
		case AMBASSADOR:
			return new AmbassadorCard();
		case ASSASSIN:
			return new AssassinCard();
		case CAPTAIN:
			return new CaptainCard();
		case CONTESSA:
			return new ContessaCard();
		case DUKE:
			return new DukeCard();
		default:
			throw new IOException("Not a valid role in this game");
		}
	}
}
