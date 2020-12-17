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
	static {
		PLAYER_CHANGE_HEADER.put("case", "playerchange");
		ROUND_START_FAIL_ATTEMPT.put("case", "roundstart");
	}

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	public void notifyTableOfPlayerChange(String playerListHtml) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", playerListHtml, PLAYER_CHANGE_HEADER);
	}
	
	public void notifyTableOfRoundStartAttempt(String message) {
		this.msgTemplate.convertAndSend("/topic/lobbyevents", message, ROUND_START_FAIL_ATTEMPT);
	}
}
