package game.systems.interrupt;

public class VoidoutInterrupt extends Interrupt{
	
	private final String voider;

	/**
	 * @param focused Inclusionary, only focused player is allowed to respond.
	 */
	public VoidoutInterrupt(String interruptId, String voider, String focused, InterruptCase interruptCase) {
		super(interruptId, focused, interruptCase);
		this.voider = voider;
	}

	public String getVoider() {
		return voider;
	}

	public String getVoided() {
		return getFocused();
	}
}
