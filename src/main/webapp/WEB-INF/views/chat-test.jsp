<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>챌린지 채팅 테스트</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
<h1>챌린지 채팅방 테스트</h1>

<h2 id="chatRoomTitle">현재 채팅방: 없음</h2>
<div id="participantCount" style="display: none;">
    현재 접속자: <span id="participantNumber">0</span>명
</div>

<!-- 테스트용 사용자 정보 -->
<%--<input type="hidden" id="userId" value="100">--%>
<%--<input type="hidden" id="userName" value="테스트유저1">--%>

<label for="userId">유저 ID:</label>
<input type="number" id="userId">

<label for="challengeId">챌린지 ID:</label>
<input type="number" id="challengeId" value="1">
<button onclick="connect()" id="connectBtn">연결</button>
<button onclick="disconnect()" id="disconnectBtn" disabled>연결 해제</button>

<br><br>

<div id="connectionStatus">연결되지 않음</div>

<br>

<input type="text" id="messageInput" placeholder="메시지를 입력하세요" disabled>
<button onclick="sendMessage()" id="sendBtn" disabled>전송</button>

<div id="chatLog" style="margin-top:20px; border:1px solid #ccc; padding:10px; height:300px; overflow-y:auto;">
    <strong>메시지 로그:</strong>
    <div id="log"></div>
</div>

<script>
    let stompClient = null;
    let isConnected = false;
    let currentChallengeId = null;

    function getCurrentChallengeId() {
        return document.getElementById("challengeId").value.trim();
    }

    function updateConnectionStatus(status, connected) {
        const statusElement = document.getElementById("connectionStatus");
        statusElement.textContent = status;

        // 버튼 상태 업데이트
        document.getElementById("connectBtn").disabled = connected;
        document.getElementById("disconnectBtn").disabled = !connected;
        document.getElementById("messageInput").disabled = !connected;
        document.getElementById("sendBtn").disabled = !connected;

        // 접속자 수 표시/숨김
        const participantCountDiv = document.getElementById("participantCount");
        if (connected) {
            participantCountDiv.style.display = "block";
        } else {
            participantCountDiv.style.display = "none";
            updateParticipantCount(0);
        }

        isConnected = connected;
    }

    function updateParticipantCount(count) {
        const participantNumber = document.getElementById("participantNumber");
        participantNumber.textContent = count;
    }

    function connect() {
        if (isConnected) {
            alert("이미 연결되어 있습니다.");
            return;
        }

        const challengeId = getCurrentChallengeId();
        if (!challengeId) {
            alert("챌린지 ID를 입력해주세요.");
            return;
        }

        currentChallengeId = challengeId;
        updateConnectionStatus("연결 중...", false);

        const socket = new SockJS("<%= contextPath %>/ws/chat");
        stompClient = Stomp.over(socket);

        stompClient.connect({},
            function onConnect() {
                console.log("💡 STOMP 연결됨. 챌린지 ID:", currentChallengeId);
                updateConnectionStatus("챌린지 " + currentChallengeId + "에 연결됨", true);
                log("✅ STOMP 연결 완료");
                document.getElementById("chatRoomTitle").textContent = "현재 채팅방: 챌린지 " + currentChallengeId + "번";

                // 메시지 구독
                stompClient.subscribe("/topic/chat/" + currentChallengeId, function (message) {
                    try {
                        const msg = JSON.parse(message.body);
                        console.log("받은 메시지:", msg);

                        // 접속자 수 업데이트 메시지 처리
                        if (msg.type === "PARTICIPANT_COUNT") {
                            updateParticipantCount(msg.count);
                            log("👥 현재 접속자: " + msg.count + "명");
                            return;
                        }

                        // 일반 채팅 메시지 처리
                        const displayName = msg.userName || msg.userId || "Unknown";
                        let messageText = "[" + displayName + "] " + msg.message;

                        if (msg.messageType === "SYSTEM") {
                            messageText = "🔔 " + msg.message;
                        } else if (msg.messageType === "ERROR") {
                            messageText = "❌ " + msg.message;
                        }

                        log(messageText);
                    } catch (e) {
                        console.error("메시지 파싱 오류:", e);
                        log("❌ 메시지 파싱 오류: " + message.body);
                    }
                });

                // 입장 메시지 전송
                const userId = document.getElementById("userId").value;
                // const userName = document.getElementById("userName").value;

                const joinMessage = {
                    challengeId: parseInt(currentChallengeId),
                    userId: parseInt(userId),
                    // userName: userName,
                    messageType: "JOIN"
                };

                console.log("입장 메시지 전송:", joinMessage);
                stompClient.send("/app/chat/" + currentChallengeId + "/join", {}, JSON.stringify(joinMessage));
            },
            function onError(error) {
                console.error("STOMP 연결 오류:", error);
                updateConnectionStatus("연결 실패", false);
                log("❌ 연결 실패: " + error);
            }
        );
    }

    function disconnect() {
        if (stompClient !== null && isConnected) {
            stompClient.disconnect();
            stompClient = null;
            currentChallengeId = null;
            updateConnectionStatus("연결 해제됨", false);
            log("🔌 연결이 해제되었습니다.");
            document.getElementById("chatRoomTitle").textContent = "현재 채팅방: 없음";
        }
    }

    function sendMessage() {
        if (!isConnected || !stompClient) {
            alert("먼저 연결해주세요.");
            return;
        }

        const userId = document.getElementById("userId").value;
        // const userName = document.getElementById("userName").value;
        const msg = document.getElementById("messageInput").value.trim();
        const challengeId = getCurrentChallengeId();

        if (!msg) {
            alert("메시지를 입력하세요.");
            return;
        }

        // 현재 연결된 챌린지 ID와 다르면 경고
        if (challengeId !== currentChallengeId) {
            if (!confirm("현재 연결된 챌린지 ID(" + currentChallengeId + ")와 다릅니다. 현재 연결로 메시지를 전송하시겠습니까?")) {
                return;
            }
        }

        const messageData = {
            challengeId: parseInt(currentChallengeId),
            userId: parseInt(userId),
            // userName: userName,
            message: msg,
            messageType: "MESSAGE"
        };

        console.log("메시지 전송:", messageData);
        stompClient.send("/app/chat/" + currentChallengeId + "/send", {}, JSON.stringify(messageData));

        document.getElementById("messageInput").value = "";
    }

    function log(message) {
        const logDiv = document.getElementById("log");
        const p = document.createElement("p");
        const timestamp = new Date().toLocaleTimeString();
        p.textContent = "[" + timestamp + "] " + message;
        logDiv.appendChild(p);

        // 스크롤을 맨 아래로
        const chatLog = document.getElementById("chatLog");
        chatLog.scrollTop = chatLog.scrollHeight;
    }

    // Enter 키로 메시지 전송
    document.getElementById("messageInput").addEventListener("keypress", function (event) {
        if (event.key === "Enter" && !this.disabled) {
            sendMessage();
        }
    });

    // 페이지를 떠날 때 연결 해제
    window.addEventListener('beforeunload', function () {
        if (isConnected) {
            disconnect();
        }
    });

    // 초기 상태 설정
    updateConnectionStatus("연결되지 않음", false);
</script>
</body>
</html>