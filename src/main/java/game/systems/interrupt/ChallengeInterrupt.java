package game.systems.interrupt;

public class ChallengeInterrupt extends Interrupt {
	
	private final String challenged;
	private final String challenger;
	private String thirdParty;
	private int revealedDefendingCardIndex = -1;

	/**
	 * Represents a player's assertion of a card being challenged
	 * Used when there are 2 possible players involved in the action being challenged 
	 * @param focused Inclusionary, only focused player is allowed to respond.
	 */
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, InterruptCase actionChallenged) {
		super(interruptId, challenged, actionChallenged);
		this.challenged = challenged;
		this.challenger = challenger;
	}
	
	/**
	 * Represents a player's assertion of a card being challenged
	 * Used when there are 3 possible players involved in the action being challenged e.g. orderHit & raid 
	 * @param focused Inclusionary, only focused player is allowed to respond.
	 */
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, InterruptCase actionChallenged, String thirdParty) {
		super(interruptId, challenged, actionChallenged);
		this.challenged = challenged;
		this.challenger = challenger;
		this.thirdParty = thirdParty;
	}
	
	/**
	 * Represents a challenge loss, initially challenged player has proven ownership of needed card and now the challenger must lose a card 
	 * Used when there are 2 possible players involved in the action being challenged
	 * @param focused Inclusionary, only focused player is allowed to respond. 
	 */
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, InterruptCase actionChallenged, int revealedDefendingCardIndex) {
		super(interruptId, challenger, actionChallenged);
		this.challenged = challenged;
		this.challenger = challenger;
		this.revealedDefendingCardIndex = revealedDefendingCardIndex;
	}
	
	/**
	 * Represents a challenge loss, initially challenged player has proven ownership of needed card and now the challenger must lose a card
	 * Used when there are 3 possible players involved in the action being challenged e.g. orderHit & raid
	 * @param focused Inclusionary, only focused player is allowed to respond. 
	 */
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, InterruptCase actionChallenged, int revealedDefendingCardIndex, String thirdParty) {
		super(interruptId, challenger, actionChallenged);
		this.challenged = challenged;
		this.challenger = challenger;
		this.revealedDefendingCardIndex = revealedDefendingCardIndex;
		this.thirdParty = thirdParty;
	}

	public String getChallenged() {
		return challenged;
	}
	
	public String getChallenger() {
		return challenger;
	}
	
	public InterruptCase getActionChallenged() {
		return getInterruptCase();
	}
	
	public int getRevealedDefendingCardIndex() {
		return revealedDefendingCardIndex;
	}

	public String getThirdParty() {
		return thirdParty;
	}
}
