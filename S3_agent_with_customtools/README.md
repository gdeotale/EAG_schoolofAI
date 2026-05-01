# Agentic Travel Planner ✈️

An AI-powered travel planner that demonstrates a true agentic workflow. Unlike standard chatbots, this application uses a multi-step reasoning loop (Query → LLM → Tool Call → Result → LLM → Answer) to fetch real-world data before providing an answer. 

The project features a **local Python backend** powered by the Gemini API and a **Chrome Extension frontend** that visually displays the AI's internal reasoning and tool usage to the user.

## Features
- **True Agentic Workflow**: The AI autonomously decides when to use tools, executes them, and reads the results to build its final answer.
- **Chrome Extension UI**: A modern, responsive chat interface that displays the agent's reasoning chain (e.g., "Calling tool: get_weather").
- **Custom Tools**: Built-in mock tools for fetching hotels, tourist attractions, weather, and travel logistics.
- **Resilient API Calls**: Includes exponential backoff retry logic to gracefully handle API rate limits and high demand (503 errors).
- **Persistent Memory**: The backend maintains conversation history, allowing for contextual follow-up questions.

## Prerequisites
- Python 3.8+
- A Google Gemini API Key

## Setup & Installation

### 1. Configure Environment
Create a `.env` file in the root directory and add your Gemini API key:
```env
GEMINI_API_KEY=your_api_key_here
```

### 2. Install Dependencies
Install the required Python packages:
```bash
pip install google-genai flask flask-cors python-dotenv
```

### 3. Start the Backend Server
The Chrome extension communicates with a local Flask server to run the AI agent. Start it by running:
```bash
python server.py
```
*Note: The server will output detailed LLM logs (prompts and JSON responses) to the terminal, which is great for debugging!*

### 4. Load the Chrome Extension
1. Open Google Chrome and navigate to `chrome://extensions/`.
2. Toggle **Developer mode** ON (top right corner).
3. Click **Load unpacked** (top left).
4. Select the `extension` folder located in this project directory.
5. The "Travel Planner" icon will appear in your extensions toolbar. Pin it and click it to start chatting!

## Project Structure
- `agent.py`: Core logic for communicating with the Gemini API, enforcing JSON output, and parsing the responses.
- `tools.py`: Definitions of the custom tools the agent can use (hotels, weather, attractions, etc.).
- `server.py`: The Flask application that serves as the bridge between the Chrome extension and the Python agent.
- `extension/`: Contains the Chrome Extension frontend (`manifest.json`, `popup.html`, `popup.css`, `popup.js`).
