let messagesState = []; // Holds the server's message history

const chatContainer = document.getElementById('chat-container');
const userInput = document.getElementById('user-input');
const sendBtn = document.getElementById('send-btn');

function appendUserMessage(text) {
    const div = document.createElement('div');
    div.className = 'message user';
    div.innerHTML = `
        <div class="avatar">👤</div>
        <div class="bubble">${escapeHtml(text)}</div>
    `;
    chatContainer.appendChild(div);
    scrollToBottom();
}

function appendAgentAnswer(text) {
    const div = document.createElement('div');
    div.className = 'message agent';
    div.innerHTML = `
        <div class="avatar">✈️</div>
        <div class="bubble">${escapeHtml(text)}</div>
    `;
    chatContainer.appendChild(div);
    scrollToBottom();
}

function appendToolEvent(type, name, data) {
    // Group tool events sequentially
    let chainContainer = document.querySelector('.tool-chain:last-of-type');
    
    // If no chain exists or the last message was an answer, create a new chain
    if (!chainContainer || chatContainer.lastElementChild.classList.contains('message')) {
        chainContainer = document.createElement('div');
        chainContainer.className = 'tool-chain';
        chatContainer.appendChild(chainContainer);
    }

    const div = document.createElement('div');
    div.className = 'tool-event';
    
    let icon = type === 'tool_call' ? '⚙️' : '✅';
    let title = type === 'tool_call' ? `Calling tool: ${name}` : `Result: ${name}`;
    let contentStr = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data);
    
    div.innerHTML = `
        <div class="tool-title">${icon} ${title}</div>
        <div class="tool-content">${escapeHtml(contentStr)}</div>
    `;
    
    chainContainer.appendChild(div);
    scrollToBottom();
}

function showTypingIndicator() {
    const div = document.createElement('div');
    div.className = 'message agent typing-container';
    div.id = 'typing-indicator';
    div.innerHTML = `
        <div class="avatar">✈️</div>
        <div class="typing-indicator bubble" style="background: transparent; box-shadow: none; display: flex; padding: 16px;">
            <div class="dot"></div>
            <div class="dot"></div>
            <div class="dot"></div>
        </div>
    `;
    chatContainer.appendChild(div);
    scrollToBottom();
}

function hideTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    if (indicator) {
        indicator.remove();
    }
}

function scrollToBottom() {
    chatContainer.scrollTop = chatContainer.scrollHeight;
}

function escapeHtml(unsafe) {
    return unsafe
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

async function handleSend() {
    const query = userInput.value.trim();
    if (!query) return;

    // Clear input
    userInput.value = '';
    
    // Add user message to UI
    appendUserMessage(query);
    showTypingIndicator();
    
    // Call Python Backend API
    try {
        const response = await fetch('http://localhost:5000/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query: query,
                messages: messagesState
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        hideTypingIndicator();
        
        // Update our local state with the new server messages array
        messagesState = data.messages;
        
        // Process the returned events (reasoning chain)
        for (const event of data.events) {
            if (event.type === 'tool_call') {
                appendToolEvent('tool_call', event.name, event.args);
            } else if (event.type === 'tool_result') {
                appendToolEvent('tool_result', event.name, event.result);
            } else if (event.type === 'answer') {
                appendAgentAnswer(event.content);
            } else if (event.type === 'error') {
                appendAgentAnswer("Sorry, I ran into an error: " + event.content);
            }
        }
    } catch (error) {
        hideTypingIndicator();
        appendAgentAnswer("Error connecting to the local server. Make sure `python server.py` is running on port 5000.");
        console.error("Fetch error:", error);
    }
}

// Event Listeners
sendBtn.addEventListener('click', handleSend);
userInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        handleSend();
    }
});
