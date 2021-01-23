var stompClient = null;
var myName = null;
var playerOrder = null;
var divNamesByPlayerNames = null;
var mySecret = null;
var activeDivNames = null;
var latestInterruptId = null;
var isVoidLocked = null;
var playerDivClickFuncs = [];

const twoPlayerDivNames = ["self-player", "2p1"];
const threePlayerDivNames = ["self-player", "3p1", "3p2"];
const fourPlayerDivNames = ["self-player", "4p1", "4p2", "4p3"];
const fivePlayerDivNames = ["self-player", "5p1", "5p2", "5p3", "5p4"];
const sixPlayerDivNames = ["self-player", "6p1", "6p2", "6p3", "6p4", "6p5"];

const captainImgSrc = "imgs/CaptainV3.svg";
const decoyImgSrc = "imgs/Decoy.svg";
const hitmanImgSrc = "imgs/Hitman.svg"
const mogulImgSrc = "imgs/Mogul.svg"
const netOpsImgSrc = "imgs/NetOps.svg"
const unrevealedImgSrc = "imgs/Unrevealed.svg"
const captainElimImgSrc = "imgs/CaptainElim.svg";
const decoyElimImgSrc = "imgs/DecoyElim.svg";
const hitmanElimImgSrc = "imgs/HitmanElim.svg"
const mogulElimImgSrc = "imgs/MogulElim.svg"
const netOpsElimImgSrc = "imgs/NetOpsElim.svg"

const captainHoverText = "[Captain]\nEnables [Raid]\nCounters [Raid]"
const decoyHoverText = "[Decoy]\nCounters [Order Hit]";
const hitmanHoverText = "[Hitman]\nEnables [Order Hit]";
const mogulHoverText = "[Mogul]\nEnables [Print Money]\nCounters [Crowdfund]";
const netOpsHoverText = "[NetOps]\nEnables [Scramble Identity]\nCounters [Raid]";
const unrevealedHoverText = '[Unrevealed]\nWho could it be?';

var challengeButton = document.createElement("button");
challengeButton.innerHTML = "Challenge";
challengeButton.setAttribute("id","challenge");
challengeButton.setAttribute("class","btn cntr-btn");
challengeButton.setAttribute("title","Challenge the last action performed");
challengeButton.setAttribute("type","submit");
var skipButton = document.createElement("button");
skipButton.innerHTML = "Skip";
skipButton.setAttribute("id","skip");
skipButton.setAttribute("class","btn cntr-btn");
skipButton.setAttribute("title","Declare no challenges or counters");
skipButton.setAttribute("type","submit");
var counterAsNetOpsButton = document.createElement("button");
counterAsNetOpsButton.innerHTML = "Counter Raid<br/>[NetOps]";
counterAsNetOpsButton.setAttribute("id","counterasnetops");
counterAsNetOpsButton.setAttribute("class","btn cntr-btn");
counterAsNetOpsButton.setAttribute("title","Counter a raid on yourself by claiming a NetOps card");
counterAsNetOpsButton.setAttribute("type","submit");
var counterAsCaptainButton = document.createElement("button");
counterAsCaptainButton.innerHTML = "Counter Raid<br/>[Captain]";
counterAsCaptainButton.setAttribute("id","counterascaptain");
counterAsCaptainButton.setAttribute("class","btn cntr-btn");
counterAsCaptainButton.setAttribute("title","Counter a raid on yourself by claiming a Captain card");
counterAsCaptainButton.setAttribute("type","submit");
var counterHitButton = document.createElement("button");
counterHitButton.innerHTML = "Counter Hit";
counterHitButton.setAttribute("id","counterhit");
counterHitButton.setAttribute("class","btn cntr-btn");
counterHitButton.setAttribute("title","Counter a hit on yourself by claiming a Decoy card");
counterHitButton.setAttribute("type","submit");
var counterCrowdfundButton = document.createElement("button");
counterCrowdfundButton.innerHTML = "Counter<br/>Crowdfund";
counterCrowdfundButton.setAttribute("id","countercrowdfund");
counterCrowdfundButton.setAttribute("class","btn cntr-btn");
counterCrowdfundButton.setAttribute("title","Counter a crowdfund by claiming a Mogul card");
counterCrowdfundButton.setAttribute("type","submit");
var cancelPlayerSelectButton = document.createElement("button");
cancelPlayerSelectButton.innerHTML = "Cancel";
cancelPlayerSelectButton.setAttribute("id","cancelpselect");
cancelPlayerSelectButton.setAttribute("class","btn cntr-btn");
cancelPlayerSelectButton.setAttribute("title","Cancel player selection");
cancelPlayerSelectButton.setAttribute("type","submit");
var skipNonTimedButton = document.createElement("button");
skipNonTimedButton.innerHTML = "Skip";
skipNonTimedButton.setAttribute("id","skipspecial");
skipNonTimedButton.setAttribute("class","btn cntr-btn");
skipNonTimedButton.setAttribute("title","Declare no challenges or counters");
skipNonTimedButton.setAttribute("type","submit");
var challengeWhenHitTargetedButton = document.createElement("button");
challengeWhenHitTargetedButton.innerHTML = "Challenge";
challengeWhenHitTargetedButton.setAttribute("id","challengespecial");
challengeWhenHitTargetedButton.setAttribute("class","btn cntr-btn");
challengeWhenHitTargetedButton.setAttribute("title","Challenge the last action performed");
challengeWhenHitTargetedButton.setAttribute("type","submit");
var acceptDefeatButton = document.createElement("button");
acceptDefeatButton.innerHTML = "Accept Defeat";
acceptDefeatButton.setAttribute("id","acceptdefeat");
acceptDefeatButton.setAttribute("class","btn cntr-btn");
acceptDefeatButton.setAttribute("title","Accept utter defeat");
acceptDefeatButton.setAttribute("type","submit");
var submitScrambleButton = document.createElement("button");
submitScrambleButton.innerHTML = "Submit";
submitScrambleButton.setAttribute("id","submitscramble");
submitScrambleButton.setAttribute("class","btn cntr-btn");
submitScrambleButton.setAttribute("title","Lock-in the card(s) to keep");
submitScrambleButton.setAttribute("type","submit");
submitScrambleButton.disabled = true;

