package game.systems.web;

import java.awt.Desktop.Action;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
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
		JSONObject rspObj = new JSONObject();
		if (table.isRoundActive()) {
			rspObj.put("code", 4);
			rspObj.put("msg", "Round already started, cannot join");
		} else if (table.isPlayerPresent(candidateName)) {
			rspObj.put("code", 1);
			rspObj.put("msg", "Name invalid, already taken: \"" + candidateName + "\"");
		} else if (candidateName.length() > 20) {
			rspObj.put("code", 2);
			rspObj.put("msg", "Name too long, must be less than 20 characters");
		} else if (candidateName.isBlank() || candidateName.length() < 3) {
			rspObj.put("code", 3);
			rspObj.put("msg", "Name too short, must be at least 3 characters");
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
			if (currentPlayerHtml != null) {
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

	// TODO Probably not necessary until rejoin logic is implemented
	// @MessageMapping("/lobbyjoin")
	// public void handlePlayerJoin(String newPlayerName) {
	// if(newPlayerName.length() < 20 && !table.isRoundActive()) {
	// table.addPlayer(newPlayerName);
	// }
	// }

	@MessageMapping("/lobbyleave")
	public void handlePlayerLeave(String playerName) {
		if (table.isPlayerPresent(playerName)) {
			table.removePlayer(playerName);
		}
	}

	@SuppressWarnings("unchecked")
	@MessageMapping("/roundstart")
	public void startRound() {
		JSONObject rspObj = new JSONObject();

		if (table.getPlayerMap().size() < 2) {
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
				tableController.notifyTableOfError(rspObj.toJSONString());
			}
		}
		tableController.notifyTableOfError(rspObj.toJSONString());
	}

	@MessageMapping("/payday")
	public void handlePayday(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayer(headers, "payday", body);
		if (authTuple.isWellFormed) {
			table.handlePayday();
		}
	}

	@MessageMapping("/crowdfund")
	public void handleCrowdfund(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayer(headers, "crowdfund", body);
		if (authTuple.isWellFormed) {
			table.handleCrowdfund();
		}
	}

	@MessageMapping("/crowdfundcounter")
	public void handleCrowdfundCounter(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "crowdfundcounter", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForInterruptingPlayer(headers, "crowdfundcounter", interruptId, body);
				if (authTuple.isWellFormed) {
					String interrupter = authTuple.playerName;
					if (!interruptId.isBlank() && !interrupter.isBlank()) {
						table.handleInterruptCrowdfundCounter(interruptId, interrupter);
					} else {
						createAndDistributeErrorMsg("crowdfundcounter|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("crowdfundcounter|EmptyBody", headers, body);
		}
	}

	@MessageMapping("/challenge")
	public void handleChallenge(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "challenge", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForInterruptingPlayer(headers, "challenge", interruptId, body);
				if (authTuple.isWellFormed) {
					String interrupter = authTuple.playerName;
					if (!interruptId.isBlank() && !interrupter.isBlank()) {
						table.handleChallenge(interruptId, interrupter);
					} else {
						createAndDistributeErrorMsg("challenge|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("challenge|EmptyBody", headers, body);
		}
	}

	@MessageMapping("/challengeresponse1")
	public void handleChallengeResponse(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "challengeResponse", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForSingleResponder(headers, "challengeresponse1", interruptId, body);
				if (authTuple.isWellFormed) {
					Integer cardIndex = Integer.valueOf((int) ((long) parsedJson.get("cardIndex")));
					if (!interruptId.isBlank() && cardIndex != null) {
						table.handleInterruptChallengeResponse(interruptId, cardIndex.intValue());
					} else {
						createAndDistributeErrorMsg("challengeResponse|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("challengeResponse|EmptyBody", headers, body);
		}
	}

	@MessageMapping("/challengeresponse2")
	public void handleSuccessfulChallengeDefense(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "successfulChallengeDefense", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForSingleResponder(headers, "challengeresponse2", interruptId, body);
				if (authTuple.isWellFormed) {
					Integer cardIndex = Integer.valueOf((int) ((long) parsedJson.get("cardIndex")));
					if (!interruptId.isBlank() && cardIndex != null) {
						table.handleInterruptSuccessfulChallengeDefense(interruptId, cardIndex.intValue());
					} else {
						createAndDistributeErrorMsg("successfulChallengeDefense|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("successfulChallengeDefense|EmptyBody", headers, body);
		}
	}

	@MessageMapping("/skip")
	public void handleSkip(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "skip", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForInterruptingPlayer(headers, "skip", interruptId, body);
				if (authTuple.isWellFormed) {
					String skipper = authTuple.playerName;
					if (!interruptId.isBlank() && !skipper.isBlank()) {
						table.handleSkip(interruptId, skipper);
					} else {
						createAndDistributeErrorMsg("skip|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("skip|EmptyBody", headers, body);
		}
	}

	@MessageMapping("/voidtargets")
	public void handleGetVoidTargets(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayerOnVoid(headers, "voidtargets", body);
		if (authTuple.isWellFormed) {
			table.handleGetTargets("void out", "voidout");
		}
	}

	@MessageMapping("/voidout")
	public void handleVoidout(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayerOnVoid(headers, "voidout", body);
		if (authTuple.isWellFormed) {
			if (body != null && !body.isBlank()) {
				JSONObject parsedJson = handleJsonInput(body, "voidout", headers);
				if (parsedJson != null) {
					String voidTarget = (String) parsedJson.get("target");
					table.handleVoidout(voidTarget);
				}
			}
		}
	}

	@MessageMapping("/voidoutresponse")
	public void handleVoidoutResponse(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "voidoutresponse", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForSingleResponder(headers, "voidoutresponse", interruptId, body);
				if (authTuple.isWellFormed) {
					Integer cardIndex = Integer.valueOf((int) ((long) parsedJson.get("cardIndex")));
					if (!interruptId.isBlank() && cardIndex != null) {
						table.handleVoidoutResponse(interruptId, cardIndex);
					} else {
						createAndDistributeErrorMsg("voidoutresponse|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("voidoutresponse|EmptyBody", headers, body);
		}
	}

	@MessageMapping("/printmoney")
	public void handlePrintMoney(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayer(headers, "printmoney", body);
		if (authTuple.isWellFormed) {
			table.handlePrintMoney();
		}
	}

	@MessageMapping("/hittargets")
	public void handleGetHitTargets(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayer(headers, "hittargets", body);
		if (authTuple.isWellFormed) {
			table.handleGetTargets("order a hit on", "orderhit");
		}
	}

	@MessageMapping("/orderhit")
	public void handleOrderHit(@Payload String body, MessageHeaders headers) {
		AuthTuple authTuple = isMessageWellFormedForActivePlayer(headers, "orderhit", body);
		if (authTuple.isWellFormed) {
			if (body != null && !body.isBlank()) {
				JSONObject parsedJson = handleJsonInput(body, "orderhit", headers);
				if (parsedJson != null) {
					String hitTarget = (String) parsedJson.get("target");
					table.handleOrderHit(hitTarget);
				}
			}
		}
	}

	@MessageMapping("/orderhitresponse")
	public void handleOrderHitResponse(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "orderhitresponse", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForHitOrderResponder(headers, "orderhitresponse", interruptId, body);
				if (authTuple.isWellFormed) {
					Integer cardIndex = Integer.valueOf((int) ((long) parsedJson.get("cardIndex")));
					if (!interruptId.isBlank() && cardIndex != null) {
						table.handleOrderHitResponse(interruptId, cardIndex);
					} else {
						createAndDistributeErrorMsg("orderhitresponse|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("orderhitresponse|EmptyBody", headers, body);
		}
	}
	
	@MessageMapping("/acceptdefeat")
	public void handleAcceptDefeat(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "acceptdefeat", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForSingleResponder(headers, "acceptdefeat", interruptId, body);
				if (authTuple.isWellFormed) {
					table.handleAcceptDefeat(interruptId);
				}
			}
		} else {
			createAndDistributeErrorMsg("acceptdefeat|EmptyBody", headers, body);
		}
	}
	
	@MessageMapping("/forcedhitresponse")
	public void handleForcedHitResponse(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "forcedhitresponse", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForSingleResponder(headers, "forcedhitresponse", interruptId, body);
				if (authTuple.isWellFormed) {
					Integer cardIndex = Integer.valueOf((int) ((long) parsedJson.get("cardIndex")));
					if (!interruptId.isBlank() && cardIndex != null) {
						table.handleOrderHitResponse(interruptId, cardIndex);
					} else {
						createAndDistributeErrorMsg("forcedhitresponse|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("forcedhitresponse|EmptyBody", headers, body);
		}
	}
	
	@MessageMapping("/hitcounter")
	public void handleHitCounter(@Payload String body, MessageHeaders headers) {
		if (body != null && !body.isBlank()) {
			JSONObject parsedJson = handleJsonInput(body, "hitcounter", headers);
			if (parsedJson != null) {
				String interruptId = (String) parsedJson.get("interruptId");
				AuthTuple authTuple = isMessageWellFormedForHitOrderResponder(headers, "hitcounter", interruptId, body);
				if (authTuple.isWellFormed) {
					if (!interruptId.isBlank()) {
						table.handleHitCounter(interruptId);
					} else {
						createAndDistributeErrorMsg("hitcounter|MissingInterruptData", headers, body);
					}
				}
			}
		} else {
			createAndDistributeErrorMsg("hitcounter|EmptyBody", headers, body);
		}
	}

	private AuthTuple isMessageWellFormed(MessageHeaders headers, String action, String body) {
		List<String> secretHeader = (List<String>) ((Map<String, Object>) headers.get("nativeHeaders")).get("secret");
		List<String> pNameHeader = (List<String>) ((Map<String, Object>) headers.get("nativeHeaders")).get("pname");
		if (secretHeader == null || secretHeader.isEmpty() || pNameHeader.isEmpty()) {
			createAndDistributeErrorMsg(action + "|EmptySecret", headers, body);
			return new AuthTuple(null, null, false);
		}
		String secret = secretHeader.get(0);
		String playerName = pNameHeader.get(0);
		if (!table.isSecretCorrect(secret, playerName)) {
			createAndDistributeErrorMsg(action + "|WrongSecret", headers, body);
			return new AuthTuple(secret, playerName, false);
		}
		return new AuthTuple(secret, playerName, true);
	}

	private AuthTuple isMessageWellFormedForActivePlayer(MessageHeaders headers, String action, String body) {
		AuthTuple stage1AuthResult = isMessageWellFormed(headers, action, body);
		if (stage1AuthResult.isWellFormed) {
			boolean isActive = table.isActivePlayer(stage1AuthResult.secret, stage1AuthResult.playerName);
			if (!isActive) {
				createAndDistributeErrorMsg(action + "|ActivePlayerExpected", headers, body);
			}
			boolean isVoidLocked = table.isPlayerVoidLocked(stage1AuthResult.playerName);
			if (isVoidLocked) {
				createAndDistributeErrorMsg(action + "|PlayerIsVoidLocked", headers, body);
			}
			return new AuthTuple(stage1AuthResult, (isActive && !isVoidLocked));
		} else {
			return stage1AuthResult;
		}
	}

	private AuthTuple isMessageWellFormedForInterruptingPlayer(MessageHeaders headers, String action, String interruptId, String body) {
		AuthTuple stage1AuthResult = isMessageWellFormed(headers, action, body);
		if (stage1AuthResult.isWellFormed) {
			if (interruptId == null || interruptId.isBlank()) {
				return new AuthTuple(stage1AuthResult, false);
			}
			boolean isValid = table.isValidResponder(interruptId, stage1AuthResult.playerName);
			if (!isValid) {
				createAndDistributeErrorMsg(action + "|SelfRespondingPlayers", headers, body);
			}
			return new AuthTuple(stage1AuthResult, isValid);
		} else {
			return stage1AuthResult;
		}
	}

	private AuthTuple isMessageWellFormedForSingleResponder(MessageHeaders headers, String action, String interruptId, String body) {
		AuthTuple stage1AuthResult = isMessageWellFormed(headers, action, body);
		if (stage1AuthResult.isWellFormed) {
			if (interruptId == null || interruptId.isBlank()) {
				return new AuthTuple(stage1AuthResult, false);
			}
			boolean isValid = table.isExpectedResponder(interruptId, stage1AuthResult.playerName);
			if (!isValid) {
				createAndDistributeErrorMsg(action + "|SelfRespondingPlayers", headers, body);
			}
			return new AuthTuple(stage1AuthResult, isValid);
		} else {
			return stage1AuthResult;
		}
	}

	private AuthTuple isMessageWellFormedForActivePlayerOnVoid(MessageHeaders headers, String action, String body) {
		AuthTuple stage1AuthResult = isMessageWellFormed(headers, action, body);
		if (stage1AuthResult.isWellFormed) {
			boolean isActive = table.isActivePlayer(stage1AuthResult.secret, stage1AuthResult.playerName);
			if (!isActive) {
				createAndDistributeErrorMsg(action + "|ActivePlayerExpected", headers, body);
			}
			return new AuthTuple(stage1AuthResult, (isActive));
		} else {
			return stage1AuthResult;
		}
	}
	
	private AuthTuple isMessageWellFormedForHitOrderResponder(MessageHeaders headers, String action, String interruptId, String body) {
		AuthTuple stage1AuthResult = isMessageWellFormed(headers, action, body);
		if (stage1AuthResult.isWellFormed) {
			if (interruptId == null || interruptId.isBlank()) {
				return new AuthTuple(stage1AuthResult, false);
			}
			boolean isValid = table.isExpectedHitResponder(interruptId, stage1AuthResult.playerName);
			if (!isValid) {
				createAndDistributeErrorMsg(action + "|TargetedPlayerExpected", headers, body);
			}
			return new AuthTuple(stage1AuthResult, isValid);
		} else {
			return stage1AuthResult;
		}
	}

	private void createAndDistributeErrorMsg(String errorKey, MessageHeaders headerMap, String body) {
		String errMsg = constructUnauthorizedMessage(errorKey, headerMap, body);
		System.err.println(errMsg);
		tableController.notifyTableOfUnauthorizedActivity(errMsg);
	}

	private String constructUnauthorizedMessage(String action, MessageHeaders headerMap, String body) {
		StringBuilder sb = new StringBuilder();
		sb.append("Received unauthorized message for action - ").append(action).append(", headers: ").append(headerMap.toString()).append(", body: ").append(body);
		return sb.toString();
	}

	private JSONObject handleJsonInput(String input, String action, MessageHeaders headerMap) {
		try {
			JSONParser parser = new JSONParser();
			JSONObject parsedJson = (JSONObject) parser.parse(input);
			return parsedJson;
		} catch (ParseException e) {
			e.printStackTrace();
			createAndDistributeErrorMsg(action + "|UnparseableBody", headerMap, input);
			return null;
		}
	}

	private class AuthTuple {
		private final String secret;
		private final String playerName;
		private final boolean isWellFormed;

		private AuthTuple(String secret, String playerName, boolean isWellFormed) {
			this.secret = secret;
			this.playerName = playerName;
			this.isWellFormed = isWellFormed;
		}

		private AuthTuple(AuthTuple oldTuple, boolean newIsWellFormed) {
			this.secret = oldTuple.secret;
			this.playerName = oldTuple.playerName;
			this.isWellFormed = newIsWellFormed;
		}
	}
}
