package game.systems;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import game.pieces.Card;
import game.pieces.Deck;
import game.pieces.Roles;
import game.pieces.impl.DeckImpl;
import game.pieces.impl.MogulCard;
import game.systems.web.PlayerController;
import game.systems.web.TableController;

@Component
public class Tabletop {
	
	private static final long INTERRUPT_WAIT_MS = 10000L;
	private static final Player DUMMY_PLAYER = new Player(null, null);
	
	private boolean roundActive = false;
	private Map<String, Player> playerMap = new LinkedHashMap<>(); //Linked to maintain a play order
	private JSONArray orderedPlayerNames;
	private Deck deck = new DeckImpl();
	private Player currActivePlayer = null;
	private ExecutorService interruptThreadPool = Executors.newCachedThreadPool();
	private Map<String, Interrupt> interruptibles = new HashMap<>();
	
	@Autowired
	private PlayerController playerController;
	
	@Autowired
	private TableController tableController;
	
	public String addPlayer(String name, String secret) {
		synchronized(playerMap) {
			if(roundActive) {
				return null;
			}
			Player newPlayer = new Player(name, secret);
			if(playerMap.size() == 0) {
				orderedPlayerNames = new JSONArray();
			} else {
				Player prevPlayer = playerMap.get(orderedPlayerNames.get(orderedPlayerNames.size()-1));
				newPlayer.setPrevPlayer(prevPlayer);
				prevPlayer.setNextPlayer(newPlayer);
			}
			orderedPlayerNames.add(name);
			playerMap.put(name, newPlayer);
			String playerListHtml = convertSetToHtml(playerMap.keySet());
			tableController.notifyTableOfPlayerChange(playerListHtml);
			return playerListHtml;
		}
	}
	
	public void removePlayer(String name) {
		synchronized(playerMap) {
			if(roundActive) {
				playerMap.remove(name);
				String playerListHtml = convertSetToHtml(playerMap.keySet());
				tableController.notifyTableOfPlayerChange(playerListHtml);
			}
		}
	}
	
	public boolean isPlayerPresent(String name) {
		return playerMap.containsKey(name);
	}
	
