package game.systems.interrupt;

public class ForcedHitInterrupt extends Interrupt{
	
	private final String originalHitOrderer;

	/**
	 * @param focused Inclusionary, only focused player is allowed to respond.
	 */
	public ForcedHitInterrupt(String interruptId, String forced, String originalHitOrderer, InterruptCase interruptCase) {
		super(interruptId, forced, interruptCase);
		this.originalHitOrderer = originalHitOrderer;
	}

	public String getOriginalHitOrderer() {
		return originalHitOrderer;
	}

	public String getForced() {
		return getFocused();
	}
}
