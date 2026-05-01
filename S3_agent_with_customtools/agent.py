import os
import time
from dotenv import load_dotenv
import json
import re
load_dotenv()

from google import genai
from google.genai import types
from tools import get_available_hotels, get_tourist_attractions, get_weather, get_travel_modes

# Initialize client
client = genai.Client()

# Define the model
MODEL_NAME = "gemini-3.1-flash-lite-preview"
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", MODEL_NAME)
THROTTLE_SECONDS = 6.0
if not GEMINI_API_KEY:
    raise RuntimeError("GEMINI_API_KEY not set. Create a .env file with GEMINI_API_KEY=...")

client = genai.Client(api_key=GEMINI_API_KEY)

def call_llm(prompt: str, max_retries: int = 5) -> str:
    """Send a prompt to Gemini and return the text response.

    Sleeps for THROTTLE_SECONDS before each call to stay under the free-tier
    rate limit. Includes automatic retries for 503 UNAVAILABLE errors.
    """
    wait_time = THROTTLE_SECONDS
    for attempt in range(max_retries):
        print(f"  [waiting {wait_time}s to respect rate limits...]", flush=True)
        time.sleep(wait_time)
        try:
            response = client.models.generate_content(model=GEMINI_MODEL, contents=prompt)
            return response.text
        except Exception as e:
            error_str = str(e)
            if "503" in error_str or "UNAVAILABLE" in error_str or "high demand" in error_str:
                if attempt < max_retries - 1:
                    print(f"  [Attempt {attempt + 1}/{max_retries} failed with 503. Retrying...]", flush=True)
                    wait_time *= 2  # Exponential backoff
                else:
                    raise e
            else:
                raise e

system_prompt = """You are a helpful AI agent that can use tools to answer questions accurately.

You have access to the following tools:

1. get_available_hotels(location: str, check_in_date: str, check_out_date: str) -> list[dict]
   Get a list of available hotels in a given location for the specified dates.
   Examples: get_available_hotels("Paris, France", "2024-05-10", "2024-05-15")

2. get_tourist_attractions(location: str) -> list[dict]
   Get a list of popular tourist attractions in a given location.
   Examples: get_tourist_attractions("Kyoto, Japan")

3. get_weather(location: str, date: str) -> dict
   Get the weather forecast for a specific location and date.
   Examples: get_weather("New York", "2024-05-10")

4. get_travel_modes(origin: str, destination: str) -> list[dict]
   Get available modes of travel between an origin and a destination, along with estimated duration and cost.
   Examples: get_travel_modes("London", "Paris")

You must respond in ONE of these two JSON formats:

If you need to use a tool:
{"tool_name": "<name>", "tool_arguments": {"<arg_name>": "<value>"}}

If you have the final answer:
{"answer": "<your final answer>"}

IMPORTANT RULES:
- Respond with ONLY the JSON. No other text. No markdown code fences.
- Use tools when you need real data like weather, hotels, attractions, or travel modes.
- After receiving a tool result, either use another tool or provide your final answer.
- For complex calculations, break them down into steps if needed.
- ALWAYS use the tools for real data — do NOT try to hallucinate real world facts.
"""

# Tool registry — maps tool names to functions
tools = {
    "get_available_hotels": get_available_hotels,
    "get_tourist_attractions": get_tourist_attractions,
    "get_weather": get_weather,
    "get_travel_modes": get_travel_modes,
}

# ============================================================
# Response Parser — Handles messy LLM output
# ============================================================

def parse_llm_response(text: str) -> dict:
    """Parse the LLM's response, handling common formatting issues"""
    text = text.strip()

    # Remove markdown code fences if present
    if text.startswith("```"):
        lines = text.split("\n")
        # Remove first line (opening fence)
        lines = lines[1:]
        # Remove last line if it's a closing fence
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines).strip()
        # Remove language identifier
        if text.startswith("json"):
            text = text[4:].strip()

    # Try direct parse
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # Try to find JSON object in the text
    json_match = re.search(r'\{.*\}', text, re.DOTALL)
    if json_match:
        try:
            return json.loads(json_match.group())
        except json.JSONDecodeError:
            pass

    raise ValueError(f"Could not parse LLM response: {text[:200]}")


# ============================================================
# The Agent Loop — This is where the magic happens
# ============================================================

def run_agent(user_query: str, max_iterations: int = 5, verbose: bool = True):
    """
    Run the agent loop:
    User query → LLM → [Tool call → Result → LLM]* → Final answer

    This is THE pattern. Everything else in this course builds on this loop.
    """
    if verbose:
        print(f"\n{'='*60}")
        print(f"  User: {user_query}")
        print(f"{'='*60}")

    # Conversation history — this is the agent's "working memory"
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_query},
    ]

    for iteration in range(max_iterations):
        if verbose:
            print(f"\n--- Iteration {iteration + 1} ---")

        # Build prompt from message history
        # Each iteration, the LLM sees EVERYTHING that happened before
        prompt = ""
        for msg in messages:
            if msg["role"] == "system":
                prompt += msg["content"] + "\n\n"
            elif msg["role"] == "user":
                prompt += f"User: {msg['content']}\n\n"
            elif msg["role"] == "assistant":
                prompt += f"Assistant: {msg['content']}\n\n"
            elif msg["role"] == "tool":
                prompt += f"Tool Result: {msg['content']}\n\n"

        # Call the LLM
        response_text = call_llm(prompt)
        if verbose:
            print(f"LLM: {response_text.strip()}")

        # Parse the response
        try:
            parsed = parse_llm_response(response_text)
        except (ValueError, json.JSONDecodeError) as e:
            if verbose:
                print(f"Parse error: {e}")
                print("Asking LLM to retry...")
            messages.append({"role": "assistant", "content": response_text})
            messages.append({"role": "user", "content": "Please respond with valid JSON only. No markdown, no extra text."})
            continue

        # Check if it's a final answer
        if "answer" in parsed:
            if verbose:
                print(f"\n{'='*60}")
                print(f"  Agent Answer: {parsed['answer']}")
                print(f"{'='*60}")
            return parsed["answer"]

        # It's a tool call — execute it
        if "tool_name" in parsed:
            tool_name = parsed["tool_name"]
            tool_args = parsed.get("tool_arguments", {})

            if verbose:
                print(f"→ Calling tool: {tool_name}({tool_args})")

            # Check if tool exists
            if tool_name not in tools:
                error_msg = json.dumps({"error": f"Unknown tool: {tool_name}. Available: {list(tools.keys())}"})
                if verbose:
                    print(f"→ Error: {error_msg}")
                messages.append({"role": "assistant", "content": response_text})
                messages.append({"role": "tool", "content": error_msg})
                continue

            # Execute the tool
            tool_result = tools[tool_name](**tool_args)
            if verbose:
                print(f"→ Result: {tool_result}")

            # Add to conversation history — the LLM will see this next iteration
            messages.append({"role": "assistant", "content": response_text})
            messages.append({"role": "tool", "content": tool_result})

    print("\nMax iterations reached. Agent could not complete the task.")

    # Print full conversation for debugging
    if verbose:
        print(f"\n{'='*60}")
        print("Full conversation history:")
        print(f"{'='*60}")
        for i, msg in enumerate(messages):
            print(f"[{i}] {msg['role']}: {msg['content'][:100]}...")

    return None

# ============================================================
# Try It Out
# ============================================================

if __name__ == "__main__":
    run_agent(
        "tell me about places i can visit in New York?",
        max_iterations=3,
        verbose=True
    )


