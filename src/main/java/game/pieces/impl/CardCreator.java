package game.pieces.impl;

import java.io.IOException;

import game.pieces.Card;
import game.pieces.Roles;

public class CardCreator {

	public static Card createCard(Roles role) throws IOException {
		switch (role) {
			case NETOPS:
				return new NetOpsCard();
			case HITMAN:
				return new HitmanCard();
			case CAPTAIN:
				return new CaptainCard();
			case DECOY:
				return new DecoyCard();
			case MOGUL:
				return new MogulCard();
			default:
				throw new IOException("Not a valid role in this game");
		}
	}
}