var progressBarParent = document.createElement("div");
progressBarParent.className = "progress";
progressBarParent.id = "singleton-progress-bar";
var progressBarChild = document.createElement("div");
progressBarChild.className = "progress-bar progress-bar-striped progress-bar-animated";
progressBarChild.setAttribute("role", "progressbar");
progressBarChild.setAttribute("aria-valuemin", "0");
progressBarChild.setAttribute("aria-valuemax", "100");
progressBarParent.appendChild(progressBarChild);

function enableJoin(){
	$("#name").prop("disabled", false);
	$("#join").prop("disabled", false);
}

function enableDC(){
	$("#name").prop("disabled", true);
	$("#disconnect").prop("disabled", false);
}

function setConnected(connected) {
    if (connected) {
		$("#join").prop("disabled", true);
    	setTimeout(enableDC, 1000);
    }
    else {
		$("#disconnect").prop("disabled", true);
    	setTimeout(enableJoin, 1000);
		$("#players").html("<tr><td>Disconnected</td></tr>");
    }
}

function connect(playerName) {
    var socket = new SockJS("/coup");
    stompClient = Stomp.over(socket);
//	stompClient.debug = function(str) {};
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log("Connected: " + frame);
        stompClient.subscribe("/topic/lobbyevents", function (message) {
            reactLobbyEvent(message);
        });
		stompClient.subscribe("/queue/" + playerName, function (message) {
            reactPersonalEvent(message);
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
		stompClient.send("/app/lobbyleave", {}, myName);
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function confirmUsername(){
	candidateName = $("#name")[0].value.replace(/[^a-z0-9áéíóúñü \.,_-]/gim,"");
	airlockResponse = $.get(
		"/airlock", 
		{"candidateName": candidateName}, 
		function(rsp){
			if(rsp.code === 0) {
    			myName = candidateName;
				$.get("/lobby");
				$("#players").html(rsp.msg);
				mySecret = rsp.secret;
				connect(myName);
			} else {
				$("#players").html(rsp.msg);
			}
		}, 
		"json")
	.body;
}

function startRound() {
	stompClient.send("/app/roundstart", {}, myName);
}

function reactLobbyEvent(message) {
	let headerCase = message.headers.case;
	switch(headerCase){
		case "playerchange":
			$("#players").html(message.body);
			break;
		case "unauthorized":
			$("#log-window").html(message.body);
			break;
		case "fail": {
				let failMsg = JSON.parse(message.body);
				$("#log-window").html(failMsg.msg);
			}
			break;
		case "simpmsg":{
				let center = document.getElementById("cell-2-3");
				center.innerHTML = message.body;
			}
			break;
		case "groupcounteropp": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				let topCenter = document.getElementById("cell-2-2");
				
				let groupCounterMsg = JSON.parse(message.body);
				let playerToCounter = groupCounterMsg.atRisk;
				let rspWindowMs = groupCounterMsg.rspWindowMs;
				
				progressBarChild.setAttribute("aria-valuenow", "100");
				progressBarChild.style.width = "100%";
				progressBarChild.innerText = (rspWindowMs / 1000) + "secs";
				
				if(myName !== playerToCounter){
					latestInterruptId = groupCounterMsg.interruptId;
					let actionToCounter = groupCounterMsg.interruptFor;
					let bottomRight = document.getElementById("cell-5-3");
					center.innerHTML = groupCounterMsg.msg;
					switch(actionToCounter){
						case "crowdfund":
							bottomRight.appendChild(counterCrowdfundButton);
							bottomRight.appendChild(skipButton);
							break;
						default:
							console.error("Unknown action to counter: "+actionToCounter); 
					}
				} else {
					center.innerHTML = "Waiting for other players...";
				}
				topCenter.appendChild(progressBarParent);
				updateProgressBar(100, 0, rspWindowMs, groupCounterMsg.interruptId);
			}
			break;
		case "challengeopp": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				let topCenter = document.getElementById("cell-2-2");
				
				let groupCounterMsg = JSON.parse(message.body);
				let playerToChallenge = groupCounterMsg.atRisk;
				let rspWindowMs = groupCounterMsg.rspWindowMs;
				latestInterruptId = groupCounterMsg.interruptId;
				
				progressBarChild.setAttribute("aria-valuenow", "100");
				progressBarChild.style.width = "100%";
				progressBarChild.innerText = (rspWindowMs / 1000) + "secs";
				
				if(myName !== playerToChallenge){
					let actionToChallenge = groupCounterMsg.interruptFor;
					let bottomRight = document.getElementById("cell-5-3");
					center.innerHTML = groupCounterMsg.msg;
					switch(actionToChallenge){
						case "crowdfundCounter":
						case "printMoney":
						case "decoy":
						case "scrambleIdentity":
						case "sabotage":
						case "fortify":
							bottomRight.appendChild(challengeButton);
							bottomRight.appendChild(skipButton);
							break;
						default:
							console.error("Unknown action to counter: "+actionToChallenge); 
					}
				} else {
					center.innerHTML = "Waiting for other players...";
				}
				topCenter.appendChild(progressBarParent);
				updateProgressBar(100, 0, rspWindowMs, groupCounterMsg.interruptId);
			}
			break;
		case "challenge": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				
				let challengeMsg = JSON.parse(message.body);
				let challenged = challengeMsg.challenged;
				let validIndices = challengeMsg.valid;
				let neededAlly = challengeMsg.needed;
				
				if(myName === challenged){
					latestInterruptId = challengeMsg.interruptId;
					center.innerHTML = "You have been challenged to show a " + neededAlly + ". Select a card in your hand to reveal.";
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCardsChallenge1(card1Div, card2Div, validIndices);
				} else {
					center.innerHTML = challengeMsg.msg;
				}
			}
			break;
		case "challengeloss": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				
				let challengeMsg = JSON.parse(message.body);
				let challenger = challengeMsg.challenger;
				let validIndices = challengeMsg.valid;
				
				if(myName === challenger){
					latestInterruptId = challengeMsg.interruptId;
					center.innerHTML = "You lost your challenge. Select a card in your hand to lose.";
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCardsChallenge2(card1Div, card2Div, validIndices);
				} else {
					center.innerHTML = challengeMsg.msg;
				}
			}
			break;
		case "void": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				
				let voidMsg = JSON.parse(message.body);
				let voidedPlayer = voidMsg.voided;
				let validIndices = voidMsg.valid;
				
				if(myName === voidedPlayer){
					latestInterruptId = voidMsg.interruptId;
					center.innerHTML = "One of your associates has been voided. Select a card in your hand to lose.";
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCardsVoidout(card1Div, card2Div, validIndices);
				} else {
					center.innerHTML = voidMsg.msg;
				}
			}
			break;
		case "roundend": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				let roundEndMsg = JSON.parse(message.body);
				center.innerHTML = roundEndMsg.msg;
				let actBtns = document.getElementsByClassName("act-btn");
				for(let btnIdx=0; btnIdx < actBtns.length; btnIdx++){
					actBtns[btnIdx].disabled =  true;
				}
				disableActionButtons();
			}
			break;
		case "hitorder": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				
				let hitOrderMsg = JSON.parse(message.body);
				let targetPlayer = hitOrderMsg.target;
				let ordererPlayer = hitOrderMsg.orderer;
				let validIndices = hitOrderMsg.valid;
				let bottomRight = document.getElementById("cell-5-3");
				
				if(myName === targetPlayer){
					latestInterruptId = hitOrderMsg.interruptId;
					center.innerHTML = "A hitman has come to collect a bounty on one of your associates. Select a card in your hand to lose or a counter response.";
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCardsHitOrder(card1Div, card2Div, validIndices);
					bottomRight.appendChild(challengeWhenHitTargetedButton);
					bottomRight.appendChild(counterHitButton);
				} else if(myName === ordererPlayer) {
					center.innerHTML = "Waiting for other players..."
				} else {
					latestInterruptId = hitOrderMsg.interruptId;
					center.innerHTML = hitOrderMsg.msg;
					bottomRight.appendChild(skipNonTimedButton);
					bottomRight.appendChild(challengeButton);
				}
			}
			break;
		case "utterdefeat": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				
				let utterDefeatMsg = JSON.parse(message.body);
				let defeated = utterDefeatMsg.defeated;
				
				if(myName === defeated){
					latestInterruptId = utterDefeatMsg.interruptId;
					let bottomRight = document.getElementById("cell-5-3");
					center.innerHTML = "The hitman was the real deal, you must now accept defeat.";
					bottomRight.appendChild(acceptDefeatButton);
				} else {
					center.innerHTML = utterDefeatMsg.msg;
				}
			}
			break;
		case "forcedhit": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				
				let forcedHitOrderMsg = JSON.parse(message.body);
				let targetPlayer = forcedHitOrderMsg.target;
				let validIndices = forcedHitOrderMsg.valid;
				
				if(myName === targetPlayer){
					latestInterruptId = forcedHitOrderMsg.interruptId;
					center.innerHTML = "The hitman was the real deal. Select a card in your hand to lose or a counter response.";
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCardsHitOrder(card1Div, card2Div, validIndices);
				} else {
					center.innerHTML = forcedHitOrderMsg.msg;
				}
			}
			break;
		case "raid": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				let topCenter = document.getElementById("cell-2-2");
				
				let raidMsg = JSON.parse(message.body);
				let targetPlayer = raidMsg.target;
				let raiderPlayer = raidMsg.raider;
				let bottomRight = document.getElementById("cell-5-3");
				let rspWindowMs = raidMsg.rspWindowMs;
				latestInterruptId = raidMsg.interruptId;
				
				progressBarChild.setAttribute("aria-valuenow", "100");
				progressBarChild.style.width = "100%";
				progressBarChild.innerText = (rspWindowMs / 1000) + "secs";
				
				if(myName === targetPlayer){
					center.innerHTML = "Your booty is about to be plundered, select a response.";
					bottomRight.appendChild(challengeButton);
					bottomRight.appendChild(counterAsNetOpsButton); 
					bottomRight.appendChild(counterAsCaptainButton);
					bottomRight.appendChild(skipButton);
				} else if(myName === raiderPlayer) {
					center.innerHTML = "Waiting for other players...";
				} else {
					center.innerHTML = raidMsg.msg;
					bottomRight.appendChild(challengeButton);
					bottomRight.appendChild(skipButton); 
				}
				
				topCenter.appendChild(progressBarParent);
				updateProgressBar(100, 0, rspWindowMs, raidMsg.interruptId);
			}
			break;
		default:
			console.error("Unknown lobby header");
			console.error(message);
	}
}

