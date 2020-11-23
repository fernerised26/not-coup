var stompClient = null;
var myName = null;

function enableJoin (){
	$("#join").prop("disabled", false);
}

function enableDC(){
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

function connect() {
    var socket = new SockJS("/coup");
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log("Connected: " + frame);
        stompClient.subscribe("/topic/lobbyevents", function (message) {
            reactLobbyEvent(message.body);
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
				connect();
			} else {
				$("#players").html(rsp.msg);
			}
		}, 
		"json")
	.body;
}

function joinWithName() {
	connect();
    stompClient.send("/app/lobbyjoin", {}, myName);
}

function reactLobbyEvent(message) {
	if(message !== "SKIP") {
    	$("#players").html(message);
	}
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#disconnect" ).click(function() { disconnect(); });
    //$( "#join" ).click(function() { joinWithName(); });
	$("#join").click(function() { confirmUsername(); });
});

