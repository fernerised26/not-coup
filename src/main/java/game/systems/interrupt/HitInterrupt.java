package game.systems.interrupt;

public class HitInterrupt extends Interrupt{
	
	private final String target;

	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public HitInterrupt(String interruptId, String hitOrderer, String target, InterruptCase interruptCase) {
		super(interruptId, hitOrderer, interruptCase);
		this.target = target;
	}

	public String getTarget() {
		return target;
	}
	
	public String getHitOrderer() {
		return getFocused();
	}

}
