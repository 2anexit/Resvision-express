import { initializeApp, getApps } from 'https://www.gstatic.com/firebasejs/13.2.0/firebase-app.js';
import { getAuth, signInAnonymously } from 'https://www.gstatic.com/firebasejs/13.2.0/firebase-auth.js';
import { addDoc, collection, deleteDoc, doc, getDoc, getFirestore } from 'https://www.gstatic.com/firebasejs/13.2.0/firebase-firestore.js';

const config = {
  apiKey: 'AIzaSyDzZkXAV7zXfO8y_fhHXSy7fuICxnG3mV8',
  authDomain: 'revision-express-69a53.firebaseapp.com',
  projectId: 'revision-express-69a53',
  storageBucket: 'revision-express-69a53.firebasestorage.app',
  messagingSenderId: '901022279034',
  appId: '1:901022279034:web:588b4b5abefd252b3a22c3'
};

let servicesPromise;
async function services() {
  if (!servicesPromise) servicesPromise = (async () => {
    const app = getApps().length ? getApps()[0] : initializeApp(config);
    const auth = getAuth(app);
    if (!auth.currentUser) await signInAnonymously(auth);
    return { auth, db: getFirestore(app) };
  })();
  return servicesPromise;
}

export async function createShare(payload) {
  const { auth, db } = await services();
  const clean = {
    owner: auth.currentUser.uid,
    public: true,
    category: String(payload.category).slice(0, 80),
    series: payload.series.slice(0, 30).map(s => ({
      name: String(s.name).slice(0, 80),
      count: Math.max(1, Math.min(200, Number(s.count) || 20)),
      questions: s.questions.slice(0, 200).map(q => ({
        question: String(q.question).slice(0, 1000),
        options: q.options.slice(0, 4).map(a => String(a).slice(0, 500)),
        correct: Math.max(0, Math.min(3, Number(q.correct) || 0))
      }))
    }))
  };
  const ref = await addDoc(collection(db, 'publicShares'), clean);
  return ref.id;
}

export async function readShare(id) {
  const { db } = await services();
  const snap = await getDoc(doc(db, 'publicShares', id));
  if (!snap.exists() || snap.data().public !== true) throw new Error('Ce partage est introuvable ou a été supprimé.');
  return snap.data();
}

export async function removeShare(id) {
  const { db } = await services();
  await deleteDoc(doc(db, 'publicShares', id));
}
