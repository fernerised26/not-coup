package game.systems.web;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import game.systems.Tabletop;

@Controller
public class CoupController {
	
	private Tabletop table = new Tabletop();

	@MessageMapping("/lobby")
	@SendTo("/topic/lobbyevents")
	public String handlePlaceholder(String newPlayerName) {
		if(newPlayerName.length() < 20) {
			String escapedName = HtmlUtils.htmlEscape(newPlayerName);
			table.addPlayer(escapedName);
			return "0"; 
		} else {
			return "1";
		}
	}
	
}
