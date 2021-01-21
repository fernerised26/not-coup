package game.systems.interrupt;

public class ScrambleInterrupt extends Interrupt{

	/**
	 * @param focused Exclusionary when checking for challenges, Inclusionary when receiving selection responses 
	 */
	public ScrambleInterrupt(String interruptId, long duration, InterruptCase interruptCase, String focused) {
		super(interruptId, duration, interruptCase, focused);
	}

	public ScrambleInterrupt(String interruptId, String focused, InterruptCase interruptCase) {
		super(interruptId, focused, interruptCase);
	}
	
}