function updateProgressBar(updateIntervalMs, elapsedMs, maxMs, interruptId){
	let newElapsed = elapsedMs + updateIntervalMs;
	let percentRemaining = ((maxMs - newElapsed) / maxMs) * 100;
	progressBarChild.style.width = percentRemaining+"%";
	progressBarChild.setAttribute("aria-valuenow", percentRemaining);
	progressBarChild.innerText = ((maxMs - newElapsed) / 1000) + "secs";
	if(parseInt(percentRemaining) >= 0 && interruptId === latestInterruptId){
		setTimeout(function(){
			updateProgressBar(updateIntervalMs, newElapsed, maxMs, interruptId);
		}, updateIntervalMs);	
	} else if(latestInterruptId === ""){
		cleanupInterrupt();
	}
}

function reactPersonalEvent(message){
	let headerCase = message.headers.case;
	switch(headerCase){
		case "init": {
				document.getElementById("startRound").disabled = true;
				let tableStarter = JSON.parse(message.body);
				let myPlayerSpot = document.getElementById("self-player");
				let myPlayerSpotHotElems = initPlayerBox(myPlayerSpot, myName);
				updatePlayerPieces(myPlayerSpotHotElems, tableStarter.boardState[myName]);
				let masterPlayerOrder = JSON.parse(message.headers.order);
				initPlayers(tableStarter.boardState, masterPlayerOrder);
				handleActivePlayer(tableStarter.activePlayer, tableStarter.boardState);
			}
			break;
		case "update": {
				let tableState = JSON.parse(message.body);
				updateGamePieces(tableState.boardState);
			}
			break;
		case "advance": {
				latestInterruptId = "";
				let tableState = JSON.parse(message.body);
				updateGamePieces(tableState.boardState);
				handleActivePlayer(tableState.activePlayer, tableState.boardState);
				cleanupInterrupt();
			}
			break;
		case "targets": {
				let targetMsg = JSON.parse(message.body);
				let nameList = targetMsg.targets;
				let action = targetMsg.action;
				if(action === "orderhit"){
					for(let nameIndex = 0; nameIndex < nameList.length; nameIndex++){
						let selectableName = nameList[nameIndex];
						let selectablePlayerDiv = document.getElementById(divNamesByPlayerNames[selectableName]);
						let playerClickFunc = orderHitFuncFactory(selectableName);
						selectablePlayerDiv.addEventListener("click", playerClickFunc, false);
						selectablePlayerDiv.classList.add("hoverable-player");
						playerDivClickFuncs.push({"pName":selectableName, "clickFunc":playerClickFunc});
					}
				} else if(action === "voidout"){
					for(let nameIndex = 0; nameIndex < nameList.length; nameIndex++){
						let selectableName = nameList[nameIndex];
						let selectablePlayerDiv = document.getElementById(divNamesByPlayerNames[selectableName]);
						let playerClickFunc = voidOutFuncFactory(selectableName);
						selectablePlayerDiv.addEventListener("click", playerClickFunc, false);
						selectablePlayerDiv.classList.add("hoverable-player");
						playerDivClickFuncs.push({"pName":selectableName, "clickFunc":playerClickFunc});
					}
				} else if(action === "raid"){
					for(let nameIndex = 0; nameIndex < nameList.length; nameIndex++){
						let selectableName = nameList[nameIndex];
						let selectablePlayerDiv = document.getElementById(divNamesByPlayerNames[selectableName]);
						let playerClickFunc = raidFuncFactory(selectableName);
						selectablePlayerDiv.addEventListener("click", playerClickFunc, false);
						selectablePlayerDiv.classList.add("hoverable-player");
						playerDivClickFuncs.push({"pName":selectableName, "clickFunc":playerClickFunc});
					}
				}
				let center = document.getElementById("cell-2-3");
				center.innerHTML = targetMsg.msg;
				
				let bottomRight = document.getElementById("cell-5-3");
				bottomRight.appendChild(cancelPlayerSelectButton);
			}
			break;
		case "scrambleselect": {
				let scrambleMsg = JSON.parse(message.body);
				let tempCardPool = scrambleMsg.tempCardPool;
				let bottomCenter = document.getElementById("cell-4-2");
				let center = document.getElementById("cell-2-3");
				let bottomRight = document.getElementById("cell-5-3");
				latestInterruptId = scrambleMsg.interruptId;
				
				for(let poolIndex = 0; poolIndex < tempCardPool.length; poolIndex++) {
					let scrambleCardImgData = getCardImageData(tempCardPool[poolIndex]);
					let scrambleCardImg = getCardDomImgObj(scrambleCardImgData);
					scrambleCardImg.classList.add("checkbox-card");
					let checkboxId = "cb"+poolIndex;
					let checkbox = document.createElement("input");
					checkbox.setAttribute("name", "scrambleselect");
					checkbox.setAttribute("value", poolIndex);
					checkbox.setAttribute("type", "checkbox");
					checkbox.setAttribute("id", checkboxId);
					checkbox.className = "scramble-checkbox";
					
					let cbLabelId = "cbl"+poolIndex;
					let cbLabel = document.createElement("label");
					let maxCount = 1;
					if(tempCardPool.length === 3) {
						cbLabel.className = "col-md-4 scramble-card";
						maxCount = 1;
					} else if(tempCardPool.length === 4) {
						cbLabel.className = "col-md-3 scramble-card";
						maxCount = 2;
					} else {
						$("#log-window").html("Invalid scramble data, incorrect number of cards: "+tempCardPool.length);
						console.error("Invalid scramble data, incorrect number of cards: "+tempCardPool.length);
						return;
					}
					cbLabel.setAttribute("for", checkboxId);
					cbLabel.appendChild(scrambleCardImg);
					cbLabel.setAttribute("id", cbLabelId);
					let clickFunc = handleClickScrambleFactory(checkboxId, cbLabelId, maxCount);
					checkbox.addEventListener("change", clickFunc);
					bottomCenter.appendChild(checkbox);
					bottomCenter.appendChild(cbLabel);
					
					center.innerHTML = scrambleMsg.msg;
					bottomRight.appendChild(submitScrambleButton);
				}
			}
			break;
		default:
			console.error("Unknown case");
			console.error(message);
	}
}

