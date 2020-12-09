package game.systems.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {

	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	public void contactPlayer(String playerName, String text) {
		this.msgTemplate.convertAndSend("/queue/"+playerName, text);
	}
}