	public boolean startRound() throws IOException {
		synchronized(playerMap) {
			roundActive = true;
			deck.initialize();
			Player lastPlayerToJoin = playerMap.get(orderedPlayerNames.get(orderedPlayerNames.size()-1));
			Player firstPlayerToJoin = playerMap.get(orderedPlayerNames.get(0));
			lastPlayerToJoin.setNextPlayer(firstPlayerToJoin);
			firstPlayerToJoin.setPrevPlayer(lastPlayerToJoin);
			
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				Player currPlayer = playerEntry.getValue();
				
//				System.out.println("dealing to "+currPlayer.name);
				
				List<Card> cardsDrawn = deck.draw(2);
//				System.out.println("cards drawn: "+cardsDrawn);
				currPlayer.addCardsInit(cardsDrawn);
				currPlayer.addCoins(2);
				
//				System.out.println("finished dealing "+ currPlayer);
			}
			
//			System.out.println(playerMap);
			currActivePlayer = playerMap.get(orderedPlayerNames.get(0));
			JSONObject returnObj = new JSONObject();
			returnObj.put("activePlayer", currActivePlayer.getName());
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				String currPlayerName = playerEntry.getKey();
				JSONObject maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
				returnObj.put("boardState", maskedPlayerJson);
				playerController.contactPlayerInitTable(currPlayerName, orderedPlayerNames.toJSONString(), returnObj.toJSONString());
			}			
			return roundActive;
		}
	}
	
	public JSONObject getMaskedPlayerMapAsJson(String targetPlayerName) {
		JSONObject playerMapJsonObj = new JSONObject();
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			String currPlayerName = playerEntry.getKey();
			if(currPlayerName.equals(targetPlayerName)) {
				playerMapJsonObj.put(currPlayerName, playerEntry.getValue().getSelf());
			} else {
				playerMapJsonObj.put(currPlayerName, playerEntry.getValue().getMaskedSelf());
			}
		}
		return playerMapJsonObj;
	}
	
	public boolean isRoundActive() {
		return roundActive;
	}

	public Map<String, Player> getPlayerMap() {
		return playerMap;
	}
	
	public Player getCurrActivePlayer() {
		return currActivePlayer;
	}
	
	public boolean isActivePlayer(String secret, String playerName) {
		return (currActivePlayer.getName().equals(playerName)
				&& currActivePlayer.isSecret(secret));
	}
	
	public boolean isSecretCorrect(String secret, String playerName) {
		return playerMap.get(playerName).isSecret(secret);
	}
	
	public boolean isValidResponder(String interruptId, String playerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			if(playerName.equals(activeInterrupt.getTriggerPlayer())){
				return false; //can't respond to your own interrupt
			}
			return true;
		} else {
			return false;
		}
	}
	
	public void handlePayday() {
		currActivePlayer.addCoins(1);
		advanceActivePlayer(); //Not possible to win or lose off this action
		sendUpdatedBoardToPlayers();
	}
	
	//Attempt crowdfund
	public void handleCrowdfund() {
		String crowdfundId = UUID.randomUUID().toString();
		
		CrowdfundInterrupt interrupt = new CrowdfundInterrupt(crowdfundId, INTERRUPT_WAIT_MS, currActivePlayer.getName());
		InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(crowdfundId, INTERRUPT_WAIT_MS, this, InterruptCase.CROWDFUND);
		interruptibles.put(crowdfundId, interrupt);
		
		JSONObject returnObj = buildInterruptOppRsp(currActivePlayer.getName(), "crowdfund", crowdfundId, currActivePlayer.getName()+" attempts to crowdfund");
		
		tableController.notifyTableOfGroupCounterOpp(returnObj.toJSONString());
		Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
		interrupt.setDefaultResolverFuture(defaultResolverFuture);
	}
	
	//Crowdfund interrupted by Counter (Mogul)
	public void handleInterruptCrowdfundCounter(String interruptId, String interruptingPlayerName) {
		Interrupt activeCrowdfundInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				activeCrowdfundInterrupt = interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(activeCrowdfundInterrupt != null) {
			activeCrowdfundInterrupt.setActive(false);
			String counterId = UUID.randomUUID().toString();
			CrowdfundCounterInterrupt interrupt = new CrowdfundCounterInterrupt(counterId, INTERRUPT_WAIT_MS, interruptingPlayerName, InterruptCase.CROWDFUND_COUNTER);
			InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(counterId, INTERRUPT_WAIT_MS, this, InterruptCase.CROWDFUND_COUNTER);
			interruptibles.put(counterId, interrupt);
			
			JSONObject returnObj = buildInterruptOppRsp(interruptingPlayerName, "crowdfundCounter", counterId, interruptingPlayerName+" blocks the crowdfund");
			
			tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
			Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
			interrupt.setDefaultResolverFuture(defaultResolverFuture);
		}
	}
	
	//Route challenges to appropriate sub-method
	public void handleChallenge(String interruptId, String interruptingPlayerName) {
		Interrupt challengeableInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				challengeableInterrupt = interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		InterruptCase interruptCase = challengeableInterrupt.getInterruptCase();
		switch(interruptCase) {
			case CROWDFUND_COUNTER:
				CrowdfundCounterInterrupt castedInterrupt = (CrowdfundCounterInterrupt) challengeableInterrupt;
				handleInterruptCrowdfundCounterChallenge(interruptingPlayerName, castedInterrupt);
				break;
		default:
			System.err.println("Not a challengeable action: "+interruptCase+"|InterruptId: "+interruptId+"|InterruptingPlayer: "+interruptingPlayerName);
			break;
		}
	}
	
	//Counter to crowdfund interrupted by challenge
	public void handleInterruptCrowdfundCounterChallenge(String interruptingPlayerName, CrowdfundCounterInterrupt activeCrowdfundCounterInterrupt) {	
		if(activeCrowdfundCounterInterrupt != null) {
			activeCrowdfundCounterInterrupt.setActive(false);
			
			String challengeId = UUID.randomUUID().toString();
			String challenged = activeCrowdfundCounterInterrupt.getCounterer();
			ChallengeInterrupt interrupt = new ChallengeInterrupt(challengeId, challenged, interruptingPlayerName, interruptingPlayerName, InterruptCase.CROWDFUND_COUNTER);
			interruptibles.put(challengeId, interrupt);
			
			Player challengedPlayer = playerMap.get(challenged);
			int validIndices = -1;
			try {
				validIndices = getValidChallengeResponseIndices(challengedPlayer);
			} catch (Exception e) {
				tableController.notifyTableOfUnauthorizedActivity("Challenged player should have already lost: "+challenged);
				e.printStackTrace();
				return;
			}
			
			JSONObject returnObj = buildChallengePhase1Rsp(challenged, interruptingPlayerName, challengeId, interruptingPlayerName+" challenges Mogul of "+challenged, validIndices);
			
			tableController.notifyTableOfChallenge(returnObj.toJSONString());
		}
	}
	
	public void handleInterruptChallengeResponse(String interruptId, int cardIndexRsp) {
		ChallengeInterrupt challengeInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				challengeInterrupt = (ChallengeInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(challengeInterrupt != null) {
			challengeInterrupt.setActive(false);
			String challenged = challengeInterrupt.getChallenged();
			switch(challengeInterrupt.getActionChallenged()) {
				case CROWDFUND_COUNTER:
					Player challengedPlayer = playerMap.get(challengeInterrupt.getChallenged());
					if(cardIndexRsp == 0 || cardIndexRsp == 1) {
						Card revealedCard = challengedPlayer.revealCardInHand(cardIndexRsp);
						String challenger = challengeInterrupt.getChallenger();
						if(Roles.MOGUL.equals(revealedCard.getRole())) {
							String challengeLossId = UUID.randomUUID().toString();
							ChallengeInterrupt interrupt = new ChallengeInterrupt(challengeLossId, challenged, challenger, challenged, InterruptCase.CROWDFUND_COUNTER_CHALLENGE_LOSS, cardIndexRsp);
							interruptibles.put(challengeLossId, interrupt);
							
							sendUpdatedBoardToPlayers(); //show revealed card
							tableController.notifyTableWithSimpleMessage(challenged+" successfully defended the challenge from "+challenger);
							
							Player challengerPlayer = playerMap.get(challenger);
							int validIndices = -1;
							try {
								validIndices = getValidChallengeResponseIndices(challengerPlayer);
							} catch (Exception e) {
								tableController.notifyTableOfUnauthorizedActivity("Challenged player should have already lost: "+challenged);
								e.printStackTrace();
								return;
							}
							
							JSONObject returnObj = buildChallengePhase2Rsp(challenger, challengeLossId, challenger+" loses the challenge", validIndices);
							tableController.notifyTableOfChallengeLoss(returnObj.toJSONString());
						} else {
							if(challengedPlayer.getCardInHand(cardIndexRsp).isEliminated()) {
								tableController.notifyTableOfUnauthorizedActivity("Player attempted to eliminate an already eliminated card: "+challenged+"| "+cardIndexRsp);
								return;
							}
							challengedPlayer.eliminateCardInHand(cardIndexRsp);
							sendUpdatedBoardToPlayers();
							tableController.notifyTableWithSimpleMessage(challenged+" loses the challenge from "+challenger);
							
							Player winnerCandidate = checkForWinner(challengedPlayer);
							if(!DUMMY_PLAYER.equals(winnerCandidate)) {
								return;
							}

							currActivePlayer.addCoins(2);
							advanceActivePlayer();
							sendUpdatedBoardToPlayers();
						}
					} else {
						tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from "+challenged+": "+cardIndexRsp);
					}
					break;
			default:
				tableController.notifyTableOfUnauthorizedActivity("Challenge defense received for invalid challenge loss: "+challengeInterrupt.getActionChallenged()+" by "+challenged);
				break;
			}
		}
	}
	
	public void handleInterruptSuccessfulChallengeDefense(String interruptId, int cardIndexRsp) {
		ChallengeInterrupt challengeInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				challengeInterrupt = (ChallengeInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(challengeInterrupt != null) {
			challengeInterrupt.setActive(false);
			String challenged = challengeInterrupt.getChallenged();
			switch(challengeInterrupt.getActionChallenged()) {
				case CROWDFUND_COUNTER_CHALLENGE_LOSS:
					String challenger = challengeInterrupt.getChallenger();
					Player challengerPlayer = playerMap.get(challenger);
					Player challengedPlayer = playerMap.get(challenged); 
					if(cardIndexRsp == 0 || cardIndexRsp == 1) {
						if(challengerPlayer.getCardInHand(cardIndexRsp).isEliminated()) {
							tableController.notifyTableOfUnauthorizedActivity("Player attempted to eliminate an already eliminated card: "+challenger+"| "+cardIndexRsp);
							return;
						}
						challengerPlayer.eliminateCardInHand(cardIndexRsp);
						Player winnerCandidate = checkForWinner(challengerPlayer);
						if(!DUMMY_PLAYER.equals(winnerCandidate)) {
							return;
						}
						
						int defenderIndexCardToReplace = challengeInterrupt.getRevealedDefendingCardIndex();
						Card defendedCard = challengedPlayer.getCardInHand(challengeInterrupt.getRevealedDefendingCardIndex());
						deck.add(defendedCard);
						deck.shuffle();
						challengedPlayer.replaceCardInHand(deck.drawOne(), defenderIndexCardToReplace);
						tableController.notifyTableWithSimpleMessage(challenged+" successfully defended the challenge from "+challenger);
						advanceActivePlayer();
						sendUpdatedBoardToPlayers();
					} else {
						tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from "+challenger+": "+cardIndexRsp);
					}
					break;
				default:
					tableController.notifyTableOfUnauthorizedActivity("Invalid challenge defense received for "+challengeInterrupt.getActionChallenged()+" by "+challenged);
					break;
			}
		}
	}
	
	public void resolveCrowdfund(String interruptId, boolean isSuccess) {
		Interrupt activeInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				activeInterrupt = interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(activeInterrupt != null) {
			if(isSuccess) {
				currActivePlayer.addCoins(2);
			}
			advanceActivePlayer();
			sendUpdatedBoardToPlayers();
		}
	}
	
	public void handleSkip(String interruptId, String skippingPlayerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			activeInterrupt.addResponder(skippingPlayerName);
			int responderCount = activeInterrupt.getNumberOfResponders();
			
			if(responderCount >= (playerMap.size() - 1)) {
				activeInterrupt.getDefaultResolverFuture().cancel(true);
			}
		}
	}
	
	private void sendUpdatedBoardToPlayers() { //multithread this
		JSONObject returnObj = new JSONObject();
		returnObj.put("activePlayer", currActivePlayer.getName());
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			String currPlayerName = playerEntry.getKey();
			JSONObject maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
			returnObj.put("boardState", maskedPlayerJson);
			playerController.contactPlayerUpdateTable(currPlayerName, returnObj.toJSONString());
		}
	}
	
	private boolean advanceActivePlayer() {
		System.out.println("CurrActivePlayer: "+currActivePlayer);
		Player candidateActivePlayer = currActivePlayer.getNextPlayer();
		while(candidateActivePlayer.isLost()) {
			System.out.println("CandidateActivePlayer: "+candidateActivePlayer);
			if(currActivePlayer.equals(candidateActivePlayer)){
				return true; //currentActivePlayer is winner
			}
			candidateActivePlayer = candidateActivePlayer.getNextPlayer();
		}
		currActivePlayer = candidateActivePlayer;
		if(currActivePlayer.getCoins() >= 10) {
			currActivePlayer.setVoidLocked(true);
		}
		return false;
	}
	
	private Player checkForWinner(Player playerAtRisk) {
		if(playerAtRisk.getCardInHand(0).isFaceUp() && playerAtRisk.getCardInHand(1).isFaceUp()) {
			playerAtRisk.eliminatePlayer();
			Player tempPlayer = null;
			boolean onePlayerNotLost = false;
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				boolean isCurrentPlayerLost = playerEntry.getValue().isLost();
				if(!isCurrentPlayerLost) {
					if(onePlayerNotLost && isCurrentPlayerLost) {
						return null; //no winner yet
					} else {
						onePlayerNotLost = true;
						tempPlayer = playerEntry.getValue();
					}
				}
			}
			
			if(onePlayerNotLost) {
				JSONObject returnObj = buildWinnerRsp(tempPlayer.getName(), tempPlayer.getName()+" is the winner!");
				tableController.notifyTableOfWinner(returnObj.toJSONString());
				return tempPlayer; //Sole survivor is winner
			} else {
				tableController.notifyTableOfUnauthorizedActivity("All players lost, should not be possible");
				return DUMMY_PLAYER;
			}
		} else {
			return DUMMY_PLAYER;
		}
	}

	private String convertSetToHtml(Set<String> playerSet) {
		StringBuilder sb = new StringBuilder();
		for(String player : playerSet) {
			sb.append("<tr><td>").append(player).append("</td></tr>");
		}
		return sb.toString();
	}
	
	private JSONObject buildInterruptOppRsp(String atRiskPlayer, String interruptFor, String interruptId, String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("atRiskPlayer", atRiskPlayer);
		returnObj.put("interruptFor", interruptFor);
		returnObj.put("interruptId", interruptId);
		returnObj.put("rspWindowMs", INTERRUPT_WAIT_MS);
		returnObj.put("msg", msg);
		return returnObj;
	}
	
	private JSONObject buildChallengePhase1Rsp(String challenged, String challenger, String interruptId, String msg, int validIndices) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("challenged", challenged);
		returnObj.put("interruptId", interruptId);
		returnObj.put("msg", msg);
		returnObj.put("valid", validIndices);
		return returnObj;
	}
	
	private JSONObject buildChallengePhase2Rsp(String challenger, String interruptId, String msg, int validIndices) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("challenger", challenger);
		returnObj.put("interruptId", interruptId);
		returnObj.put("msg", msg);
		returnObj.put("valid", validIndices);
		return returnObj;
	}
	
	private JSONObject buildWinnerRsp(String winnerName, String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("winner", winnerName);
		returnObj.put("msg", msg);
		return returnObj;
	}
	
	private int getValidChallengeResponseIndices(Player playerToReveal) throws Exception {
		Card card0 = playerToReveal.getCardInHand(0);
		Card card1 = playerToReveal.getCardInHand(1);
		if(!card0.isFaceUp() && !card1.isFaceUp()) {
			return 2;
		} else if(card0.isFaceUp() && !card1.isFaceUp()) {
			return 1;
		} else if(card1.isFaceUp() && !card0.isFaceUp()) {
			return 0;
		} 
		throw new GameException("Player should have already lost: "+playerToReveal.getName());
	}
}

