// Client-side timer with WebSocket for real-time sync
function startCountdown(elementId, endTime) {
    const timerElement = document.getElementById(elementId);

    const updateTimer = () => {
        const now = new Date();
        const timeLeft = new Date(endTime) - now;

        if (timeLeft <= 0) {
            timerElement.textContent = 'Ended';
            return;
        }

        const days = Math.floor(timeLeft / (1000 * 60 * 60 * 24));
        const hours = Math.floor((timeLeft % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((timeLeft % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((timeLeft % (1000 * 60)) / 1000);

        timerElement.textContent = `${days}d ${hours}h ${minutes}m ${seconds}s`;
        setTimeout(updateTimer, 1000);
    };

    updateTimer();
}

// Initialize timers on page load
document.querySelectorAll('[id^="timer-"]').forEach(span => {
    let endTime = span.getAttribute('data-end-time');
    startCountdown(span.id, endTime);

    // Subscribe to WebSocket for updates (extensions, closure)
    const auctionId = span.id.split('-')[1];
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        stompClient.subscribe('/topic/auctions/' + auctionId, (message) => {
            const updatedAuction = JSON.parse(message.body);
            endTime = updatedAuction.currentEndTime;
            startCountdown(span.id, endTime);

            if (updatedAuction.status === 'Closed') {
                span.textContent = 'Ended';
            }
        });
    });
});

// Placeholder: integrate with Bid Management
function placeBid(auctionId) {
    alert('Placing bid for auction ' + auctionId);
}
