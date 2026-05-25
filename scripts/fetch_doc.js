#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

function usage() {
  console.log('Uso: node fetch_doc.js --serviceAccount=./key.json --doc="trips/ID/requests/ID" [--project=projectId]');
  process.exit(1);
}

const argv = require('minimist')(process.argv.slice(2));
const svcPath = argv.serviceAccount || argv.s;
const docPath = argv.doc || argv.d;
const projectId = argv.project || argv.p || process.env.GCLOUD_PROJECT;

if (!svcPath || !docPath) usage();

const fullPath = path.resolve(svcPath);
if (!fs.existsSync(fullPath)) {
  console.error('No se encontró el archivo de cuenta de servicio en:', fullPath);
  process.exit(2);
}

const serviceAccount = require(fullPath);
const initOpts = { credential: admin.credential.cert(serviceAccount) };
if (projectId) initOpts.projectId = projectId;

admin.initializeApp(initOpts);

const db = admin.firestore();

async function run() {
  try {
    const ref = db.doc(docPath);
    const snap = await ref.get();
    if (!snap.exists) {
      console.log('Documento no encontrado:', docPath);
      process.exit(0);
    }
    console.log('Documento:', docPath);
    console.log(JSON.stringify(snap.data(), null, 2));
  } catch (e) {
    console.error('Error leyendo documento:', e);
    process.exitCode = 1;
  }
}

run();
