import os
import time
from dotenv import load_dotenv
import json
import re
load_dotenv()

from google import genai
from google.genai import types

# Initialize client
client = genai.Client()

# Define the model
MODEL_NAME = "gemini-3.1-flash-lite-preview"
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", MODEL_NAME)
MAX_ITERATIONS = 10
LLM_SLEEP_SECONDS = 5
LLM_TIMEOUT = 120

if not GEMINI_API_KEY:
    raise RuntimeError("GEMINI_API_KEY not set. Create a .env file with GEMINI_API_KEY=...")

client = genai.Client(api_key=GEMINI_API_KEY)

async def generate_with_timeout(prompt: str, timeout: int = LLM_TIMEOUT):
    """Run the blocking Gemini call in a thread with a timeout."""
    loop = asyncio.get_event_loop()
    return await asyncio.wait_for(
        loop.run_in_executor(
            None,
            lambda: client.models.generate_content(model=MODEL_NAME, contents=prompt),
        ),
        timeout=timeout,
    )


def describe_tools(tools) -> str:
    lines = []
    for i, t in enumerate(tools, 1):
        props = (t.inputSchema or {}).get("properties", {})
        params = ", ".join(f"{n}: {p.get('type', '?')}" for n, p in props.items()) or "no params"
        lines.append(f"{i}. {t.name}({params}) — {t.description or ''}")
    return "\n".join(lines)


def coerce(value: str, schema_type: str):
    import json
    import re
    if schema_type == "integer":
        return int(value)
    if schema_type == "number":
        return float(value)
    if schema_type == "boolean":
        return value.lower() in ("true", "1", "yes")
        
    cleaned_value = re.sub(r"^[a-zA-Z_]+\s*=\s*", "", value).strip()
    
    if schema_type in ("array", "object") or cleaned_value.startswith(("{", "[")):
        try:
            return json.loads(cleaned_value)
        except Exception:
            try:
                return eval(cleaned_value)
            except Exception:
                pass
    return cleaned_value

######################### --- MCP CLIENT INTEGRATION --###############################
import asyncio
from mcp.client.stdio import stdio_client, StdioServerParameters
from mcp.client.session import ClientSession

