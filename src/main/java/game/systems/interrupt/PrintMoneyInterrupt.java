package game.systems.interrupt;

import java.util.List;

public class PrintMoneyInterrupt extends Interrupt {
	
	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public PrintMoneyInterrupt(String interruptId, long duration, InterruptCase interruptCase, String focused, List<String> eliminatedPlayerNames) {
		super(interruptId, duration, interruptCase, focused, eliminatedPlayerNames);
	}

}
