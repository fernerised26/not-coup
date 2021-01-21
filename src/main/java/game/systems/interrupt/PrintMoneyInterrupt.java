package game.systems.interrupt;

public class PrintMoneyInterrupt extends Interrupt {
	
	/**
	 * @param focused Exclusionary, only focused player is not allowed to respond.
	 */
	public PrintMoneyInterrupt(String interruptId, long duration, InterruptCase interruptCase, String focused) {
		super(interruptId, duration, interruptCase, focused);
	}

}
