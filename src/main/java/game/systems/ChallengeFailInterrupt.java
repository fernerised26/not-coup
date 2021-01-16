package game.systems;

public class ChallengeFailInterrupt extends Interrupt {
	
	private final String failedChallenger;

	public ChallengeFailInterrupt(String interruptId, String failedChallenger, InterruptCase actionChallenged, String triggerPlayer) {
		super(interruptId, triggerPlayer, actionChallenged);
		this.failedChallenger = failedChallenger;
	}
	
	public String getFailedChallenger() {
		return failedChallenger;
	}
	
	public InterruptCase getActionChallenged() {
		return getInterruptCase();
	}
}
