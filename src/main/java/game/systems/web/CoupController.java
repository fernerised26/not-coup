package game.systems.web;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import game.systems.Tabletop;

@RestController
public class CoupController {
	
	public static String SKIP = "SKIP";
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
		if(newPlayerName.length() < 20 && !table.roundActive) {
			return table.addPlayer(newPlayerName);
		} else {
			return SKIP;
		}
	}
	
	@MessageMapping("/lobbyleave")
	@SendTo("/topic/lobbyevents")
	public String handlePlayerLeave(String playerName) {
		if(table.isPlayerPresent(playerName)) {
			return table.removePlayer(playerName);
		} else {
			return SKIP;
		}
	}
	
	@SuppressWarnings("unchecked")
	@MessageMapping("/roundstart")
	@SendTo("/topic/lobbyevents")
	public String startRound() {
		JSONObject rspObj = new JSONObject();
		
		if(table.playerMap.size() < 2) {
			rspObj.put("code", 4);
			rspObj.put("msg", "Cannot start, at least 2 players required");
		} else {
			try {
				table.startRound();
				rspObj.put("code", 200);
				rspObj.put("msg", "Round started successfully server-side");
			} catch (IOException e) {
				rspObj.put("code", 5);
				rspObj.put("msg", "Invalid player role");
				return rspObj.toJSONString();
			}
		}
		return rspObj.toJSONString();
	}
	
	
}
