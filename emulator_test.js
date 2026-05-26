const fs = require('fs');
const firebase = require('@firebase/testing');

const TEST_UID = 'sejSDR1qHPMvqAxv1vuOPG3cfcD3';
const PROJECT_ID = 'rutacompartida-tests';

async function run() {
  const rules = fs.readFileSync('firestore.rules', 'utf8');
  await firebase.loadFirestoreRules({ projectId: PROJECT_ID, rules });

  // Create an admin app to seed data (bypasses rules)
  const adminApp = firebase.initializeAdminApp({ projectId: PROJECT_ID });
  const adminDb = adminApp.firestore();
  // seed a test document with pasajeroId
  await adminDb.doc('trips/testTrip/requests/testRequest').set({ pasajeroId: TEST_UID, createdAt: Date.now() });

  const app = firebase.initializeTestApp({ projectId: PROJECT_ID, auth: { uid: TEST_UID } });
  const db = app.firestore();

  try {
    console.log('Running collectionGroup("requests") query for uid:', TEST_UID);
    const q = db.collectionGroup('requests').where('pasajeroId', '==', TEST_UID);
    const snap = await q.get();
    console.log('Query succeeded. docs count =', snap.size);
    snap.forEach(d => console.log(d.ref.path, d.data()));
  } catch (err) {
    console.error('Query failed:', err && err.code ? err.code : err);
  } finally {
    await firebase.clearFirestoreData({ projectId: PROJECT_ID });
    Promise.all(firebase.apps().map(a => a.delete()));
  }
}

run().catch(e => { console.error(e); process.exitCode = 1; });
