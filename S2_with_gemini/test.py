import os
from dotenv import load_dotenv
from google import genai

load_dotenv()

# The client gets the API key from the environment variable `GEMINI_API_KEY`.
client = genai.Client()

prompt = """
You are a senior Chrome Extension developer. 
Design a Chrome plugin that displays the top 5 gainers and top 5 losers of the Indian market today in a clean HTML table.
When hovering over a particular stock, show a tooltip with key ratios: P/E, Debt to Equity, P/B, Book value, and Current Market Price.

CRITICAL IMPLEMENTATION RULES for the plugin's code:
1. DO NOT USE MOCK DATA. You must write JavaScript that fetches real, live data.
2. Use a hardcoded array of 20 Nifty 50 symbols with '.NS' appended (e.g., 'RELIANCE.NS', 'TCS.NS', 'HDFCBANK.NS').
3. To get live price and change %, use `Promise.all` to fetch `https://query2.finance.yahoo.com/v8/finance/chart/${symbol}?interval=1d&range=1d`. From the JSON response (`res.chart.result[0].meta`), use `regularMarketPrice` and `chartPreviousClose` to calculate the percentage change.
4. To get deeper ratios (P/E, Book Value), use `fetch()` on `https://www.screener.in/company/${symbolWithoutNS}/consolidated/`, then use `DOMParser` to parse the HTML and find the values by iterating over the `#top-ratios li` class elements. (If unavailable, fallback to 'N/A').
5. Use "Rs." instead of the Rupee symbol string to prevent encoding glitches in some browsers.
6. Your `manifest.json` MUST include `host_permissions: ["https://query2.finance.yahoo.com/*", "https://www.screener.in/*"]`.

Return ONLY a raw, valid JSON object where keys are the specific filenames (e.g., "manifest.json", "popup.html", "popup.js", "style.css") and the values are their corresponding completed raw code strings. Do not use Markdown formatting.
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