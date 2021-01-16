package game.systems;

public class ChallengeInterrupt extends Interrupt {
	
	private final String challenged;
	private final String challenger;
	private final Action actionChallenged;
	private int revealedDefendingCardIndex = -1;

	public ChallengeInterrupt(String interruptId, String challenged, String challenger, Action actionChallenged) {
		super(interruptId);
		this.challenged = challenged;
		this.challenger = challenger;
		this.actionChallenged = actionChallenged;
	}
	
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, Action actionChallenged, int revealedDefendingCardIndex) {
		super(interruptId);
		this.challenged = challenged;
		this.challenger = challenger;
		this.actionChallenged = actionChallenged;
		this.revealedDefendingCardIndex = revealedDefendingCardIndex;
	}

	public String getChallenged() {
		return challenged;
	}
	
	public String getChallenger() {
		return challenger;
	}
	
	public Action getActionChallenged() {
		return actionChallenged;
	}
	
	public int getRevealedDefendingCardIndex() {
		return revealedDefendingCardIndex;
	}
}
