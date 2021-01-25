package game.systems.interrupt;

import java.util.List;

public class CrowdfundInterrupt extends Interrupt {

	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public CrowdfundInterrupt(String interruptId, long duration, String focused, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, focused, eliminatedPlayerNames);
	}

}
