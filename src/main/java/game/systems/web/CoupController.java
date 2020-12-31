package game.systems.web;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import game.systems.Tabletop;

@RestController
public class CoupController {
	
	public static String SKIP = "SKIP";
	
	@Autowired
	private Tabletop table;
	
	@Autowired
	private TableController tableController;
	
	@SuppressWarnings("unchecked")
	@GetMapping(path = "/airlock", params = "candidateName")
	public String validateNames(String candidateName, HttpServletRequest rqst) {
		System.out.println(rqst == null);
		System.out.println(rqst.getUserPrincipal());
		JSONObject rspObj = new JSONObject();
		if(table.isRoundActive()) {
			rspObj.put("code", 4);
			rspObj.put("msg", "Round already started, cannot join");
		} else if(table.isPlayerPresent(candidateName)) {
			rspObj.put("code", 1);
			rspObj.put("msg", "Name invalid, already taken: \""+candidateName+"\"");
		} else if(candidateName.length() > 20) {
			rspObj.put("code", 2);
			rspObj.put("msg", "Name too long, must be less than 20 characters");
		} else if(candidateName.length() < 1) {
			rspObj.put("code", 3);
			rspObj.put("msg", "Name too short, must be at least 1 characters");
		} else {
			String secret;
			try {
				secret = SecretUtil.generateSecret();
			} catch (NoSuchAlgorithmException e) {
				rspObj.put("code", 6);
				rspObj.put("msg", "Unable to generate secret");
				return rspObj.toJSONString();
			}
			
			String currentPlayerHtml = table.addPlayer(candidateName, secret);
			if(currentPlayerHtml != null) {
				rspObj.put("code", 0);
				rspObj.put("msg", currentPlayerHtml);
				rspObj.put("secret", secret);
			} else {
				rspObj.put("code", 4);
				rspObj.put("msg", "Round already started, cannot join");
			}
		}
		return rspObj.toJSONString();
	}
	
	@GetMapping(path = "/lobby")
	public int lobby() {
		return 0;
	}

	//TODO Probably not necessary until rejoin logic is implemented
//	@MessageMapping("/lobbyjoin")
//	public void handlePlayerJoin(String newPlayerName) {
//		if(newPlayerName.length() < 20 && !table.isRoundActive()) {
//			table.addPlayer(newPlayerName);
//		} 
//	}
	
	@MessageMapping("/lobbyleave")
	public void handlePlayerLeave(String playerName) {
		if(table.isPlayerPresent(playerName)) {
			table.removePlayer(playerName);
		}
	}
	
	@SuppressWarnings("unchecked")
	@MessageMapping("/roundstart")
	public void startRound() {
		JSONObject rspObj = new JSONObject();
		
		if(table.getPlayerMap().size() < 2) {
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
				tableController.notifyTableOfRoundStartAttempt(rspObj.toJSONString());
			}
		}
		tableController.notifyTableOfRoundStartAttempt(rspObj.toJSONString());
	}
	
	@MessageMapping("/gameaction")
	public void handlePlayerGameAction(
				@Payload String body,
				@Headers Map<String, String> headers,
				java.security.Principal principal
			) {
		System.out.println("Handling a game action");
		System.out.println("Body: " + body);
		System.out.println("Headers: " + headers.toString());
		System.out.println(principal.toString());
	}
}
