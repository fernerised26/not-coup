var stompClient = null;
var myName = null;
var playerOrder = null;

const twoPlayerDivNames = ["2p1"];
const threePlayerDivNames = ["3p1", "3p2"];
const fourPlayerDivNames = ["4p1", "4p2", "4p3"];
const fivePlayerDivNames = ["5p1", "5p2", "5p3", "5p4"];
const sixPlayerDivNames = ["6p1", "6p2", "6p3", "6p4", "6p5"];

const captainImgSrc = "imgs/CaptainV3.svg";
const decoyImgSrc = "imgs/Decoy.svg";
const hitmanImgSrc = "imgs/Hitman.svg"
const mogulImgSrc = "imgs/Mogul.svg"
const netOpsImgSrc = "imgs/NetOps.svg"
const unrevealedImgSrc = "imgs/Unrevealed.svg"

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
				connect(myName);
			} else {
				$("#players").html(rsp.msg);
			}
		}, 
		"json")
	.body;
}

//function joinWithName() {
//	connect();
//    stompClient.send("/app/lobbyjoin", {}, myName);
//}

function startRound() {
	stompClient.send("/app/roundstart", {}, myName);
}

function reactLobbyEvent(message) {
	let headerCase = message.headers.case;
	switch(headerCase){
		case "playerchange":
			$("#players").html(message.body);
			break;
		case "roundstart":
			$("#log-window").html(message.body.msg);
			break;
		default:
			console.log("Unknown case");
			console.log(message);
	}
}

function reactPersonalEvent(message){
	let headerCase = message.headers.case;
	switch(headerCase){
		case "init":
			let tableStarter = JSON.parse(message.body);
			
			let myPlayerSpot = document.getElementById("self-player");
			let myPlayerSpotChildren = initPlayerBox(myPlayerSpot, myName);
			updatePlayerState(myPlayerSpotChildren, tableStarter[myName]);
			let masterPlayerOrder = JSON.parse(message.headers.order);
			initPlayers(tableStarter, myName, masterPlayerOrder);
			break;
		default:
			console.log("Unknown case");
			console.log(message);
	}
}

let playerBumper = document.createElement("div");
playerBumper.className = "col-md-1 p-bumper";

function initPlayerBox(playerSpot, name) {
	let nameplate = document.createElement("div");
	nameplate.className = "row nameplate";
	nameplate.innerHTML = name;
	
	let ownedPieces = document.createElement("div");
	ownedPieces.className = "row owned-pieces";
	let coinCounter = document.createElement("div");
	let newCard1 = document.createElement("div");
	let newCard2 = document.createElement("div");
	coinCounter.className = "col-md-2 coin-counter";
	newCard1.className = "col-md-4 card-1";
	newCard2.className = "col-md-4 card-2";
	
	ownedPieces.appendChild(playerBumper);
	ownedPieces.appendChild(coinCounter);
	ownedPieces.appendChild(newCard1);
	ownedPieces.appendChild(newCard2);
	ownedPieces.appendChild(playerBumper);
	
	playerSpot.appendChild(ownedPieces);
	playerSpot.appendChild(nameplate);
	
	playerSpot.className = "player-cell";
	
	return [coinCounter, newCard1, newCard2];
}

function initPlayers(tableStarter, myName, masterPlayerOrder){
	playerOrder = [];
	let myPlayerIndex = masterPlayerOrder.indexOf(myName);
	
	let incrementCounter = 0;
	let activeDivNames = null;
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
	console.log("masterPlayerOrder: "+masterPlayerOrder);
	console.log(activeDivNames);
	console.log("myPlayerIndex:"+myPlayerIndex);
		 
	for(let currPlayerIndex = (myPlayerIndex + 1); currPlayerIndex !== myPlayerIndex; currPlayerIndex++){
		console.log("currPlayerIndex:"+currPlayerIndex);
		if(currPlayerIndex === masterPlayerOrder.length){
			currPlayerIndex = 0;
		}
		console.log("currPlayerIndex after check:"+currPlayerIndex);
		console.log("incrementCounter:"+incrementCounter);
		
		let currPlayerSpot = document.getElementById(activeDivNames[incrementCounter]);
		let currPlayerName = masterPlayerOrder[currPlayerIndex];
		playerOrder.push(currPlayerName);
		
		let currPlayerSpotChildren = initPlayerBox(currPlayerSpot, currPlayerName);
		updatePlayerState(currPlayerSpotChildren, tableStarter[currPlayerName]);
				
		incrementCounter++;
		console.log("playerOrder:"+playerOrder);
	}
	console.log("playerOrder:"+playerOrder);
}

function updatePlayerState(playerDivChildren, playerObj){
	playerDivChildren[0].innerHTML = playerObj.coins;
	let card1Img = document.createElement("IMG");
	card1Img.setAttribute("src", getCardImageSrc(playerObj.cardsOwned[0]));
	card1Img.setAttribute("height", "100%");
	card1Img.setAttribute("width", "100%");
	let card2Img = document.createElement("IMG");
	card2Img.setAttribute("src", getCardImageSrc(playerObj.cardsOwned[1]));
	card2Img.setAttribute("height", "100%");
	card2Img.setAttribute("width", "100%");
	playerDivChildren[1].appendChild(card1Img);
	playerDivChildren[2].appendChild(card2Img);
}

function getCardImageSrc(cardName){
	switch(cardName){
		case "Captain":
			return captainImgSrc;
		case "Decoy":
			return decoyImgSrc;
		case "Hitman":
			return hitmanImgSrc;
		case "Mogul":
			return mogulImgSrc;
		case "NetOps":
			return netOpsImgSrc;
		case "FACEDOWN":
			return unrevealedImgSrc;
		default:
			console.log("Invalid Card Name:"+cardName);
	}
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#disconnect").click(function() { disconnect(); });
	$("#join").click(function() { confirmUsername(); });
	$("#startRound").click(function() { startRound(); });
});

