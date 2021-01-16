package game.systems.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TableController {
	
	private static final Map<String, Object> PLAYER_CHANGE_HEADER = new HashMap<>();
	private static final Map<String, Object> FAIL = new HashMap<>();
	private static final Map<String, Object> UNAUTHORIZED_ACTIVITY_DETECTED = new HashMap<>();
	private static final Map<String, Object> GROUP_COUNTER_OPP = new HashMap<>();
	private static final Map<String, Object> CHALLENGE_OPP = new HashMap<>();
	private static final Map<String, Object> CHALLENGE = new HashMap<>();
	private static final Map<String, Object> CHALLENGE_LOSS = new HashMap<>();
	private static final Map<String, Object> ROUND_END = new HashMap<>();
	private static final Map<String, Object> SIMPLE_MSG = new HashMap<>();
//	private static final Map<String, Object> ACTIVE_PLAYER_UPDATE = new HashMap<>();
//	private static final Map<String, Object> PLAYER_ORDER = new HashMap<>();
	static {
		PLAYER_CHANGE_HEADER.put("case", "playerchange");
		FAIL.put("case", "fail");
		UNAUTHORIZED_ACTIVITY_DETECTED.put("case", "unauthorized");
		GROUP_COUNTER_OPP.put("case", "groupcounteropp");
		CHALLENGE_OPP.put("case", "challengeopp");
		CHALLENGE.put("case", "challenge");
		CHALLENGE_LOSS.put("case", "challengeloss");
		ROUND_END.put("case", "roundend");
		SIMPLE_MSG.put("case", "simpmsg");
//		ACTIVE_PLAYER_UPDATE.put("case", "activeplayer");
//		PLAYER_ORDER.put("case", "playerorder");
	}

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	public void notifyTableOfPlayerChange(String playerListHtml) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", playerListHtml, PLAYER_CHANGE_HEADER);
	}
	
	public void notifyTableOfError(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, FAIL);
	}
	
	public void notifyTableOfUnauthorizedActivity(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, UNAUTHORIZED_ACTIVITY_DETECTED);
	}
	
	public void notifyTableOfGroupCounterOpp(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, GROUP_COUNTER_OPP);
	}

	public void notifyTableOfChallengeOpp(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, CHALLENGE_OPP);
	}
	
	public void notifyTableOfChallenge(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, CHALLENGE);
	}
	
	public void notifyTableOfChallengeLoss(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, CHALLENGE_LOSS);
	}
	
	public void notifyTableOfWinner(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, ROUND_END);
	}
	
	public void notifyTableWithSimpleMessage(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, SIMPLE_MSG);
	}
	
//	public void notifyTableOfCurrentActivePlayer(String message) {
//		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, ACTIVE_PLAYER_UPDATE);
//	}
	
//	public void notifyTableOfPlayerOrder(String message) {
//		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, PLAYER_ORDER);
//	}
}
