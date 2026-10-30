/*
 * Smoke test for the Duty Tour web app, run in jsdom.
 *
 * It seeds a couple of tours into localStorage, walks every screen, submits the
 * form, and prints a single-tour / all-tours / date-range sheet — checking the
 * rendering, the paise maths (travel + hotel + work charged + other), the
 * watermark wording and its blank state, and the PDF footer.
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

const dom = new JSDOM(html, {
  url: 'http://localhost:8000/#/tours',
  runScripts: 'outside-only',
  pretendToBeVisual: true,
});
const { window } = dom;
window.print = () => { window.__printed = true; };
window.confirm = () => true;
window.scrollTo = () => {};

/*
 * Seed data. Totals are deliberately uneven so a swapped field would show:
 *   a1 = 1,20,050 + 2,50,000 + 5,00,000 (work) + 4,500   = 8,74,550  -> ₹8,745.50
 *   b2 = 80,000                                          = 80,000   -> ₹800.00
 *   grand                                                = 9,54,550  -> ₹9,545.50
 */
const seeded = [
  {
    id: 'a1', startLocation: 'Delhi', destination: 'Jaipur', startDate: '2026-10-04', endDate: '2026-10-07',
    purposeLine: 'Plant visit — QA', travelMode: 'Train', travelAmount: 120050, travelNote: 'PNR 4421',
    hotelName: 'Hotel Neelam', hotelNights: 2, hotelAmount: 250000,
    workAmount: 500000, otherAmount: 4500, otherNote: 'Food',
    workTitle: 'Line audit', workDetails: 'Checked torque stations.\nWrote report.', createdAt: '2026-10-04T09:00:00.000Z'
  },
  {
    id: 'b2', startLocation: 'Jaipur', destination: 'Ajmer', startDate: '2026-10-07', endDate: '',
    travelMode: 'Cab', travelAmount: 80000, workTitle: 'Dealer meeting', createdAt: '2026-10-07T09:00:00.000Z'
  }
];
window.localStorage.setItem('duty-tour:v1:tours', JSON.stringify(seeded));
window.localStorage.setItem('duty-tour:v1:settings', JSON.stringify({
  travellerName: 'Puneet', defaultCity: 'Delhi', watermark: true, watermarkText: 'Hendrickson'
}));


window.eval(js);

function route(hash) {
  window.location.hash = hash;
  window.dispatchEvent(new window.Event('hashchange'));
  return window.document.getElementById('app').innerHTML;
}

const checks = [];
function check(name, cond, extra) { checks.push({ name, ok: !!cond, extra }); }
const count = (text, pattern) => (text.match(pattern) || []).length;

// ---------------------------------------------------------------- list
let out = route('#/tours');
check('list shows masthead', out.includes('Tour log'));
check('list shows route', out.includes('Delhi → Jaipur'));
check('list total includes work charged', out.includes('₹8,745.50'), out.match(/₹[\d,]+\.\d\d/)?.[0]);
check('list grand total includes work charged', out.includes('₹9,545.50'), out.match(/in total[^<]*/)?.[0]);

// ---------------------------------------------------------------- detail
out = route('#/tour/a1');
check('detail shows row total', out.includes('₹8,745.50'));
check('detail shows work charged line', out.includes('Work charged') && out.includes('₹5,000.00'));
check('detail shows others line', out.includes('Others') && out.includes('₹45.00'));
check('detail work text', out.includes('Checked torque stations'));
check('detail traveller tag', out.includes('Puneet'));

// ---------------------------------------------------------------- edit form
out = route('#/tour/a1/edit');
check('edit prefills route', out.includes('value="Delhi"'));
check('edit prefills work amount', out.includes('id="workAmount"') && out.includes('value="5000.00"'));
check('edit total includes work', out.includes('₹8,745.50'));

// ---------------------------------------------------------------- export + settings
out = route('#/export');
check('export offers three scopes',
  out.includes('Single tour') && out.includes('All tours') && out.includes('By date range'));
check('export preview counts tours', out.includes('2 tours'));
out = route('#/settings');
check('settings keeps name', out.includes('value="Puneet"'));
check('settings has watermark text field', out.includes('id="watermarkText"') && out.includes('value="Hendrickson"'));
check('settings explains blank means none', out.includes('Blank means no watermark'));

// ---------------------------------------------------------------- single-tour sheet
route('#/tour/a1');
window.document.querySelector('[data-print-one]').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const sheet = window.document.getElementById('print-root').innerHTML;
check('print called', window.__printed === true);
check('sheet titled DUTY TOUR', sheet.includes('DUTY TOUR'));
check('sheet has company line', sheet.includes('Hendrickson'));
check('sheet lists four cost heads',
  sheet.includes('Travel cost') && sheet.includes('Hotel cost') &&
  sheet.includes('Work charged') && sheet.includes('Others'));
check('sheet total includes work charged', sheet.includes('8,745.50'));
check('sheet carries footer', sheet.includes('Personal duty-tour record. Not a GST invoice.'));
check('sheet has an approval strip', sheet.includes('Submitted by') && sheet.includes('Approved by'));
check('sheet has watermark', sheet.includes('sheet-watermark'));