async def mcp_agent_flow(task_str: str = None, chat_context: str = ""):
    # 1. Define how to start your local MCP server
    server_params = StdioServerParameters(
        command="python",
        args=["server.py"]
    )

    print("Connecting to local MCP server...")
    # 2. Connect to the server
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # 3. Initialize the connection
            await session.initialize()
            print("Connected!\n")

            # 4. Fetch the tools from server.py
            response = await session.list_tools()
            tools = response.tools
            
            print("=== Tools available on server ===")
            
            tools_desc = describe_tools(tools)
            print(tools_desc)
            print(f"Loaded {len(tools)} tools\n")

            system_prompt = f"""You are a stock market analysis agent working inside a sandboxed MCP server.
            You solve tasks by calling tools ONE AT A TIME and observing their results.

            Available tools:
            {tools_desc}

            When you need to call a tool, respond with EXACTLY ONE line:
            FUNCTION_CALL: tool_name|arg1|arg2|...

            When you have completed the task, you MUST respond with:
            FINAL_ANSWER:
            <Your complete, detailed answer, including any markdown tables, data comparisons, and analysis. Feel free to use multiple lines and markdown formatting here.>
            
            If your task requires plotting or you are returning tabular data (like dividend history or peer comparison), you MUST append a JSON block at the very end of your FINAL_ANSWER in this exact format, with NO text after it:
            ```json
            {{
              "x_axis": "XAxisColumnName",
              "graphs": [
                {{"title": "Metric 1 Comparison", "y_axis": "Metric1Column"}},
                {{"title": "Metric 2 Comparison", "y_axis": "Metric2Column"}}
              ],
              "data": [
                {{"XAxisColumnName": "Val1", "Metric1Column": 10, "Metric2Column": 20}},
                {{"XAxisColumnName": "Val2", "Metric1Column": 15, "Metric2Column": 25}}
              ]
            }}
            ```
            CRITICAL INSTRUCTIONS FOR FINAL_ANSWER: 
            1. Do NOT just output the JSON block! You MUST provide your complete detailed analysis and the raw markdown table as text first, THEN put the JSON block at the very end.
            2. You MUST include a graph object in the `graphs` array for EVERY numerical column returned in your tabular data. if N/A in column value, use 0 and show N/A in the tabular data for plotting.

            Rules:
            - Provide args in the exact order of the tool's parameters.
            - Do not invent tools that are not listed above.
            - After each FUNCTION_CALL you'll receive the result; use it to decide the next step.
            - Prefer the simplest tool sequence that solves the task.
            - IMPORTANT: If the user asks you to save, write down, or export the "chat history" or "conversation history", you MUST write the EXACT VERBATIM text from the 'Previous Conversation History' section below. Do NOT summarize or paraphrase it.
            """

            task = task_str if task_str else (
                "Get peer comparison of Force Motors (ticker: FORCEMOT), include at least 8 peers"
            )
            MAX_ITERATIONS = 10
            LLM_SLEEP_SECONDS = 5
            history: list[str] = []
            for iteration in range(1, MAX_ITERATIONS + 1):
                yield f"--- Iteration {iteration} ---"
                print(f"\n--- Iteration {iteration} ---")

                context = "\n".join(history) if history else "(no prior steps)"
                
                chat_history_section = ""
                if chat_context:
                    chat_history_section = f"Previous Conversation History:\n{chat_context}\n\n"
                    
                prompt = (
                    f"{system_prompt}\n"
                    f"{chat_history_section}"
                    f"Task: {task}\n\n"
                    f"Previous steps:\n{context}\n\n"
                    f"What is your next single action?"
                )

                yield f"Sleeping {LLM_SLEEP_SECONDS}s before calling LLM to avoid rate limits..."
                print(f"Sleeping {LLM_SLEEP_SECONDS}s before LLM call...")
                await asyncio.sleep(LLM_SLEEP_SECONDS)

                try:
                    response = await generate_with_timeout(prompt)
                except (TimeoutError, asyncio.TimeoutError):
                    print("LLM timed out — stopping.")
                    break
                except Exception as e:
                    print(f"LLM error: {e}")
                    break

                # Parse the response robustly
                lines = (response.text or "").strip().splitlines()
                func_call_line = next((line for line in lines if line.strip().startswith("FUNCTION_CALL:")), None)

                if "FINAL_ANSWER:" in (response.text or ""):
                    answer_start = response.text.find("FINAL_ANSWER:")
                    answer = response.text[answer_start + len("FINAL_ANSWER:"):].strip()
                    yield f"FINAL_ANSWER_PAYLOAD:{answer}"
                    yield "Final Analysis generated! Opening UI dashboard..."
                    print("\n=== Agent done ===")
                    print(answer)
                    
                    # File saving is now fully autonomous: The agent will only save files if the user explicitly requests it via its tool set.
                    
                    try:
                        from prefab_ui.app import PrefabApp
                        from prefab_ui.components import Markdown, Container, Heading
                        from prefab_ui.components.charts import BarChart, ChartSeries
                        import re
                        import json
                        
                        ui_children = [
                            Heading("Agent Response"),
                            Markdown(answer)
                        ]
                        app_state = {}
                        
                        # Try to parse JSON for chart
                        json_match = re.search(r"```json\s*(.*?)\s*```", answer, re.DOTALL)
                        if json_match:
                            try:
                                chart_info = json.loads(json_match.group(1))
                                if isinstance(chart_info, dict) and "data" in chart_info and len(chart_info["data"]) > 0:
                                    x_axis = chart_info.get("x_axis", list(chart_info["data"][0].keys())[0])
                                    
                                    # Dynamically generate graphs for EVERY numerical column in the data
                                    graphs = []
                                    for key, value in chart_info["data"][0].items():
                                        print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
                                        print(f"Key: {key}, Value: {value}")
                                        if key != x_axis and isinstance(value, (int, float)):
                                            graphs.append({"title": f"{key} Comparison", "y_axis": key})
                                            
                                    if graphs:
                                        from prefab_ui.components import Select, SelectOption, If
                                        initial_selected = graphs[0]["y_axis"]
                                        options = [SelectOption(value=g["y_axis"], label=g["title"]) for g in graphs]
                                        
                                        ui_children.append(Heading("Select Metric to View:", level=3))
                                        ui_children.append(Select(name="selected_metric", value=initial_selected, children=options))
                                        
                                        for graph in graphs:
                                            y_axis = graph["y_axis"]
                                            chart = BarChart(
                                                data=chart_info["data"],
                                                x_axis=x_axis,
                                                series=[ChartSeries(name=y_axis, dataKey=y_axis, color="blue")]
                                            )
                                            ui_children.append(If(condition=f"selected_metric == '{y_axis}'", children=[
                                                Heading(graph["title"], level=4),
                                                chart
                                            ]))
                                        app_state["selected_metric"] = initial_selected
                                elif isinstance(chart_info, dict) and "data" in chart_info:
                                    # Fallback for single graph
                                    ui_children.append(Heading(chart_info.get("title", "Chart Data"), level=3))
                                    ui_children.append(BarChart(
                                        data=chart_info["data"],
                                        x_axis=chart_info.get("x_axis", list(chart_info["data"][0].keys())[0]),
                                        series=[ChartSeries(name=chart_info.get("y_axis", list(chart_info["data"][0].keys())[1]), dataKey=chart_info.get("y_axis", list(chart_info["data"][0].keys())[1]), color="blue")]
                                    ))
                                elif isinstance(chart_info, list) and chart_info:
                                    # Fallback to the old format
                                    ui_children.append(Heading("Chart Data", level=3))
                                    ui_children.append(BarChart(
                                        data=chart_info,
                                        x_axis=list(chart_info[0].keys())[0],
                                        series=[ChartSeries(name=list(chart_info[0].keys())[1], dataKey=list(chart_info[0].keys())[1], color="blue")]
                                    ))
                                    
                                answer_clean = answer.replace(json_match.group(0), "").strip()
                                ui_children[1] = Markdown(answer_clean)
                            except Exception as e:
                                print("Could not parse JSON for chart:", e)
                        
                        app = PrefabApp(
                            title="Agent Response",
                            view=Container(children=ui_children),
                            state=app_state,
                            css_class="p-8 max-w-4xl mx-auto"
                        )
                        with open("response.html", "w", encoding="utf-8") as f:
                            html_str = app.html()
                            html_str = html_str.replace("</head>", "<style>body { background-color: #f3e8ff !important; }</style></head>")
                            f.write(html_str)
                        
                        import webbrowser
                        import os
                        webbrowser.open("file://" + os.path.realpath("response.html"))
                    except Exception as e:
                        print(f"Error displaying UI: {e}")

                    break

                if not func_call_line:
                    msg = "Unexpected response format — must contain FUNCTION_CALL: or FINAL_ANSWER:"
                    print(msg)
                    history.append(f"Iteration {iteration}: ERROR - {msg}. Please follow the exact format.")
                    continue

                _, call = func_call_line.split(":", 1)
                parts = [p.strip() for p in call.split("|")]
                func_name = parts[0]
                raw_args = parts[1:] if len(parts) > 1 else []

                tool = next((t for t in tools if t.name == func_name), None)
                if tool is None:
                    msg = f"Unknown tool '{func_name}'. Available tools: {', '.join([t.name for t in tools])}"
                    print(msg)
                    history.append(f"Iteration {iteration}: ERROR - {msg}")
                    continue

                props = (tool.inputSchema or {}).get("properties", {})
                arguments = {
                    name: coerce(val, info.get("type", "string"))
                    for (name, info), val in zip(props.items(), raw_args)
                }

                yield f"Executing Tool: {func_name}..."
                print(f"→ {func_name}({arguments})")
                try:
                    result = await session.call_tool(func_name, arguments=arguments)
                    payload = (
                        result.content[0].text
                        if result.content and hasattr(result.content[0], "text")
                        else str(result)
                    )
                except Exception as e:
                    payload = f"ERROR: {e}"

                print(f"← {payload}")
                history.append(
                    f"Iteration {iteration}: called {func_name}({arguments}) → {payload}"
                )
            else:
                print("\nReached MAX_ITERATIONS without FINAL_ANSWER.")

#####******************************************************************************#####


if __name__ == "__main__":
    async def run_cli():
        async for msg in mcp_agent_flow():
            pass
    asyncio.run(run_cli())