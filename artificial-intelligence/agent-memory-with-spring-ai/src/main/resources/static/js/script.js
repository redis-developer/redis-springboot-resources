// Variables to store the current state
let currentTab = 'episodic';
let currentUserId = '';

// Document ready function
document.addEventListener('DOMContentLoaded', function() {
    // Initialize tab functionality
    initializeTabs();

    // Initialize chat functionality
    initializeChat();
});

// Function to initialize tab functionality
function initializeTabs() {
    const tabs = document.querySelectorAll('.memory-tab');

    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

// Function to switch tabs
function switchTab(tabId) {
    // Update current tab
    currentTab = tabId;

    // Remove active class from all tabs
    document.querySelectorAll('.memory-tab').forEach(tab => {
        tab.classList.remove('active');
    });

    // Hide all memory lists
    document.querySelectorAll('.memory-list').forEach(list => {
        list.style.display = 'none';
    });

    // Add active class to selected tab
    document.querySelector(`.memory-tab[data-tab="${tabId}"]`).classList.add('active');

    // Show the selected memory list
    const listId = `${tabId}-memories`;
    const list = document.getElementById(listId);
    if (list) {
        list.style.display = 'flex';
    }
}

// Function to fetch and display memories
function fetchAndDisplayMemories() {
    if (!currentUserId) {
        console.log('No user ID set, skipping memory fetch');
        return;
    }

    // Fetch all memories for the current user
    const url = `/api/memory/retrieve?&userId=${encodeURIComponent(currentUserId)}`;

    fetch(url)
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        console.log('Retrieved memories:', data);

        // Separate memories by type
        const episodicMemories = data.filter(memory => memory.memoryType === 'EPISODIC');
        const semanticMemories = data.filter(memory => memory.memoryType === 'SEMANTIC');

        // Update episodic memories list
        updateMemoryList('episodic-memories', episodicMemories);

        // Update semantic memories list
        updateMemoryList('semantic-memories', semanticMemories);
    })
    .catch(error => {
        console.error('Error retrieving memories:', error);
        showError('episodic-memories', `Error retrieving memories: ${error.message}`);
        showError('semantic-memories', `Error retrieving memories: ${error.message}`);
    });
}

// Function to update a memory list
function updateMemoryList(listId, memories) {
    const listContainer = document.getElementById(listId);

    if (!memories || memories.length === 0) {
        listContainer.innerHTML = `<div class="no-memories">No ${listId.split('-')[0]} memories yet. Start chatting to create memories.</div>`;
        return;
    }

    let html = '';

    // Sort memories by creation date (newest first)
    memories.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    memories.forEach(memory => {
        html += `
            <div class="memory-card">
                <div class="memory-type ${memory.memoryType.toLowerCase()}">${memory.memoryType}</div>
                <div class="memory-content-text">${memory.content}</div>
                <div class="memory-timestamp">Created: ${formatDate(memory.createdAt)}</div>
            </div>
        `;
    });

    listContainer.innerHTML = html;
}

// Helper function to show error messages
function showError(containerId, message) {
    const container = document.getElementById(containerId);
    container.innerHTML = `<div class="error-message">${message}</div>`;
}

// Helper function to format dates
function formatDate(dateString) {
    if (!dateString) return '';

    try {
        const date = new Date(dateString);
        return date.toLocaleString();
    } catch (e) {
        return dateString;
    }
}

