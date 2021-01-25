package game.systems.interrupt;

import java.util.List;

public class DecoyInterrupt extends Interrupt {

	private final String hitOrderer;
	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public DecoyInterrupt(String interruptId, long duration, InterruptCase interruptCase, String focused, String hitOrderer, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, interruptCase, focused, eliminatedPlayerNames);
		this.hitOrderer = hitOrderer;
	}
	
	public String getHitOrderer() {
		return hitOrderer;
	}

	public String getDecoyDeployer() {
		return getFocused();
	}
}
