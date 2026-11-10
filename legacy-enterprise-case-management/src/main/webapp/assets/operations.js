'use strict';
(() => {
  let current = [], comparison = [];
  const byId = id => document.getElementById(id);
  const cell = (row, value) => { const td = document.createElement('td'); td.textContent = String(value ?? ''); row.appendChild(td); };
  function render() {
    const events = current.concat(comparison);
    const summary = byId('summary'); summary.replaceChildren();
    [...new Set(events.map(e => e.implementation))].forEach(implementation => {
      const requests = events.filter(e => e.event === 'request' && e.implementation === implementation);
      const durations = requests.map(e => Number(e.durationSeconds) * 1000).sort((a, b) => a - b);
      const percentile = p => durations.length ? durations[Math.max(0, Math.ceil(durations.length * p) - 1)].toFixed(2) : '—';
      const row = document.createElement('tr');
      [implementation, requests.length, requests.filter(e => e.outcome === 'error').length, percentile(.5), percentile(.95)].forEach(v => cell(row, v));
      summary.appendChild(row);
    });
    const filter = byId('request-id').value.trim();
    const body = byId('events'); body.replaceChildren();
    events.filter(e => !filter || e.requestId === filter).slice().reverse().forEach(e => {
      const row = document.createElement('tr');
      [e.timestamp, e.requestId, `${e.implementation} / ${e.version}`, e.operation, e.event,
        `${e.status} / ${e.outcome}`, (Number(e.durationSeconds) * 1000).toFixed(2)].forEach(v => cell(row, v));
      body.appendChild(row);
    });
  }
  async function refresh() {
    try {
      const response = await fetch('operations.do?view=events', {cache: 'no-store', redirect: 'error'});
      if (!response.ok) throw new Error('Event snapshot request failed');
      current = (await response.json()).events;
      render(); byId('notice').textContent = 'Snapshot refreshed. Comparison percentiles describe these snapshots, not all traffic.';
    } catch {
      // Show a fixed message so server details never appear in the dashboard.
      byId('notice').textContent = 'Unable to load events. Sign in as an active administrator and refresh.';
    }
  }
  byId('refresh').addEventListener('click', refresh);
  byId('request-id').addEventListener('input', render);
  byId('import').addEventListener('change', async event => {
    try {
      const file = event.target.files[0];
      if (!file || file.size > 5 * 1024 * 1024) throw new Error('Snapshot file missing or too large');
      const data = JSON.parse(await file.text());
      if (!Array.isArray(data.events) || data.events.length > 10000 || data.events.some(e => !e || typeof e !== 'object')) throw new Error('Invalid snapshot events');
      comparison = data.events; render(); byId('notice').textContent = 'Comparison snapshot loaded locally.';
    } catch {
      // Imported parser errors may contain sensitive snapshot contents; show only validation guidance.
      byId('notice').textContent = 'Choose a valid event snapshot smaller than 5 MB with at most 10,000 events.';
    }
  });
  void refresh();
})();
