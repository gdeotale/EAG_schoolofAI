from flask import Flask, request, jsonify, Response, stream_with_context
import asyncio
import traceback
import json
from agent import mcp_agent_flow

app = Flask(__name__)

GLOBAL_CHAT_HISTORY = []

@app.after_request
def after_request(response):
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type,Authorization')
    response.headers.add('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE,OPTIONS')
    return response

@app.route('/api/chat', methods=['POST', 'OPTIONS'])
def chat():
    if request.method == 'OPTIONS':
        return jsonify({}), 200
        
    data = request.json
    task = data.get('task')
    
    if not task:
        return jsonify({"status": "error", "message": "No task provided"}), 400
        
    def generate():
        global GLOBAL_CHAT_HISTORY
        
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        
        # Build context string from global memory
        context_str = "\n".join([f"{msg['role'].upper()}: {msg['content']}" for msg in GLOBAL_CHAT_HISTORY])
        print(f"--- Sending Context with {len(GLOBAL_CHAT_HISTORY)} historical messages ---")
        
        # Add the new task to global memory
        GLOBAL_CHAT_HISTORY.append({"role": "user", "content": task})
        
        import os
        os.makedirs("sandbox", exist_ok=True)
        with open("sandbox/current_chat.txt", "a", encoding="utf-8") as f:
            f.write(f"USER: {task}\n\n")
        
        gen = mcp_agent_flow(task_str=task, chat_context=context_str)
        final_answer = ""
        
        while True:
            try:
                msg = loop.run_until_complete(gen.__anext__())
                
                # Capture the final answer to store in memory
                if msg.startswith("FINAL_ANSWER_PAYLOAD:"):
                    final_answer = msg.replace("FINAL_ANSWER_PAYLOAD:", "").strip()
                    GLOBAL_CHAT_HISTORY.append({"role": "agent", "content": final_answer})
                    print("--- Appended Agent Response to GLOBAL_CHAT_HISTORY! ---")
                    with open("sandbox/current_chat.txt", "a", encoding="utf-8") as f:
                        f.write(f"AGENT:\n{final_answer}\n\n")
                
                yield f"data: {json.dumps({'status': 'stream', 'message': msg})}\n\n"
            except StopAsyncIteration:
                yield f"data: {json.dumps({'status': 'success', 'message': 'Analysis completed!'})}\n\n"
                break
            except Exception as e:
                yield f"data: {json.dumps({'status': 'error', 'message': str(e)})}\n\n"
                break
        
        loop.close()

    return Response(stream_with_context(generate()), mimetype='text/event-stream')

if __name__ == '__main__':
    print("="*60)
    print("Starting EAG Stock Analyst Backend on http://127.0.0.1:5000")
    print("Leave this terminal open. The Chrome plugin will connect to it!")
    print("="*60)
    app.run(host='127.0.0.1', port=5000, debug=False, threaded=True)
