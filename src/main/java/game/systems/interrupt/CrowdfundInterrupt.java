package game.systems.interrupt;

public class CrowdfundInterrupt extends Interrupt {

	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public CrowdfundInterrupt(String interruptId, long duration, String focused) {
		super(interruptId, duration, focused);
	}

}
