var stompClient = null;
var myName = null;

const twoPlayerDivNames = ["2p1"];
const threePlayerDivNames = ["3p1", "3p2"];
const fourPlayerDivNames = ["4p1", "4p2", "4p3"];
const fivePlayerDivNames = ["5p1", "5p2", "5p3", "5p4"];
const sixPlayerDivNames = ["6p1", "6p2", "6p3", "6p4", "6p5"];

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
			let msgBody = message.body;
			
			let myPlayerSpot = document.getElementById("self-player");
			initPlayerBox(myPlayerSpot, myName);
		
			break;
		default:
			console.log("Unknown case");
			console.log(message);
	}
}

let playerBumper = document.createElement("div");
playerBumper.className = "col-md-1";

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
	
	return [coinCounter, newCard1, newCard2];
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#disconnect").click(function() { disconnect(); });
	$("#join").click(function() { confirmUsername(); });
	$("#startRound").click(function() { startRound(); });
});

