const CACHE='revision-express-pwa-v11';
const FILES=['./','./index.html','./styles.css','./app.js','./firebase-share.js','./manifest.webmanifest','./pwa-icon.png',...['anglais.csv','francais.csv','histoire_gen.csv','histoire_term.csv','maths_1e.csv','maths_term.csv','physique_1e.csv','physique_gen.csv','physique_term.csv','tout_melange_histoire.csv','tout_melange_math.csv','tout_melange_physique.csv','tout_melange.csv'].map(f=>`./app/src/main/assets/${f}`)];
self.addEventListener('install',e=>e.waitUntil(caches.open(CACHE).then(c=>c.addAll(FILES))));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('message',e=>{if(e.data?.type==='SKIP_WAITING')self.skipWaiting();});
self.addEventListener('fetch',e=>{if(e.request.method!=='GET')return;e.respondWith(caches.match(e.request).then(hit=>hit||fetch(e.request).then(res=>{if(res.ok){const copy=res.clone();caches.open(CACHE).then(c=>c.put(e.request,copy));}return res;}).catch(()=>caches.match('./index.html'))));});