function updateGamePieces(boardState){
	if(activeDivNames === null){
		console.error("Unable to update, activeDivNames not initialized");
		return;
	} else {
		let playerNamesFromKeys = Object.keys(boardState);
		for(let keyIndex = 0; keyIndex < playerNamesFromKeys.length; keyIndex++){
			let currPlayerName = playerNamesFromKeys[keyIndex]
			let currPlayerDiv = document.getElementById(divNamesByPlayerNames[currPlayerName]);
			let currPlayerHotElems = [currPlayerDiv.getElementsByClassName("coin-counter")[0],
										currPlayerDiv.getElementsByClassName("card-1")[0],
										currPlayerDiv.getElementsByClassName("card-2")[0]];
			updatePlayerPieces(currPlayerHotElems, boardState[currPlayerName]);
		}
	}
}

function initPlayerBox(playerSpot, name) {
	let nameplate = document.createElement("div");
	nameplate.className = "row nameplate";
	nameplate.innerHTML = name;
	
	let ownedPieces = document.createElement("div");
	ownedPieces.className = "row owned-pieces";
	let coinSpot = document.createElement("div");
	let coinCounter = document.createElement("div");
	let newCard1 = document.createElement("div");
	let newCard2 = document.createElement("div");
	coinSpot.className = "col-md-2 coins";
	coinCounter.className = "coin-counter"
	newCard1.className = "col-md-4 card-1";
	newCard2.className = "col-md-4 card-2";
	
	let coinImg = document.createElement("IMG");
	coinImg.setAttribute("src", "imgs/Coin.svg");
	coinImg.setAttribute("height", "50%");
	coinImg.setAttribute("width", "80%");
	coinImg.setAttribute("title", "Coins currently owned, [Void]-ing is forced when starting your turn with 10 or more coins");
	
	coinSpot.appendChild(coinCounter);
	coinSpot.appendChild(coinImg);
	
	let playerBumper1 = document.createElement("div");
	playerBumper1.className = "col-md-1 p-bumper";
	
	let playerBumper2 = document.createElement("div");
	playerBumper2.className = "col-md-1 p-bumper";
	
	ownedPieces.appendChild(playerBumper1);
	ownedPieces.appendChild(coinSpot);
	ownedPieces.appendChild(newCard1);
	ownedPieces.appendChild(newCard2);
	ownedPieces.appendChild(playerBumper2);
	
	playerSpot.appendChild(ownedPieces);
	playerSpot.appendChild(nameplate);
	
	playerSpot.className = "player-cell";
	
	return [coinCounter, newCard1, newCard2];
}

