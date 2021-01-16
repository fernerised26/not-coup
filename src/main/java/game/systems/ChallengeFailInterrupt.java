package game.systems;

public class ChallengeFailInterrupt extends Interrupt {
	
	private final String failedChallenger;
	private final Action actionChallenged;

	public ChallengeFailInterrupt(String interruptId, String failedChallenger, Action actionChallenged, String triggerPlayer) {
		super(interruptId, triggerPlayer);
		this.failedChallenger = failedChallenger;
		this.actionChallenged = actionChallenged;
	}
	
	public String getFailedChallenger() {
		return failedChallenger;
	}
	
	public Action getActionChallenged() {
		return actionChallenged;
	}
}
