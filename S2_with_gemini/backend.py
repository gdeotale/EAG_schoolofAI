from flask import Flask, jsonify
from flask_cors import CORS
import yfinance as yf

app = Flask(__name__)
CORS(app)  # Allow Chrome extension to fetch data from localhost

# A list of active NIFTY 50 stock symbols
SYMBOLS = [
    'RELIANCE.NS', 'TCS.NS', 'HDFCBANK.NS', 'INFY.NS', 'ICICIBANK.NS',
    'BHARTIARTL.NS', 'SBIN.NS', 'LT.NS', 'ITC.NS', 'KOTAKBANK.NS',
    'HINDALCO.NS', 'TATASTEEL.NS', 'WIPRO.NS', 'JSWSTEEL.NS', 'ONGC.NS',
    'GRASIM.NS', 'NTPC.NS', 'COALINDIA.NS', 'M&M.NS', 'TATAMOTORS.NS'
]

@app.route('/api/stocks', methods=['GET'])
def get_stocks():
    try:
        # Fetch data for all symbols
        tickers = yf.Tickers(' '.join(SYMBOLS))
        
        stock_list = []
        for symbol in SYMBOLS:
            ticker = tickers.tickers[symbol]
            info = ticker.fast_info
            details = ticker.info # Full info necessary for ratios
            
            try:
                cmp = info.last_price
                prev_close = info.previous_close
                if prev_close and prev_close > 0:
                    change_pct = ((cmp - prev_close) / prev_close) * 100
                else:
                    change_pct = 0
            except:
                continue

            # Safely fetch fields, falling back to 'N/A' if yfinance misses them occasionally
            stock_data = {
                'symbol': symbol.replace('.NS', ''),
                'ltp': f"{cmp:.2f}",
                'change': f"{change_pct:+.2f}",
                'cmp': f"{cmp:.2f}",
                'pe': str(details.get('trailingPE', 'N/A')),
                'de': str(details.get('debtToEquity', 'N/A')),
                'pb': str(details.get('priceToBook', 'N/A')),
                'bv': str(details.get('bookValue', 'N/A'))
            }
            stock_list.append(stock_data)

        # Sort by percentage change primarily 
        stock_list.sort(key=lambda x: float(x['change'].replace('+', '')), reverse=True)

        return jsonify({
            'gainers': stock_list[:5], # Top 5 gainers
            'losers': stock_list[-5:][::-1] # Top 5 losers
        })
    except Exception as e:
        print("Error fetching data:", e)
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("Starting Live Indian Stock API on http://127.0.0.1:5000")
    app.run(port=5000, debug=True, use_reloader=False)
