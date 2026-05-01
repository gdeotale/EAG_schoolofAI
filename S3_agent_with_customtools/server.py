import os
import json
from flask import Flask, request, jsonify
from flask_cors import CORS
from agent import call_llm, parse_llm_response, system_prompt, tools

app = Flask(__name__)
CORS(app)

@app.route('/chat', methods=['POST'])
def chat():
    data = request.json
    user_query = data.get("query", "")
    messages = data.get("messages", [])
    
    if not messages:
        messages = [{"role": "system", "content": system_prompt}]
        
    messages.append({"role": "user", "content": user_query})
    
    events = []
    max_iterations = 5
    
    for iteration in range(max_iterations):
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
                
        # Call LLM
        print(f"\n{'='*40} LLM PROMPT {'='*40}")
        print(prompt)
        print(f"{'='*92}\n")
        
        response_text = call_llm(prompt)
        
        print(f"\n{'='*40} LLM RESPONSE {'='*40}")
        print(response_text.strip())
        print(f"{'='*94}\n")
        
        try:
            parsed = parse_llm_response(response_text)
        except (ValueError, json.JSONDecodeError) as e:
            messages.append({"role": "assistant", "content": response_text})
            messages.append({"role": "user", "content": "Please respond with valid JSON only. No markdown, no extra text."})
            continue
            
        if "answer" in parsed:
            answer = parsed["answer"]
            events.append({"type": "answer", "content": answer})
            messages.append({"role": "assistant", "content": json.dumps({"answer": answer})})
            break
            
        if "tool_name" in parsed:
            tool_name = parsed["tool_name"]
            tool_args = parsed.get("tool_arguments", {})
            
            # Log the tool call
            events.append({"type": "tool_call", "name": tool_name, "args": tool_args})
            
            if tool_name not in tools:
                error_msg = json.dumps({"error": f"Unknown tool: {tool_name}"})
                messages.append({"role": "assistant", "content": response_text})
                messages.append({"role": "tool", "content": error_msg})
                events.append({"type": "tool_result", "name": tool_name, "result": "Unknown tool"})
                continue
                
            # Execute the tool
            try:
                tool_result = tools[tool_name](**tool_args)
                tool_result_str = json.dumps(tool_result)
            except Exception as e:
                tool_result_str = json.dumps({"error": str(e)})
                tool_result = {"error": str(e)}
                
            # Log the tool result
            events.append({"type": "tool_result", "name": tool_name, "result": tool_result})
            
            messages.append({"role": "assistant", "content": response_text})
            messages.append({"role": "tool", "content": tool_result_str})
    else:
        events.append({"type": "error", "content": "Max iterations reached"})
        
    return jsonify({"events": events, "messages": messages})

if __name__ == '__main__':
    app.run(port=5000, debug=True)
