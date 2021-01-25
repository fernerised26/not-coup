package game.systems.interrupt;

import java.util.List;

public class CrowdfundCounterInterrupt extends Interrupt {
	
	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public CrowdfundCounterInterrupt(String interruptId, long duration, String counterer, InterruptCase interruptCase, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, interruptCase, counterer, eliminatedPlayerNames);
	}
	
	public String getCounterer() {
		return getFocused();
	}
}
