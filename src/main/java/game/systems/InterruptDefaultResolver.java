package game.systems;

import game.systems.interrupt.InterruptCase;

public class InterruptDefaultResolver implements Runnable{

	private final String interruptId;
	private final long delayMs;
	private final Tabletop table;
	private final InterruptCase defaultableAction;
	
	public InterruptDefaultResolver(String interruptId, long delayMs, Tabletop table, InterruptCase defaultableAction) {
		this.interruptId = interruptId;
		this.delayMs = delayMs;
		this.table = table;
		this.defaultableAction = defaultableAction;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(delayMs);
		} catch (InterruptedException e) {
			System.out.println("Default Resolver woken for interruptId:" + interruptId);
		}
		switch(defaultableAction) {
			case CROWDFUND:
				table.resolveCrowdfund(interruptId, true);
				break;
			case CROWDFUND_COUNTER:
				table.resolveCrowdfund(interruptId, false);
				break;
			case PRINT_MONEY:
				table.resolvePrintMoney(interruptId);
				break;
			case ORDER_HIT_COUNTER:
				table.resolveHitCounter(interruptId);
				break;
			case SCRAMBLE_IDENTITY:
				table.resolveScrambleIdentity1(interruptId);
				break;
			case RAID:
				table.resolveRaid(interruptId);
				break;
			case RAID_COUNTER:
				table.resolveRaidCounter(interruptId);
				break;
			default:
				System.err.println("Invalid case, no valid default behavior: " + defaultableAction);
		}
	}
}
