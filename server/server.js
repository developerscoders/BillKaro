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

app.use(cors());
app.use(express.json({ limit: '10mb' }));

let dbClient = null;
let db = null;

function getCleanMongoUri() {
  const raw = process.env.MONGODB_URI || '';
  // Strip optional surrounding single/double quotes or leading/trailing whitespace
  return raw.trim().replace(/^["']|["']$/g, '');
}

function getDatabaseName() {
  return (process.env.DB_NAME || 'billkaro').trim();
}

async function getDatabase() {
  const uri = getCleanMongoUri();
  const dbName = getDatabaseName();

  if (!uri) {
    throw new Error('MONGODB_URI environment variable is missing or empty.');
  }

  // If already connected and active, return cached DB
  if (db && dbClient) {
    return db;
  }

  try {
    const client = new MongoClient(uri, {
      serverSelectionTimeoutMS: 8000,
      connectTimeoutMS: 10000
    });

    await client.connect();
    dbClient = client;
    db = client.db(dbName);
    console.log(`Connected successfully to MongoDB database: ${dbName}`);
    return db;
  } catch (err) {
    // Reset so subsequent requests can retry cleanly
    dbClient = null;
    db = null;
    throw err;
  }
}

// Token Verification Middleware (optional security layer)
function authMiddleware(req, res, next) {
  const gatewayToken = (process.env.GATEWAY_AUTH_TOKEN || '').trim();
  if (!gatewayToken) return next();
  const authHeader = req.headers['authorization'] || req.headers['apikey'] || req.headers['api-key'];
  const token = authHeader ? authHeader.replace(/^Bearer\s+/i, '').trim() : '';
  if (token !== gatewayToken) {
    return res.status(401).json({ error: 'Unauthorized: Invalid gateway token' });
  }
  next();
}

// 0. Root Endpoint: Quick check to verify server is responding
app.get('/', (req, res) => {
  const hasUri = Boolean(getCleanMongoUri());
  res.json({
    status: 'online',
    service: 'BillKaro Cloud Sync Gateway',
    version: '1.0.0',
    databaseConfigured: hasUri,
    databaseName: getDatabaseName(),
    endpoints: {
      health: '/health',
      pull: '/api/sync/pull?collection=invoices',
      push: '/api/sync/push'
    },
    timestamp: Date.now()
  });
});

// 1. Health & Database Connectivity Check
app.get('/health', async (req, res) => {
  const uri = getCleanMongoUri();
  const dbName = getDatabaseName();

  if (!uri) {
    return res.status(503).json({
      status: 'waiting_for_mongodb_uri',
      service: 'BillKaro Cloud Sync Gateway',
      message: 'Server is running, but MONGODB_URI environment variable is not configured on your hosting platform (Render).',
      timestamp: Date.now()
    });
  }

  try {
    const database = await getDatabase();
    if (!database) {
      throw new Error('Database instance is null after connection attempt.');
    }
    await database.command({ ping: 1 });
    res.json({
      status: 'ok',
      service: 'BillKaro Cloud Sync Gateway',
      database: dbName,
      message: 'MongoDB connected successfully',
      timestamp: Date.now()
    });
  } catch (err) {
    const errMsg = err.message || '';
    let suggestion = 'Check your connection string credentials.';
    if (errMsg.includes('bad auth') || errMsg.includes('Authentication failed')) {
      suggestion = 'Authentication failed. Please verify your MongoDB username and password in MONGODB_URI on Render.';
    } else if (errMsg.includes('Server selection timed out') || errMsg.includes('ETIMEDOUT') || errMsg.includes('ECONNREFUSED')) {
      suggestion = 'Connection timed out. In MongoDB Atlas, go to Network Access -> Add IP Address -> choose "Allow Access from Anywhere" (0.0.0.0/0).';
    }

    res.status(503).json({
      status: 'error',
      message: 'Database connection failed',
      error: errMsg,
      help: suggestion
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