function initPlayers(tableStarter, masterPlayerOrder){
	playerOrder = [];
	divNamesByPlayerNames = new Object();
	let myPlayerIndex = masterPlayerOrder.indexOf(myName);
	let incrementCounter = 1;
	switch(masterPlayerOrder.length){
		case 2:
			activeDivNames = twoPlayerDivNames;
			break;
		case 3:
			activeDivNames = threePlayerDivNames;
			break;
		case 4:
			activeDivNames = fourPlayerDivNames;
			break;
		case 5:
			activeDivNames = fivePlayerDivNames;
			break;
		case 6:
			activeDivNames = sixPlayerDivNames;
			break;
		default:
			console.error("Invalid player count:"+masterPlayerOrder.length);
	}
	
	playerOrder.push(myName);
	divNamesByPlayerNames[myName] = "self-player";
	for(let currPlayerIndex = (myPlayerIndex + 1); currPlayerIndex !== myPlayerIndex; currPlayerIndex++){
		if(currPlayerIndex === masterPlayerOrder.length){
			if(myPlayerIndex === 0) {
				break;
			}
			currPlayerIndex = 0;
		}
		let currDivName = activeDivNames[incrementCounter];
		let currPlayerSpot = document.getElementById(currDivName);
		let currPlayerName = masterPlayerOrder[currPlayerIndex];
		playerOrder.push(currPlayerName);
		divNamesByPlayerNames[currPlayerName] = currDivName;
		
		let currPlayerSpotChildren = initPlayerBox(currPlayerSpot, currPlayerName);
		updatePlayerPieces(currPlayerSpotChildren, tableStarter[currPlayerName]);
				
		incrementCounter++;
	}
}

function updatePlayerPieces(playerDivChildren, playerObj){
	playerDivChildren[0].innerHTML = playerObj.coins;
	
	let card1ImgData = getCardImageData(playerObj.cardsOwned[0]);
	let card2ImgData = getCardImageData(playerObj.cardsOwned[1]);
	if(card1ImgData === null || card2ImgData === null){
		console.error("Error updating player state");
		return;
	}
	let currCard1 = playerDivChildren[1].firstElementChild;
	if(currCard1 !== null){
		if(currCard1.dataset.cardName !== card1ImgData[2]){
			let card1Img = getCardDomImgObj(card1ImgData);
			playerDivChildren[1].replaceChild(card1Img, playerDivChildren[1].firstElementChild)
		}
	} else {
		let card1Img = getCardDomImgObj(card1ImgData);
		playerDivChildren[1].appendChild(card1Img);
	}
	
	let currCard2 = playerDivChildren[2].firstElementChild;
	if(currCard2 !== null){
		if(currCard2.dataset.cardName !== card2ImgData[2]){
			let card2Img = getCardDomImgObj(card2ImgData);
			playerDivChildren[2].replaceChild(card2Img, playerDivChildren[2].firstElementChild)
		}
	} else {
		let card2Img = getCardDomImgObj(card2ImgData);
		playerDivChildren[2].appendChild(card2Img);
	}
}

function getCardImageData(cardName){
	switch(cardName){
		case "Captain":
			return [captainImgSrc, captainHoverText, cardName];
		case "Decoy":
			return [decoyImgSrc, decoyHoverText, cardName];
		case "Hitman":
			return [hitmanImgSrc, hitmanHoverText, cardName];
		case "Mogul":
			return [mogulImgSrc, mogulHoverText, cardName];
		case "NetOps":
			return [netOpsImgSrc, netOpsHoverText, cardName];
		case "CaptainElim":
			return [captainElimImgSrc, captainHoverText, cardName];
		case "DecoyElim":
			return [decoyElimImgSrc, decoyHoverText, cardName];
		case "HitmanElim":
			return [hitmanElimImgSrc, hitmanHoverText, cardName];
		case "MogulElim":
			return [mogulElimImgSrc, mogulHoverText, cardName];
		case "NetOpsElim":
			return [netOpsElimImgSrc, netOpsHoverText, cardName];
		case "FACEDOWN":
			return [unrevealedImgSrc, unrevealedHoverText, cardName];
		default:
			console.error("Invalid Card Name:"+cardName);
	}
}

