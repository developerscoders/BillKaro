# BillKaro Secure Sync Gateway (Production Proxy)

This backend proxy ensures that your **`MONGODB_URI`** and database credentials remain **100% isolated on your secure server**, completely removed from your published Android APK.

### Architecture

```
[ Android App / APK ]  ──(HTTPS Request)──>  [ Sync Gateway ]  ──(MONGODB_URI)──>  [ MongoDB Atlas ]
(Zero Database Secrets)                     (Holds Secret Safely)
```

### 1-Minute Deployment Options

#### Option A: Google Cloud Run (Recommended & Free Tier friendly)
Run from the root or `server` directory:
```bash
gcloud run deploy billkaro-sync-gateway \
  --source ./server \
  --set-env-vars="MONGODB_URI=mongodb+srv://<user>:<password>@cluster0.abcde.mongodb.net/billkaro,DB_NAME=billkaro" \
  --allow-unauthenticated
```
Copy the generated URL (e.g., `https://billkaro-sync-gateway-xyz.run.app`) and set it as `SYNC_GATEWAY_URL`.

#### Option B: Render / Railway / Vercel
1. Link your repository.
2. Set root directory to `server`.
3. Add Environment Variable:
   - `MONGODB_URI`: your MongoDB Atlas connection string.
   - `DB_NAME`: `billkaro`
4. Deploy!

### Available Endpoints
- `GET /health`: Health check and database ping.
- `GET /api/sync/pull?collection=invoices`: Fetch cloud records.
- `POST /api/sync/push`: Push & upsert local records.
