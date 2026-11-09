const {test}=require('node:test');
const assert=require('node:assert/strict');
const {assertVerified}=require('./rules-verification.cjs');
const current={'firestore.rules':'rulesHash','rules.test.cjs':'testHash','package-lock.json':'lockHash','firebase.json':'configHash'};
test('BLOCKED, FAIL, RUNNING and missing reports forbid deployment',()=>{for(const status of ['BLOCKED','FAIL','RUNNING',undefined])assert.throws(()=>assertVerified({status,hashes:current},current));});
test('missing or stale exact-source fingerprints forbid deployment',()=>{assert.throws(()=>assertVerified({status:'PASS'},current));assert.throws(()=>assertVerified({status:'PASS',hashes:{...current,'firestore.rules':'old'}},current));});
test('only passing unchanged fingerprints satisfy verification guard',()=>{assert.doesNotThrow(()=>assertVerified({status:'PASS',hashes:current},current));});
