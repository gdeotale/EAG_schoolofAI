const tableBody = document.getElementById('table-body');
const tooltip = document.getElementById('tooltip');
const btnGainers = document.getElementById('btn-gainers');
const btnLosers = document.getElementById('btn-losers');

let currentData = { gainers: [], losers: [] };

function renderTable(type) {
    const data = currentData[type];
    tableBody.innerHTML = '';
    
    if (!data || data.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center;">Loading...</td></tr>';
        return;
    }

    data.forEach(stock => {
        const row = document.createElement('tr');
        const isGainer = stock.change.startsWith('+');
        
        row.innerHTML = `
            <td>${stock.symbol}</td>
            <td>Rs. ${stock.ltp || stock.price}</td>
            <td class="${isGainer ? 'gainer-val' : 'loser-val'}">${stock.change}%</td>
        `;

        row.addEventListener('mousemove', (e) => showTooltip(e, stock));
        row.addEventListener('mouseleave', hideTooltip);
        
        tableBody.appendChild(row);
    });
}

function showTooltip(e, stock) {
    tooltip.classList.remove('hidden');
    tooltip.style.opacity = '1';
    
    document.getElementById('tt-name').textContent = stock.symbol;
    document.getElementById('tt-cmp').textContent = 'Rs. ' + stock.cmp;
    document.getElementById('tt-pe').textContent = stock.pe;
    document.getElementById('tt-pb').textContent = stock.pb;
    document.getElementById('tt-de').textContent = stock.de;
    document.getElementById('tt-bv').textContent = stock.bv;

    // Position the tooltip
    const tooltipWidth = 220;
    const tooltipHeight = 150;
    let x = e.clientX + 15;
    let y = e.clientY + 15;

    if (x + tooltipWidth > window.innerWidth) x = e.clientX - tooltipWidth - 15;
    if (y + tooltipHeight > window.innerHeight) y = e.clientY - tooltipHeight - 15;

    tooltip.style.left = x + 'px';
    tooltip.style.top = y + 'px';
}

function hideTooltip() {
    tooltip.classList.add('hidden');
    tooltip.style.opacity = '0';
}

btnGainers.addEventListener('click', () => {
    btnGainers.classList.add('active');
    btnLosers.classList.remove('active');
    renderTable('gainers');
});

btnLosers.addEventListener('click', () => {
    btnLosers.classList.add('active');
    btnGainers.classList.remove('active');
    renderTable('losers');
});

// Fetch Data from Local Python Server
tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center;">Connecting to backend...</td></tr>';

fetch('http://127.0.0.1:5000/api/stocks')
    .then(res => res.json())
    .then(data => {
        currentData = data;
        // Re-render currently active tab
        if (btnGainers.classList.contains('active')) {
            renderTable('gainers');
        } else {
            renderTable('losers');
        }
    })
    .catch(err => {
        tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center; color:red;">Ensure backend.py is running</td></tr>';
    });