const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 5173;
const apkPath = path.join(__dirname, 'scrolle.apk');

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];
  console.log(`[${new Date().toLocaleTimeString()}] ${req.method} ${url}`);

  if (url === '/scrolle.apk' || url === '/download') {
    if (fs.existsSync(apkPath)) {
      const stat = fs.statSync(apkPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="scrolle.apk"',
        'Access-Control-Allow-Origin': '*'
      });
      fs.createReadStream(apkPath).pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK file not found on server');
      return;
    }
  }

  // Mobile landing page
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Download Scrolle APK</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: linear-gradient(135deg, #0a071b 0%, #1a1235 50%, #0d0924 100%);
      color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 24px;
      text-align: center;
    }
    .card {
      background: rgba(255, 255, 255, 0.07);
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 28px;
      padding: 36px 24px;
      max-width: 380px;
      width: 100%;
      box-shadow: 0 12px 40px rgba(0,0,0,0.6);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
    }
    .icon { font-size: 52px; margin-bottom: 12px; }
    h1 { font-size: 28px; font-weight: 900; margin-bottom: 6px; }
    .badge {
      display: inline-block;
      background: rgba(34, 197, 94, 0.15);
      border: 1px solid rgba(34, 197, 94, 0.4);
      color: #4ade80;
      border-radius: 50px;
      padding: 5px 14px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.5px;
      margin-top: 6px;
    }
    p {
      color: rgba(255, 255, 255, 0.75);
      font-size: 14px;
      line-height: 1.5;
      margin: 18px 0 24px;
    }
    .apps {
      display: flex;
      justify-content: center;
      gap: 12px;
      margin-bottom: 24px;
      font-size: 22px;
    }
    .btn {
      display: block;
      background: linear-gradient(90deg, #E1306C, #833AB4);
      color: white;
      text-decoration: none;
      font-weight: 800;
      font-size: 17px;
      padding: 16px 24px;
      border-radius: 50px;
      box-shadow: 0 8px 24px rgba(225, 48, 108, 0.45);
      transition: transform 0.15s ease;
    }
    .btn:active { transform: scale(0.96); }
    .note {
      color: rgba(255, 255, 255, 0.45);
      font-size: 11px;
      margin-top: 20px;
      line-height: 1.4;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">📱</div>
    <h1>Scrolle</h1>
    <div class="badge">Universal Release APK • v1.0</div>
    <p>Auto-counts your short videos with a floating live pill!</p>
    <div class="apps">
      <span title="Instagram Reels">🎬</span>
      <span title="YouTube Shorts">🔴</span>
      <span title="TikTok">🎵</span>
      <span title="Snapchat Spotlight">👻</span>
      <span title="Facebook Reels">📘</span>
    </div>
    <a href="/scrolle.apk" class="btn" download="scrolle.apk">
      📥 Download Scrolle APK
    </a>
    <div class="note">
      Universal signed APK • Installs on any Android phone (Samsung, Xiaomi, Oppo, Vivo, Realme, OnePlus, etc.)
    </div>
  </div>
</body>
</html>`);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Scrolle APK Server is LIVE at http://0.0.0.0:${PORT}/`);
});
