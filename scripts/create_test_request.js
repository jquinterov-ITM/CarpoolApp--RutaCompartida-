#!/usr/bin/env node
const admin = require('firebase-admin');
const argv = require('minimist')(process.argv.slice(2));
const path = require('path');

function usage() {
  console.log('Uso: node scripts/create_test_request.js --serviceAccount=./key.json --tripId=TRIP_ID --requestId=REQ_ID --pasajeroId=UID [--project=projectId]');
  process.exit(1);
}

const svc = argv.serviceAccount || argv.s;
const tripId = argv.tripId || argv.t;
const requestId = argv.requestId || argv.r;
const pasajeroId = argv.pasajeroId || argv.u;
const projectId = argv.project || argv.pj;

if (!svc || !tripId || !requestId || !pasajeroId) usage();

const svcPath = path.resolve(svc);
const serviceAccount = require(svcPath);

const initOpts = { credential: admin.credential.cert(serviceAccount) };
if (projectId) initOpts.projectId = projectId;
admin.initializeApp(initOpts);
const db = admin.firestore();

async function run() {
  try {
    const docRef = db.doc(`trips/${tripId}/requests/${requestId}`);
    const data = {
      pasajeroId: pasajeroId,
      estado: 'PENDIENTE',
      createdAt: admin.firestore.Timestamp.now()
    };
    await docRef.set(data, { merge: true });
    console.log('Documento creado:', docRef.path);
    console.log(JSON.stringify(data, null, 2));
  } catch (e) {
    console.error('Error creando documento:', e);
    process.exitCode = 1;
  }
}

run();
