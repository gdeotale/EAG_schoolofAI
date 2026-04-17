import os
from dotenv import load_dotenv
from google import genai

load_dotenv()

# The client gets the API key from the environment variable `GEMINI_API_KEY`.
client = genai.Client()

prompt = """
You are a code generator.
Design a chrome plugin which will find top 10 gainers and losers of todays indian market and show them in a table, when i hover on particular stock it should show key rations related to stock like P/E, Debt to equity ratio, P/B, Book value and current market price.

Return a JSON object where the keys are the filenames (e.g., "manifest.json", "index.html", "script.js") and the values are their corresponding raw code contents. Return ONLY the JSON, without any markdown formatting wrappers.
"""

response = client.models.generate_content(
    model="gemini-3-flash-preview", 
    contents=prompt
)

import json
text_response = response.text.strip()
# Clean up markdown codeblocks if the model included them
if text_response.startswith('```json'):
    text_response = text_response[7:]
if text_response.endswith('```'):
    text_response = text_response[:-3]
    
try:
    files_data = json.loads(text_response.strip())
    
    output_dir = "my_chrome_plugin"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    for filename, code in files_data.items():
        file_path = os.path.join(output_dir, filename)
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(code)
            
    print(f"Success! Saved {len(files_data)} files into the '{output_dir}' directory.")
except Exception as e:
    print(f"Error parsing JSON or writing files: {e}")
    print("Raw Output:")
    print(response.text)