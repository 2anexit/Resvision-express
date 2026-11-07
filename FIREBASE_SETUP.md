# Activer le partage Firebase

La configuration Web du projet Firebase est déjà intégrée dans `firebase-share.js`.

1. Dans Firebase Console, active **Authentication → Sign-in method → Anonymous**.
2. Crée une base **Cloud Firestore**.
3. Dans **Firestore Database → Rules**, publie le contenu de `firestore.rules`.
4. Dans **Authentication → Settings → Authorized domains**, vérifie que le domaine GitHub Pages de l’application est autorisé (par exemple `2anexit.github.io`).
5. Publie les changements du dépôt sur GitHub Pages. Le partage exige une connexion Internet ; le reste de l’application continue à fonctionner hors connexion.

Les liens partagés sont publics en lecture pour toute personne qui possède l’URL. Ils sont créés par une session Firebase anonyme, et les règles empêchent les visiteurs de modifier ou lister les documents partagés. Toute personne qui ouvre un lien peut importer sa série dans son stockage local.
