package game.systems;

public class ChallengeInterrupt extends Interrupt {
	
	private final String challenged;
	private final Action actionChallenged;
	private int revealedDefendingCardIndex = -1;

	public ChallengeInterrupt(String interruptId, String challenged, String challenger, Action actionChallenged) {
		super(interruptId, challenger);
		this.challenged = challenged;
		this.actionChallenged = actionChallenged;
	}
	
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, Action actionChallenged, int revealedDefendingCardIndex) {
		super(interruptId, challenger);
		this.challenged = challenged;
		this.actionChallenged = actionChallenged;
		this.revealedDefendingCardIndex = revealedDefendingCardIndex;
	}

	public String getChallenged() {
		return challenged;
	}
	
	public String getChallenger() {
		return getTriggerPlayer();
	}
	
	public Action getActionChallenged() {
		return actionChallenged;
	}
	
	public int getRevealedDefendingCardIndex() {
		return revealedDefendingCardIndex;
	}
}
