package game.systems;

public class CrowdfundCounterInterrupt extends Interrupt {
	

	public CrowdfundCounterInterrupt(String interruptId, long duration, String counterer, InterruptCase interruptCase) {
		super(interruptId, duration, interruptCase, counterer);
	}
	
	public String getCounterer() {
		return getTriggerPlayer();
	}
}
