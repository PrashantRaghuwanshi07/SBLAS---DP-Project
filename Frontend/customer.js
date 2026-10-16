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
    form.reset(); updateLoanOptions();
  } catch (error) {
    showResult('error', 'Application not submitted', error.message || 'Check the form and try again.');
  } finally {
    button.disabled = false; button.innerHTML = 'Submit application <span>↗</span>';
  }
});

