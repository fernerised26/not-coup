package game.systems;

public class CrowdfundCounterInterrupt extends Interrupt {
	
	private final String counterer; 

	public CrowdfundCounterInterrupt(String interruptId, long duration, String counterer, InterruptCase interruptCase) {
		super(interruptId, duration, interruptCase);
		this.counterer = counterer;
	}
	
	public String getCounterer() {
		return counterer;
	}
}
