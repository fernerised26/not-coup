package game.systems.web;

import org.json.simple.JSONObject;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import game.systems.Tabletop;

@RestController
public class CoupController {
	
	private Tabletop table = new Tabletop();
	
	@SuppressWarnings("unchecked")
	@GetMapping(path = "/airlock", params = "candidateName")
	public String validateNames(String candidateName) {
		JSONObject rspObj = new JSONObject();
		if(table.isPlayerPresent(candidateName)) {
			rspObj.put("code", 1);
			rspObj.put("msg", "Name invalid, already taken: \""+candidateName+"\"");
		} else if(candidateName.length() > 20) {
			rspObj.put("code", 2);
			rspObj.put("msg", "Name too long, must be less than 20 characters");
		} else if(candidateName.length() < 1) {
			rspObj.put("code", 3);
			rspObj.put("msg", "Name too short, must be at least 1 characters");
		} else {
			rspObj.put("code", 0);
			rspObj.put("msg", table.addPlayer(candidateName));
		}
		return rspObj.toJSONString();
	}
	
	@GetMapping(path = "/lobby")
	public int lobby() {
		return 0;
	}

	@MessageMapping("/lobbyjoin")
	@SendTo("/topic/lobbyevents")
	public String handlePlayerJoin(String newPlayerName) {
		if(newPlayerName.length() < 20) {
			return table.addPlayer(newPlayerName);
		} else {
			return "SKIP";
		}
	}
	
	@MessageMapping("/lobbyleave")
	@SendTo("/topic/lobbyevents")
	public String handlePlayerLeave(String playerName) {
		if(table.isPlayerPresent(playerName)) {
			return table.removePlayer(playerName);
		} else {
			return "SKIP";
		}
	}
}
