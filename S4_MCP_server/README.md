# EAG Stock Analyst: Agentic Chrome Extension

A highly capable, real-time agentic stock market analysis chatbot that runs directly in your browser as a Chrome Side Panel. This project uses a **Local LLM Agent** built on the Model Context Protocol (MCP) to scrape live Indian Stock Market data (NSE/BSE), generate insights, render beautiful markdown tables, and maintain full conversational memory.

https://www.youtube.com/watch?v=r3MqiOw_jLE

---

## ✨ Key Features

- **Chrome Side Panel**: The agent lives persistently in a Chrome Side Panel, allowing you to browse the web and analyze stocks simultaneously without the window closing.
- **Real-Time Streaming**: Watch the agent's internal reasoning! The backend streams the agent's "thoughts" and tool executions (via Server-Sent Events) instantly to the UI before presenting the final answer.
- **Conversational Memory**: The backend maintains a global chat history across queries, so you can ask contextual follow-up questions (e.g., *"What is its dividend history?"*).
- **Auto-Logging & Saving**: The backend automatically acts as a stenographer, silently logging your full chat transcript to `sandbox/current_chat.txt`. You can ask the agent to *"save this chat as force.txt"*, and it will autonomously use its file tools to rename and export your history.
- **Rich Markdown UI**: Final agent responses are parsed beautifully into HTML. Data comparisons are automatically formatted into crisp tables.

---

## 🏗️ Architecture

1. **`backend.py` (The Bridge)**: A Flask HTTP server running locally on `127.0.0.1:5000`. It receives queries from the Chrome Extension, manages the global conversation memory, and uses Python Generators to stream Server-Sent Events back to the UI.
2. **`agent.py` (The Brain)**: The core LLM logic. It uses the `mcp` SDK to connect to `server.py` via `stdio`. It runs a ReAct loop, yielding its thought process step-by-step and enforcing verbatim file saving rules.
3. **`server.py` (The Hands)**: The underlying MCP tool server. It defines tools for fetching financial data (`yfinance`), comparing peers, reading news, and saving text files locally.
4. **`chrome_plugin/` (The Face)**: A Manifest V3 Chrome Extension. It uses `fetch` to read the HTTP stream, dynamically creates chat bubbles, and uses `marked.js` to render markdown.

---

## 🚀 Installation & Setup

### 1. Backend Setup
1. Clone the repository and navigate to the project root.
2. Install the necessary Python dependencies:
   ```bash
   pip install -r requirements.txt
   pip install flask
   ```
3. Start the Flask streaming backend:
   ```bash
   python backend.py
   ```
   *Note: Keep this terminal window open. The Chrome plugin requires it to function.*

### 2. Chrome Extension Setup
1. Open Google Chrome and navigate to `chrome://extensions/`.
2. Enable **Developer mode** in the top right corner.
3. Click **Load unpacked** and select the `chrome_plugin` folder located inside this repository.
4. Pin the **EAG Stock Analyst** extension to your browser toolbar.

---

## 💡 Usage

1. Click the extension icon in your Chrome toolbar. A persistent Side Panel will open on the right side of your screen.
2. Type a query into the chat box, such as:
   - *"Analyze the fundamentals of Force Motors (FORCEMOT)."*
   - *"Compare HDFCBANK and ICICIBANK."*
3. Watch the agent's reasoning stream live in the chat bubble.
4. Ask a contextual follow-up:
   - *"What is its dividend history?"*
5. Save your research:
   - *"Save our conversation to my_research.txt"*
6. Find all your exported `.txt` transcripts safely stored in the `sandbox/` directory!

---

## ⚠️ Disclaimer

This tool is for **educational and research purposes only**. Data is sourced from Yahoo Finance and may contain errors or delays. This is not financial advice. Always verify data from official NSE/BSE sources before making investment decisions.
