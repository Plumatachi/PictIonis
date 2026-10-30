# PictIonis
## Mise en place Firebase

Le jeu utilise **Firebase Auth** (email / mot de passe) et **Firebase Realtime Database**.

1. Console Firebase > Build > Realtime Database > *Créer une base de données*, région **europe-west1 (Belgique)**, mode verrouillé.
   L'URL attendue est `https://pictionis-f5fe5-default-rtdb.europe-west1.firebasedatabase.app` (constante `DATABASE_URL` dans `GameManager.kt`).
2. Publier les règles de sécurité `PictIonis/database.rules.json` :
   - soit en les copiant dans l'onglet *Règles* de la console,
   - soit avec la CLI depuis `PictIonis/` : `firebase login` puis `firebase deploy --only database --project pictionis-f5fe5`.

Tester sans toucher la vraie base : `firebase emulators:start --only auth,database --project pictionis-f5fe5` (Java 21+), puis appeler
`useEmulator("10.0.2.2", 9099)` / `useEmulator("10.0.2.2", 9000)` sur FirebaseAuth et FirebaseDatabase au démarrage de l'app (build debug, trafic HTTP autorisé).

Données : `games/{code}` (état de la partie), `drawings/{code}` (traits), `chats/{code}` (messages).
