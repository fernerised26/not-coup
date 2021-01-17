package game.systems;

public class VoidoutInterrupt extends Interrupt{
	
	private final String voidedPlayer;

	public VoidoutInterrupt(String interruptId, String triggerPlayer, String voidedPlayer, InterruptCase interruptCase) {
		super(interruptId, triggerPlayer, interruptCase);
		this.voidedPlayer = voidedPlayer;
	}

	public String getVoidedPlayer() {
		return voidedPlayer;
	}

}
