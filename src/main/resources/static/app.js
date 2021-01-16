var stompClient = null;
var myName = null;
var playerOrder = null;
var divNamesByPlayerNames = null;
var mySecret = null;
var activeDivNames = null;
var latestInterruptId = null;

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

const actBtns = document.getElementsByClassName("act-btn");

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
				let simpMsg = JSON.parse(message.body);
				let center = document.getElementById("cell-2-3");
				center.innerHTML = simpMsg.msg;
			}
			break;
		case "groupcounteropp": {
				cleanupInterrupt();
				let center = document.getElementById("cell-2-3");
				let topCenter = document.getElementById("cell-2-2");
				
				let groupCounterMsg = JSON.parse(message.body);
				let playerToCounter = groupCounterMsg.atRiskPlayer;
				let rspWindowMs = groupCounterMsg.rspWindowMs;
				
				progressBarChild.setAttribute("aria-valuenow", "100");
				progressBarChild.style.width = "100%";
				progressBarChild.innerText = (rspWindowMs / 1000) + "secs";
				
				if(myName !== playerToCounter){
					console.log("parsing group counter")
					console.log(groupCounterMsg);
					latestInterruptId = groupCounterMsg.interruptId;
					let actionToCounter = groupCounterMsg.interruptFor;
					
					console.log(latestInterruptId);
					console.log(actionToCounter);
					let bottomRight = document.getElementById("cell-5-3");
					center.innerHTML = groupCounterMsg.msg;
					switch(actionToCounter){
						case "crowdfund":
							bottomRight.appendChild(counterCrowdfundButton);
							bottomRight.appendChild(skipButton);
							break;
						default:
							console.log("Unknown action to counter: "+actionToCounter); 
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
				let playerToChallenge = groupCounterMsg.atRiskPlayer;
				let rspWindowMs = groupCounterMsg.rspWindowMs;
				
				progressBarChild.setAttribute("aria-valuenow", "100");
				progressBarChild.style.width = "100%";
				progressBarChild.innerText = (rspWindowMs / 1000) + "secs";
				
				if(myName !== playerToChallenge){
					latestInterruptId = groupCounterMsg.interruptId;
					let actionToChallenge = groupCounterMsg.interruptFor;
					let bottomRight = document.getElementById("cell-5-3");
					center.innerHTML = groupCounterMsg.msg;
					switch(actionToChallenge){
						case "crowdfundCounter":
							bottomRight.appendChild(challengeButton);
							bottomRight.appendChild(skipButton);
							break;
						default:
							console.log("Unknown action to counter: "+actionToChallenge); 
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
				
				if(myName === challenged){
					latestInterruptId = challengeMsg.interruptId;
					center.innerHTML = "You have been challenged. Select a card in your hand to reveal."
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCards(card1Div, card2Div, validIndices);
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
					center.innerHTML = "You lost your challenge. Select a card in your hand to lose."
					let myPlayerSpot = document.getElementById("self-player");
					let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
					let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
					enableValidClickableCards(card1Div, card2Div, validIndices);
				} else {
					center.innerHTML = challengeMsg.msg;
				}
			}
			break;
		case "roundend": {
			cleanupInterrupt();
			let center = document.getElementById("cell-2-3");
			let roundEndMsg = JSON.parse(message.body);
			center.innerHTML = roundEndMsg.msg;
			for(let btnIdx=0; btnIdx < actBtns.length; btnIdx++){
				actBtns[btnIdx].disabled =  true;
			}
		}
		default:
			console.log("Unknown lobby header");
			console.log(message);
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
	} else {
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
				handleActivePlayer(tableStarter.activePlayer);
			}
			break;
		case "update": {
				let tableState = JSON.parse(message.body);
				updateGamePieces(tableState.boardState);
				handleActivePlayer(tableState.activePlayer);
			}
			break;
		default:
			console.log("Unknown case");
			console.log(message);
	}
}

function updateGamePieces(boardState){
	if(activeDivNames === null){
		console.log("Unable to update, activeDivNames not initialized");
		return;
	} else {
		console.log("updateGamePieces START");
		let playerNamesFromKeys = Object.keys(boardState);
		console.log("playerNamesFromKeys: "+playerNamesFromKeys);
		for(let keyIndex = 0; keyIndex < playerNamesFromKeys.length; keyIndex++){
			console.log("KeyIndex: "+keyIndex);
			let currPlayerName = playerNamesFromKeys[keyIndex]
			console.log("currPlayerName: "+currPlayerName);
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
			console.log("Invalid player count:"+masterPlayerOrder.length);
	}
//	console.log("masterPlayerOrder: "+masterPlayerOrder);
//	console.log(activeDivNames);
//	console.log("myPlayerIndex:"+myPlayerIndex);
	
	playerOrder.push(myName);
	divNamesByPlayerNames[myName] = "self-player";
	for(let currPlayerIndex = (myPlayerIndex + 1); currPlayerIndex !== myPlayerIndex; currPlayerIndex++){
//		console.log("currPlayerIndex:"+currPlayerIndex);
		if(currPlayerIndex === masterPlayerOrder.length){
			if(myPlayerIndex === 0) {
				break;
			}
			currPlayerIndex = 0;
		}
//		console.log("currPlayerIndex after check:"+currPlayerIndex);
//		console.log("incrementCounter:"+incrementCounter);
		let currDivName = activeDivNames[incrementCounter];
		let currPlayerSpot = document.getElementById(currDivName);
		let currPlayerName = masterPlayerOrder[currPlayerIndex];
		playerOrder.push(currPlayerName);
		divNamesByPlayerNames[currPlayerName] = currDivName;
		
		let currPlayerSpotChildren = initPlayerBox(currPlayerSpot, currPlayerName);
		updatePlayerPieces(currPlayerSpotChildren, tableStarter[currPlayerName]);
				
		incrementCounter++;
//		console.log("playerOrder:"+playerOrder);
//		console.log(divNamesByPlayerNames);
	}
//	console.log("playerOrder:"+playerOrder);
//	console.log("divNamesByPlayerNames:"+divNamesByPlayerNames);
}

function updatePlayerPieces(playerDivChildren, playerObj){
	playerDivChildren[0].innerHTML = playerObj.coins;
	
	let card1ImgData = getCardImageData(playerObj.cardsOwned[0]);
	let card2ImgData = getCardImageData(playerObj.cardsOwned[1]);
	if(card1ImgData === null || card2ImgData === null){
		console.log("Error updating player state");
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
			console.log("Invalid Card Name:"+cardName);
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

//	stompClient.send("/app/gameaction", {}, myName);
//	let bottomright = document.getElementById("cell-5-3");
//	bottomright.appendChild(challengeButton);
//	bottomright.appendChild(skipButton);
//	bottomright.appendChild(counterAsNetOpsButton);
//	bottomright.appendChild(counterAsCaptainButton);
//	bottomright.appendChild(counterHitButton);
//	bottomright.appendChild(counterCrowdfundButton);

function payday() {
	stompClient.send("/app/payday", {"secret":mySecret, "pname":myName}, myName);
}

function crowdfund() {
	stompClient.send("/app/crowdfund", {"secret":mySecret, "pname":myName}, myName);
}

function printMoney() {
	stompClient.send("/app/printmoney", {"secret":mySecret, "pname":myName}, myName);
}

function scramble() {
	stompClient.send("/app/scramble", {"secret":mySecret, "pname":myName}, myName);
}

function orderHit() {
//	stompClient.send("/app/orderhit", {}, mySecret);
}

function raid() {
//	stompClient.send("/app/raid", {}, mySecret);
}

function voidOut() {
//	stompClient.send("/app/voidout", {}, mySecret);
}

function challenge(){
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
	
	latestInterruptId = null;
}

function counterStealAsCaptain(){
	cleanupInterrupt();
	
	latestInterruptId = null;
}

function counterHit(){
	cleanupInterrupt();
	
	latestInterruptId = null;
}

function enableValidClickableCards(card1Div, card2Div, validIndices){
	switch(validIndices) {
		case 2: 
			card1Div.classList.add("hoverable-card");
			card2Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respond1Card1);
			card2Div.addEventListener("click", respond1Card2);
			break;
		case 1:
			card2Div.classList.add("hoverable-card");
			card2Div.addEventListener("click", respond1Card2);
			break;
		case 0:
			card1Div.classList.add("hoverable-card");
			card1Div.addEventListener("click", respond1Card1);
			break;
		default:
			console.log("Unknown validity code for challenge response:"+validIndices);
	}
}

function respond1Card1(){
	let cardDivs = cleanupChallengeResponse(); 
	cardDivs[0].removeEventListener("click", respond1Card1);
	cardDivs[1].removeEventListener("click", respond1Card2);
	stompClient.send("/app/challengeresponse1", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
}

function respond1Card2(){
	let cardDivs = cleanupChallengeResponse(); 
	cardDivs[0].removeEventListener("click", respond1Card1);
	cardDivs[1].removeEventListener("click", respond1Card2);
	stompClient.send("/app/challengeresponse1", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function respond2Card1(){
	let cardDivs = cleanupChallengeResponse(); 
	cardDivs[0].removeEventListener("click", respond2Card1);
	cardDivs[1].removeEventListener("click", respond2Card2);
	stompClient.send("/app/challengeresponse2", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":0}));
	
}

function respond2Card2(){
	let cardDivs = cleanupChallengeResponse(); 
	cardDivs[0].removeEventListener("click", respond2Card1);
	cardDivs[1].removeEventListener("click", respond2Card2);
	stompClient.send("/app/challengeresponse2", 
		{"secret":mySecret, "pname":myName}, 
		JSON.stringify({"interruptId":latestInterruptId, "cardIndex":1}));
}

function cleanupChallengeResponse(){
	let myPlayerSpot = document.getElementById("self-player");
	let card1Div = myPlayerSpot.getElementsByClassName("card-1")[0];
	let card2Div = myPlayerSpot.getElementsByClassName("card-2")[0];
	card1Div.classList.remove("hoverable-card");
	card2Div.classList.remove("hoverable-card");
	return [card1Div, card2Div];
}

function testButton() {
	let topCenter = document.getElementById("cell-2-2");
	topCenter.innerHTML = "test1 test2";
}
function testButton2() {
	let topCenter = document.getElementById("cell-2-2");
//	topCenter.innerHTML = "";
	progressBarChild.setAttribute("aria-valuenow", "100");
	progressBarChild.style.width = "100%";
	topCenter.appendChild(progressBarParent);
}

function handleActivePlayer(activePlayerName) {
	let selfIsActive = (activePlayerName === myName);
	for(let btnIdx=0; btnIdx < actBtns.length; btnIdx++){
		actBtns[btnIdx].disabled =  !selfIsActive;
	}
	let currActiveCell = document.getElementsByClassName("active-cell");
	if(currActiveCell.length > 0){
		currActiveCell[0].classList.remove("active-cell");
	}
	let cellToActivate = document.getElementById(divNamesByPlayerNames[activePlayerName]);
	cellToActivate.classList.add("active-cell");
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
	$("#orderhit").click(function() { orderHit(); });
	$("#raid").click(function() { raid(); });
	$("#void").click(function() { voidOut(); });
	counterCrowdfundButton.addEventListener("click", counterCrowdfund);
	challengeButton.addEventListener("click", challenge);
	$("#testbutton").click(function() { testButton(); });
	$("#testbutton2").click(function() { testButton2(); });
});

//function initiateRoundReset(){
//}
//function roundReset(){
//	stompClient.send("/app/roundreset", {}, myName);
//}