function getCardDomImgObj(cardImgData){
	let cardImg = document.createElement("IMG");
	cardImg.setAttribute("src", cardImgData[0]);
	cardImg.setAttribute("title", cardImgData[1]);
	cardImg.setAttribute("height", "100%");
	cardImg.setAttribute("width", "100%");
	cardImg.setAttribute("data-cardName", cardImgData[2])
	return cardImg;
}

function payday() {
	disableActionButtons();
	stompClient.send("/app/payday", {"secret":mySecret, "pname":myName}, myName);
}

function crowdfund() {
	disableActionButtons();
	stompClient.send("/app/crowdfund", {"secret":mySecret, "pname":myName}, myName);
}

function printMoney() {
	disableActionButtons();
	stompClient.send("/app/printmoney", {"secret":mySecret, "pname":myName}, myName);
}

function scramble() {
	stompClient.send("/app/scramble", {"secret":mySecret, "pname":myName}, myName);
}

function selectRaidTarget() {
	disableActionButtons();
	stompClient.send("/app/raidtargets", {"secret":mySecret, "pname":myName}, myName);
}

function selectVoidOutTarget() {
	disableActionButtons();
	stompClient.send("/app/voidtargets", {"secret":mySecret, "pname":myName}, myName);
}

function selectHitTarget() {
	disableActionButtons();
	stompClient.send("/app/hittargets", {"secret":mySecret, "pname":myName}, myName);
}

function voidOutFuncFactory(playerName){
	return function () {
		let currClickWrapper = playerDivClickFuncs.pop();
		while(currClickWrapper !== undefined){
			let clickablePlayerDiv = document.getElementById(divNamesByPlayerNames[currClickWrapper.pName]);
			clickablePlayerDiv.removeEventListener("click", currClickWrapper.clickFunc, false);
			clickablePlayerDiv.classList.remove("hoverable-player");
			currClickWrapper = playerDivClickFuncs.pop();
		}
		stompClient.send("/app/voidout", 
			{"secret":mySecret, "pname":myName}, 
			JSON.stringify({"target":playerName}));
	}
}

function orderHitFuncFactory(playerName){
	return function () {
		let currClickWrapper = playerDivClickFuncs.pop();
		while(currClickWrapper !== undefined){
			let clickablePlayerDiv = document.getElementById(divNamesByPlayerNames[currClickWrapper.pName]);
			clickablePlayerDiv.removeEventListener("click", currClickWrapper.clickFunc, false);
			clickablePlayerDiv.classList.remove("hoverable-player");
			currClickWrapper = playerDivClickFuncs.pop();
		}
		stompClient.send("/app/orderhit", 
			{"secret":mySecret, "pname":myName}, 
			JSON.stringify({"target":playerName}));
	}
}

function raidFuncFactory(playerName){
	return function () {
		let currClickWrapper = playerDivClickFuncs.pop();
		while(currClickWrapper !== undefined){
			let clickablePlayerDiv = document.getElementById(divNamesByPlayerNames[currClickWrapper.pName]);
			clickablePlayerDiv.removeEventListener("click", currClickWrapper.clickFunc, false);
			clickablePlayerDiv.classList.remove("hoverable-player");
			currClickWrapper = playerDivClickFuncs.pop();
		}
		stompClient.send("/app/raid", 
			{"secret":mySecret, "pname":myName}, 
			JSON.stringify({"target":playerName}));
	}
}