// Function to initialize chat functionality
function initializeChat() {
    const sendButton = document.getElementById('send-message-btn');
    const messageInput = document.getElementById('user-message');
    const chatMessages = document.getElementById('chat-messages');
    const startChatButton = document.getElementById('start-chat-btn');
    const clearChatButton = document.getElementById('clear-chat-btn');
    const userIdInput = document.getElementById('chat-user-id');

    // Start chat button click handler
    startChatButton.addEventListener('click', function() {
        const userId = userIdInput.value.trim();
        if (!userId) {
            alert('Please enter a user ID to start the chat.');
            return;
        }

        // Set the current user ID
        currentUserId = userId;

        // Enable the message input and send button
        messageInput.disabled = false;
        sendButton.disabled = false;

        // Focus on the message input
        messageInput.focus();

        // Load existing conversation history
        fetch(`/api/chat/history?userId=${encodeURIComponent(userId)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(history => {
            console.log('Loaded conversation history:', history);

            if (history && history.length > 0) {
                // Clear the chat messages
                chatMessages.innerHTML = '';

                // Display the conversation history
                history.forEach(message => {
                    if (message.role === 'system') {
                        chatMessages.innerHTML += `
                            <div class="system-message">
                                <p>${message.content}</p>
                            </div>
                        `;
                    } else if (message.role === 'user') {
                        chatMessages.innerHTML += `
                            <div class="user-message">
                                <p>${message.content}</p>
                            </div>
                        `;
                    } else if (message.role === 'assistant') {
                        chatMessages.innerHTML += `
                            <div class="assistant-message">
                                <p>${message.content}</p>
                            </div>
                        `;
                    }
                });
            } else {
                // No history, show welcome message
                chatMessages.innerHTML = `
                    <div class="system-message">
                        <p>Welcome to the Travel Assistant! I can help you plan your trips and provide personalized recommendations. How can I assist you today?</p>
                    </div>
                `;
            }

            // Scroll to the bottom of the chat
            chatMessages.scrollTop = chatMessages.scrollHeight;
        })
        .catch(error => {
            console.error('Error loading conversation history:', error);

            // Show welcome message in case of error
            chatMessages.innerHTML = `
                <div class="system-message">
                    <p>Welcome to the Travel Assistant! I can help you plan your trips and provide personalized recommendations. How can I assist you today?</p>
                </div>
                <div class="system-message error-message">
                    <p>Error loading conversation history: ${error.message}</p>
                </div>
            `;
        });

        // Fetch and display memories for this user
        fetchAndDisplayMemories();
    });

    // Clear chat button click handler
    clearChatButton.addEventListener('click', function() {
        if (!currentUserId) {
            alert('Please enter a user ID first.');
            return;
        }

        // Clear the conversation history on the server
        fetch(`/api/chat/history?userId=${encodeURIComponent(currentUserId)}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Conversation cleared:', data);

            // Clear the chat messages
            chatMessages.innerHTML = `
                <div class="system-message">
                    <p>Conversation cleared. How can I assist you today?</p>
                </div>
            `;
        })
        .catch(error => {
            console.error('Error clearing conversation:', error);
            alert(`Error clearing conversation: ${error.message}`);
        });
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
        const userId = userIdInput.value.trim();

        if (!message) {
            return;
        }

        if (!userId) {
            alert('Please enter a user ID first.');
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
        fetch(`/api/chat/send?message=${encodeURIComponent(message)}&userId=${encodeURIComponent(userId)}`, {
            method: 'POST'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
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
                        <div class="metrics-header">Performance Metrics:</div>
                        <div class="metrics-item">Embedding time: ${data.metrics.embeddingTimeMs}ms</div>
                        <div class="metrics-item">Memory retrieval time: ${data.metrics.memoryRetrievalTimeMs}ms</div>
                        <div class="metrics-item">LLM memory extraction time: ${data.metrics.memoryExtractionTimeMs}ms</div>
                        <div class="metrics-item">Memory storage time: ${data.metrics.memoryStorageTimeMs}ms</div>
                        <div class="metrics-item">LLM time: ${data.metrics.llmTimeMs}ms</div>
                    </div>
                </div>
            `;

            // Scroll to the bottom of the chat
            chatMessages.scrollTop = chatMessages.scrollHeight;

            // Fetch and display updated memories
            setTimeout(fetchAndDisplayMemories, 500); // Small delay to allow memories to be processed
        })
        .catch(error => {
            console.error('Error sending message:', error);

            // Remove the loading indicator
            const loadingElement = document.getElementById(loadingId);
            if (loadingElement) {
                loadingElement.remove();
            }

            // Add error message to the chat
            chatMessages.innerHTML += `
                <div class="system-message error-message">
                    <p>Error sending message: ${error.message}</p>
                </div>
            `;

            // Scroll to the bottom of the chat
            chatMessages.scrollTop = chatMessages.scrollHeight;
        });
    }

    // Initially disable the message input and send button
    messageInput.disabled = true;
    sendButton.disabled = true;
}

// Add CSS class for error messages and metrics
document.addEventListener('DOMContentLoaded', function() {
    const style = document.createElement('style');
    style.textContent = `
        .error-message {
            color: #d92b2b;
            padding: 10px;
            background-color: #ffebeb;
            border-radius: 4px;
            margin-top: 10px;
        }

        .loading {
            padding: 10px;
            text-align: center;
            color: #666;
        }

        .no-results {
            padding: 10px;
            text-align: center;
            color: #666;
            font-style: italic;
        }

        .loading-message {
            opacity: 0.7;
        }

        .metrics-container {
            margin-top: 8px;
            padding: 8px;
            background-color: #f5f5f5;
            border-radius: 4px;
            font-size: 0.85em;
            color: #666;
            border-left: 3px solid #dc382c;
        }

        .metrics-header {
            font-weight: bold;
            margin-bottom: 4px;
            color: #333;
        }

        .metrics-item {
            margin: 2px 0;
        }
    `;
    document.head.appendChild(style);
});
