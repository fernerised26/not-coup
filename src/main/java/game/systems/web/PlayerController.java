package game.systems.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {
	
	private static final Map<String, Object> INIT_HEADER = new HashMap<>();
	static {
		INIT_HEADER.put("case", "init");
	}

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	public void contactPlayerInitTable(String playerName, String text) {
		this.msgTemplate.convertAndSend("/queue/"+playerName, text, INIT_HEADER);
	}
}
