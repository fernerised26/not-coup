package game.systems.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TableController {
	
	private static final Map<String, Object> PLAYER_CHANGE_HEADER = new HashMap<>();
	private static final Map<String, Object> ROUND_START_FAIL_ATTEMPT = new HashMap<>();
	private static final Map<String, Object> UNAUTHORIZED_ACTIVITY_DETECTED = new HashMap<>();
//	private static final Map<String, Object> ACTIVE_PLAYER_UPDATE = new HashMap<>();
//	private static final Map<String, Object> PLAYER_ORDER = new HashMap<>();
	static {
		PLAYER_CHANGE_HEADER.put("case", "playerchange");
		ROUND_START_FAIL_ATTEMPT.put("case", "roundstart");
		UNAUTHORIZED_ACTIVITY_DETECTED.put("case", "unauthorized");
//		ACTIVE_PLAYER_UPDATE.put("case", "activeplayer");
//		PLAYER_ORDER.put("case", "playerorder");
	}

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	public void notifyTableOfPlayerChange(String playerListHtml) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", playerListHtml, PLAYER_CHANGE_HEADER);
	}
	
	public void notifyTableOfRoundStartAttempt(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, ROUND_START_FAIL_ATTEMPT);
	}
	
	public void notifyTableOfUnauthorizedActivity(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, UNAUTHORIZED_ACTIVITY_DETECTED);
	}
	
//	public void notifyTableOfCurrentActivePlayer(String message) {
//		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, ACTIVE_PLAYER_UPDATE);
//	}
	
//	public void notifyTableOfPlayerOrder(String message) {
//		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, PLAYER_ORDER);
//	}
}
