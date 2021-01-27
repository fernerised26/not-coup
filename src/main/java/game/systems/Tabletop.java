package game.systems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import game.systems.interrupt.BlockInterrupt;
import game.systems.interrupt.ChallengeInterrupt;
import game.systems.interrupt.CrowdfundCounterInterrupt;
import game.systems.interrupt.CrowdfundInterrupt;
import game.systems.interrupt.DecoyInterrupt;
import game.systems.interrupt.ForcedHitInterrupt;
import game.systems.interrupt.HitInterrupt;
import game.systems.interrupt.Interrupt;
import game.systems.interrupt.InterruptCase;
import game.systems.interrupt.PrintMoneyInterrupt;
import game.systems.interrupt.RaidInterrupt;
import game.systems.interrupt.ScrambleInterrupt;
import game.systems.interrupt.VoidoutInterrupt;
import game.systems.web.PlayerController;
import game.systems.web.TableController;

@Component
public class Tabletop {
	
	private static final long INTERRUPT_WAIT_MS = 15000L;
	private static final Player DUMMY_PLAYER = new Player(null, null);
	
	private boolean roundActive = false;
	private boolean resetReady = false;
	private Map<String, Player> playerMap = new LinkedHashMap<>(); //Linked to maintain a play order
	private JSONArray orderedPlayerNames;
	private JSONArray eliminatedPlayerNames = new JSONArray();
	private Deck deck = new DeckImpl();
	private List<Card> tempHoldingSpace = new ArrayList<>();
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
				orderedPlayerNames.remove(name);
				String playerListHtml = convertSetToHtml(playerMap.keySet());
				tableController.notifyTableOfPlayerChange(playerListHtml);
			}
		}
	}
	
	public boolean isPlayerPresent(String name) {
		return playerMap.containsKey(name);
	}
	
	public void shufflePlayers() {
		if(playerMap.size() <= 1) {
			return;
		}
		synchronized(playerMap) {
			if(roundActive) {
				return;
			}
			Random rand = new Random();
			for (int i = orderedPlayerNames.size(); i > 1; i--) {
				int randomPoint = rand.nextInt(i);
				String tmpPlayerName = (String) orderedPlayerNames.get(i - 1);
				orderedPlayerNames.set(i - 1, orderedPlayerNames.get(randomPoint));
				orderedPlayerNames.set(randomPoint, tmpPlayerName);
			}
			
			Map<String, Player> newPlayerMap = new LinkedHashMap<>();
			
			for (int i = 0; i < orderedPlayerNames.size(); i++) {
				if(i == 0) {
					String player1Name = (String) orderedPlayerNames.get(0);
					Player player1 = playerMap.get(player1Name);
					
					String player2Name = (String) orderedPlayerNames.get(1);
					Player player2 = playerMap.get(player2Name);
					player1.setNextPlayer(player2);
					newPlayerMap.put(player1Name, player1);
				} else if(i == (orderedPlayerNames.size() - 1)) {
					String playerLastName = (String) orderedPlayerNames.get(i);
					Player playerLast = playerMap.get(playerLastName);
					
					String playerPenultimateName = (String) orderedPlayerNames.get(i-1);
					Player playerPenultimate = playerMap.get(playerPenultimateName);
					playerLast.setPrevPlayer(playerPenultimate);
					newPlayerMap.put(playerLastName, playerLast);
				} else {
					String currPlayerName = (String) orderedPlayerNames.get(i);
					Player currPlayer = playerMap.get(currPlayerName);
					
					String prevPlayerName = (String) orderedPlayerNames.get(i-1);
					Player prevPlayer = playerMap.get(prevPlayerName);
					currPlayer.setPrevPlayer(prevPlayer);
					
					String nextPlayerName = (String) orderedPlayerNames.get(i+1);
					Player nextPlayer = playerMap.get(nextPlayerName);
					currPlayer.setNextPlayer(nextPlayer);
					
					newPlayerMap.put(currPlayerName, currPlayer);
				}
			}
			playerMap.clear();
			playerMap.putAll(newPlayerMap);
			String playerListHtml = convertSetToHtml(playerMap.keySet());
			tableController.notifyTableOfPlayerChange(playerListHtml);
		}
	}
	
	public boolean startRound() throws IOException {
		synchronized(playerMap) {
			if(roundActive) {
				return false;
			}
			roundActive = true;
			deck.initialize();
			Player lastPlayerToJoin = playerMap.get(orderedPlayerNames.get(orderedPlayerNames.size()-1));
			Player firstPlayerToJoin = playerMap.get(orderedPlayerNames.get(0));
			lastPlayerToJoin.setNextPlayer(firstPlayerToJoin);
			firstPlayerToJoin.setPrevPlayer(lastPlayerToJoin);
			
			if(playerMap.size() == 2) {
				for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
					Player currPlayer = playerEntry.getValue();
					
					List<Card> cardsDrawn = deck.draw(2);
					currPlayer.addCardsInit(cardsDrawn);
					currPlayer.addCoins(1);
				}
			} else {
				for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
					Player currPlayer = playerEntry.getValue();
					
					List<Card> cardsDrawn = deck.draw(2);
					currPlayer.addCardsInit(cardsDrawn);
					currPlayer.addCoins(2);
				}
			}
			
			currActivePlayer = playerMap.get(orderedPlayerNames.get(0));
			JSONObject returnObj = new JSONObject();
			returnObj.put("activePlayer", currActivePlayer.getName());
			returnObj.put("deckSize", "Cards in Deck: "+deck.getDeckSize());
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
	
	public boolean isPlayerVoidLocked(String playerName) {
		return playerMap.get(playerName).isVoidLocked();
	}
	
	public boolean isSecretCorrect(String secret, String playerName) {
		return playerMap.get(playerName).isSecret(secret);
	}
	
	public boolean isValidResponder(String interruptId, String playerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			if(playerName.equals(activeInterrupt.getFocused())){
				return false; //can't respond to your own action
			}
			Player responder = playerMap.get(playerName);
			if(responder.isLost()) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isExpectedResponder(String interruptId, String playerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			if(playerName.equals(activeInterrupt.getFocused())){
				return true;
			}
			return false;
		} else {
			return false;
		}
	}
	
	public boolean isExpectedHitResponder(String interruptId, String playerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			if(!(activeInterrupt instanceof HitInterrupt)) {
				return false;
			}
			if(activeInterrupt instanceof HitInterrupt) {
				HitInterrupt activeHitInterrupt = (HitInterrupt) activeInterrupt;
				if(playerName.equals(activeHitInterrupt.getTarget())){
					return true;
				}
			} 
			
//			|| activeInterrupt instanceof ForcedHitInterrupt
//			else if(activeInterrupt instanceof ForcedHitInterrupt){
//				ForcedHitInterrupt activeHitInterrupt = (ForcedHitInterrupt) activeInterrupt;
//				if(playerName.equals(activeHitInterrupt.getForced())){
//					return true;
//				}
//			}
			return false;
		} else {
			return false;
		}
	}
	
	public boolean isExpectedRaidResponder(String interruptId, String playerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			if(!(activeInterrupt instanceof RaidInterrupt)) {
				return false;
			}
			RaidInterrupt activeHitInterrupt = (RaidInterrupt) activeInterrupt;
			if(playerName.equals(activeHitInterrupt.getTarget())){
				return true;
			}
			return false;
		} else {
			return false;
		}
	}
	
	public void handlePayday() {
		currActivePlayer.addCoins(1);
		tableController.notifyTableWithSimpleMessage(currActivePlayer.getName()+" takes a payday.");
		advanceActivePlayer(); //Not possible to win or lose off this action
		sendUpdatedBoardToPlayers(true);
	}
	
	public void handleCrowdfund() {
		String crowdfundId = UUID.randomUUID().toString();
		
		CrowdfundInterrupt interrupt = new CrowdfundInterrupt(crowdfundId, INTERRUPT_WAIT_MS, currActivePlayer.getName(), eliminatedPlayerNames);
		InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(crowdfundId, INTERRUPT_WAIT_MS, this, InterruptCase.CROWDFUND);
		interruptibles.put(crowdfundId, interrupt);
		
		JSONObject returnObj = buildInterruptOppRsp(currActivePlayer.getName(), "crowdfund", crowdfundId, currActivePlayer.getName()+" attempts to crowdfund.");
		
		tableController.notifyTableOfGroupCounterOpp(returnObj.toJSONString());
		Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
		interrupt.setDefaultResolverFuture(defaultResolverFuture);
	}
	
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
			CrowdfundCounterInterrupt interrupt = new CrowdfundCounterInterrupt(counterId, INTERRUPT_WAIT_MS, interruptingPlayerName, InterruptCase.CROWDFUND_COUNTER, eliminatedPlayerNames);
			InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(counterId, INTERRUPT_WAIT_MS, this, InterruptCase.CROWDFUND_COUNTER);
			interruptibles.put(counterId, interrupt);
			
			JSONObject returnObj = buildInterruptOppRsp(interruptingPlayerName, "crowdfundCounter", counterId, interruptingPlayerName+" blocks the crowdfund.");
			
			tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
			Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
			interrupt.setDefaultResolverFuture(defaultResolverFuture);
		}
	}
	
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
				CrowdfundCounterInterrupt activeCrowdfundCounterInterrupt = (CrowdfundCounterInterrupt) challengeableInterrupt;
				String crowdfundCounterChallenged = activeCrowdfundCounterInterrupt.getCounterer();
				handleInterruptChallenge(interruptingPlayerName, activeCrowdfundCounterInterrupt, crowdfundCounterChallenged, Roles.MOGUL.name(), InterruptCase.CROWDFUND_COUNTER);
				break;
			case PRINT_MONEY:
				PrintMoneyInterrupt printMoneyInterrupt = (PrintMoneyInterrupt) challengeableInterrupt;
				String printMoneyChallenged = printMoneyInterrupt.getFocused();
				handleInterruptChallenge(interruptingPlayerName, printMoneyInterrupt, printMoneyChallenged, Roles.MOGUL.name(), InterruptCase.PRINT_MONEY);
				break;
			case ORDER_HIT:
				HitInterrupt hitInterrupt = (HitInterrupt) challengeableInterrupt;
				String hitChallenged = hitInterrupt.getHitOrderer();
				handleInterruptChallenge(interruptingPlayerName, hitInterrupt, hitChallenged, Roles.HITMAN.name(), InterruptCase.ORDER_HIT);
				break;
			case ORDER_HIT_COUNTER:
				DecoyInterrupt decoyInterrupt = (DecoyInterrupt) challengeableInterrupt;
				String decoyChallenged = decoyInterrupt.getDecoyDeployer();
				handleInterruptChallenge(interruptingPlayerName, decoyInterrupt, decoyChallenged, Roles.DECOY.name(), InterruptCase.ORDER_HIT_COUNTER);
				break;
			case SCRAMBLE_IDENTITY:
				ScrambleInterrupt scrambleInterrupt = (ScrambleInterrupt) challengeableInterrupt;
				String scrambleChallenged = scrambleInterrupt.getFocused();
				handleInterruptChallenge(interruptingPlayerName, scrambleInterrupt, scrambleChallenged, Roles.NETOPS.name(), InterruptCase.SCRAMBLE_IDENTITY);
				break;
			case RAID:
				RaidInterrupt raidInterrupt = (RaidInterrupt) challengeableInterrupt;
				String raidChallenged = raidInterrupt.getRaider();
				handleInterruptChallenge(interruptingPlayerName, raidInterrupt, raidChallenged, Roles.CAPTAIN.name(), InterruptCase.RAID);
				break;
			case RAID_COUNTER:
				BlockInterrupt blockInterrupt = (BlockInterrupt) challengeableInterrupt;
				String blockChallenged = blockInterrupt.getBlocker();
				if(blockInterrupt.getBlockingRole().equals(Roles.CAPTAIN)) {
					handleInterruptChallenge(interruptingPlayerName, blockInterrupt, blockChallenged, blockInterrupt.getBlockingRole().name(), InterruptCase.RAID_COUNTER_CPT);
				} else {
					handleInterruptChallenge(interruptingPlayerName, blockInterrupt, blockChallenged, blockInterrupt.getBlockingRole().name(), InterruptCase.RAID_COUNTER_NOPS);
				}
				break;
		default:
			tableController.notifyTableOfUnauthorizedActivity("Not a challengeable action: "+interruptCase+"|InterruptId: "+interruptId+"|InterruptingPlayer: "+interruptingPlayerName);
			System.err.println("Not a challengeable action: "+interruptCase+"|InterruptId: "+interruptId+"|InterruptingPlayer: "+interruptingPlayerName);
			break;
		}
	}
	
	public void handleInterruptChallenge(String interruptingPlayerName, Interrupt challengeableInterrupt, String challenged, String adlib, InterruptCase interruptCase) {	
		if(challengeableInterrupt != null) {
			challengeableInterrupt.setActive(false);
			
			String challengeId = UUID.randomUUID().toString();
			switch(interruptCase) {
				case CROWDFUND_COUNTER:
				case PRINT_MONEY:
				case SCRAMBLE_IDENTITY:
					ChallengeInterrupt twoPartyInterrupt = new ChallengeInterrupt(challengeId, challenged, interruptingPlayerName, interruptCase);
					interruptibles.put(challengeId, twoPartyInterrupt);
					break;
				case ORDER_HIT:
					HitInterrupt hitInterrupt = (HitInterrupt) challengeableInterrupt;
					ChallengeInterrupt challengedHitInterrupt = new ChallengeInterrupt(challengeId, challenged, interruptingPlayerName, interruptCase, hitInterrupt.getTarget());
					interruptibles.put(challengeId, challengedHitInterrupt);
					break;
				case ORDER_HIT_COUNTER:
					DecoyInterrupt decoyInterrupt = (DecoyInterrupt) challengeableInterrupt;
					ChallengeInterrupt challengedDecoyInterrupt = new ChallengeInterrupt(challengeId, challenged, interruptingPlayerName, interruptCase, decoyInterrupt.getHitOrderer());
					interruptibles.put(challengeId, challengedDecoyInterrupt);
					break;
				case RAID:
					RaidInterrupt raidInterrupt = (RaidInterrupt) challengeableInterrupt;
					ChallengeInterrupt challengedRaidInterrupt = new ChallengeInterrupt(challengeId, challenged, interruptingPlayerName, interruptCase, raidInterrupt.getTarget());
					interruptibles.put(challengeId, challengedRaidInterrupt);
					break;
				case RAID_COUNTER_CPT:
				case RAID_COUNTER_NOPS:
					BlockInterrupt blockInterrupt = (BlockInterrupt) challengeableInterrupt;
					ChallengeInterrupt challengedBlockInterrupt = new ChallengeInterrupt(challengeId, challenged, interruptingPlayerName, interruptCase, blockInterrupt.getRaider());
					interruptibles.put(challengeId, challengedBlockInterrupt);
					break;
				default:
					tableController.notifyTableOfUnauthorizedActivity("Not a challengeable action: "+interruptCase+"|InterruptId: "+challengeId+"|InterruptingPlayer: "+interruptingPlayerName);
					System.err.println("Not a challengeable action: "+interruptCase+"|InterruptId: "+challengeId+"|InterruptingPlayer: "+interruptingPlayerName);
					return;
			}
			
			Player challengedPlayer = playerMap.get(challenged);
			int validIndices = -1;
			try {
				validIndices = getValidCardLossIndices(challengedPlayer);
			} catch (GameException e) {
				tableController.notifyTableOfUnauthorizedActivity("Challenged player should have already lost: "+challenged);
				System.err.println("Challenged player should have already lost: "+challenged);
				return;
			}
			
			JSONObject returnObj = buildChallengePhase1Rsp(challenged, adlib, interruptingPlayerName, challengeId, interruptingPlayerName+" challenges " + challenged + " to show an allied " + adlib + ".", validIndices);
			
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
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.CROWDFUND_COUNTER_CHALLENGE_LOSS, Roles.MOGUL);
					break;
				case PRINT_MONEY:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.PRINT_MONEY_CHALLENGE_LOSS, Roles.MOGUL);
					break;
				case ORDER_HIT:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.ORDER_HIT_CHALLENGE_LOSS, Roles.HITMAN);
					break;
				case ORDER_HIT_COUNTER:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.ORDER_HIT_COUNTER_CHALLENGE_LOSS, Roles.DECOY);
					break;
				case SCRAMBLE_IDENTITY:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.SCRAMBLE_IDENTITY_CHALLENGE_LOSS, Roles.NETOPS);
					break;
				case RAID:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.RAID_CHALLENGE_LOSS, Roles.CAPTAIN);
					break;
				case RAID_COUNTER_CPT:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.RAID_COUNTER_CHALLENGE_LOSS, Roles.CAPTAIN);
					break;
				case RAID_COUNTER_NOPS:
					arbitrateChallenge(challengeInterrupt, cardIndexRsp, challenged, InterruptCase.RAID_COUNTER_CHALLENGE_LOSS, Roles.NETOPS);
					break;
			default:
				tableController.notifyTableOfUnauthorizedActivity("Challenge response received for invalid original action: "+challengeInterrupt.getActionChallenged()+" by "+challenged);
				break;
			}
		}
	}
	
	public void arbitrateChallenge(ChallengeInterrupt challengeInterrupt, int cardIndexRsp, String challenged, InterruptCase challengeLossCase, Roles neededRole) {
		Player challengedPlayer = playerMap.get(challengeInterrupt.getChallenged());
		InterruptCase actionToResolve = challengeInterrupt.getActionChallenged();
		if(cardIndexRsp == 0 || cardIndexRsp == 1) {
			Card revealedCard = challengedPlayer.revealCardInHand(cardIndexRsp);
			String challenger = challengeInterrupt.getChallenger();
			if(neededRole.equals(revealedCard.getRole())) {
				String challengeLossId = UUID.randomUUID().toString();
				ChallengeInterrupt challengeLossInterrupt;
				switch(actionToResolve) {
					case CROWDFUND_COUNTER:
					case PRINT_MONEY:
					case SCRAMBLE_IDENTITY:
						challengeLossInterrupt = new ChallengeInterrupt(challengeLossId, challenged, challenger, challengeLossCase, cardIndexRsp);
						interruptibles.put(challengeLossId, challengeLossInterrupt);
						break;
					case ORDER_HIT:
						challengeLossInterrupt = new ChallengeInterrupt(challengeLossId, challenged, challenger, challengeLossCase, cardIndexRsp, challengeInterrupt.getThirdParty());
						interruptibles.put(challengeLossId, challengeLossInterrupt);
						if(challenger.equals(challengeInterrupt.getThirdParty())) {
							sendUpdatedBoardToPlayers(false);
							JSONObject returnObj = buildUtterDefeatRsp(challenger, challengeLossId, challenger+" utterly loses the challenge");
							tableController.notifyTableOfUtterDefeat(returnObj.toJSONString());
							return;
						}
					case ORDER_HIT_COUNTER:
					case RAID:
					case RAID_COUNTER_CPT:
					case RAID_COUNTER_NOPS:
						challengeLossInterrupt = new ChallengeInterrupt(challengeLossId, challenged, challenger, challengeLossCase, cardIndexRsp, challengeInterrupt.getThirdParty());
						interruptibles.put(challengeLossId, challengeLossInterrupt);
						break;
					default:
						tableController.notifyTableOfUnauthorizedActivity("Unable to track defended challenge, invalid challenge case: "+actionToResolve);
				}
				
				sendUpdatedBoardToPlayers(false); //show revealed card
				
				Player challengerPlayer = playerMap.get(challenger);
				int validIndices = -1;
				try {
					validIndices = getValidCardLossIndices(challengerPlayer);
				} catch (GameException e) {
					tableController.notifyTableOfUnauthorizedActivity("Challenged player should have already lost: "+challenged);
					System.err.println("Challenged player should have already lost: "+challenged);
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
				tableController.notifyTableWithSimpleMessage(challenged+" loses the challenge from "+challenger);
				switch(actionToResolve) {
					case CROWDFUND_COUNTER:
						//Counter to crowdfund was fraudulent, crowdfund proceeds
						currActivePlayer.addCoins(2);
						tableController.notifyTableWithSimpleMessage(currActivePlayer.getName() + " gets the word out on wheelOfFunding.com");
						break;
					case PRINT_MONEY:
					case SCRAMBLE_IDENTITY:
					case RAID:
						//Printing/Scramble/Raid was fraudulent, no effect
						tableController.notifyTableWithSimpleMessage("Nothing happens.");
						break;
					case ORDER_HIT:
						//Hit was fraudulent, fizzles, but player still loses money spent
						currActivePlayer.addCoins(-3);
						tableController.notifyTableWithSimpleMessage(challenged+"'s hitman was a fraud; he ran off with the down payment.");
						break;
					case ORDER_HIT_COUNTER:
						//Decoy was fraudulent, challenged player immediately loses, hit orderer loses 3 coins
						currActivePlayer.addCoins(-3);
						challengedPlayer.eliminateCardInHand(0);
						challengedPlayer.eliminateCardInHand(1);
						tableController.notifyTableWithSimpleMessage(challenged+"'s decoy was a fraud; "+currActivePlayer.getName()+"'s hitman causes devastating collateral damage.");
						break;
					case RAID_COUNTER_CPT:
						executeRaidCoinMove(challenged, challengeInterrupt.getThirdParty());
						tableController.notifyTableWithSimpleMessage(challenged+"'s captain was a fraud; "+currActivePlayer.getName()+"'s raiding party neuters the poorly executed counterattack.");
						break;
					case RAID_COUNTER_NOPS:
						executeRaidCoinMove(challenged, challengeInterrupt.getThirdParty());
						tableController.notifyTableWithSimpleMessage(challenged+"'s netOps was a fraud; "+currActivePlayer.getName()+"'s raiding party's tech redundancies kick in.");
						break;
					default:
						tableController.notifyTableOfUnauthorizedActivity("Invalid challenge loss case: "+challengeLossCase);
				}
				
				Player winnerCandidate = checkForWinner(challengedPlayer);
				if(winnerCandidate != null) {
					executeWinSequence(winnerCandidate);
					return;
				}
				
				advanceActivePlayer();
				sendUpdatedBoardToPlayers(true);
			}
		} else {
			tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from "+challenged+": "+cardIndexRsp);
		}
	}
	
	public void handleInterruptSuccessfulChallengeDefense(String interruptId, int cardIndexRsp) {
		ChallengeInterrupt challengeLossInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				challengeLossInterrupt = (ChallengeInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(challengeLossInterrupt != null) {
			challengeLossInterrupt.setActive(false);
			String challenged = challengeLossInterrupt.getChallenged();
			String challenger = challengeLossInterrupt.getChallenger();
			Player challengerPlayer = playerMap.get(challenger);
			Player challengedPlayer = playerMap.get(challenged);
			StringBuilder flavorMsg = new StringBuilder();
			if(cardIndexRsp == 0 || cardIndexRsp == 1) {
				if(challengerPlayer.getCardInHand(cardIndexRsp).isEliminated()) {
					tableController.notifyTableOfUnauthorizedActivity("Player attempted to eliminate an already eliminated card: "+challenger+"| "+cardIndexRsp);
					return;
				}
				challengerPlayer.eliminateCardInHand(cardIndexRsp);
				Player winnerCandidate = checkForWinner(challengerPlayer);
				if(winnerCandidate != null) {
					executeWinSequence(winnerCandidate);
					return;
				}
				
				int defenderIndexCardToReplace = challengeLossInterrupt.getRevealedDefendingCardIndex();
				Card defendedCard = challengedPlayer.getCardInHand(challengeLossInterrupt.getRevealedDefendingCardIndex());
				deck.add(defendedCard);
				deck.shuffle();
				challengedPlayer.replaceCardInHand(deck.drawOne(), defenderIndexCardToReplace);
				flavorMsg.append(challenged+" replaces the revealed " + defendedCard.getRole().name()+". ");
			} else {
				tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from "+challenger+": "+cardIndexRsp);
			}
			
			InterruptCase actionToResolve = challengeLossInterrupt.getActionChallenged();
			switch(actionToResolve) {
				case CROWDFUND_COUNTER_CHALLENGE_LOSS:
				case RAID_COUNTER_CHALLENGE_LOSS:
					//Nothing should happen besides cards being lost and swapped. Counter prevents coins from being added.
					flavorMsg.append("Action fizzles.");
					tableController.notifyTableWithSimpleMessage(flavorMsg.toString());
					advanceActivePlayer();
					sendUpdatedBoardToPlayers(true);
					break;
				case PRINT_MONEY_CHALLENGE_LOSS:
					challengedPlayer.addCoins(3);
					flavorMsg.append(challengedPlayer.getName() + " sets sales records with a new product.");
					tableController.notifyTableWithSimpleMessage(flavorMsg.toString());
					advanceActivePlayer();
					sendUpdatedBoardToPlayers(true);
					break;
				case ORDER_HIT_CHALLENGE_LOSS:
					handleForcedHit(challengeLossInterrupt.getThirdParty());
					sendUpdatedBoardToPlayers(false);
					break;
				case ORDER_HIT_COUNTER_CHALLENGE_LOSS:
					Player hitOrderer = playerMap.get(challengeLossInterrupt.getThirdParty());
					hitOrderer.addCoins(-3);
					flavorMsg.append(hitOrderer.getName() + "'s hit on " + challengeLossInterrupt.getChallenged() + " is botched.");
					tableController.notifyTableWithSimpleMessage(flavorMsg.toString());
					advanceActivePlayer();
					sendUpdatedBoardToPlayers(true);
					break;
				case SCRAMBLE_IDENTITY_CHALLENGE_LOSS:
					sendUpdatedBoardToPlayers(false);
					String scrambleGuaranteedId = UUID.randomUUID().toString();
					ScrambleInterrupt interrupt = new ScrambleInterrupt(scrambleGuaranteedId,currActivePlayer.getName(), InterruptCase.SCRAMBLE_IDENTITY, eliminatedPlayerNames);
					interruptibles.put(scrambleGuaranteedId, interrupt);
					resolveScrambleIdentity1(scrambleGuaranteedId);
					break;
				case RAID_CHALLENGE_LOSS:
					executeRaidCoinMove(challengeLossInterrupt.getThirdParty(), challenged);
					flavorMsg.append(challengeLossInterrupt.getThirdParty() + "'s booty is plundered by " + challenged + ".");
					tableController.notifyTableWithSimpleMessage(flavorMsg.toString());
					advanceActivePlayer();
					sendUpdatedBoardToPlayers(true);
					break;
				default:
					tableController.notifyTableOfUnauthorizedActivity("Cannot resolve failed challenge of type " + actionToResolve + " failed by " + challenger);
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
				tableController.notifyTableWithSimpleMessage(currActivePlayer.getName()+"'s fundMyThing campaign for " + CrowdfundRandomizer.getRandomCampaign() + " is a success.");
			} else {
				tableController.notifyTableWithSimpleMessage(currActivePlayer.getName()+"'s gibMoneyPlz campaign for " + CrowdfundRandomizer.getRandomCampaign() + " fizzles out.");
			}
			advanceActivePlayer();
			sendUpdatedBoardToPlayers(true);
		}
	}
	
	public void handleSkip(String interruptId, String skippingPlayerName) {
		if(interruptibles.containsKey(interruptId)) {
			Interrupt activeInterrupt = interruptibles.get(interruptId);
			activeInterrupt.addResponder(skippingPlayerName);
			int responderCount = activeInterrupt.getNumberOfResponders();
			
			if(responderCount >= (playerMap.size()-1)) {
				activeInterrupt.getDefaultResolverFuture().cancel(true);
			}
		}
	}
	
	public void handleGetTargets(String actionAdlib, String action) {
		JSONObject targetMsg = new JSONObject();
		JSONArray validTargets = new JSONArray();
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			Player currPlayer = playerEntry.getValue();
			boolean isCurrentPlayerLost = currPlayer.isLost();
			if(!isCurrentPlayerLost && !currPlayer.equals(currActivePlayer)) {
				validTargets.add(currPlayer.getName());
			}
		}
		targetMsg.put("targets", validTargets);
		targetMsg.put("action", action);
		targetMsg.put("msg", "Select an opponent to " + actionAdlib);
		playerController.contactPlayerValidTargets(currActivePlayer.getName(), targetMsg.toJSONString());
	}
	
	public void handleVoidout(String voidedPlayerName) {
		Player voidedPlayer = playerMap.get(voidedPlayerName);
		if(currActivePlayer.getCoins() < 7) {
			tableController.notifyTableOfUnauthorizedActivity("Received Voidout attempt without sufficient cash: " + currActivePlayer.getName());
			return;
		}
		if(voidedPlayer != null && !voidedPlayer.equals(currActivePlayer) && !voidedPlayer.isLost()) {
			String voidoutId = UUID.randomUUID().toString();
			VoidoutInterrupt interrupt = new VoidoutInterrupt(voidoutId, currActivePlayer.getName(), voidedPlayerName, InterruptCase.VOIDOUT);
			interruptibles.put(voidoutId, interrupt);
			try {
				int validRspIndices = getValidCardLossIndices(voidedPlayer);
				JSONObject returnObj = buildVoidoutRsp(voidedPlayerName, voidoutId, currActivePlayer.getName()+" is voiding out an ally of " + voidedPlayerName + ".", validRspIndices);
				tableController.notifyTableOfVoidout(returnObj.toJSONString());	
			} catch (GameException e) {
				tableController.notifyTableOfUnauthorizedActivity("Voided player should have already lost: " + voidedPlayerName);
				System.err.println("Voided player should have already lost: " + voidedPlayerName);
				return;
			}
		} else {
			tableController.notifyTableOfUnauthorizedActivity("Invalid voidout target: " + voidedPlayerName);
		}
	}
	
	public void handleVoidoutResponse(String interruptId, int cardIndexRsp) {
		VoidoutInterrupt voidoutInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				voidoutInterrupt = (VoidoutInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(voidoutInterrupt != null) {
			voidoutInterrupt.setActive(false);
			String voided = voidoutInterrupt.getVoided();
			String voider = voidoutInterrupt.getVoider();
			Player voidedPlayer = playerMap.get(voided);
			Player voiderPlayer = playerMap.get(voider); 
			
			if(cardIndexRsp == 0 || cardIndexRsp == 1) {
				if(voidedPlayer.getCardInHand(cardIndexRsp).isEliminated()) {
					tableController.notifyTableOfUnauthorizedActivity("Player attempted to eliminate an already eliminated card: "+voided+"| "+cardIndexRsp);
					return;
				}
				Card voidedCard = voidedPlayer.eliminateCardInHand(cardIndexRsp);
				Player winnerCandidate = checkForWinner(voidedPlayer);
				if(winnerCandidate != null) {
					executeWinSequence(winnerCandidate);
					return;
				}
				voiderPlayer.addCoins(-7);
				if(voiderPlayer.getCoins() < 10) {
					voiderPlayer.setVoidLocked(false);
				}
				tableController.notifyTableWithSimpleMessage(voider + " dubbed " + voided + "'s " + voidedCard.getRole().name() + " to be persona-non-grata.");
				advanceActivePlayer();
				sendUpdatedBoardToPlayers(true);
			} else {
				tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from " + voided + ": " + cardIndexRsp);
			}
		}
	}
	
	public void handlePrintMoney() {
		String printMoneyId = UUID.randomUUID().toString();
		
		PrintMoneyInterrupt interrupt = new PrintMoneyInterrupt(printMoneyId, INTERRUPT_WAIT_MS, InterruptCase.PRINT_MONEY, currActivePlayer.getName(), eliminatedPlayerNames);
		InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(printMoneyId, INTERRUPT_WAIT_MS, this, InterruptCase.PRINT_MONEY);
		interruptibles.put(printMoneyId, interrupt);
		
		JSONObject returnObj = buildInterruptOppRsp(currActivePlayer.getName(), "printMoney", printMoneyId, currActivePlayer.getName()+" attempts to print money.");
		
		tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
		Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
		interrupt.setDefaultResolverFuture(defaultResolverFuture);
	}
	
	public void resolvePrintMoney(String interruptId) {
		Interrupt activeInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				activeInterrupt = interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(activeInterrupt != null) {
			currActivePlayer.addCoins(3);
			tableController.notifyTableWithSimpleMessage(currActivePlayer.getName() + " sets sales records with a new product.");
			advanceActivePlayer();
			sendUpdatedBoardToPlayers(true);
		}
	}
	
	public void handleOrderHit(String targetPlayerName) {
		Player targetPlayer = playerMap.get(targetPlayerName);
		String orderer = currActivePlayer.getName();
		if(currActivePlayer.getCoins() < 3) {
			tableController.notifyTableOfUnauthorizedActivity("Received Hit attempt without sufficient cash: " + currActivePlayer.getName());
			return;
		}
		if(targetPlayer != null && !targetPlayer.equals(currActivePlayer) && !targetPlayer.isLost()) {
			String hitId = UUID.randomUUID().toString();
			HitInterrupt interrupt = new HitInterrupt(hitId, orderer, targetPlayerName, InterruptCase.ORDER_HIT, eliminatedPlayerNames);
			interruptibles.put(hitId, interrupt);
			try { 
				int validRspIndices = getValidCardLossIndices(targetPlayer);
				JSONObject returnObj = buildHitRsp(targetPlayerName, orderer, hitId, orderer + " takes out a hit on an ally of " + targetPlayerName + ".", validRspIndices);
				tableController.notifyTableOfHitOrder(returnObj.toJSONString());	
			} catch (GameException e) {
				tableController.notifyTableOfUnauthorizedActivity("Targeted player should have already lost: " + targetPlayerName);
				System.err.println("Targeted player should have already lost: " + targetPlayerName);
				return;
			}
		} else {
			tableController.notifyTableOfUnauthorizedActivity("Invalid hit target: " + targetPlayerName);
		}
	}
	
	public void handleOrderHitResponse(String interruptId, int cardIndexRsp) {
		HitInterrupt hitInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				hitInterrupt = (HitInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(hitInterrupt != null) {
			hitInterrupt.setActive(false);
			String target = hitInterrupt.getTarget();
			String hitOrderer = hitInterrupt.getHitOrderer();
			Player targetPlayer = playerMap.get(target); 
			Player hitOrdererPlayer = playerMap.get(hitOrderer);
			
			if(cardIndexRsp == 0 || cardIndexRsp == 1) {
				if(targetPlayer.getCardInHand(cardIndexRsp).isEliminated()) {
					tableController.notifyTableOfUnauthorizedActivity("Player attempted to eliminate an already eliminated card: "+target+"| "+cardIndexRsp);
					return;
				}
				Card eliminatedCard = targetPlayer.eliminateCardInHand(cardIndexRsp);
				hitOrdererPlayer.addCoins(-3);
				Player winnerCandidate = checkForWinner(targetPlayer);
				if(winnerCandidate != null) {
					executeWinSequence(winnerCandidate);
					return;
				}
				tableController.notifyTableWithSimpleMessage(hitOrderer+"'s hitman eliminates " + target + "'s "+eliminatedCard.getRole().name() + ".");
				advanceActivePlayer();
				sendUpdatedBoardToPlayers(true);
			} else {
				tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from "+target+": "+cardIndexRsp);
			}
		}
	}
	
	public void handleForcedHitResponse(String interruptId, int cardIndexRsp) {
		ForcedHitInterrupt forcedHitInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				forcedHitInterrupt = (ForcedHitInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(forcedHitInterrupt != null) {
			forcedHitInterrupt.setActive(false);
			String originalHitOrderer = forcedHitInterrupt.getHitOrderer();
			String forced = forcedHitInterrupt.getTarget();
			Player forcedPlayer = playerMap.get(forced); 
			Player originalHitOrdererPlayer = playerMap.get(originalHitOrderer);
			
			if(cardIndexRsp == 0 || cardIndexRsp == 1) {
				if(forcedPlayer.getCardInHand(cardIndexRsp).isEliminated()) {
					tableController.notifyTableOfUnauthorizedActivity("Player attempted to eliminate an already eliminated card: "+forced+"| "+cardIndexRsp);
					return;
				}
				forcedPlayer.eliminateCardInHand(cardIndexRsp);
				Player winnerCandidate = checkForWinner(forcedPlayer);
				if(winnerCandidate != null) {
					executeWinSequence(winnerCandidate);
					return;
				}
				originalHitOrdererPlayer.addCoins(-3);
				tableController.notifyTableWithSimpleMessage(originalHitOrderer+"'s hitman left " + forced + " broken.");
				advanceActivePlayer();
				sendUpdatedBoardToPlayers(true);
			} else {
				tableController.notifyTableOfUnauthorizedActivity("Invalid card index (not 0 or 1) from "+forced+": "+cardIndexRsp);
			}
		}
	}
	
	public void handleAcceptDefeat(String interruptId) {
		ChallengeInterrupt challengeUtterLossInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				challengeUtterLossInterrupt = (ChallengeInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(challengeUtterLossInterrupt != null) {
			challengeUtterLossInterrupt.setActive(false);
			String challenged = challengeUtterLossInterrupt.getChallenged();
			String utterlyLost = challengeUtterLossInterrupt.getChallenger();
			Player utterlyLostPlayer = playerMap.get(utterlyLost);
			Player challengedPlayer = playerMap.get(challenged);
			utterlyLostPlayer.eliminateCardInHand(0);
			utterlyLostPlayer.eliminateCardInHand(1);
			
			InterruptCase actionToResolve = challengeUtterLossInterrupt.getActionChallenged();
			switch(actionToResolve) {
				case ORDER_HIT_CHALLENGE_LOSS:
					challengedPlayer.addCoins(-3);
					break;
				default:
					tableController.notifyTableOfUnauthorizedActivity("Cannot resolve utter loss of type " + actionToResolve + " by " + utterlyLost);
					break;
			}
			
			Player winnerCandidate = checkForWinner(utterlyLostPlayer);
			if(winnerCandidate != null) {
				executeWinSequence(winnerCandidate);
				return;
			}
			int defenderIndexCardToReplace = challengeUtterLossInterrupt.getRevealedDefendingCardIndex();
			Card defendedCard = challengedPlayer.getCardInHand(challengeUtterLossInterrupt.getRevealedDefendingCardIndex());
			deck.add(defendedCard);
			deck.shuffle();
			challengedPlayer.replaceCardInHand(deck.drawOne(), defenderIndexCardToReplace);
			tableController.notifyTableWithSimpleMessage(challenged+" replaces the revealed " + defendedCard.getRole().name() + " with a new card.");
			advanceActivePlayer();
			sendUpdatedBoardToPlayers(true);
		}
	}
	
	public void handleHitCounter(String interruptId) {
		HitInterrupt hitInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				hitInterrupt = (HitInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(hitInterrupt != null) {
			hitInterrupt.setActive(false);
			String decoyId = UUID.randomUUID().toString();
			String orderer = hitInterrupt.getHitOrderer();
			String target = hitInterrupt.getTarget();
			DecoyInterrupt decoyInterrupt = new DecoyInterrupt(decoyId, INTERRUPT_WAIT_MS, InterruptCase.ORDER_HIT_COUNTER, target, orderer, eliminatedPlayerNames);
			InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(decoyId, INTERRUPT_WAIT_MS, this, InterruptCase.ORDER_HIT_COUNTER);
			interruptibles.put(decoyId, decoyInterrupt);
			
			JSONObject returnObj = buildInterruptOppRsp(target, "decoy", decoyId, target+" attempts to fool the hit with a decoy.");
			
			tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
			Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
			decoyInterrupt.setDefaultResolverFuture(defaultResolverFuture);
		}
	}
	
	public void resolveHitCounter(String interruptId) {
		DecoyInterrupt decoyInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				decoyInterrupt = (DecoyInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(decoyInterrupt != null) {
			currActivePlayer.addCoins(-3);
			tableController.notifyTableWithSimpleMessage(currActivePlayer.getName() + "'s hit killed nothing but time.");
			advanceActivePlayer();
			sendUpdatedBoardToPlayers(true);
		}
	}
	
	public void handleScrambleIdentity() {
		String scrambleId = UUID.randomUUID().toString();
		ScrambleInterrupt interrupt = new ScrambleInterrupt(scrambleId, INTERRUPT_WAIT_MS, InterruptCase.SCRAMBLE_IDENTITY, currActivePlayer.getName(), eliminatedPlayerNames);
		InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(scrambleId, INTERRUPT_WAIT_MS, this, InterruptCase.SCRAMBLE_IDENTITY);
		interruptibles.put(scrambleId, interrupt);
		
		JSONObject returnObj = buildInterruptOppRsp(currActivePlayer.getName(), "scrambleIdentity", scrambleId, currActivePlayer.getName()+" attempts to rewrite the identities of their allies.");
		
		tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
		Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
		interrupt.setDefaultResolverFuture(defaultResolverFuture);
	}
	
	public void resolveScrambleIdentity1(String interruptId) {
		ScrambleInterrupt scrambleInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				scrambleInterrupt = (ScrambleInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(scrambleInterrupt != null) {
			if(!tempHoldingSpace.isEmpty()) {
				tableController.notifyTableOfUnauthorizedActivity("Temp space for scrambleIdentity is not initially empty! Unable to proceed.");
				return;
			}
			
			scrambleInterrupt.setActive(false);
			String scrambleSelectId = UUID.randomUUID().toString();
			ScrambleInterrupt scrambleSelectInterrupt = new ScrambleInterrupt(scrambleSelectId, currActivePlayer.getName(), InterruptCase.SCRAMBLE_SELECT, eliminatedPlayerNames);
			interruptibles.put(scrambleSelectId, scrambleSelectInterrupt);
				
			List<Card> additionalCards = deck.draw(2);
			tempHoldingSpace.addAll(additionalCards);
			for(Card cardInHand : currActivePlayer.getCardsOwned()) {
				if(!cardInHand.isFaceUp() && !cardInHand.isEliminated()) {
					tempHoldingSpace.add(cardInHand);
				}
			}
			JSONArray returnList = new JSONArray(); 
			for(Card cardInLimbo : tempHoldingSpace) {
				returnList.add(cardInLimbo.toString());
			}
			JSONObject returnObj = buildScrambleSelectRsp(returnList, scrambleSelectId, "Choose which card(s) to replace your unrevealed card(s) with in the area above your player card.");
			tableController.notifyTableWithSimpleMessage(currActivePlayer.getName()+"'s allies' identities are being rewritten...");
			playerController.contactPlayerScrambleSelect(currActivePlayer.getName(), returnObj.toJSONString());
		}
	}
	
	public void resolveScrambleIdentity2(String interruptId, List<Integer> cardIndicesToKeep) {
		ScrambleInterrupt scrambleSelectInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				scrambleSelectInterrupt = (ScrambleInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(scrambleSelectInterrupt != null) {
			List<Card> currCards = currActivePlayer.getCardsOwned();
			int replaceableCount = 0;
			if(!currCards.get(1).isEliminated() && !currCards.get(1).isFaceUp()) {
				currCards.remove(1);
				replaceableCount++;
			}
			if(!currCards.get(0).isEliminated() && !currCards.get(0).isFaceUp()) {
				currCards.remove(0);
				replaceableCount++;
			}
			if(cardIndicesToKeep.size() == replaceableCount) {
				Set<Integer> dupeChecker = new HashSet<>();
				dupeChecker.addAll(cardIndicesToKeep);
				if(dupeChecker.size() != cardIndicesToKeep.size()) {
					tableController.notifyTableOfUnauthorizedActivity("Invalid input for scramble identity, duplicate values detected: "+cardIndicesToKeep.toString());
					return;
				}
				
				List<Card> cardsToAdd = new ArrayList<>();
				for(Integer indexToKeep : cardIndicesToKeep) {
					Card cardToKeep = tempHoldingSpace.get(indexToKeep.intValue());
					cardsToAdd.add(cardToKeep);
				}
				
				for(int i=0; i<cardsToAdd.size(); i++) {
					Card cardToAdd = cardsToAdd.get(i);
					tempHoldingSpace.remove(cardToAdd);
					currCards.add(cardToAdd);
				}
				currActivePlayer.updateJsonHandForExchange();
				deck.add(tempHoldingSpace);
				tempHoldingSpace.clear();
				tableController.notifyTableWithSimpleMessage(currActivePlayer.getName()+" has new allies.");
				advanceActivePlayer();
				sendUpdatedBoardToPlayers(true);
			} else {
				tableController.notifyTableOfUnauthorizedActivity("Temp space for scrambleIdentity is not initially empty! Unable to proceed.");
			}
		}
	}
	
	public void handleRaid(String targetPlayerName) {
		Player targetPlayer = playerMap.get(targetPlayerName);
		String raider = currActivePlayer.getName();
		
		if(targetPlayer != null && !targetPlayer.equals(currActivePlayer) && !targetPlayer.isLost()) {
			String raidId = UUID.randomUUID().toString();
			RaidInterrupt interrupt = new RaidInterrupt(raidId, INTERRUPT_WAIT_MS, raider, targetPlayerName, InterruptCase.RAID, eliminatedPlayerNames);
			InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(raidId, INTERRUPT_WAIT_MS, this, InterruptCase.RAID);
			interruptibles.put(raidId, interrupt);
			JSONObject returnObj = buildRaidRsp(targetPlayerName, raider, raidId, raider + " is raiding the assets of " + targetPlayerName + ".");
			
			tableController.notifyTableOfRaid(returnObj.toJSONString());
			Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
			interrupt.setDefaultResolverFuture(defaultResolverFuture);
		} else {
			tableController.notifyTableOfUnauthorizedActivity("Invalid raid target: " + targetPlayerName);
		}
	}
	
	public void resolveRaid(String interruptId) {
		RaidInterrupt raidInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				raidInterrupt = (RaidInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(raidInterrupt != null) {
			raidInterrupt.setActive(false);
			String target = raidInterrupt.getTarget();
			String raider = raidInterrupt.getRaider();
			executeRaidCoinMove(target, raider);
			tableController.notifyTableWithSimpleMessage(raider + " plunders some booty from " + target + ".");
			advanceActivePlayer();
			sendUpdatedBoardToPlayers(true);
		}
	}
	
	public void handleRaidCounter(String interruptId, Roles blockingRole) {
		RaidInterrupt raidInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				raidInterrupt = (RaidInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(raidInterrupt != null) {
			raidInterrupt.setActive(false);
			
			String blockId = UUID.randomUUID().toString();
			String raider = raidInterrupt.getRaider();
			String blocker = raidInterrupt.getTarget();
			BlockInterrupt blockInterrupt = new BlockInterrupt(blockId, INTERRUPT_WAIT_MS, InterruptCase.RAID_COUNTER, blocker, raider, blockingRole, eliminatedPlayerNames);
			InterruptDefaultResolver interruptKillswitch = new InterruptDefaultResolver(blockId, INTERRUPT_WAIT_MS, this, InterruptCase.RAID_COUNTER);
			interruptibles.put(blockId, blockInterrupt);
			
			if(Roles.NETOPS.equals(blockingRole)) {
				JSONObject returnObj = buildInterruptOppRsp(blocker, "sabotage", blockId, blocker+"'s NetOps attempts to sabotage the raiding team's comms.");
				tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
			} else {
				JSONObject returnObj = buildInterruptOppRsp(blocker, "fortify", blockId, blocker+"'s Captain prepares a counterattack on the raiding team.");
				tableController.notifyTableOfChallengeOpp(returnObj.toJSONString());
			}
			Future<?> defaultResolverFuture = interruptThreadPool.submit(interruptKillswitch);
			blockInterrupt.setDefaultResolverFuture(defaultResolverFuture);
		}
	}
	
	public void resolveRaidCounter(String interruptId) {
		BlockInterrupt blockInterrupt = null;
		synchronized(interruptibles) {
			if(interruptibles.containsKey(interruptId)) {
				blockInterrupt = (BlockInterrupt) interruptibles.get(interruptId);
				interruptibles.remove(interruptId);
			}
		}
		
		if(blockInterrupt != null) {
			if(blockInterrupt.getBlockingRole().equals(Roles.CAPTAIN)) {
				tableController.notifyTableWithSimpleMessage(currActivePlayer.getName() + "'s raid on " + blockInterrupt.getBlocker() + " is broken on its flanks.");
			} else {
				tableController.notifyTableWithSimpleMessage(blockInterrupt.getBlocker() + " hacks into " + currActivePlayer.getName() + "'s network to throw the raiders into disarray.");
			}
			advanceActivePlayer();
			sendUpdatedBoardToPlayers(true);
		}
	}
	
	public void reset(String resettingPlayer) {
		synchronized(playerMap) {
			if(roundActive && resetReady) {
				Map<String, Player> resetPlayerMap = new LinkedHashMap<>();
				
				for (int i = 0; i < orderedPlayerNames.size(); i++) {
					if(i == 0) {
						String player1Name = (String) orderedPlayerNames.get(0);
						Player player1 = playerMap.get(player1Name);
						Player resetPlayer1 = player1.cloneForReset();
						
						String player2Name = (String) orderedPlayerNames.get(1);
						Player player2 = playerMap.get(player2Name);
						Player resetPlayer2 = player2.cloneForReset();
						resetPlayer1.setNextPlayer(resetPlayer2);
						resetPlayerMap.put(player1Name, resetPlayer1);
						resetPlayerMap.put(player2Name, resetPlayer2);
					} else if(i == (orderedPlayerNames.size() - 1)) {
						String playerLastName = (String) orderedPlayerNames.get(i);
						Player playerResetLast = resetPlayerMap.get(playerLastName);
						
						String playerPenultimateName = (String) orderedPlayerNames.get(i-1);
						Player playerResetPenultimate = resetPlayerMap.get(playerPenultimateName);
						playerResetLast.setPrevPlayer(playerResetPenultimate);
					} else {
						String currPlayerName = (String) orderedPlayerNames.get(i);
						Player currResetPlayer = resetPlayerMap.get(currPlayerName);
						
						String prevPlayerName = (String) orderedPlayerNames.get(i-1);
						Player prevResetPlayer = resetPlayerMap.get(prevPlayerName);
						currResetPlayer.setPrevPlayer(prevResetPlayer);
						
						String nextPlayerName = (String) orderedPlayerNames.get(i+1);
						Player nextResetPlayer = playerMap.get(nextPlayerName).cloneForReset();
						currResetPlayer.setNextPlayer(nextResetPlayer);
						
						resetPlayerMap.put(nextPlayerName, nextResetPlayer);
					}
				}
				
				eliminatedPlayerNames = new JSONArray();
				deck = new DeckImpl();
				tempHoldingSpace = new ArrayList<>();
				currActivePlayer = null;
				interruptibles = new HashMap<>();
				resetReady = false;
				roundActive = false;
				JSONObject returnObj = buildResetMsg("Table was reset by: "+resettingPlayer);
				tableController.notifyTableOfReset(returnObj.toJSONString());
				playerMap.clear();
				playerMap.putAll(resetPlayerMap);
			}
		}
	}
	
	private void executeRaidCoinMove(String targetName, String raiderName) {
		Player targetPlayer = playerMap.get(targetName); 
		Player raiderPlayer = playerMap.get(raiderName);
		
		int targetCoins = targetPlayer.getCoins();
		if(targetCoins >= 2) {
			targetPlayer.addCoins(-2);
			raiderPlayer.addCoins(2);
		} else {
			int diff = targetCoins % 2;
			targetPlayer.addCoins(-1*diff);
			raiderPlayer.addCoins(diff);
		}
	}
	
	private void handleForcedHit(String targetPlayerName) {
		Player targetPlayer = playerMap.get(targetPlayerName);
		String orderer = currActivePlayer.getName();
		String forcedHitId = UUID.randomUUID().toString();
		ForcedHitInterrupt forcedHitInterrupt = new ForcedHitInterrupt(forcedHitId, targetPlayerName, currActivePlayer.getName(), InterruptCase.FORCED_HIT);
		interruptibles.put(forcedHitId, forcedHitInterrupt);
		try {
			int validRspIndices = getValidCardLossIndices(targetPlayer);
			JSONObject returnObj = buildHitRsp(targetPlayerName, orderer, forcedHitId, orderer+"'s hit on " + targetPlayerName + " goes through. "+ targetPlayerName + " must handle the hit.", validRspIndices);
			tableController.notifyTableOfForcedHitOrder(returnObj.toJSONString());	
		} catch (GameException e) {
			tableController.notifyTableOfUnauthorizedActivity("Targeted player should have already lost: " + targetPlayerName);
			System.err.println("Targeted player should have already lost: " + targetPlayerName);
			return;
		}
	}
	
	private void sendUpdatedBoardToPlayers(boolean activePlayerChange) { //TODO multithread this
		JSONObject returnObj = new JSONObject();
		returnObj.put("activePlayer", currActivePlayer.getName());
		for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
			String currPlayerName = playerEntry.getKey();
			JSONObject maskedPlayerJson = getMaskedPlayerMapAsJson(currPlayerName);
			returnObj.put("boardState", maskedPlayerJson);
			if(activePlayerChange) {
				playerController.contactPlayerAdvanceTable(currPlayerName, returnObj.toJSONString());
			} else {
				playerController.contactPlayerUpdateTable(currPlayerName, returnObj.toJSONString());
			}
		}
	}
	
	private boolean advanceActivePlayer() {
		Player candidateActivePlayer = currActivePlayer.getNextPlayer();
		while(candidateActivePlayer.isLost()) {
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
		if(playerAtRisk.getCardInHand(0).isEliminated() && playerAtRisk.getCardInHand(1).isEliminated()) {
			playerAtRisk.eliminatePlayer();
			eliminatedPlayerNames.add(playerAtRisk.getName());
			Player tempPlayer = null;
			boolean onePlayerNotLost = false;
			for(Entry<String, Player> playerEntry : playerMap.entrySet()) {
				boolean isCurrentPlayerLost = playerEntry.getValue().isLost();
				if(!isCurrentPlayerLost) {
					if(onePlayerNotLost && !isCurrentPlayerLost) {
						return null; //no winner yet
					} else {
						onePlayerNotLost = true;
						tempPlayer = playerEntry.getValue();
					}
				}
			}
			
			if(onePlayerNotLost) {
				return tempPlayer; //Sole survivor is winner
			} else {
				tableController.notifyTableOfUnauthorizedActivity("All players lost, should not be possible");
				return null;
			}
		} else {
			return null;
		}
	}

	private String convertSetToHtml(Set<String> playerSet) {
		StringBuilder sb = new StringBuilder();
		for(String player : playerSet) {
			sb.append("<tr><td>").append(player).append("</td></tr>");
		}
		return sb.toString();
	}
	
	private JSONObject buildInterruptOppRsp(String atRiskName, String interruptFor, String interruptId, String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("atRisk", atRiskName);
		returnObj.put("interruptFor", interruptFor);
		returnObj.put("interruptId", interruptId);
		returnObj.put("rspWindowMs", INTERRUPT_WAIT_MS);
		returnObj.put("lost", eliminatedPlayerNames);
		returnObj.put("msg", msg);
		return returnObj;
	}
	
	private JSONObject buildChallengePhase1Rsp(String challenged, String needed, String challenger, String interruptId, String msg, int validIndices) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("challenged", challenged);
		returnObj.put("needed", needed);
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
	
	private JSONObject buildVoidoutRsp(String voidedOutName, String interruptId, String msg, int validIndices) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("voided", voidedOutName);
		returnObj.put("interruptId", interruptId);
		returnObj.put("msg", msg);
		returnObj.put("valid", validIndices);
		return returnObj;
	}
	
	private JSONObject buildHitRsp(String targetName, String ordererName, String interruptId, String msg, int validIndices) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("target", targetName);
		returnObj.put("orderer", ordererName);
		returnObj.put("interruptId", interruptId);
		returnObj.put("msg", msg);
		returnObj.put("valid", validIndices);
		returnObj.put("lost", eliminatedPlayerNames);
		return returnObj;
	}
	
	private JSONObject buildUtterDefeatRsp(String defeated, String interruptId, String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("defeated", defeated);
		returnObj.put("interruptId", interruptId);
		returnObj.put("msg", msg);
		return returnObj;
	}
	
	private JSONObject buildScrambleSelectRsp(JSONArray tempCardPool, String interruptId, String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("tempCardPool", tempCardPool);
		returnObj.put("interruptId", interruptId);
		returnObj.put("msg", msg);
		return returnObj;
	}
	
	private JSONObject buildRaidRsp(String targetName, String raiderName, String interruptId, String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("target", targetName);
		returnObj.put("raider", raiderName);
		returnObj.put("interruptId", interruptId);
		returnObj.put("rspWindowMs", INTERRUPT_WAIT_MS);
		returnObj.put("msg", msg);
		returnObj.put("lost", eliminatedPlayerNames);
		return returnObj;
	}
	
	private void executeWinSequence(Player winner) {
		resetReady = true;
		sendUpdatedBoardToPlayers(false);
		JSONObject returnObj = buildWinnerRsp(winner.getName(), winner.getName()+" is the winner!");
		tableController.notifyTableOfWinner(returnObj.toJSONString());
	}
	
	private JSONObject buildResetMsg(String msg) {
		JSONObject returnObj = new JSONObject();
		returnObj.put("msg", msg);
		return returnObj;
	}
	
	private int getValidCardLossIndices(Player playerToReveal) throws GameException {
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

