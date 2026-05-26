#!/usr/bin/env node
const admin = require('firebase-admin');
const argv = require('minimist')(process.argv.slice(2));
const path = require('path');

function usage() {
  console.log('Uso: node scripts/list_requests.js --serviceAccount=./key.json --tripId=TRIP_ID [--project=projectId]');
  process.exit(1);
}

const svc = argv.serviceAccount || argv.s;
const tripId = argv.tripId || argv.t;
const projectId = argv.project || argv.p;
if (!svc || !tripId) usage();
const svcPath = path.resolve(svc);
const serviceAccount = require(svcPath);

const initOpts = { credential: admin.credential.cert(serviceAccount) };
if (projectId) initOpts.projectId = projectId;
admin.initializeApp(initOpts);
const db = admin.firestore();

async function run() {
  try {
    const colRef = db.collection('trips').doc(tripId).collection('requests');
    const snap = await colRef.listDocuments();
    console.log('Encontrados', snap.length, 'requests en trip', tripId, ':');
    snap.forEach(d => console.log('-', d.id));
  } catch (e) {
    console.error('Error listando requests:', e);
    process.exitCode = 1;
  }
}

run();
