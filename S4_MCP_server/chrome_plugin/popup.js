function appendMessage(role, text) {
    const chatHistory = document.getElementById('chatHistory');
    const bubble = document.createElement('div');
    bubble.className = `bubble ${role}`;
    
    if (role === 'user') {
        bubble.innerText = text;
    } else {
        // Agent bubble skeleton
        bubble.innerHTML = `
            <div class="status-indicator">
                <span class="spinner"></span> <b>Agent is thinking...</b>
            </div>
            <div class="stream-log" style="display: block;"></div>
            <div class="final-answer" style="display: none;"></div>
        `;
    }
    
    chatHistory.appendChild(bubble);
    chatHistory.scrollTop = chatHistory.scrollHeight;
    return bubble;
}

document.getElementById('submitBtn').addEventListener('click', async () => {
    const taskInput = document.getElementById('taskInput');
    const task = taskInput.value.trim();
    if (!task) return;

    const btn = document.getElementById('submitBtn');
    
    // UI Update
    btn.disabled = true;
    taskInput.value = '';
    
    appendMessage('user', task);
    const agentBubble = appendMessage('agent', '');
    
    const statusIndicator = agentBubble.querySelector('.status-indicator');
    const streamLog = agentBubble.querySelector('.stream-log');
    const finalAnswer = agentBubble.querySelector('.final-answer');

    try {
        const response = await fetch('http://127.0.0.1:5000/api/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ task: task })
        });
        
        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            
            const chunk = decoder.decode(value, {stream: true});
            const lines = chunk.split('\n');
            
            for (let line of lines) {
                if (line.startsWith('data: ')) {
                    const dataStr = line.replace('data: ', '').trim();
                    if (!dataStr) continue;
                    
                    try {
                        const data = JSON.parse(dataStr);
                        if (data.status === 'stream') {
                            if (data.message.startsWith('FINAL_ANSWER_PAYLOAD:')) {
                                const answerText = data.message.replace('FINAL_ANSWER_PAYLOAD:', '');
                                finalAnswer.innerHTML = marked.parse(answerText);
                                finalAnswer.style.display = 'block';
                                streamLog.style.display = 'none'; // hide thoughts when final answer arrives
                            } else {
                                streamLog.innerText += `> ${data.message}\n`;
                                streamLog.scrollTop = streamLog.scrollHeight;
                                agentBubble.scrollIntoView({ behavior: "smooth", block: "end" });
                            }
                        } else if (data.status === 'success') {
                            statusIndicator.innerHTML = `<span style="color: green;">✔ ${data.message}</span>`;
                        } else if (data.status === 'error') {
                            statusIndicator.innerHTML = `<span style="color: red;">✖ Error: ${data.message}</span>`;
                            streamLog.style.display = 'block';
                        }
                    } catch(e) {
                        console.error("Parse error", e);
                    }
                }
            }
        }
    } catch (error) {
        statusIndicator.innerHTML = `<span style="color: red;">✖ Connection Error: Could not reach backend.</span>`;
        console.error(error);
    } finally {
        btn.disabled = false;
        // Wait for next input
    }
});
