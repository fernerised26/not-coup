package game.systems.interrupt;

import java.util.List;

import game.pieces.Roles;

public class BlockInterrupt extends Interrupt {

	private final String raider;
	private final Roles blockingRole;
	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public BlockInterrupt(String interruptId, long duration, InterruptCase interruptCase, String focused, String raider, Roles blockingRole, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, interruptCase, focused, eliminatedPlayerNames);
		this.raider = raider;
		this.blockingRole = blockingRole;
	}
	
	public String getRaider() {
		return raider;
	}

	public String getBlocker() {
		return getFocused();
	}
	
	public Roles getBlockingRole() {
		return blockingRole;
	}
}