// ---------------------------------------------------------------- all-tours sheet
route('#/export');
window.document.getElementById('do-print').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const all = window.document.getElementById('print-root').innerHTML;
check('summary sheet present', all.includes('Duty tour summary'));
check('summary + two tour sheets = 3', count(all, /class="sheet"/g) === 3, count(all, /class="sheet"/g));
check('summary grand total includes work', all.includes('9,545.50'));

// ---------------------------------------------------------------- date range
route('#/export');
window.document.getElementById('scope-range').checked = true;
window.document.getElementById('range-from').value = '2026-10-07';
window.document.getElementById('range-to').value = '2026-10-31';
window.document.getElementById('range-from').dispatchEvent(new window.Event('change'));
window.document.getElementById('do-print').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
check('range narrows to one tour', count(window.document.getElementById('print-root').innerHTML, /class="sheet"/g) === 1,
  count(window.document.getElementById('print-root').innerHTML, /class="sheet"/g));

// ---------------------------------------------------------------- custom watermark wording
route('#/settings');
const markText = window.document.getElementById('watermarkText');
markText.value = 'Internal review';
markText.dispatchEvent(new window.Event('input'));
window.document.getElementById('save-settings').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
route('#/tour/a1');
window.document.querySelector('[data-print-one]').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const customMark = window.document.getElementById('print-root').innerHTML;
const markBlock = customMark.match(/<div class="sheet-watermark">[\s\S]*?<\/div>/)?.[0] || '';
check('custom watermark wording printed', markBlock.includes('Internal review'), markBlock);
check('company watermark replaced, not doubled', !markBlock.includes('Hendrickson'), markBlock);
check('custom watermark shrinks for long text', /font-size:\d+pt/.test(customMark));

// ---------------------------------------------------------------- watermark alignment
out = route('#/settings');
check('settings offers both alignments',
  out.includes('id="wmAlignDiagonal"') && out.includes('id="wmAlignCenter"'));
window.document.getElementById('wmAlignCenter').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
route('#/tour/a1');
window.document.querySelector('[data-print-one]').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const centerMark = window.document.getElementById('print-root').innerHTML;
check('center alignment reaches the sheet', centerMark.includes('sheet-watermark center'),
  centerMark.match(/sheet-watermark[^"]*/)?.[0]);
route('#/settings');
window.document.getElementById('wmAlignDiagonal').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
route('#/tour/a1');
window.document.querySelector('[data-print-one]').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const diagMark = window.document.getElementById('print-root').innerHTML;
check('diagonal alignment restores default', diagMark.includes('class="sheet-watermark"'),
  diagMark.match(/sheet-watermark[^"]*/)?.[0]);

// ---------------------------------------------------------------- blank watermark = none
route('#/settings');
const blankMark = window.document.getElementById('watermarkText');
blankMark.value = '';
blankMark.dispatchEvent(new window.Event('input'));
window.document.getElementById('save-settings').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
route('#/tour/a1');
window.document.querySelector('[data-print-one]').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
check('blank watermark prints nothing', !window.document.getElementById('print-root').innerHTML.includes('sheet-watermark'));

// ---------------------------------------------------------------- add a tour
route('#/tour/new');
const form = window.document.getElementById('tour-form');
const type = (id, value) => {
  const el = form.elements[id];
  el.value = value;
  el.dispatchEvent(new window.Event('input'));
};
type('startLocation', 'Gurugram');
type('destination', 'Pune');
type('travelAmount', '2,500.50');
type('workAmount', '1500');
check('live total adds work charged', window.document.getElementById('sticky-total').textContent === '₹4,000.50',
  window.document.getElementById('sticky-total').textContent);
type('workTitle', 'Vendor audit');
form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));

const stored = JSON.parse(window.localStorage.getItem('duty-tour:v1:tours'));
const saved = stored.find(t => t.destination === 'Pune');
check('tour saved', stored.length === 3, stored.length);
check('travel stored in paise', saved && saved.travelAmount === 250050, saved && saved.travelAmount);
check('work amount stored in paise', saved && saved.workAmount === 150000, saved && saved.workAmount);
check('traveller copied from settings', saved && saved.traveller === 'Puneet', saved && saved.traveller);
check('navigated to detail', window.location.hash.startsWith('#/tour/'), window.location.hash);

// ---------------------------------------------------------------- validation
route('#/tour/new');
const form2 = window.document.getElementById('tour-form');
const type2 = (id, value) => {
  const el = form2.elements[id];
  el.value = value;
  el.dispatchEvent(new window.Event('input'));
};
type2('startLocation', '');
type2('destination', 'Pune');
form2.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
check('validation message', window.document.getElementById('form-error').textContent.includes('Start location'),
  window.document.getElementById('form-error').textContent);

let failed = 0;
for (const c of checks) {
  if (!c.ok) failed++;
  console.log((c.ok ? 'PASS ' : 'FAIL ') + c.name + (c.ok ? '' : '  -> ' + JSON.stringify(c.extra)));
}
console.log('\n' + (checks.length - failed) + '/' + checks.length + ' passed');
process.exit(failed ? 1 : 0);
