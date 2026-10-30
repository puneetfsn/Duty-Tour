/*
 * Duty Tour — web edition.
 *
 * Plain HTML/CSS/JS, no build step, no dependencies, no network calls.
 * Everything the traveller types lives in this browser's localStorage.
 * Print / Save as PDF uses the browser's own print engine: no PDF library.
 */
(function () {
  'use strict';

  // ---------------------------------------------------------------- constants

  var COMPANY = 'Hendrickson';
  var CURRENCY = '₹';
  var DEFAULT_WATERMARK = COMPANY;
  var STORE_TOURS = 'duty-tour:v1:tours';
  var STORE_SETTINGS = 'duty-tour:v1:settings';
  var TRAVEL_MODES = ['Train', 'Bus', 'Cab', 'Flight', 'Other'];

  // ------------------------------------------------------------------ storage

  function readJSON(key, fallback) {
    try {
      var raw = localStorage.getItem(key);
      if (!raw) return fallback;
      var value = JSON.parse(raw);
      return value == null ? fallback : value;
    } catch (err) {
      return fallback;
    }
  }

  function writeJSON(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch (err) {
      toast('Could not save — this browser may be out of storage.');
      return false;
    }
  }

  function normaliseSettings(raw) {
    raw = raw || {};
    return {
      travellerName: typeof raw.travellerName === 'string' ? raw.travellerName : '',
      defaultCity: typeof raw.defaultCity === 'string' ? raw.defaultCity : '',
      watermark: raw.watermark === true,
      // Custom watermark text. Empty means "print no watermark at all".
      watermarkText: typeof raw.watermarkText === 'string' ? raw.watermarkText : DEFAULT_WATERMARK,
      // Watermark placement on the sheet: 'diagonal' (default) or 'center'.
      watermarkAlign: raw.watermarkAlign === 'center' ? 'center' : 'diagonal'
    };
  }

  function normaliseTour(raw) {
    raw = raw || {};
    var modes = TRAVEL_MODES;
    return {
      id: String(raw.id || uid()),
      traveller: str(raw.traveller),
      startLocation: str(raw.startLocation),
      destination: str(raw.destination),
      startDate: isoOrEmpty(raw.startDate),
      endDate: isoOrEmpty(raw.endDate),
      purposeLine: str(raw.purposeLine),
      travelMode: modes.indexOf(raw.travelMode) >= 0 ? raw.travelMode : 'Train',
      travelAmount: int(raw.travelAmount),
      travelNote: str(raw.travelNote),
      hotelName: str(raw.hotelName),
      hotelNights: int(raw.hotelNights),
      hotelAmount: int(raw.hotelAmount),
      workAmount: int(raw.workAmount),
      otherAmount: int(raw.otherAmount),
      otherNote: str(raw.otherNote),
      workTitle: str(raw.workTitle),
      workDetails: str(raw.workDetails),
      createdAt: raw.createdAt || new Date().toISOString(),
      updatedAt: raw.updatedAt || raw.createdAt || new Date().toISOString()
    };
  }

  function str(value) { return typeof value === 'string' ? value : ''; }
  function int(value) {
    var n = Number(value);
    return isFinite(n) && n > 0 ? Math.round(n) : 0;
  }
  function isoOrEmpty(value) {
    return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : '';
  }
  function uid() {
    return 't' + Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
  }

  var settings = normaliseSettings(readJSON(STORE_SETTINGS, {}));
  var tours = (function () {
    var raw = readJSON(STORE_TOURS, []);
    return Array.isArray(raw) ? raw.map(normaliseTour) : [];
  })();

  function saveSettings() { writeJSON(STORE_SETTINGS, settings); }
  function saveTours() { writeJSON(STORE_TOURS, tours); }

  function tourById(id) {
    for (var i = 0; i < tours.length; i++) if (tours[i].id === String(id)) return tours[i];
    return null;
  }

  function sortedTours() {
    return tours.slice().sort(function (a, b) {
      if (a.startDate === b.startDate) return a.createdAt < b.createdAt ? 1 : -1;
      return a.startDate < b.startDate ? 1 : -1;
    });
  }

  /** Travel + hotel + work charged + other. One number, four buckets, no hidden extras. */
  function totalOf(tour) {
    return (tour.travelAmount || 0) + (tour.hotelAmount || 0) +
      (tour.workAmount || 0) + (tour.otherAmount || 0);
  }

  // -------------------------------------------------------------------- money

  var inr = new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

  function money(paise) { return CURRENCY + amount(paise); }
  function amount(paise) { return inr.format((int(paise) || 0) / 100); }

  /** "1,200.50" -> 120050. Junk becomes 0; typing never throws. */
  function parsePaise(input) {
    var cleaned = String(input == null ? '' : input).replace(/[^\d.]/g, '');
    if (!cleaned || cleaned === '.') return 0;
    var firstDot = cleaned.indexOf('.');
    if (firstDot >= 0) {
      cleaned = cleaned.slice(0, firstDot + 1) + cleaned.slice(firstDot + 1).replace(/\./g, '');
    }
    var value = parseFloat(cleaned);
    if (!isFinite(value) || value < 0) return 0;
    return Math.round(value * 100);
  }

  function editableAmount(paise) {
    return paise ? (paise / 100).toFixed(2) : '';
  }

  // -------------------------------------------------------------------- dates

  function todayISO() {
    var d = new Date();
    var m = String(d.getMonth() + 1).padStart(2, '0');
    var day = String(d.getDate()).padStart(2, '0');
    return d.getFullYear() + '-' + m + '-' + day;
  }

  var dayFmt = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  var shortFmt = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short' });

  function fromISO(iso) {
    if (!iso) return null;
    var parts = iso.split('-');
    var d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
    return isNaN(d.getTime()) ? null : d;
  }

  function formatDate(iso) {
    var d = fromISO(iso);
    return d ? dayFmt.format(d) : '—';
  }

  function formatRange(startISO, endISO) {
    if (!endISO || endISO === startISO) return formatDate(startISO);
    var a = fromISO(startISO);
    var b = fromISO(endISO);
    if (!a || !b) return formatDate(startISO);
    if (a.getFullYear() === b.getFullYear()) {
      return shortFmt.format(a) + ' – ' + dayFmt.format(b);
    }
    return dayFmt.format(a) + ' – ' + dayFmt.format(b);
  }

  function nightsBetween(startISO, endISO) {
    var a = fromISO(startISO);
    var b = fromISO(endISO);
    if (!a || !b) return 0;
    var days = Math.round((b - a) / 86400000);
    return days > 0 ? days : 0;
  }

  // -------------------------------------------------------------------- utils

  function esc(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  var toastTimer = null;
  function toast(message) {
    var el = document.querySelector('.toast');
    if (!el) {
      el = document.createElement('div');
      el.className = 'toast';
      document.body.appendChild(el);
    }
    el.textContent = message;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove('show'); }, 2600);
  }

  function routeLabel(tour) {
    return (tour.startLocation || '—') + ' → ' + (tour.destination || '—');
  }

  // ------------------------------------------------------------------ routing

  var app = document.getElementById('app');

  /** The working copy while a form is open; null everywhere else. */
  var draft = null;

  function parseRoute() {
    var hash = location.hash.replace(/^#\/?/, '');
    var parts = hash.split('/').filter(Boolean);
    if (parts.length === 0) return { view: 'tours' };
    if (parts[0] === 'tour') {
      if (parts[1] === 'new') return { view: 'form', id: null };
      if (parts[1]) {
        if (parts[2] === 'edit') return { view: 'form', id: parts[1] };
        return { view: 'detail', id: parts[1] };
      }
    }
    if (parts[0] === 'export') return { view: 'export' };
    if (parts[0] === 'settings') return { view: 'settings' };
    return { view: 'tours' };
  }

  function render() {
    var route = parseRoute();
    var draftId = route.view === 'form' ? (route.id || 'new') : null;

    // Rebuild the working copy only when the form target changes, so typing survives.
    if (route.view === 'form') {
      if (!draft || draft.__key !== draftId) {
        draft = buildDraft(route.id);
        draft.__key = draftId;
      }
    } else {
      draft = null;
    }

    if (route.view === 'tours') app.innerHTML = viewTours();
    else if (route.view === 'form') app.innerHTML = viewForm();
    else if (route.view === 'detail') app.innerHTML = viewDetail(route.id);
    else if (route.view === 'export') app.innerHTML = viewExport();
    else if (route.view === 'settings') app.innerHTML = viewSettings();

    markTab(route.view);
    wire(route);
    updateNameBanner();
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function markTab(view) {
    var tab = view === 'form' || view === 'detail' ? 'tours' : view;
    document.querySelectorAll('.tabs a').forEach(function (a) {
      if (a.dataset.tab === tab) a.setAttribute('aria-current', 'page');
      else a.removeAttribute('aria-current');
    });
  }

  function updateNameBanner() {
    var banner = document.getElementById('name-banner');
    if (!banner) return;
    banner.hidden = !!settings.travellerName.trim();
  }

  function go(path) {
    if (location.hash === '#/' + path) render();
    else location.hash = '#/' + path;
  }

  // ------------------------------------------------------------------- views

  function viewTours() {
    var list = sortedTours();
    var grand = list.reduce(function (sum, t) { return sum + totalOf(t); }, 0);

    var body;
    if (list.length === 0) {
      body =
        '<div class="empty">' +
          '<h2>No tours yet.</h2>' +
          '<p>Each tour is one sitting: route, dates, what was spent, what was done.</p>' +
          '<p>Everything stays in this browser, on this device.</p>' +
          '<p style="margin-top:14px"><a class="btn" href="#/tour/new">Add the first tour</a></p>' +
        '</div>';
    } else {
      body = '<ul class="tour-list">' + list.map(function (t) {
        return '<li>' +
          '<button type="button" class="tour-row" data-open="' + esc(t.id) + '">' +
            '<span class="tour-main">' +
              '<span class="tour-route">' + esc(routeLabel(t)) + '</span>' +
              '<span class="tour-meta">' + esc(formatRange(t.startDate, t.endDate)) +
                (t.workTitle ? ' · ' + esc(t.workTitle) : '') + '</span>' +
            '</span>' +
            '<span class="tour-total">' + money(totalOf(t)) + '</span>' +
          '</button>' +
        '</li>';
      }).join('') + '</ul>';
    }

    return '' +
      '<div class="page-head">' +
        '<div>' +
          '<h2 class="page-title">Tour log</h2>' +
          '<p class="page-note">' + (list.length === 0
            ? 'Nothing recorded yet'
            : list.length + (list.length === 1 ? ' tour' : ' tours') +
              ' · ' + money(grand) + ' in total') + '</p>' +
        '</div>' +
        '<div class="btn-row">' +
          (list.length ? '<a class="btn ghost" href="#/export">Export</a>' : '') +
          '<a class="btn" href="#/tour/new">New tour</a>' +
        '</div>' +
      '</div>' + body;
  }

  function viewForm() {
    var d = draft;
    var editing = !!d.id;
    var nights = nightsBetween(d.startDate, d.endDate);

    return '' +
      '<div class="page-head">' +
        '<div>' +
          '<h2 class="page-title">' + (editing ? 'Edit tour' : 'New tour') + '</h2>' +
          '<p class="page-note">' + COMPANY + '</p>' +
        '</div>' +
        '<div class="btn-row">' +
          '<a class="btn quiet" href="' + (editing ? '#/tour/' + esc(d.id) : '#/tours') + '">Cancel</a>' +
          '<button type="submit" form="tour-form" class="btn">Save tour</button>' +
        '</div>' +
      '</div>' +

      '<form id="tour-form" novalidate>' +

        '<div class="section" style="border-top:none;padding-top:0;margin-top:0">' +
          '<p class="section-title">Route</p>' +
          '<div class="grid-2">' +
            field('startLocation', 'Start location', d.startLocation, 'text', 'Where the tour began') +
            field('destination', 'Destination', d.destination, 'text', 'Where it ended') +
          '</div>' +
          field('purposeLine', 'Client / office / purpose (optional)', d.purposeLine, 'text', '') +
        '</div>' +

        '<div class="section">' +
          '<p class="section-title">Dates</p>' +
          '<div class="grid-2">' +
            field('startDate', 'Start date', d.startDate, 'date', '') +
            field('endDate', 'End date (optional)', d.endDate, 'date', nights ? nights + ' nights away' : 'One-day tour if left blank') +
          '</div>' +
        '</div>' +

        '<div class="section">' +
          '<p class="section-title">Travel spend</p>' +
          '<div class="chips" role="group" aria-label="Travel mode">' +
            TRAVEL_MODES.map(function (mode) {
              return '<button type="button" class="chip" data-mode="' + mode + '" aria-pressed="' +
                (d.travelMode === mode ? 'true' : 'false') + '">' + mode + '</button>';
            }).join('') +
          '</div>' +
          '<div class="grid-money">' + field('travelAmount', 'Travel amount', d.travelAmountText, 'text', 'Rupees and paise, e.g. 1200.50') + '</div>' +
          field('travelNote', 'Note (optional)', d.travelNote, 'text', 'PNR, cab number, anything useful') +
        '</div>' +

        '<div class="section">' +
          '<p class="section-title">Hotel (optional)</p>' +
          field('hotelName', 'Hotel name', d.hotelName, 'text', '') +
          '<div class="grid-2">' +
            field('hotelNights', 'Nights', d.hotelNightsText, 'number', '') +
            field('hotelAmount', 'Hotel amount', d.hotelAmountText, 'text', '') +
          '</div>' +
        '</div>' +

        '<div class="section">' +
          '<p class="section-title">Other spend (optional)</p>' +
          '<div class="grid-2">' +
            field('otherAmount', 'Other amount', d.otherAmountText, 'text', '') +
            field('otherNote', 'What was it for?', d.otherNote, 'text', 'Food, parking, tips') +
          '</div>' +
        '</div>' +

        '<div class="section">' +
          '<p class="section-title">Work done</p>' +
          field('workTitle', 'Short title', d.workTitle, 'text', '') +
          '<div class="field">' +
            '<label for="workDetails">Details</label>' +
            '<textarea id="workDetails" name="workDetails" rows="5">' + esc(d.workDetails) + '</textarea>' +
          '</div>' +
          '<div class="grid-money">' +
            field('workAmount', 'Work charged amount', d.workAmountText, 'text',
              'Charged for this work. Added to the total as its own line.') +
          '</div>' +
        '</div>' +

        '<div class="section">' +
          '<p class="section-title">Traveller on this entry</p>' +
          field('traveller', 'Name printed on the PDF', d.traveller, 'text',
            'Defaults to the name saved in Settings, and can differ per entry.') +
        '</div>' +

        '<div class="totals" id="totals">' + totalsMarkup(d) + '</div>' +
        '<p class="error" id="form-error" role="alert"></p>' +

        '<div class="sticky-total">' +
          '<div>' +
            '<div class="label">Total</div>' +
            '<div class="value" id="sticky-total">' + money(draftTotal()) + '</div>' +
          '</div>' +
          '<button type="submit" form="tour-form" class="btn">Save tour</button>' +
        '</div>' +

      '</form>';
  }

  function field(name, label, value, type, hint) {
    return '<div class="field">' +
      '<label for="' + name + '">' + esc(label) + '</label>' +
      '<input id="' + name + '" name="' + name + '" type="' + type + '"' +
        (type === 'text' ? ' autocomplete="off"' : '') +
        (type === 'text' && /amount/i.test(name) ? ' inputmode="decimal"' : '') +
        ' value="' + esc(value == null ? '' : value) + '" />' +
      (hint ? '<span class="hint">' + esc(hint) + '</span>' : '') +
    '</div>';
  }

  function totalsMarkup(d) {
    var travel = parsePaise(d.travelAmountText);
    var hotel = parsePaise(d.hotelAmountText);
    var work = parsePaise(d.workAmountText);
    var other = parsePaise(d.otherAmountText);
    return '' +
      line('Travel cost', travel) +
      line('Hotel cost', hotel) +
      line('Work charged', work) +
      line('Others', other) +
      '<div class="total-line grand"><span>Total</span><span>' +
        money(travel + hotel + work + other) + '</span></div>';

    function line(label, paise) {
      return '<div class="total-line"><span>' + label + '</span><span>' + money(paise) + '</span></div>';
    }
  }

  function draftTotal() {
    return parsePaise(draft.travelAmountText) +
      parsePaise(draft.hotelAmountText) +
      parsePaise(draft.workAmountText) +
      parsePaise(draft.otherAmountText);
  }

  function viewDetail(id) {
    var t = tourById(id);
    if (!t) return missing();

    var rows = '';
    rows += kv('Travel (' + esc(t.travelMode) + ')', money(t.travelAmount), t.travelNote);
    if (t.hotelName || t.hotelNights || t.hotelAmount) {
      var hotelLabel = 'Hotel' + (t.hotelName ? ' — ' + esc(t.hotelName) : '') +
        (t.hotelNights ? ' (' + t.hotelNights + (t.hotelNights === 1 ? ' night' : ' nights') + ')' : '');
      rows += kv(hotelLabel, money(t.hotelAmount), '');
    }
    if (t.workAmount) rows += kv('Work charged', money(t.workAmount), '');
    if (t.otherAmount) rows += kv('Others' + (t.otherNote ? ' — ' + esc(t.otherNote) : ''), money(t.otherAmount), '');
    rows += '<div class="kv total"><span class="k">Total</span><span class="v">' + money(totalOf(t)) + '</span></div>';

    var work = (t.workTitle || t.workDetails)
      ? (t.workTitle ? '<p class="work-title" style="font-weight:600;margin:0 0 6px">' + esc(t.workTitle) + '</p>' : '') +
        (t.workDetails ? '<p class="prose">' + esc(t.workDetails) + '</p>' : '')
      : '<p class="page-note">Nothing recorded.</p>';

    return '' +
      '<div class="page-head">' +
        '<div class="detail-head">' +
          '<h2>' + esc(routeLabel(t)) + '</h2>' +
          '<p>' + esc(formatRange(t.startDate, t.endDate)) +
            (t.purposeLine ? ' · ' + esc(t.purposeLine) : '') +
            (function () {
              var who = t.traveller || settings.travellerName;
              return who ? '<span class="tag">' + esc(who) + '</span>' : '';
            })() + '</p>' +
        '</div>' +
        '<div class="btn-row">' +
          '<a class="btn ghost" href="#/tours">Back</a>' +
          '<a class="btn ghost" href="#/tour/' + esc(t.id) + '/edit">Edit</a>' +
          '<button type="button" class="btn" data-print-one="' + esc(t.id) + '">Print / PDF</button>' +
        '</div>' +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">Spend</p>' + rows +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">Work done</p>' + work +
      '</div>' +

      '<div class="card">' +
        '<div class="btn-row" style="justify-content:space-between">' +
          '<button type="button" class="btn danger" data-delete="' + esc(t.id) + '">Delete tour</button>' +
          '<span class="page-note">Saved on this device only</span>' +
        '</div>' +
      '</div>';
  }

  function kv(label, value, note) {
    return '<div class="kv"><span class="k">' + label +
      (note ? '<br /><span style="font-size:0.8rem">' + esc(note) + '</span>' : '') +
      '</span><span class="v">' + value + '</span></div>';
  }

  /** One honest sentence about what the watermark will do on this sheet. */
  function watermarkNote() {
    var text = (settings.watermarkText || '').trim();
    if (!settings.watermark || !text) return 'No watermark will be printed.';
    return 'Watermark: “' + esc(text) + '”.';
  }

  function viewExport() {
    var list = sortedTours();
    var options = list.map(function (t) {
      return '<option value="' + esc(t.id) + '">' +
        esc(formatRange(t.startDate, t.endDate)) + ' — ' + esc(routeLabel(t)) +
        ' · ' + money(totalOf(t)) + '</option>';
    }).join('');

    if (list.length === 0) {
      return '<div class="page-head"><div><h2 class="page-title">Export</h2>' +
        '<p class="page-note">Nothing to export yet.</p></div></div>' +
        '<div class="empty"><h2>No tours yet.</h2><p>Add a tour first, then export it.</p>' +
        '<p style="margin-top:14px"><a class="btn" href="#/tour/new">Add a tour</a></p></div>';
    }

    var oldest = list[list.length - 1].startDate || todayISO();
    var newest = list[0].startDate || todayISO();

    return '' +
      '<div class="page-head">' +
        '<div>' +
          '<h2 class="page-title">Export</h2>' +
          '<p class="page-note">A4 sheets. ' + watermarkNote() + ' Use your browser’s print dialog and choose <strong>Save as PDF</strong>.</p>' +
        '</div>' +
      '</div>' +

      '<div class="card" id="export-card">' +
        '<div class="export-option">' +
          '<input type="radio" name="scope" id="scope-one" value="one" />' +
          '<div>' +
            '<label for="scope-one"><h3>Single tour</h3>' +
            '<p>One sheet for one tour.</p></label>' +
            '<div class="field" style="margin:10px 0 0;max-width:520px">' +
              '<label for="export-tour">Which tour</label>' +
              '<select id="export-tour">' + options + '</select>' +
            '</div>' +
          '</div>' +
          '<div></div>' +
        '</div>' +

        '<div class="export-option">' +
          '<input type="radio" name="scope" id="scope-all" value="all" checked />' +
          '<div>' +
            '<label for="scope-all"><h3>All tours</h3>' +
            '<p>One PDF, one sheet per tour, oldest first.</p></label>' +
          '</div>' +
          '<div></div>' +
        '</div>' +

        '<div class="export-option">' +
          '<input type="radio" name="scope" id="scope-range" value="range" />' +
          '<div>' +
            '<label for="scope-range"><h3>By date range</h3>' +
            '<p>Include every tour that starts inside the range.</p></label>' +
            '<div class="grid-2" style="margin-top:10px;max-width:460px">' +
              '<div class="field"><label for="range-from">From</label>' +
                '<input type="date" id="range-from" value="' + esc(oldest) + '" /></div>' +
              '<div class="field"><label for="range-to">To</label>' +
                '<input type="date" id="range-to" value="' + esc(newest) + '" /></div>' +
            '</div>' +
          '</div>' +
          '<div></div>' +
        '</div>' +

        '<div class="section" style="margin-top:8px">' +
          '<label class="checkbox-row">' +
            '<input type="checkbox" id="include-summary" checked />' +
            '<span>Add a summary sheet first (traveller, period, grand total)</span>' +
          '</label>' +
        '</div>' +

        '<p class="page-note" id="export-preview" style="margin-top:14px"></p>' +

        '<div class="btn-row" style="margin-top:16px">' +
          '<button type="button" class="btn" id="do-print">Print / Save as PDF</button>' +
          '<span class="page-note">Nothing is uploaded. The PDF is built by this browser.</span>' +
        '</div>' +
      '</div>';
  }

  function viewSettings() {
    return '' +
      '<div class="page-head">' +
        '<div>' +
          '<h2 class="page-title">Settings</h2>' +
          '<p class="page-note">Saved in this browser, for every entry.</p>' +
        '</div>' +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">Traveller</p>' +
        '<div class="field">' +
          '<label for="travellerName">Your name</label>' +
          '<input id="travellerName" type="text" autocomplete="name" value="' + esc(settings.travellerName) + '" placeholder="Name printed on PDFs" />' +
          '<span class="hint">Pre-fills new entries. Each entry can still override it.</span>' +
        '</div>' +
        '<div class="field">' +
          '<label for="defaultCity">Default city</label>' +
          '<input id="defaultCity" type="text" autocomplete="off" value="' + esc(settings.defaultCity) + '" placeholder="Start location for a new tour" />' +
        '</div>' +
        '<div class="btn-row"><button type="button" class="btn" id="save-settings">Save</button></div>' +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">Company</p>' +
        '<div class="kv"><span class="k">Company</span><span class="v">' + COMPANY + '</span></div>' +
        '<div class="kv"><span class="k">Currency</span><span class="v">' + CURRENCY + ' INR — amounts kept to the paise</span></div>' +
        '<div class="kv"><span class="k">Records</span><span class="v">' + tours.length + ' on this device</span></div>' +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">PDF</p>' +
        '<div class="switch-row">' +
          '<div class="switch-text">' +
            '<strong>Watermark</strong>' +
            '<small>A light word on each printed sheet. Your wording and your placement — leave the box empty for no watermark at all.</small>' +
          '</div>' +
          '<label class="checkbox-row"><input type="checkbox" id="watermark" ' + (settings.watermark ? 'checked' : '') + ' /> <span>Show</span></label>' +
        '</div>' +
        '<div class="field" style="margin-top:14px;max-width:420px">' +
          '<label for="watermarkText">Watermark text</label>' +
          '<input id="watermarkText" type="text" autocomplete="off" maxlength="40" value="' + esc(settings.watermarkText) + '" placeholder="Leave blank for none" />' +
          '<span class="hint">Blank means no watermark. Saved with the button above.</span>' +
        '</div>' +
        '<div class="field" style="margin-top:14px;max-width:420px">' +
          '<label>Alignment</label>' +
          '<div class="chips" role="group">' +
            '<button type="button" class="chip" id="wmAlignDiagonal" aria-pressed="' + (settings.watermarkAlign !== 'center') + '">Diagonal</button>' +
            '<button type="button" class="chip" id="wmAlignCenter" aria-pressed="' + (settings.watermarkAlign === 'center') + '">Center</button>' +
          '</div>' +
          '<span class="hint">Diagonal tilts across the sheet; Center sits horizontal in the middle.</span>' +
        '</div>' +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">Data</p>' +
        '<p class="page-note" style="margin-bottom:12px">Backups are plain JSON files you keep yourself. No account is involved.</p>' +
        '<div class="btn-row">' +
          '<button type="button" class="btn ghost" id="backup">Download backup</button>' +
          '<button type="button" class="btn ghost" id="restore">Restore backup</button>' +
          '<input type="file" id="restore-file" accept="application/json,.json" hidden />' +
          '<button type="button" class="btn danger" id="clear-all">Clear all data</button>' +
        '</div>' +
      '</div>' +

      '<div class="card">' +
        '<p class="section-title">About</p>' +
        '<p class="page-note">Duty Tour runs entirely in this browser: no account, no cloud, no analytics, no ads, ' +
        'and no network requests at all. Data lives in localStorage on this device, so a different browser ' +
        'or a cleared cache starts empty — take a backup before clearing anything.</p>' +
        '<p class="page-note" style="margin-top:8px">Amounts and place names only. Duty Tour never asks for bank passwords, UPI PINs, card CVV, or OTPs.</p>' +
        '<p class="page-note" style="margin-top:8px">Version 1.0 · ' + COMPANY + '</p>' +
      '</div>';
  }

  function missing() {
    return '<div class="page-head"><div><h2 class="page-title">Tour not found</h2>' +
      '<p class="page-note">It may have been deleted.</p></div>' +
      '<a class="btn" href="#/tours">Back to tours</a></div>';
  }

  // -------------------------------------------------------------------- draft

  function buildDraft(id) {
    var existing = id ? tourById(id) : null;
    if (existing) {
      return {
        __key: id,
        id: existing.id,
        traveller: existing.traveller || settings.travellerName,
        startLocation: existing.startLocation,
        destination: existing.destination,
        purposeLine: existing.purposeLine,
        startDate: existing.startDate,
        endDate: existing.endDate,
        travelMode: existing.travelMode,
        travelAmountText: editableAmount(existing.travelAmount),
        travelNote: existing.travelNote,
        hotelName: existing.hotelName,
        hotelNightsText: existing.hotelNights ? String(existing.hotelNights) : '',
        hotelAmountText: editableAmount(existing.hotelAmount),
        workAmountText: editableAmount(existing.workAmount),
        otherAmountText: editableAmount(existing.otherAmount),
        otherNote: existing.otherNote,
        workTitle: existing.workTitle,
        workDetails: existing.workDetails,
        createdAt: existing.createdAt
      };
    }
    return {
      __key: id || 'new',
      id: null,
      traveller: settings.travellerName,
      startLocation: settings.defaultCity,
      destination: '',
      purposeLine: '',
      startDate: todayISO(),
      endDate: '',
      travelMode: 'Train',
      travelAmountText: '',
      travelNote: '',
      hotelName: '',
      hotelNightsText: '',
      hotelAmountText: '',
      workAmountText: '',
      otherAmountText: '',
      otherNote: '',
      workTitle: '',
      workDetails: '',
      createdAt: new Date().toISOString()
    };
  }

  function submitForm() {
    var d = draft;
    var errorEl = document.getElementById('form-error');

    var problem = '';
    if (!d.startLocation.trim()) problem = 'Start location is required.';
    else if (!d.destination.trim()) problem = 'Destination is required.';
    else if (!d.startDate) problem = 'Start date is required.';
    else if (d.endDate && d.endDate < d.startDate) problem = 'End date cannot be before the start date.';

    if (problem) {
      if (errorEl) errorEl.textContent = problem;
      return;
    }

    var payload = {
      id: d.id || uid(),
      traveller: d.traveller.trim() || settings.travellerName.trim(),
      startLocation: d.startLocation.trim(),
      destination: d.destination.trim(),
      purposeLine: d.purposeLine.trim(),
      startDate: d.startDate,
      endDate: d.endDate || '',
      travelMode: d.travelMode,
      travelAmount: parsePaise(d.travelAmountText),
      travelNote: d.travelNote.trim(),
      hotelName: d.hotelName.trim(),
      hotelNights: Number(d.hotelNightsText) || 0,
      hotelAmount: parsePaise(d.hotelAmountText),
      workAmount: parsePaise(d.workAmountText),
      otherAmount: parsePaise(d.otherAmountText),
      otherNote: d.otherNote.trim(),
      workTitle: d.workTitle.trim(),
      workDetails: d.workDetails.trim(),
      createdAt: d.createdAt,
      updatedAt: new Date().toISOString()
    };

    var index = -1;
    for (var i = 0; i < tours.length; i++) if (tours[i].id === payload.id) index = i;
    if (index >= 0) tours[index] = normaliseTour(payload);
    else tours.push(normaliseTour(payload));

    saveTours();
    draft = null;
    toast(index >= 0 ? 'Tour updated' : 'Tour saved');
    go('tour/' + payload.id);
  }

  // -------------------------------------------------------------------- wiring

  function wire(route) {
    if (route.view === 'tours') {
      app.querySelectorAll('[data-open]').forEach(function (el) {
        el.addEventListener('click', function () { go('tour/' + el.dataset.open); });
      });
    }

    if (route.view === 'form') wireForm();

    if (route.view === 'detail') {
      var printOne = app.querySelector('[data-print-one]');
      if (printOne) printOne.addEventListener('click', function () {
        printTours([tourById(printOne.dataset.printOne)], { summary: false });
      });
      var del = app.querySelector('[data-delete]');
      if (del) del.addEventListener('click', function () {
        var t = tourById(del.dataset.delete);
        if (!t) return;
        if (!window.confirm('Delete this tour? This cannot be undone.')) return;
        tours = tours.filter(function (x) { return x.id !== t.id; });
        saveTours();
        toast('Tour deleted');
        go('tours');
      });
    }

    if (route.view === 'export') wireExport();
    if (route.view === 'settings') wireSettings();
  }

  function wireForm() {
    var form = document.getElementById('tour-form');
    if (!form) return;

    var simpleFields = [
      'startLocation', 'destination', 'purposeLine', 'startDate', 'endDate',
      'travelNote', 'hotelName', 'otherNote', 'workTitle', 'workDetails', 'traveller'
    ];

    simpleFields.forEach(function (name) {
      var el = form.elements[name];
      if (!el) return;
      el.addEventListener('input', function () {
        draft[name] = el.value;
        if (name === 'startDate' || name === 'endDate') refreshTotals();
      });
    });

    ['travelAmount', 'hotelAmount', 'workAmount', 'otherAmount'].forEach(function (name) {
      var el = form.elements[name];
      if (!el) return;
      el.addEventListener('input', function () {
        draft[name + 'Text'] = el.value;
        refreshTotals();
      });
    });

    var nights = form.elements['hotelNights'];
    if (nights) nights.addEventListener('input', function () {
      draft.hotelNightsText = nights.value.replace(/[^\d]/g, '');
    });

    app.querySelectorAll('.chip[data-mode]').forEach(function (chip) {
      chip.addEventListener('click', function () {
        draft.travelMode = chip.dataset.mode;
        app.querySelectorAll('.chip[data-mode]').forEach(function (other) {
          other.setAttribute('aria-pressed', other === chip ? 'true' : 'false');
        });
      });
    });

    form.addEventListener('submit', function (event) {
      event.preventDefault();
      submitForm();
    });

    function refreshTotals() {
      var totals = document.getElementById('totals');
      if (totals) totals.innerHTML = totalsMarkup(draft);
      var sticky = document.getElementById('sticky-total');
      if (sticky) sticky.textContent = money(draftTotal());
      var errorEl = document.getElementById('form-error');
      if (errorEl) errorEl.textContent = '';
    }
  }

  function currentExportSelection() {
    var scope = document.querySelector('input[name="scope"]:checked');
    var value = scope ? scope.value : 'all';
    if (value === 'one') {
      var select = document.getElementById('export-tour');
      var t = select ? tourById(select.value) : null;
      return { list: t ? [t] : [], label: 'Selected tour' };
    }
    if (value === 'range') {
      var from = (document.getElementById('range-from') || {}).value || '';
      var to = (document.getElementById('range-to') || {}).value || '';
      var list = sortedTours().filter(function (t) {
        if (from && t.startDate < from) return false;
        if (to && t.startDate > to) return false;
        return true;
      });
      return { list: list, label: 'Range ' + (from ? formatDate(from) : 'start') + ' – ' + (to ? formatDate(to) : 'today'), from: from, to: to };
    }
    return { list: sortedTours(), label: 'All tours' };
  }

  function wireExport() {
    var preview = document.getElementById('export-preview');

    function refresh() {
      var selection = currentExportSelection();
      var sum = selection.list.reduce(function (acc, t) { return acc + totalOf(t); }, 0);
      preview.textContent = selection.list.length === 0
        ? 'No tours match this selection.'
        : selection.list.length + (selection.list.length === 1 ? ' tour' : ' tours') +
          ' · ' + money(sum) + ' · ' + selection.list.length +
          (selection.list.length === 1 ? ' sheet' : ' sheets');
    }

    document.querySelectorAll('input[name="scope"]').forEach(function (radio) {
      radio.addEventListener('change', refresh);
    });
    ['export-tour', 'range-from', 'range-to'].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.addEventListener('change', refresh);
    });
    refresh();

    var button = document.getElementById('do-print');
    if (button) button.addEventListener('click', function () {
      var selection = currentExportSelection();
      if (selection.list.length === 0) {
        toast('Nothing matches that selection.');
        return;
      }
      var summaryBox = document.getElementById('include-summary');
      printTours(selection.list, {
        summary: summaryBox ? summaryBox.checked : true,
        period: selection.label
      });
    });
  }

  function wireSettings() {
    var save = document.getElementById('save-settings');
    if (save) save.addEventListener('click', function () {
      settings.travellerName = (document.getElementById('travellerName').value || '').trim();
      settings.defaultCity = (document.getElementById('defaultCity').value || '').trim();
      var markText = document.getElementById('watermarkText');
      if (markText) settings.watermarkText = (markText.value || '').trim();
      saveSettings();
      updateNameBanner();
      toast('Settings saved');
    });

    var watermark = document.getElementById('watermark');
    if (watermark) watermark.addEventListener('change', function () {
      settings.watermark = watermark.checked;
      saveSettings();
      toast(watermark.checked ? 'Watermark on' : 'Watermark off');
    });

    ['wmAlignDiagonal', 'wmAlignCenter'].forEach(function (id) {
      var chip = document.getElementById(id);
      if (chip) chip.addEventListener('click', function () {
        settings.watermarkAlign = id === 'wmAlignCenter' ? 'center' : 'diagonal';
        saveSettings();
        var d = document.getElementById('wmAlignDiagonal');
        var c = document.getElementById('wmAlignCenter');
        if (d) d.setAttribute('aria-pressed', String(settings.watermarkAlign !== 'center'));
        if (c) c.setAttribute('aria-pressed', String(settings.watermarkAlign === 'center'));
        toast('Watermark alignment: ' + (settings.watermarkAlign === 'center' ? 'Center' : 'Diagonal'));
      });
    });

    var backup = document.getElementById('backup');
    if (backup) backup.addEventListener('click', downloadBackup);

    var restore = document.getElementById('restore');
    var file = document.getElementById('restore-file');
    if (restore && file) {
      restore.addEventListener('click', function () { file.click(); });
      file.addEventListener('change', function () {
        var chosen = file.files && file.files[0];
        if (!chosen) return;
        var reader = new FileReader();
        reader.onload = function () {
          try {
            var data = JSON.parse(String(reader.result));
            var incoming = Array.isArray(data.tours) ? data.tours : [];
            if (!incoming.length && !data.settings) throw new Error('empty');
            if (!window.confirm('Replace the ' + tours.length + ' tours on this device with ' +
              incoming.length + ' from the backup?')) return;
            tours = incoming.map(normaliseTour);
            if (data.settings) {
              settings = normaliseSettings(data.settings);
              saveSettings();
            }
            saveTours();
            toast('Backup restored');
            render();
          } catch (err) {
            toast('That file is not a Duty Tour backup.');
          }
        };
        reader.readAsText(chosen);
        file.value = '';
      });
    }

    var clear = document.getElementById('clear-all');
    if (clear) clear.addEventListener('click', function () {
      if (!window.confirm('Delete all ' + tours.length + ' tours from this browser? This cannot be undone.')) return;
      tours = [];
      saveTours();
      toast('All tours deleted');
      go('tours');
    });
  }

  function downloadBackup() {
    var payload = {
      app: 'Duty Tour',
      company: COMPANY,
      version: 1,
      exportedAt: new Date().toISOString(),
      settings: settings,
      tours: tours
    };
    var blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = 'duty-tour-backup-' + todayISO() + '.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  }

  // -------------------------------------------------------------------- print

  function printTours(list, options) {
    options = options || {};
    var root = document.getElementById('print-root');
    var html = '';

    if (options.summary && list.length > 1) {
      html += summarySheet(list, options.period || 'All tours');
    }
    list.forEach(function (tour) { html += tourSheet(tour); });

    root.innerHTML = html;

    var previousTitle = document.title;
    document.title = 'Duty Tour' + (list.length === 1
      ? ' - ' + (list[0].destination || 'tour')
      : ' - ' + list.length + ' tours');

    window.print();

    window.setTimeout(function () {
      document.title = previousTitle;
      root.innerHTML = '';
    }, 1500);
  }

  /**
   * The diagonal mark is optional and its wording is the traveller's.
   * Toggle off, or clear the text, and the sheet prints with no watermark.
   */
  function watermarkMarkup() {
    if (!settings.watermark) return '';
    var text = (settings.watermarkText || '').trim();
    if (!text) return '';
    // Long wording shrinks so it still fits across the page.
    var size = Math.max(22, Math.min(84, Math.round(980 / text.length)));
    var cls = settings.watermarkAlign === 'center' ? 'sheet-watermark center' : 'sheet-watermark';
    return '<div class="' + cls + '"><span style="font-size:' + size +
      'pt">' + esc(text) + '</span></div>';
  }

  function tourSheet(tour) {
    var traveller = tour.traveller || settings.travellerName || '';
    var rows = '';

    rows += '<tr>' +
      '<td>Travel cost</td>' +
      '<td class="muted">' + esc(tour.travelMode) + (tour.travelNote ? ' · ' + esc(tour.travelNote) : '') + '</td>' +
      '<td class="num">' + amount(tour.travelAmount) + '</td>' +
      '</tr>';

    if (tour.hotelName || tour.hotelNights || tour.hotelAmount) {
      var hotelDetail = [];
      if (tour.hotelName) hotelDetail.push(tour.hotelName);
      if (tour.hotelNights) hotelDetail.push(tour.hotelNights + (tour.hotelNights === 1 ? ' night' : ' nights'));
      rows += '<tr>' +
        '<td>Hotel cost</td>' +
        '<td class="muted">' + esc(hotelDetail.join(' · ') || '—') + '</td>' +
        '<td class="num">' + amount(tour.hotelAmount) + '</td>' +
        '</tr>';
    }

    if (tour.workAmount) {
      rows += '<tr>' +
        '<td>Work charged</td>' +
        '<td class="muted">' + esc(tour.workTitle || '—') + '</td>' +
        '<td class="num">' + amount(tour.workAmount) + '</td>' +
        '</tr>';
    }

    if (tour.otherAmount) {
      rows += '<tr>' +
        '<td>Others</td>' +
        '<td class="muted">' + esc(tour.otherNote || '—') + '</td>' +
        '<td class="num">' + amount(tour.otherAmount) + '</td>' +
        '</tr>';
    }

    rows += '<tr class="total">' +
      '<td colspan="2">Total</td>' +
      '<td class="num">' + CURRENCY + ' ' + amount(totalOf(tour)) + '</td>' +
      '</tr>';

    var meta = '';
    meta += metaRow('Dates', formatRange(tour.startDate, tour.endDate));
    if (tour.purposeLine) meta += metaRow('Purpose', tour.purposeLine);
    if (traveller) meta += metaRow('Traveller', traveller);
    var nights = nightsBetween(tour.startDate, tour.endDate);
    if (nights) meta += metaRow('Nights away', String(nights));

    var work = (tour.workTitle || tour.workDetails)
      ? '<div class="doc-work">' +
          '<h3>Work done</h3>' +
          (tour.workTitle ? '<p class="work-title">' + esc(tour.workTitle) + '</p>' : '') +
          (tour.workDetails ? '<p class="work-body">' + esc(tour.workDetails) + '</p>' : '') +
        '</div>'
      : '';

    return '<section class="sheet">' +
      watermarkMarkup() +
      '<div class="sheet-content">' +
        '<div class="doc-head">' +
          '<div>' +
            '<p class="doc-title">DUTY TOUR</p>' +
            '<p class="doc-company">' + esc(COMPANY) + '</p>' +
          '</div>' +
          '<div class="doc-stamp">' +
            'Single tour record<br />' +
            'Generated on this device<br />' +
            esc(formatDate(todayISO())) +
          '</div>' +
        '</div>' +

        '<p class="doc-route">' + esc(routeLabel(tour)) + '</p>' +
        '<p class="doc-sub">' + esc(formatRange(tour.startDate, tour.endDate)) + '</p>' +

        '<dl class="doc-meta">' + meta + '</dl>' +

        '<table class="doc-table">' +
          '<thead><tr>' +
            '<th style="width:24%">Head</th>' +
            '<th>Detail</th>' +
            '<th class="num" style="width:22%">Amount (' + CURRENCY + ')</th>' +
          '</tr></thead>' +
          '<tbody>' + rows + '</tbody>' +
        '</table>' +

        work +

        '<div class="doc-approval">' +
          '<div><span>Submitted by</span>' + esc(traveller || '____________________') + '</div>' +
          '<div><span>Approved by</span>____________________</div>' +
        '</div>' +
      '</div>' +
      '<div class="doc-foot">' +
        '<span>Personal duty-tour record. Not a GST invoice.</span>' +
        '<span>' + esc(COMPANY) + '</span>' +
      '</div>' +
    '</section>';
  }

  function metaRow(label, value) {
    return '<div><dt>' + esc(label) + '</dt><dd>' + esc(value) + '</dd></div>';
  }

  function summarySheet(list, period) {
    var grand = list.reduce(function (sum, t) { return sum + totalOf(t); }, 0);
    var traveller = settings.travellerName || (list[0] && list[0].traveller) || '';

    var rows = list.map(function (t, index) {
      return '<tr>' +
        '<td class="num">' + (index + 1) + '</td>' +
        '<td>' + esc(formatRange(t.startDate, t.endDate)) + '</td>' +
        '<td>' + esc(routeLabel(t)) + '</td>' +
        '<td class="num">' + amount(totalOf(t)) + '</td>' +
        '</tr>';
    }).join('');

    var meta = '';
    if (traveller) meta += metaRow('Traveller', traveller);
    meta += metaRow('Period', period);
    meta += metaRow('Tours', String(list.length));
    meta += metaRow('Prepared', formatDate(todayISO()));

    return '<section class="sheet">' +
      watermarkMarkup() +
      '<div class="sheet-content">' +
        '<div class="doc-head">' +
          '<div>' +
            '<p class="doc-title">DUTY TOUR</p>' +
            '<p class="doc-company">' + esc(COMPANY) + '</p>' +
          '</div>' +
          '<div class="doc-stamp">' +
            'Summary<br />' +
            'Generated on this device<br />' +
            esc(formatDate(todayISO())) +
          '</div>' +
        '</div>' +

        '<p class="doc-route">Duty tour summary</p>' +
        '<p class="doc-sub">' + esc(period) + '</p>' +
        '<dl class="doc-meta">' + meta + '</dl>' +

        '<table class="doc-table summary-table">' +
          '<thead><tr>' +
            '<th class="num" style="width:7%">#</th>' +
            '<th style="width:30%">Dates</th>' +
            '<th>Route</th>' +
            '<th class="num" style="width:22%">Total (' + CURRENCY + ')</th>' +
          '</tr></thead>' +
          '<tbody>' + rows +
            '<tr class="grand">' +
              '<td colspan="3">Grand total</td>' +
              '<td class="num">' + CURRENCY + ' ' + amount(grand) + '</td>' +
            '</tr>' +
          '</tbody>' +
        '</table>' +
      '</div>' +
      '<div class="doc-foot">' +
        '<span>Personal duty-tour record. Not a GST invoice.</span>' +
        '<span>' + esc(COMPANY) + '</span>' +
      '</div>' +
    '</section>';
  }

  // --------------------------------------------------------------------- boot

  window.addEventListener('hashchange', render);
  document.addEventListener('DOMContentLoaded', function () {
    if (!location.hash) location.replace('#/tours');
    render();
  });

  // render() is also safe to call directly if the script loads after DOMContentLoaded.
  if (document.readyState !== 'loading') {
    if (!location.hash) location.replace('#/tours');
    render();
  }
})();
