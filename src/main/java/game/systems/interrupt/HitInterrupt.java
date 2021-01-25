package game.systems.interrupt;

import java.util.List;

public class HitInterrupt extends Interrupt{
	
	private final String target;

	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public HitInterrupt(String interruptId, String hitOrderer, String target, InterruptCase interruptCase, List<String> eliminatedPlayerNames) {
		super(interruptId, hitOrderer, interruptCase, eliminatedPlayerNames);
		this.target = target;
	}

	public String getTarget() {
		return target;
	}
	
	public String getHitOrderer() {
		return getFocused();
	}

}
