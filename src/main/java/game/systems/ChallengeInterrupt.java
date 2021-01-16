package game.systems;

public class ChallengeInterrupt extends Interrupt {
	
	private final String challenged;
	private final String challenger;
	private int revealedDefendingCardIndex = -1;

	public ChallengeInterrupt(String interruptId, String challenged, String challenger, String triggerPlayer, InterruptCase actionChallenged) {
		super(interruptId, triggerPlayer, actionChallenged);
		this.challenged = challenged;
		this.challenger = challenger;
	}
	
	public ChallengeInterrupt(String interruptId, String challenged, String challenger, String triggerPlayer, InterruptCase actionChallenged, int revealedDefendingCardIndex) {
		super(interruptId, triggerPlayer, actionChallenged);
		this.challenged = challenged;
		this.challenger = challenger;
		this.revealedDefendingCardIndex = revealedDefendingCardIndex;
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
}
