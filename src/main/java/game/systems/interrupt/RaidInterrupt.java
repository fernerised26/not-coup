package game.systems.interrupt;

public class RaidInterrupt extends Interrupt{
	
	private final String target;

	/**
	 * @param raider Exclusionary, only focused player is not allowed to respond.
	 */
	public RaidInterrupt(String interruptId, String raider, String target, InterruptCase interruptCase) {
		super(interruptId, raider, interruptCase);
		this.target = target;
	}
	
	/**
	 * @param raider Exclusionary, only focused player is not allowed to respond.
	 */
	public RaidInterrupt(String interruptId, long duration, String raider, String target, InterruptCase interruptCase) {
		super(interruptId, duration, interruptCase, raider);
		this.target = target;
	}

	public String getTarget() {
		return target;
	}
	
	public String getRaider() {
		return getFocused();
	}

}
