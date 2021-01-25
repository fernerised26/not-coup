package game.systems.interrupt;

import java.util.List;

public class ScrambleInterrupt extends Interrupt{

	/**
	 * @param focused Exclusionary when checking for challenges, Inclusionary when receiving selection responses 
	 */
	public ScrambleInterrupt(String interruptId, long duration, InterruptCase interruptCase, String focused, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, interruptCase, focused, eliminatedPlayerNames);
	}

	public ScrambleInterrupt(String interruptId, String focused, InterruptCase interruptCase, List<String> eliminatedPlayerNames) {
		super(interruptId, focused, interruptCase, eliminatedPlayerNames);
	}
	
}
