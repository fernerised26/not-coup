var stompClient = null;
var myName = null;

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
	headerCase = message.headers.case;
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
	headerCase = message.headers.case;
	switch(headerCase){
		case "init":
			msgBody = message.body;
			
			coinCounter = document.createElement("div");
			newCard1 = document.createElement("div");
			newCard2 = document.createElement("div");
			coinCounter.className = "col-md-2 coin-counter";
			newCard1.className = "col-md-4 card-1";
			newCard2.className = "col-md-4 card-2";
			
			myPlayerSpot = document.getElementById("self-player");
			fillPlayerBox(coinCounter, newCard1, newCard2);
		
			break;
		default:
			console.log("Unknown case");
			console.log(message);
	}
}

var playerBumper = document.createElement("div");
playerBumper.className = "col-md-1";

function fillPlayerBox(playerSpot, coinEle, card1Ele, card2Ele) {
	playerSpot.appendChild(playerBumper);
	playerSpot.appendChild(coinEle);
	playerSpot.appendChild(card1Ele);
	playerSpot.appendChild(card2Ele);
	playerSpot.appendChild(playerBumper);
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $("#disconnect").click(function() { disconnect(); });
	$("#join").click(function() { confirmUsername(); });
	$("#startRound").click(function() { startRound(); });
});

