/**
 * BillKaro Secure Sync Gateway Server
 * -------------------------------------------------------------
 * This server keeps your MONGODB_URI and database credentials
 * strictly on the server-side, completely isolated from client APKs.
 * 
 * Deployment targets: Google Cloud Run, Render, Vercel, Railway, or VPS.
 */

const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');

const app = express();
const port = process.env.PORT || 8080;

// Environment Variables (Set these on your server/hosting platform)
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/billkaro';
const DB_NAME = process.env.DB_NAME || 'billkaro';
const GATEWAY_AUTH_TOKEN = process.env.GATEWAY_AUTH_TOKEN || '';

app.use(cors());
app.use(express.json({ limit: '10mb' }));

let dbClient = null;
let db = null;

async function getDatabase() {
  if (!dbClient) {
    dbClient = new MongoClient(MONGODB_URI);
    await dbClient.connect();
    db = dbClient.db(DB_NAME);
    console.log(`Connected successfully to MongoDB database: ${DB_NAME}`);
  }
  return db;
}

// Token Verification Middleware (optional security layer)
function authMiddleware(req, res, next) {
  if (!GATEWAY_AUTH_TOKEN) return next();
  const authHeader = req.headers['authorization'] || req.headers['apikey'] || req.headers['api-key'];
  const token = authHeader ? authHeader.replace(/^Bearer\s+/i, '').trim() : '';
  if (token !== GATEWAY_AUTH_TOKEN) {
    return res.status(401).json({ error: 'Unauthorized: Invalid gateway token' });
  }
  next();
}

// 1. Health & Status Check
app.get('/health', async (req, res) => {
  try {
    const database = await getDatabase();
    await database.command({ ping: 1 });
    res.json({
      status: 'ok',
      service: 'BillKaro Cloud Sync Gateway',
      database: DB_NAME,
      timestamp: Date.now()
    });
  } catch (err) {
    res.status(503).json({
      status: 'error',
      message: 'Database connection failed',
      error: err.message
    });
  }
});

// 2. Pull / Fetch Documents from Cloud
app.get('/api/sync/pull', authMiddleware, async (req, res) => {
  try {
    const collectionName = req.query.collection || 'invoices';
    const limit = Math.min(parseInt(req.query.limit, 10) || 200, 1000);
    const database = await getDatabase();
    const collection = database.collection(collectionName);

    const documents = await collection.find({}).limit(limit).toArray();
    res.json({ success: true, count: documents.length, documents });
  } catch (err) {
    console.error('Error in /api/sync/pull:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 3. Push / Upsert Document into Cloud
app.post('/api/sync/push', authMiddleware, async (req, res) => {
  try {
    const { collection: colName, filterField, filterValue, document } = req.body;
    if (!colName || !filterField || filterValue === undefined || !document) {
      return res.status(400).json({ error: 'Missing required sync parameters' });
    }

    const database = await getDatabase();
    const collection = database.collection(colName);

    const filter = { [filterField]: filterValue };
    const result = await collection.updateOne(filter, { $set: document }, { upsert: true });

    res.json({
      success: true,
      upsertedId: result.upsertedId || null,
      matchedCount: result.matchedCount,
      modifiedCount: result.modifiedCount
    });
  } catch (err) {
    console.error('Error in /api/sync/push:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4. Atlas Data API Compatibility Endpoints
app.post('/action/find', authMiddleware, async (req, res) => {
  try {
    const { collection: colName, filter = {}, limit = 100 } = req.body;
    const database = await getDatabase();
    const collection = database.collection(colName || 'invoices');
    const documents = await collection.find(filter).limit(limit).toArray();
    res.json({ documents });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/action/updateOne', authMiddleware, async (req, res) => {
  try {
    const { collection: colName, filter = {}, update = {}, upsert = false } = req.body;
    const database = await getDatabase();
    const collection = database.collection(colName || 'invoices');
    const result = await collection.updateOne(filter, update, { upsert });
    res.json({ matchedCount: result.matchedCount, modifiedCount: result.modifiedCount, upsertedId: result.upsertedId });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Start Server
app.listen(port, () => {
  console.log(`BillKaro Secure Sync Gateway running on port ${port}`);
});
