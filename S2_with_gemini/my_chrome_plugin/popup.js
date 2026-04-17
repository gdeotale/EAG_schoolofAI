document.addEventListener('DOMContentLoaded', () => {
  const gainerBody = document.getElementById('gainers-body');
  const loserBody = document.getElementById('losers-body');
  const loader = document.getElementById('loader');
  const tooltip = document.getElementById('tooltip');

  document.body.addEventListener('mousemove', (e) => {
    if (tooltip.style.display !== 'none') {
      tooltip.style.left = e.pageX + 10 + 'px';
      tooltip.style.top = e.pageY + 10 + 'px';
    }
  });

  function showTooltip(stock) {
    tooltip.innerHTML = `
      <strong>${stock.symbol}</strong><br>
      CMP: Rs. ${stock.cmp}<br>
      P/E: ${stock.pe}<br>
      P/B: ${stock.pb}<br>
      D/E: ${stock.de}<br>
      Book Value: ${stock.bv}
    `;
    tooltip.style.display = 'block';
  }

  function hideTooltip() {
    tooltip.style.display = 'none';
  }

  function renderTable(dataArray, tbody, className) {
    dataArray.forEach(stock => {
      const tr = document.createElement('tr');
      tr.className = className;
      
      tr.innerHTML = `
        <td>${stock.symbol}</td>
        <td>Rs. ${stock.ltp}</td>
        <td class="${parseFloat(stock.change) >= 0 ? 'gainer-val' : 'loser-val'}">${stock.change}%</td>
      `;

      tr.addEventListener('mouseenter', () => showTooltip(stock));
      tr.addEventListener('mouseleave', () => hideTooltip());
      
      tbody.appendChild(tr);
    });
  }

  // Fetch fully augmented data from Local Python Backend
  fetch('http://127.0.0.1:5000/api/stocks')
    .then(res => res.json())
    .then(data => {
      loader.style.display = 'none';

      if (data.error) {
        gainerBody.innerHTML = `<tr><td colspan="3" style="color:red;text-align:center;">Backend Error: ${data.error}</td></tr>`;
        return;
      }

      renderTable(data.gainers, gainerBody, 'gainer-row');
      renderTable(data.losers, loserBody, 'loser-row');
    })
    .catch(err => {
      loader.style.display = 'none';
      gainerBody.innerHTML = `<tr><td colspan="3" style="color:red;text-align:center;">Please run backend.py!</td></tr>`;
      loserBody.innerHTML = `<tr><td colspan="3" style="color:red;text-align:center;">Connection refused</td></tr>`;
    });
});