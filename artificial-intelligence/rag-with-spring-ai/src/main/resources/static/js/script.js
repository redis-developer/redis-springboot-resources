// Variables to store the current state
let currentChatId = '';

// Document ready function
document.addEventListener('DOMContentLoaded', function() {
    // Initialize chat functionality
    initializeChat();
});

// Function to initialize chat functionality
function initializeChat() {
    const sendButton = document.getElementById('send-message-btn');
    const messageInput = document.getElementById('user-message');
    const chatMessages = document.getElementById('chat-messages');
    const startChatButton = document.getElementById('start-chat-btn');
    const clearChatButton = document.getElementById('clear-chat-btn');

    // Start chat button click handler
    startChatButton.addEventListener('click', function() {
        // Start a new chat session
        fetch('/chat/startChat', {
            method: 'POST'
        })
        .then(response => {
            if (!response.ok) {
                const error = new Error(`HTTP error! status: ${response.status}`);
                error.response = response;
                throw error;
            }
            return response.json();
        })
        .then(data => {
            console.log('Chat started:', data);

            // Set the current chat ID
            currentChatId = data.message;

            // Enable the message input and send button
            messageInput.disabled = false;
            sendButton.disabled = false;

            // Focus on the message input
            messageInput.focus();

            // Clear previous messages
            chatMessages.innerHTML = `
                <div class="system-message">
                    <p>Welcome to the Beer Knowledge Assistant! I can help you with questions about beer products. Try asking about specific beers, their ABV, IBU, or other characteristics.</p>
                </div>
            `;
        })
        .catch(error => {
            console.error('Error starting chat:', error);

            // Handle the response
            if (error.message.includes('503')) {
                // Show popup for embeddings not ready
                error.response.json().then(data => {
                    alert(data.error || 'Embeddings are still being created. Please try again later.');
                }).catch(() => {
                    alert('Embeddings are still being created. Please try again later.');
                });
            } else {
                // Show error message in chat
                chatMessages.innerHTML += `
                    <div class="system-message error-message">
                        <p>Error starting chat: ${error.message}</p>
                    </div>
                `;
            }
        });
    });

    // Clear chat button click handler
    clearChatButton.addEventListener('click', function() {
        // Reset the chat
        currentChatId = '';

        // Disable the message input and send button
        messageInput.disabled = true;
        sendButton.disabled = true;

        // Clear the chat messages
        chatMessages.innerHTML = `
            <div class="system-message">
                <p>Chat cleared. Click "Start New Chat" to begin a new conversation.</p>
            </div>
        `;
    });

    // Send button click handler
    sendButton.addEventListener('click', function() {
        sendMessage();
    });

    // Enter key press handler
    messageInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // Function to send a message
    function sendMessage() {
        const message = messageInput.value.trim();

        if (!message) {
            return;
        }

        if (!currentChatId) {
            alert('Please start a new chat first.');
            return;
        }

        // Add user message to the chat
        chatMessages.innerHTML += `
            <div class="user-message">
                <p>${message}</p>
            </div>
        `;

        // Clear the message input
        messageInput.value = '';

        // Scroll to the bottom of the chat
        chatMessages.scrollTop = chatMessages.scrollHeight;

        // Add loading indicator
        const loadingId = 'loading-' + Date.now();
        chatMessages.innerHTML += `
            <div id="${loadingId}" class="assistant-message loading-message">
                <p>Thinking...</p>
            </div>
        `;

        // Scroll to the bottom of the chat
        chatMessages.scrollTop = chatMessages.scrollHeight;

        // Send the message to the server
        fetch(`/chat/${currentChatId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ prompt: message })
        })
        .then(response => {
            if (!response.ok) {
                const error = new Error(`HTTP error! status: ${response.status}`);
                error.response = response;
                throw error;
            }
            return response.json();
        })
        .then(data => {
            console.log('Message sent:', data);

            // Remove the loading indicator
            const loadingElement = document.getElementById(loadingId);
            if (loadingElement) {
                loadingElement.remove();
            }

            // Add assistant message to the chat with metrics
            chatMessages.innerHTML += `
                <div class="assistant-message">
                    <p>${data.message}</p>
                    <div class="metrics-container">
                        <p class="metrics-title">Processing Times:</p>
                        <ul class="metrics-list">
                            <li>Embedding: ${data.metrics.embeddingTimeMs}ms</li>
                            <li>Search: ${data.metrics.searchTimeMs}ms</li>
                            <li>LLM: ${data.metrics.llmTimeMs}ms</li>
                            <li>Total: ${data.metrics.embeddingTimeMs + data.metrics.searchTimeMs + data.metrics.llmTimeMs}ms</li>
                        </ul>
                    </div>
                </div>
            `;

            // Scroll to the bottom of the chat
            chatMessages.scrollTop = chatMessages.scrollHeight;
        })
        .catch(error => {
            console.error('Error sending message:', error);

            // Remove the loading indicator
            const loadingElement = document.getElementById(loadingId);
            if (loadingElement) {
                loadingElement.remove();
            }

            // Handle the response
            if (error.message.includes('503')) {
                // Show popup for embeddings not ready
                error.response.json().then(data => {
                    alert(data.error || 'Embeddings are still being created. Please try again later.');
                }).catch(() => {
                    alert('Embeddings are still being created. Please try again later.');
                });
            } else {
                // Add error message to the chat
                chatMessages.innerHTML += `
                    <div class="system-message error-message">
                        <p>Error sending message: ${error.message}</p>
                    </div>
                `;
            }

            // Scroll to the bottom of the chat
            chatMessages.scrollTop = chatMessages.scrollHeight;
        });
    }

    // Initially disable the message input and send button
    messageInput.disabled = true;
    sendButton.disabled = true;
}
