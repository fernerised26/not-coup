package game.systems.interrupt;

import java.util.List;

public class RaidInterrupt extends Interrupt{
	
	private final String target;

	/**
	 * @param raider Exclusionary, only focused player is not allowed to respond.
	 */
	public RaidInterrupt(String interruptId, String raider, String target, InterruptCase interruptCase, List<String> eliminatedPlayerNames) {
		super(interruptId, raider, interruptCase, eliminatedPlayerNames);
		this.target = target;
	}
	
	/**
	 * @param raider Exclusionary, only focused player is not allowed to respond.
	 */
	public RaidInterrupt(String interruptId, long duration, String raider, String target, InterruptCase interruptCase, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, interruptCase, raider, eliminatedPlayerNames);
		this.target = target;
	}

	public String getTarget() {
		return target;
	}
	
	public String getRaider() {
		return getFocused();
	}

}