function challenge(){
	cleanupInterrupt();
	stompClient.send("/app/challenge", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function challengeWhenTargeted(){
	cleanupInterrupt();
	stompClient.send("/app/challenge", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function counterCrowdfund(){
	cleanupInterrupt();
	stompClient.send("/app/crowdfundcounter", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function counterStealAsNetOps(){
	cleanupInterrupt();
	stompClient.send("/app/raidcounternops", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function counterStealAsCaptain(){
	cleanupInterrupt();
	stompClient.send("/app/raidcountercpt", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function counterHit(){
	cleanupInterrupt();
	stompClient.send("/app/hitcounter", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function skip(){
	cleanupInterrupt();
	stompClient.send("/app/skip", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function cancelTargeting(){
	let currClickWrapper = playerDivClickFuncs.pop();
	while(currClickWrapper !== undefined){
		let clickablePlayerDiv = document.getElementById(divNamesByPlayerNames[currClickWrapper.pName]);
		clickablePlayerDiv.removeEventListener("click", currClickWrapper.clickFunc, false);
		clickablePlayerDiv.classList.remove("hoverable-player");
		currClickWrapper = playerDivClickFuncs.pop();
	}
		
	reenableActionButtons();
	let bottomRight = document.getElementById("cell-5-3");
	while (bottomRight.firstChild) {
 	   bottomRight.removeChild(bottomRight.firstChild);
	}
	let center = document.getElementById("cell-2-3");
	center.innerHTML = "Targeting canceled. Please select an action.";
}

function acceptDefeat(){
	cleanupInterrupt();
	stompClient.send("/app/acceptdefeat", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId}));
	latestInterruptId = null;
}

function respondChallenge1Card1(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondChallenge1Card1);
	cardDivs[1].removeEventListener("click", respondChallenge1Card2);
	stompClient.send("/app/challengeresponse1", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
}

function respondChallenge1Card2(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondChallenge1Card1);
	cardDivs[1].removeEventListener("click", respondChallenge1Card2);
	stompClient.send("/app/challengeresponse1", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function respondChallenge2Card1(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondChallenge2Card1);
	cardDivs[1].removeEventListener("click", respondChallenge2Card2);
	stompClient.send("/app/challengeresponse2", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
	
}

function respondChallenge2Card2(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondChallenge2Card1);
	cardDivs[1].removeEventListener("click", respondChallenge2Card2);
	stompClient.send("/app/challengeresponse2", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function respondVoidCard1(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondVoidCard1);
	cardDivs[1].removeEventListener("click", respondVoidCard2);
	stompClient.send("/app/voidoutresponse", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
	
}

function respondVoidCard2(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondVoidCard1);
	cardDivs[1].removeEventListener("click", respondVoidCard2);
	stompClient.send("/app/voidoutresponse", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function respondHitCard1(){
	cleanupInterrupt();
	cleanupHitResponses();
	stompClient.send("/app/orderhitresponse", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
	
}

function respondHitCard2(){
	cleanupInterrupt();
	cleanupHitResponses();
	stompClient.send("/app/orderhitresponse", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function respondForcedHitCard1(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondForcedHitCard1);
	cardDivs[1].removeEventListener("click", respondForcedHitCard2);
	stompClient.send("/app/forcedhitresponse", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
	
}

function respondForcedHitCard2(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondForcedHitCard1);
	cardDivs[1].removeEventListener("click", respondForcedHitCard2);
	stompClient.send("/app/forcedhitresponse", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function submitScramble(){
	cleanupInterrupt();
	
	let checkboxes = document.getElementsByClassName("scramble-checkbox");
	let chosen = [];
	for(let cbIndex = 0; cbIndex < checkboxes.length; cbIndex++) {
		if(checkboxes[cbIndex].checked){
			chosen.push(parseInt(checkboxes[cbIndex].value));
		}
	}
		
	let bottomCenter = document.getElementById("cell-4-2");
	while (bottomCenter.firstChild) {
 	   bottomCenter.removeChild(bottomCenter.firstChild);
	}
	stompClient.send("/app/scrambleselect", {"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "chosen":chosen}));
}

function enableValidClickableCardsChallenge1(card1Div, card2Div, validIndices){
	switch(validIndices) {
		case 2: 
			card1Div.classList.add("hoverable-card");
			card2Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondChallenge1Card1);
			card2Div.addEventListener("click", respondChallenge1Card2);
			break;
		case 1:
			card2Div.classList.add("hoverable-card");
			card2Div.addEventListener("click", respondChallenge1Card2);
			break;
		case 0:
			card1Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondChallenge1Card1);
			break;
		default:
			console.error("Unknown validity code for challenge response:"+validIndices);
	}
}

function enableValidClickableCardsChallenge2(card1Div, card2Div, validIndices){
	switch(validIndices) {
		case 2: 
			card1Div.classList.add("hoverable-card");
			card2Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondChallenge2Card1);
			card2Div.addEventListener("click", respondChallenge2Card2);
			break;
		case 1:
			card2Div.classList.add("hoverable-card");
			card2Div.addEventListener("click", respondChallenge2Card2);
			break;
		case 0:
			card1Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondChallenge2Card1);
			break;
		default:
			console.error("Unknown validity code for challenge response:"+validIndices);
	}
}

function enableValidClickableCardsVoidout(card1Div, card2Div, validIndices){
	switch(validIndices) {
		case 2: 
			card1Div.classList.add("hoverable-card");
			card2Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondVoidCard1);
			card2Div.addEventListener("click", respondVoidCard2);
			break;
		case 1:
			card2Div.classList.add("hoverable-card");
			card2Div.addEventListener("click", respondVoidCard2);
			break;
		case 0:
			card1Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondVoidCard1);
			break;
		default:
			console.error("Unknown validity code for voidout response:"+validIndices);
	}
}

function enableValidClickableCardsHitOrder(card1Div, card2Div, validIndices){
	switch(validIndices) {
		case 2: 
			card1Div.classList.add("hoverable-card");
			card2Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondHitCard1);
			card2Div.addEventListener("click", respondHitCard2);
			break;
		case 1:
			card2Div.classList.add("hoverable-card");
			card2Div.addEventListener("click", respondHitCard2);
			break;
		case 0:
			card1Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondHitCard1);
			break;
		default:
			console.error("Unknown validity code for hit order response:"+validIndices);
	}
}

function enableValidClickableCardsForcedHitOrder(card1Div, card2Div, validIndices){
	switch(validIndices) {
		case 2: 
			card1Div.classList.add("hoverable-card");
			card2Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondForcedHitCard1);
			card2Div.addEventListener("click", respondForcedHitCard2);
			break;
		case 1:
			card2Div.classList.add("hoverable-card");
			card2Div.addEventListener("click", respondForcedHitCard2);
			break;
		case 0:
			card1Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respondForcedHitCard1);
			break;
		default:
			console.error("Unknown validity code for forced hit response:"+validIndices);
	}
}

function cleanupHitResponses(){
	let cardDivs = cleanupHoverableCardClass(); 
	cardDivs[0].removeEventListener("click", respondHitCard1);
	cardDivs[1].removeEventListener("click", respondHitCard2);
}

function cleanupHoverableCardClass(){
	let myPlayerSpot = document.getElementById("self-player");
	let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
	let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
	card1Div.classList.remove("hoverable-card");
	card2Div.classList.remove("hoverable-card");
	return [card1Div, card2Div];
}

function handleClickScrambleFactory(linkedId, labelId, maxCount) {
	return function (){
		let checkboxes = document.getElementsByClassName("scramble-checkbox");
		let checkCount = 0;
		for(let cbIndex = 0; cbIndex < checkboxes.length; cbIndex++) {
			if(checkboxes[cbIndex].checked){
				checkCount++;
			}
		}
		let thisCheckbox = document.getElementById(linkedId);
		let thisLabel = document.getElementById(labelId);
		if(thisCheckbox.checked === true) {
			if(checkCount > maxCount){
				thisCheckbox.checked = false;
				thisLabel.classList.remove("selected-scramble-card");
				submitScrambleButton.disabled = false;
				return false;
			} else if(checkCount === maxCount){
				thisLabel.classList.add("selected-scramble-card");
				submitScrambleButton.disabled = false;
				return true;
			} else {
				thisCheckbox.checked = true;
				thisLabel.classList.add("selected-scramble-card");
				submitScrambleButton.disabled = true;
				return true;
			}
		} else {
			thisLabel.classList.remove("selected-scramble-card");
			submitScrambleButton.disabled = true;
		}
	}
}


function handleActivePlayer(activePlayerName, boardState) {
	let actBtns = document.getElementsByClassName("act-btn");
	let selfIsActive = (activePlayerName === myName);
	let playerObj = boardState[myName];
	let isVoidLocked = playerObj.isVoidLocked;
	if(selfIsActive && isVoidLocked){
		for(let btnIdx=0; btnIdx < actBtns.length; btnIdx++){
			actBtns[btnIdx].disabled =  true;
		}
		let actBtnVoid = document.getElementById("void");
		actBtnVoid.disabled = false;
	} else if(selfIsActive){
		let actBtnPayday = document.getElementById("payday");
		let actBtnCrowdfund = document.getElementById("crowdfund");
		let actBtnPrintMoney = document.getElementById("printmoney");
		let actBtnScramble = document.getElementById("scramble");
		let actBtnOrderHit = document.getElementById("orderhit");
		let actBtnRaid = document.getElementById("raid");
		let actBtnVoid = document.getElementById("void");
		
		actBtnPayday.disabled = false;
		actBtnCrowdfund.disabled = false;
		actBtnPrintMoney.disabled = false;
		actBtnScramble.disabled = false;
		actBtnRaid.disabled = false;
		
		if(playerObj.coins >= 3){
			actBtnOrderHit.disabled = false;
		} else {
			actBtnOrderHit.disabled = true;
		}
		
		if(playerObj.coins >= 7){
			actBtnVoid.disabled = false;
		} else {
			actBtnVoid.disabled = true;
		}
	} else {
		for(let btnIdx=0; btnIdx < actBtns.length; btnIdx++){
			actBtns[btnIdx].disabled =  !selfIsActive;
		}
	}
	let currActiveCell = document.getElementsByClassName("active-cell");
	if(currActiveCell.length > 0){
		currActiveCell[0].classList.remove("active-cell");
	}
	let cellToActivate = document.getElementById(divNamesByPlayerNames[activePlayerName]);
	cellToActivate.classList.add("active-cell");
}

function disableActionButtons(){
	let actBtns = document.getElementsByClassName("act-btn");
	for(let btnIdx=0; btnIdx < actBtns.length; btnIdx++){
		actBtns[btnIdx].disabled = true;
	}
}

function reenableActionButtons(){
	let actBtnPayday = document.getElementById("payday");
	let actBtnCrowdfund = document.getElementById("crowdfund");
	let actBtnPrintMoney = document.getElementById("printmoney");
	let actBtnScramble = document.getElementById("scramble");
	let actBtnOrderHit = document.getElementById("orderhit");
	let actBtnRaid = document.getElementById("raid");
	let actBtnVoid = document.getElementById("void");
	
	actBtnPayday.disabled = false;
	actBtnCrowdfund.disabled = false;
	actBtnPrintMoney.disabled = false;
	actBtnScramble.disabled = false;
	actBtnRaid.disabled = false;
	
	let selfPlayerDiv = document.getElementById(divNamesByPlayerNames[myName]);
	let coinCount = parseInt(selfPlayerDiv.getElementsByClassName("coin-counter")[0].innerText);
	if(coinCount >= 3){
		actBtnOrderHit.disabled = false;
	} else {
		actBtnOrderHit.disabled = true;
	}
	if(coinCount >= 7){
		actBtnVoid.disabled = false;
	} else {
		actBtnVoid.disabled = true;
	}
}

function cleanupInterrupt(){
	let bottomRight = document.getElementById("cell-5-3");
	let topCenter = document.getElementById("cell-2-2");
	while (bottomRight.firstChild) {
 	   bottomRight.removeChild(bottomRight.firstChild);
	}
	while (topCenter.firstChild) {
 	   topCenter.removeChild(topCenter.firstChild);
	}
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#disconnect").click(function() { disconnect(); });
	$("#join").click(function() { confirmUsername(); });
	$("#startRound").click(function() { startRound(); });
	$("#payday").click(function() { payday(); });
	$("#crowdfund").click(function() { crowdfund(); });
	$("#printmoney").click(function() { printMoney(); });
	$("#scramble").click(function() { scramble(); });
	$("#orderhit").click(function() { selectHitTarget(); });
	$("#raid").click(function() { selectRaidTarget(); });
	$("#void").click(function() { selectVoidOutTarget(); });
	counterCrowdfundButton.addEventListener("click", counterCrowdfund);
	challengeButton.addEventListener("click", challenge);
	skipButton.addEventListener("click", skip);
	skipNonTimedButton.addEventListener("click", cleanupInterrupt);
	skipNonTimedButton.addEventListener("click", cleanupHitResponses);
	cancelPlayerSelectButton.addEventListener("click", cancelTargeting);
	challengeWhenHitTargetedButton.addEventListener("click", challenge);
	challengeWhenHitTargetedButton.addEventListener("click", cleanupHitResponses);
	acceptDefeatButton.addEventListener("click", acceptDefeat);
	counterHitButton.addEventListener("click", counterHit);
	counterHitButton.addEventListener("click", cleanupHitResponses);
	submitScrambleButton.addEventListener("click", submitScramble);
	counterAsNetOpsButton.addEventListener("click", counterStealAsNetOps);
	counterAsCaptainButton.addEventListener("click", counterStealAsCaptain);
	$("#testbutton").click(function() { testButton(); }); 
	$("#testbutton2").click(function() { testButton2(); });
});

//function initiateRoundReset(){
//}
//function roundReset(){
//	stompClient.send("/app/roundreset", {}, myName);
//}