/*
 * Smoke test for the Duty Tour web app, run in jsdom.
 *
 * It seeds a couple of tours into localStorage, walks every screen, submits the
 * form, and prints a single-tour / all-tours / date-range sheet — checking the
 * rendering, the paise maths, the watermark toggle, and the PDF footer.
 *
 *   cd web && npm install && npm test
 */
import { JSDOM } from 'jsdom';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const webRoot = path.resolve(here, '..');
const html = fs.readFileSync(path.join(webRoot, 'index.html'), 'utf8');
const js = fs.readFileSync(path.join(webRoot, 'app.js'), 'utf8');

const errors = [];
const dom = new JSDOM(html, {
  url: 'http://localhost:8000/#/tours',
  runScripts: 'outside-only',
  pretendToBeVisual: true,
});
const { window } = dom;
window.print = () => { window.__printed = true; };
window.confirm = () => true;

const seeded = [
  {
    id: 'a1', startLocation: 'Delhi', destination: 'Jaipur', startDate: '2026-10-04', endDate: '2026-10-07',
    purposeLine: 'Plant visit — QA', travelMode: 'Train', travelAmount: 120050, travelNote: 'PNR 4421',
    hotelName: 'Hotel Neelam', hotelNights: 2, hotelAmount: 250000, otherAmount: 4500, otherNote: 'Food',
    workTitle: 'Line audit', workDetails: 'Checked torque stations.\nWrote report.', createdAt: '2026-10-04T09:00:00.000Z'
  },
  {
    id: 'b2', startLocation: 'Jaipur', destination: 'Ajmer', startDate: '2026-10-07', endDate: '',
    travelMode: 'Cab', travelAmount: 80000, workTitle: 'Dealer meeting', createdAt: '2026-10-07T09:00:00.000Z'
  }
];
window.localStorage.setItem('duty-tour:v1:tours', JSON.stringify(seeded));
window.localStorage.setItem('duty-tour:v1:settings', JSON.stringify({ travellerName: 'Puneet', defaultCity: 'Delhi', watermark: true }));

window.eval(js);

function route(hash) {
  window.location.hash = hash;
  // jsdom fires hashchange async; force a render via the exposed listener
  window.dispatchEvent(new window.Event('hashchange'));
  return window.document.getElementById('app').innerHTML;
}

const checks = [];
function check(name, cond, extra) { checks.push({ name, ok: !!cond, extra }); }

let out = route('#/tours');
check('list shows masthead', out.includes('Tour log'));
check('list shows route', out.includes('Delhi → Jaipur'));
check('list shows total', out.includes('₹3,745.50'));
check('list shows grand total note', out.includes('₹4,545.50'), out.match(/in total[^<]*/));

out = route('#/tour/a1');
check('detail route', out.includes('Delhi → Jaipur'));
check('detail total', out.includes('₹3,745.50'));
check('detail work', out.includes('Checked torque stations'));
check('detail traveller tag', out.includes('Puneet'));

out = route('#/tour/a1/edit');
check('edit prefills', out.includes('value="Delhi"'));
check('edit amounts', out.includes('value="1200.50"'));
check('edit total', out.includes('₹3,745.50'));

out = route('#/export');
check('export options', out.includes('Single tour') && out.includes('All tours') && out.includes('By date range'));
check('export preview count', out.includes('2 tours'), out.match(/2 tours[^<]*/));

out = route('#/settings');
check('settings name', out.includes('value="Puneet"'));
check('settings company', out.includes('Hendrickson'));
check('settings watermark', out.includes('id="watermark" checked'));

// printing
route('#/tours');
const btn = window.document.querySelector('[data-print-one]');
route('#/tour/a1');
const pbtn = window.document.querySelector('[data-print-one]');
pbtn.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const printed = window.document.getElementById('print-root').innerHTML;
check('print called', window.__printed === true);
check('print has title', printed.includes('DUTY TOUR'));
check('print has company', printed.includes('Hendrickson'));
check('print has watermark span', printed.includes('sheet-watermark'));
check('print has table total', printed.includes('3,745.50'));
check('print footer', printed.includes('Personal duty-tour record. Not a GST invoice.'));

// all-tours export via export screen
route('#/export');
window.document.getElementById('do-print').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const printedAll = window.document.getElementById('print-root').innerHTML;
check('summary sheet present', printedAll.includes('Duty tour summary'));
check('two tour sheets', (printedAll.match(/class="sheet"/g) || []).length === 3, (printedAll.match(/class="sheet"/g) || []).length);

// range export respecting filter
route('#/export');
window.document.getElementById('scope-range').checked = true;
window.document.getElementById('range-from').value = '2026-10-07';
window.document.getElementById('range-to').value = '2026-10-31';
window.document.getElementById('range-from').dispatchEvent(new window.Event('change'));
window.document.getElementById('do-print').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const printedRange = window.document.getElementById('print-root').innerHTML;
check('range picks one', (printedRange.match(/class="sheet"/g) || []).length === 1, (printedRange.match(/class="sheet"/g) || []).length);

// watermark off
window.localStorage.setItem('duty-tour:v1:settings', JSON.stringify({ travellerName: 'Puneet', defaultCity: 'Delhi', watermark: false }));
route('#/settings');
window.document.getElementById('watermark').checked = false;
window.document.getElementById('watermark').dispatchEvent(new window.Event('change'));
route('#/tour/a1');
window.document.querySelector('[data-print-one]').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
check('watermark can be off', !window.document.getElementById('print-root').innerHTML.includes('sheet-watermark'));

// add a tour through the form
route('#/tour/new');
const form = window.document.getElementById('tour-form');
form.elements['startLocation'].value = 'Gurugram';
form.elements['startLocation'].dispatchEvent(new window.Event('input'));
form.elements['destination'].value = 'Pune';
form.elements['destination'].dispatchEvent(new window.Event('input'));
form.elements['travelAmount'].value = '2,500.50';
form.elements['travelAmount'].dispatchEvent(new window.Event('input'));
check('live total updates', window.document.getElementById('sticky-total').textContent === '₹2,500.50', window.document.getElementById('sticky-total').textContent);
form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
const stored = JSON.parse(window.localStorage.getItem('duty-tour:v1:tours'));
check('tour saved', stored.length === 3, stored.length);
check('paise stored', stored[2].travelAmount === 250050, stored[2].travelAmount);
check('traveller copied from settings', stored[2].traveller === 'Puneet', stored[2].traveller);
check('navigated to detail', window.location.hash.startsWith('#/tour/'), window.location.hash);

// validation
route('#/tour/new');
const form2 = window.document.getElementById('tour-form');
form2.elements['startLocation'].value = '';
form2.elements['startLocation'].dispatchEvent(new window.Event('input'));
form2.elements['destination'].value = 'Pune';
form2.elements['destination'].dispatchEvent(new window.Event('input'));
form2.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
check('validation message', window.document.getElementById('form-error').textContent.includes('Start location'), window.document.getElementById('form-error').textContent);

let failed = 0;
for (const c of checks) {
  if (!c.ok) failed++;
  console.log((c.ok ? 'PASS ' : 'FAIL ') + c.name + (c.ok ? '' : '  -> ' + JSON.stringify(c.extra)));
}
console.log('\n' + (checks.length - failed) + '/' + checks.length + ' passed');
process.exit(failed ? 1 : 0);
