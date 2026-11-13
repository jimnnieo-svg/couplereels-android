// Run only where the official Firestore Emulator is available. Does not deploy.
const fs = require('node:fs');
const path = require('node:path');
const {spawnSync} = require('node:child_process');
const {root,fingerprints} = require('./rules-verification.cjs');
const destination = path.join(root,'verification-status.json');
const hashes = fingerprints();
const write = (status, reason) => fs.writeFileSync(destination,JSON.stringify({status,reason,checkedAt:new Date().toISOString(),hashes},null,2)+'\n');
write('RUNNING','Local official Emulator tests in progress; deployment prohibited.');
const result = spawnSync(process.execPath,[require.resolve('firebase-tools/lib/bin/firebase.js'),'emulators:exec','--only','firestore','--project','demo-couplereels','node --test --test-reporter=tap rules.test.cjs'],{cwd:root,encoding:'utf8',stdio:['inherit','pipe','pipe'],maxBuffer:10*1024*1024});
if (result.stdout) process.stdout.write(result.stdout);
if (result.stderr) process.stderr.write(result.stderr);
const complete = /^# tests 13\s*$/m.test(result.stdout || '') && /^# pass 13\s*$/m.test(result.stdout || '') && /^# fail 0\s*$/m.test(result.stdout || '') && /^# skipped 0\s*$/m.test(result.stdout || '');
if (result.status !== 0) { write('FAIL',result.error?.message || 'Emulator command failed. Inspect output; deployment prohibited.'); process.exitCode = 1; }
else if (!complete) { write('FAIL','Expected 13 passing TAP tests with no failures/skips; inspect test output.'); process.exitCode = 1; }
else if (JSON.stringify(hashes) !== JSON.stringify(fingerprints())) { write('FAIL','Source changed during verification; rerun tests.'); process.exitCode = 1; }
else { write('PASS','Official Emulator test command exited successfully for the recorded hashes. No deployment performed.'); }
