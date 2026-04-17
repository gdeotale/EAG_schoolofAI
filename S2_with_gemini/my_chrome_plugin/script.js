// Mock data simulating a real-time market API response for NSE
const marketData = {
  gainers: [
    { symbol: 'RELIANCE', price: '2,950.40', change: '+2.45', pe: '28.4', de: '0.41', pb: '2.5', bv: '1180', name: 'Reliance Industries Ltd' },
    { symbol: 'TCS', price: '4,120.15', change: '+1.80', pe: '31.2', de: '0.02', pb: '15.1', bv: '272', name: 'Tata Consultancy Services' },
    { symbol: 'HDFCBANK', price: '1,450.00', change: '+1.65', pe: '17.8', de: '0.90', pb: '2.8', bv: '517', name: 'HDFC Bank Ltd' },
    { symbol: 'ICICIBANK', price: '1,085.30', change: '+1.40', pe: '18.2', de: '0.85', pb: '3.1', bv: '350', name: 'ICICI Bank Ltd' },
    { symbol: 'BHARTIARTL', price: '1,210.00', change: '+1.20', pe: '55.4', de: '1.20', pb: '4.2', bv: '288', name: 'Bharti Airtel Ltd' },
    { symbol: 'INFY', price: '1,540.20', change: '+1.10', pe: '24.5', de: '0.00', pb: '8.4', bv: '183', name: 'Infosys Ltd' },
    { symbol: 'ITC', price: '420.50', change: '+0.95', pe: '26.1', de: '0.00', pb: '7.2', bv: '58', name: 'ITC Ltd' },
    { symbol: 'SBIN', price: '760.40', change: '+0.88', pe: '9.4', de: '1.40', pb: '1.6', bv: '475', name: 'State Bank of India' },
    { symbol: 'L&T', price: '3,450.00', change: '+0.75', pe: '35.6', de: '1.10', pb: '4.8', bv: '718', name: 'Larsen & Toubro Ltd' },
    { symbol: 'AXISBANK', price: '1,050.20', change: '+0.65', pe: '12.8', de: '0.95', pb: '2.1', bv: '500', name: 'Axis Bank Ltd' }
  ],
  losers: [
    { symbol: 'ADANIENT', price: '3,120.00', change: '-3.20', pe: '98.5', de: '0.70', pb: '9.2', bv: '339', name: 'Adani Enterprises Ltd' },
    { symbol: 'TATASTEEL', price: '142.10', change: '-2.45', pe: '14.2', de: '1.05', pb: '1.5', bv: '94', name: 'Tata Steel Ltd' },
    { symbol: 'WIPRO', price: '480.20', change: '-2.10', pe: '22.1', de: '0.15', pb: '3.8', bv: '126', name: 'Wipro Ltd' },
    { symbol: 'HINDALCO', price: '520.40', change: '-1.85', pe: '11.4', de: '0.55', pb: '1.2', bv: '433', name: 'Hindalco Industries Ltd' },
    { symbol: 'M&M', price: '1,850.00', change: '-1.60', pe: '19.4', de: '0.35', pb: '3.5', bv: '528', name: 'Mahindra & Mahindra Ltd' },
    { symbol: 'JSWSTEEL', price: '810.20', change: '-1.45', pe: '16.8', de: '1.20', pb: '2.4', bv: '337', name: 'JSW Steel Ltd' },
    { symbol: 'GRASIM', price: '2,150.00', change: '-1.30', pe: '24.2', de: '0.60', pb: '1.8', bv: '1194', name: 'Grasim Industries Ltd' },
    { symbol: 'NESTLEIND', price: '2,540.00', change: '-1.25', pe: '75.2', de: '0.05', pb: '22.4', bv: '113', name: 'Nestle India Ltd' },
    { symbol: 'TITAN', price: '3,620.40', change: '-1.15', pe: '88.4', de: '0.20', pb: '18.2', bv: '198', name: 'Titan Company Ltd' },
    { symbol: 'HCLTECH', price: '1,610.20', change: '-1.05', pe: '27.4', de: '0.10', pb: '6.5', bv: '247', name: 'HCL Technologies Ltd' }
  ]
};

const tableBody = document.getElementById('table-body');
const tooltip = document.getElementById('tooltip');
const tabGainers = document.getElementById('tab-gainers');
const tabLosers = document.getElementById('tab-losers');

function renderTable(type) {
  const data = marketData[type];
  tableBody.innerHTML = '';
  
  data.forEach(stock => {
    const row = document.createElement('tr');
    const colorClass = type === 'gainers' ? 'gainer' : 'loser';
    
    row.innerHTML = `
      <td>${stock.symbol}</td>
      <td>₹${stock.price}</td>
      <td class="${colorClass}">${stock.change}%</td>
    `;

    row.addEventListener('mouseover', (e) => showTooltip(e, stock));
    row.addEventListener('mousemove', (e) => moveTooltip(e));
    row.addEventListener('mouseout', hideTooltip);
    
    tableBody.appendChild(row);
  });
}

function showTooltip(e, stock) {
  tooltip.classList.remove('hidden');
  document.getElementById('tt-name').textContent = stock.name;
  document.getElementById('tt-cmp').textContent = '₹' + stock.price;
  document.getElementById('tt-pe').textContent = stock.pe;
  document.getElementById('tt-de').textContent = stock.de;
  document.getElementById('tt-pb').textContent = stock.pb;
  document.getElementById('tt-bv').textContent = '₹' + stock.bv;
}

function moveTooltip(e) {
  const padding = 15;
  let x = e.clientX + padding;
  let y = e.clientY + padding;
  
  // Basic bounds checking
  if (x + 200 > window.innerWidth) x = e.clientX - 210;
  if (y + 150 > window.innerHeight) y = e.clientY - 150;

  tooltip.style.left = x + 'px';
  tooltip.style.top = y + 'px';
}

function hideTooltip() {
  tooltip.classList.add('hidden');
}

tabGainers.addEventListener('click', () => {
  tabGainers.classList.add('active');
  tabLosers.classList.remove('active');
  renderTable('gainers');
});

tabLosers.addEventListener('click', () => {
  tabLosers.classList.add('active');
  tabGainers.classList.remove('active');
  renderTable('losers');
});

// Initial Load
renderTable('gainers');