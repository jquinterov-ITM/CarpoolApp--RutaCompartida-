#!/usr/bin/env node
const admin = require('firebase-admin');
const argv = require('minimist')(process.argv.slice(2));
const path = require('path');

function usage() {
  console.log('Uso: node scripts/list_trips.js --serviceAccount=./key.json [--project=projectId]');
  process.exit(1);
}

const svc = argv.serviceAccount || argv.s;
const projectId = argv.project || argv.p;
if (!svc) usage();
const svcPath = path.resolve(svc);
const serviceAccount = require(svcPath);

const initOpts = { credential: admin.credential.cert(serviceAccount) };
if (projectId) initOpts.projectId = projectId;
admin.initializeApp(initOpts);
const db = admin.firestore();

async function run() {
  try {
    const snap = await db.collection('trips').listDocuments();
    console.log('Encontrados', snap.length, 'trips:');
    snap.forEach(d => console.log('-', d.id));
  } catch (e) {
    console.error('Error listando trips:', e);
    process.exitCode = 1;
  }
}

run();
