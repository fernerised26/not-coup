package game.systems.interrupt;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

public abstract class Interrupt {

	private final String interruptId;
	private final Instant startTime;
	private final long duration;
	
	/**
	 * Focused player is tracked to prevent someone from responding to their own action
	 * or someone other than a select player from responding.
	 * Concrete implementations will have one of the 2 cases above applied. 
	 */
	private final String focused;
	
	private boolean active = true;
	private Set<String> responderNames;
	private InterruptCase interruptCase;
	private Future defaultResolverFuture;
	
	//Used for interrupts that can be followed up by a challenge
	public Interrupt(String interruptId, long duration, InterruptCase interruptCase, String focused) {
		super();
		this.interruptId = interruptId;
		this.startTime = Instant.now();
		this.responderNames = new HashSet<>();
		this.duration = duration;
		this.interruptCase = interruptCase;
		this.focused = focused;
	}
	
	//For interrupts that can only be countered
	public Interrupt(String interruptId, long duration, String focused) {
		super();
		this.interruptId = interruptId;
		this.startTime = Instant.now();
		this.responderNames = new HashSet<>();
		this.duration = duration;
		this.focused = focused;
	}
	
	//For interrupts that must wait indefinitely for game to proceed
	public Interrupt(String interruptId, String focused, InterruptCase interruptCase) {
		super();
		this.interruptId = interruptId;
		this.startTime = Instant.now();
		this.responderNames = new HashSet<>();
		this.duration = -1L;
		this.focused = focused;
		this.interruptCase = interruptCase;
	}

	public String getInterruptId() {
		return interruptId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Set<String> getResponderNames() {
		return responderNames;
	}

	public void addResponder(String responderName) {
		responderNames.add(responderName);
	}
	
	public int getNumberOfResponders() {
		return responderNames.size();
	}
	
	public long getDuration() {
		return duration;
	}
	
	public Instant getStartTime() { 
		return startTime;
	}
	
	public InterruptCase getInterruptCase() {
		return interruptCase;
	}

	public Future getDefaultResolverFuture() {
		return defaultResolverFuture;
	}

	public void setDefaultResolverFuture(Future defaultResolverFuture) {
		this.defaultResolverFuture = defaultResolverFuture;
	}

	public String getFocused() {
		return focused;
	}
}
