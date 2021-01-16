package game.systems;

public class InterruptDefaultResolver implements Runnable{

	private Interrupt interrupt;
	private long delayMs;
	private Tabletop table;
	private Action defaultableAction;
	
	public InterruptDefaultResolver(Interrupt interrupt, long delayMs, Tabletop table, Action defaultableAction) {
		this.interrupt = interrupt;
		this.delayMs = delayMs;
		this.table = table;
		this.defaultableAction = defaultableAction;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(delayMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		switch(defaultableAction) {
			case CROWDFUND:
				table.resolveCrowdfund(interrupt.getInterruptId(), true);
				break;
			case CROWDFUND_COUNTER:
				table.resolveCrowdfund(interrupt.getInterruptId(), false);
				break;
		}
	}
}
