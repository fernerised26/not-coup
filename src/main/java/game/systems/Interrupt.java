package game.systems;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public abstract class Interrupt {

	private final String interruptId;
	private final Instant startTime;
	private final long duration;
	
	private boolean active = true;
	private Set<String> responderNames;
	private InterruptCase interruptCase;
	
	//Used for interrupts that can be followed up by a challenge
	public Interrupt(String interruptId, long duration, InterruptCase interruptCase) {
		super();
		this.interruptId = interruptId;
		this.startTime = Instant.now();
		this.responderNames = new HashSet<>();
		this.duration = duration;
		this.interruptCase = interruptCase;
	}
	
	//For interrupts that can only be countered
	public Interrupt(String interruptId, long duration) {
		super();
		this.interruptId = interruptId;
		this.startTime = Instant.now();
		this.responderNames = new HashSet<>();
		this.duration = duration;
	}
	
	//For challenge interrupts (must wait indefinitely for game to proceed)
	public Interrupt(String interruptId) {
		super();
		this.interruptId = interruptId;
		this.startTime = Instant.now();
		this.responderNames = new HashSet<>();
		this.duration = -1L;
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
	
	public long getDuration() {
		return duration;
	}
	
	public Instant getStartTime() { 
		return startTime;
	}
	
	public InterruptCase getInterruptCase() {
		return interruptCase;
	}
}
