const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
function fingerprints(base = root) {
 return Object.fromEntries(['firestore.rules','rules.test.cjs','package-lock.json','firebase.json'].map(file => [file, crypto.createHash('sha256').update(fs.readFileSync(path.join(base,file))).digest('hex')]));
}
function assertVerified(report, current) {
 if (report.status !== 'PASS') throw new Error(`Firestore rules verification ${report.status || 'MISSING'}: deployment prohibited.`);
 for (const [file, hash] of Object.entries(current)) if (report.hashes?.[file] !== hash) throw new Error(`${file} changed since verification: deployment prohibited.`);
}
module.exports = { root, fingerprints, assertVerified };
if (require.main === module) {
 try { assertVerified(JSON.parse(fs.readFileSync(path.join(root,'verification-status.json'),'utf8')), fingerprints()); console.log('Exact rules/test/config versions passed local Emulator verification. Deployment still requires authorization.'); }
 catch(e) { console.error(e.message); process.exitCode = 1; }
}
