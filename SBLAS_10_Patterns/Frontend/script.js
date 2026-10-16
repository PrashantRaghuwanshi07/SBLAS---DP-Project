const $ = (selector) => document.querySelector(selector);
const form = $('#applicationForm');
const typeSelect = $('#customerType');
const loanSelect = $('#loanType');
const resultBox = $('#resultBox');
const staffMessage = $('#staffMessage');
let staffCredentials = null;

const loanOptions = {
  'Salaried': ['Personal Loan', 'Home Loan', 'Car Loan'],
  'Self-Employed': ['Business Loan', 'Working Capital Loan', 'Home Loan']
};
function updateLoanOptions() {
  const previous = loanSelect.value;
  loanSelect.replaceChildren();
  for (const value of loanOptions[typeSelect.value] || []) {
    const option = document.createElement('option'); option.value = value; option.textContent = value; loanSelect.append(option);
  }
  if ([...loanSelect.options].some(o => o.value === previous)) loanSelect.value = previous;
}
typeSelect.addEventListener('change', updateLoanOptions);
updateLoanOptions();

function showResult(kind, title, message, details = []) {
  resultBox.className = `result-box ${kind}`;
  resultBox.replaceChildren();
  const heading = document.createElement('strong'); heading.textContent = title; resultBox.append(heading);
  const paragraph = document.createElement('p'); paragraph.textContent = message; resultBox.append(paragraph);
  if (details.length) {
    const list = document.createElement('ul');
    for (const detail of details) { const li = document.createElement('li'); li.textContent = detail; list.append(li); }
    resultBox.append(list);
  }
  resultBox.classList.remove('hidden');
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true; button.textContent = 'Submitting securely…';
  try {
    const response = await fetch('/api/apply', {
      method: 'POST', headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
      body: new URLSearchParams(new FormData(form))
    });
    const data = await response.json();
    if (!response.ok || !data.success) throw new Error(data.message || 'Submission failed.');
    showResult('success', `Application #${data.loanId} received`, data.message, [
      `Credit risk: ${data.risk}`, `Affordability estimate: ${data.debtToIncomePercent}% debt-to-income`,
      `Fraud review flag: ${data.fraudFlag ? 'Yes — manual review required' : 'No rule-based flag'}`,
      `Approval deadline: ${new Date(data.approvalDueAt).toLocaleString()}`
    ]);
    form.reset(); $('#staffUsername').value = ''; updateLoanOptions();
    if (staffCredentials) await loadLoans();
  } catch (error) {
    showResult('error', 'Application not submitted', error.message || 'Check the form and try again.');
  } finally {
    button.disabled = false; button.innerHTML = 'Submit application <span>↗</span>';
  }
});

$('#staffLogin').addEventListener('submit', async (event) => {
  event.preventDefault();
  staffCredentials = {username: $('#staffUsername').value.trim(), password: $('#staffPassword').value};
  await loadLoans();
});

async function staffFetch(url, options = {}) {
  if (!staffCredentials) throw new Error('Enter staff credentials first.');
  const headers = new Headers(options.headers || {});
  headers.set('X-Username', staffCredentials.username); headers.set('X-Password', staffCredentials.password);
  const response = await fetch(url, {...options, headers});
  const data = await response.json();
  if (!response.ok || !data.success) throw new Error(data.message || 'Request failed.');
  return data;
}
function cell(text) { const td = document.createElement('td'); td.textContent = text == null ? '—' : String(text); return td; }
function makeActionButton(label, className, callback) { const button = document.createElement('button'); button.textContent = label; button.className = className; button.type = 'button'; button.addEventListener('click', callback); return button; }
async function loadLoans() {
  const tbody = $('#loanRows'); tbody.replaceChildren();
  try {
    const result = await staffFetch('/api/loans');
    const rows = result.data || [];
    if (!rows.length) { const tr = document.createElement('tr'); const td = cell('No applications yet.'); td.colSpan = 7; td.className = 'empty-row'; tr.append(td); tbody.append(tr); }
    for (const row of rows) {
      const tr = document.createElement('tr');
      tr.append(cell(`#${row.LoanID}`), cell(`${row.Name} (${row.CustomerID})`), cell(`${row.LoanType} · ₹${Number(row.Amount).toLocaleString('en-IN')}`));
      const riskTd = document.createElement('td'); const risk = document.createElement('span'); risk.className = `risk ${(row.CreditRisk || 'medium').toLowerCase()}`; risk.textContent = row.CreditRisk || 'UNKNOWN'; riskTd.append(risk); tr.append(riskTd);
      tr.append(cell(row.ApprovedBy));
      const statusTd = document.createElement('td'); const status = document.createElement('span'); status.className = `status ${(row.Status || '').toLowerCase()}`; status.textContent = row.Status; statusTd.append(status);
      if (Number(row.FraudFlag) === 1) { const flag = document.createElement('div'); flag.textContent = '⚑ Review flag'; flag.className = 'risk high'; flag.style.marginTop = '5px'; statusTd.append(flag); }
      tr.append(statusTd);
      const actionTd = document.createElement('td'); actionTd.className = 'action-cell';
      if (row.Status === 'PENDING') {
        actionTd.append(makeActionButton('Approve','approve',()=>submitDecision(row.LoanID,'APPROVED')),makeActionButton('Reject','reject',()=>submitDecision(row.LoanID,'REJECTED')));
      } else actionTd.textContent = 'Completed';
      tr.append(actionTd); tbody.append(tr);
    }
    staffMessage.textContent = `Loaded ${rows.length} application(s). The backend enforces assignment and prevents a second decision.`;
    staffMessage.classList.remove('hidden'); staffMessage.style.background = '#eef8f1'; staffMessage.style.color = '#0f6659';
  } catch (error) {
    staffMessage.textContent = error.message; staffMessage.classList.remove('hidden'); staffMessage.style.background = '#fff1f1'; staffMessage.style.color = '#9d3737';
    const tr = document.createElement('tr'); const td = cell('Could not load queue. Check staff login.'); td.colSpan = 7; td.className = 'empty-row'; tr.append(td); tbody.append(tr);
  }
}
async function submitDecision(loanId, status) {
  if (!confirm(`Are you sure you want to ${status.toLowerCase()} loan #${loanId}? This action is audited.`)) return;
  try {
    const body = new URLSearchParams({username: staffCredentials.username, password: staffCredentials.password, loanId: String(loanId), status});
    const response = await fetch('/api/decision', {method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body});
    const data = await response.json(); if (!response.ok || !data.success) throw new Error(data.message || 'Decision failed.');
    staffMessage.textContent = data.message; staffMessage.classList.remove('hidden'); await loadLoans();
  } catch (error) { staffMessage.textContent = error.message; staffMessage.classList.remove('hidden'); staffMessage.style.background='#fff1f1';staffMessage.style.color='#9d3737'; }
}

$('#loadAudit').addEventListener('click', async () => {
  try { const result = await staffFetch('/api/audit'); $('#auditOutput').textContent = JSON.stringify(result.data, null, 2); }
  catch (error) { $('#auditOutput').textContent = error.message; }
});
