package game.systems.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {
	
	private static final Map<String, Object> UPDATE_HEADER = new HashMap<>();
	private static final Map<String, Object> TARGETS_HEADER = new HashMap<>();
	private static final Map<String, Object> ADVANCE_HEADER = new HashMap<>();
	private static final Map<String, Object> SCRAMBLE_SELECT = new HashMap<>();
	static {
		UPDATE_HEADER.put("case", "update");
		TARGETS_HEADER.put("case", "targets");
		ADVANCE_HEADER.put("case", "advance");
		SCRAMBLE_SELECT.put("case", "scrambleselect");
	}

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	public void contactPlayerInitTable(String playerName, String orderHeaderText, String text) {
		Map<String, Object> initHeader = new HashMap<>();
		initHeader.put("case", "init");
		initHeader.put("order", orderHeaderText);
		
		this.msgTemplate.convertAndSend("/queue/"+playerName, text, initHeader);
	}
	
	public void contactPlayerUpdateTable(String playerName, String text) {
		this.msgTemplate.convertAndSend("/queue/"+playerName, text, UPDATE_HEADER);
	}
	
	public void contactPlayerAdvanceTable(String playerName, String text) {
		this.msgTemplate.convertAndSend("/queue/"+playerName, text, ADVANCE_HEADER);
	}
	
	public void contactPlayerValidTargets(String playerName, String text) {
		this.msgTemplate.convertAndSend("/queue/"+playerName, text, TARGETS_HEADER);
	}
	
	public void contactPlayerScrambleSelect(String playerName, String text) {
		this.msgTemplate.convertAndSend("/queue/"+playerName, text, SCRAMBLE_SELECT);
	}
}
