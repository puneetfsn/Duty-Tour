/*
 * Bundles the Duty Tour web app into one self-contained HTML file:
 *   web/duty-tour.html
 *
 * The result has the CSS and JS inlined, so it can be copied to a phone or a
 * laptop and opened straight from the filesystem — no server, no internet.
 *
 *   cd web && npm run build:single
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const webRoot = here;

const read = (name) => fs.readFileSync(path.join(webRoot, name), 'utf8');

const index = read('index.html');
const css = read('styles.css');
const js = read('app.js');

const banner = `<!--
  Duty Tour — Hendrickson
  Single-file build. Generated from index.html + styles.css + app.js by
  web/build-single.mjs. Edit those files, then run: cd web && npm run build:single

  Private, on this device. No account, no cloud, no analytics, no ads, no network.
  Keep this file if you want a copy you can open without any server.
-->`;

const out = index
  .replace('<head>', '<head>\n' + banner)
  .replace(
    /\s*<link rel="stylesheet" href="styles\.css" \/>/,
    () => '\n  <style>\n' + css.trimEnd() + '\n  </style>'
  )
  .replace(
    /\s*<script src="app\.js"><\/script>/,
    () => '\n  <script>\n' + js.trimEnd() + '\n  </script>'
  );

if (out.includes('href="styles.css"') || out.includes('src="app.js"')) {
  console.error('build-single: could not inline styles.css / app.js — check index.html link tags.');
  process.exit(1);
}

fs.writeFileSync(path.join(webRoot, 'duty-tour.html'), out, 'utf8');
console.log('wrote web/duty-tour.html (' + (out.length / 1024).toFixed(1) + ' KB)');
