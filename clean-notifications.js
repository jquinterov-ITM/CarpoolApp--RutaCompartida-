const admin = require('firebase-admin');

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function cleanNotifications() {
  const userId = 'SokRoPqhbQas9yYSXwPIkFyowUy2';
  const snapshot = await db.collection('notifications')
    .where('userId', '==', userId)
    .get();
  
  console.log(`Encontrados ${snapshot.docs.length} documentos`);
  
  const batch = db.batch();
  snapshot.docs.forEach(doc => {
    batch.delete(doc.ref);
  });
  
  await batch.commit();
  console.log('Notificaciones eliminadas!');
  process.exit(0);
}

cleanNotifications().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
