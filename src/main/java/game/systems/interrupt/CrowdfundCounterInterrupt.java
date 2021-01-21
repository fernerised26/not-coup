package game.systems.interrupt;

public class CrowdfundCounterInterrupt extends Interrupt {
	
	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public CrowdfundCounterInterrupt(String interruptId, long duration, String counterer, InterruptCase interruptCase) {
		super(interruptId, duration, interruptCase, counterer);
	}
	
	public String getCounterer() {
		return getFocused();
	}
}
