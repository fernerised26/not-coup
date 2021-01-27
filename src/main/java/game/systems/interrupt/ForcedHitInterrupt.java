package game.systems.interrupt;

import java.util.ArrayList;
import java.util.List;

public class ForcedHitInterrupt extends HitInterrupt{
	
	private static final List<String> EMPTY_LIST = new ArrayList<>();

	/**
	 * @param focused Inclusionary, only focused player is allowed to respond.
	 */
	public ForcedHitInterrupt(String interruptId, String forced, String originalHitOrderer, InterruptCase interruptCase) {
		super(interruptId, originalHitOrderer, forced, interruptCase, EMPTY_LIST);
	}
}
