const SYMBOLS = [
  'RELIANCE.NS', 'TCS.NS', 'HDFCBANK.NS', 'INFY.NS', 'ICICIBANK.NS',
  'BHARTIARTL.NS', 'SBIN.NS', 'LT.NS', 'ITC.NS', 'KOTAKBANK.NS',
  'HINDALCO.NS', 'TATASTEEL.NS', 'WIPRO.NS', 'JSWSTEEL.NS', 'ONGC.NS',
  'GRASIM.NS', 'NTPC.NS', 'COALINDIA.NS', 'M&M.NS', 'TATAMOTORS.NS'
];

document.addEventListener('DOMContentLoaded', () => {
  const gainerBody = document.getElementById('gainers-body');
  const loserBody = document.getElementById('losers-body');
  const tooltip = document.getElementById('tooltip');

  function populateTable(data, container, className) {
    if (!data || data.length === 0) {
      container.innerHTML = `<tr><td colspan="3" style="text-align:center;">No data available</td></tr>`;
      return;
    }
    container.innerHTML = '';
    data.forEach(stock => {
      const row = document.createElement('tr');
      row.className = className;
      row.innerHTML = `
        <td>${stock.symbol}</td>
        <td>${stock.ltp}</td>
        <td>${stock.change}%</td>
      `;
      
      row.addEventListener('mouseenter', (e) => showTooltip(e, stock));
      row.addEventListener('mousemove', moveTooltip);
      row.addEventListener('mouseleave', hideTooltip);
      
      container.appendChild(row);
    });
  }

  function showTooltip(e, stock) {
    document.getElementById('tt-symbol').textContent = stock.symbol;
    document.getElementById('tt-cmp').textContent = stock.cmp;
    document.getElementById('tt-pe').textContent = stock.pe;
    document.getElementById('tt-de').textContent = stock.de;
    document.getElementById('tt-pb').textContent = stock.pb;
    document.getElementById('tt-bv').textContent = stock.bv;
    
    tooltip.classList.remove('hidden');
    tooltip.style.opacity = '1';
    moveTooltip(e);
  }

  function moveTooltip(e) {
    const x = e.clientX + 15;
    const y = e.clientY + 15;
    
    const tooltipWidth = 180;
    const tooltipHeight = 130;
    const winWidth = window.innerWidth;
    const winHeight = window.innerHeight;

    let posX = x;
    let posY = y;

    if (x + tooltipWidth > winWidth) posX = x - tooltipWidth - 20;
    if (y + tooltipHeight > winHeight) posY = y - tooltipHeight - 20;

    tooltip.style.left = posX + 'px';
    tooltip.style.top = posY + 'px';
  }

  function hideTooltip() {
    tooltip.classList.add('hidden');
    tooltip.style.opacity = '0';
  }

  gainerBody.innerHTML = `<tr><td colspan="3" style="text-align:center;">Fetching live data...</td></tr>`;
  loserBody.innerHTML = `<tr><td colspan="3" style="text-align:center;">Fetching live data...</td></tr>`;

  // Fetch purely from client side (no backend needed!)
  Promise.all(SYMBOLS.map(async (sym) => {
    let stockInfo = null;
    const baseSym = sym.replace('.NS', '');
    try {
      // 1. Fetch current price from Yahoo Finance
      const yfRes = await fetch(`https://query2.finance.yahoo.com/v8/finance/chart/${sym}?interval=1d&range=1d`);
      const yfData = await yfRes.json();
      
      if(yfData && yfData.chart && yfData.chart.result && yfData.chart.result.length > 0) {
        const meta = yfData.chart.result[0].meta;
        const cmp = meta.regularMarketPrice;
        const prev = meta.chartPreviousClose;
        if(cmp && prev) {
          const change = ((cmp - prev) / prev) * 100;
          stockInfo = {
            symbol: baseSym,
            ltp: cmp.toFixed(2),
            cmp: cmp.toFixed(2),
            change: change.toFixed(2),
            pe: 'N/A',
            de: 'N/A',
            pb: 'N/A',
            bv: 'N/A'
          };
        }
      }

      // 2. Fetch fundamental ratios from Screener.in if previous step succeeded
      if (stockInfo) {
        let screenerSym = baseSym;
        if (baseSym === 'M&M') screenerSym = 'M&M';
        const scRes = await fetch(`https://www.screener.in/company/${screenerSym}/consolidated/`);
        const html = await scRes.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, "text/html");
        
        const listItems = doc.querySelectorAll('#top-ratios li');
        listItems.forEach(li => {
          const nameEl = li.querySelector('.name');
          const valEl = li.querySelector('.number');
          if (nameEl && valEl) {
            const name = nameEl.textContent.trim();
            const val = valEl.textContent.trim();
            if (name.includes('Stock P/E')) stockInfo.pe = val;
            if (name.includes('Book Value')) stockInfo.bv = val;
          }
        });

        // Compute Price to book if Book Value exists
        if (stockInfo.bv !== 'N/A') {
          const numBv = parseFloat(stockInfo.bv.replace(/,/g, ''));
          if (!isNaN(numBv) && numBv > 0) {
             stockInfo.pb = (stockInfo.cmp / numBv).toFixed(2);
          }
        }
      }
      return stockInfo;
    } catch (e) {
      return null;
    }
  })).then(results => {
    let stockData = results.filter(res => res !== null);

    stockData.sort((a, b) => parseFloat(b.change) - parseFloat(a.change));

    const gainers = stockData.slice(0, 5);
    const losers = stockData.slice(-5).reverse();

    populateTable(gainers, gainerBody, 'gainer-row');
    populateTable(losers, loserBody, 'loser-row');
  }).catch(err => {
    gainerBody.innerHTML = `<tr><td colspan="3" style="text-align:center; color:red;">Data fetch failed</td></tr>`;
    loserBody.innerHTML = `<tr><td colspan="3" style="text-align:center; color:red;">Data fetch failed</td></tr>`;
  });
});