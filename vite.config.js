import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'fs'
import path from 'path'

export default defineConfig({
  plugins: [
    react(),
    {
      name: 'serve-apk-clean',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const cleanUrl = req.url?.split('?')[0]
          if (cleanUrl === '/scrolle.apk') {
            const apkPath = path.resolve(__dirname, 'public/scrolle.apk')
            if (fs.existsSync(apkPath)) {
              const stat = fs.statSync(apkPath)
              res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Length': stat.size,
                'Content-Disposition': 'attachment; filename="scrolle.apk"',
                'Cache-Control': 'no-cache, no-store, must-revalidate',
                'Pragma': 'no-cache',
                'Expires': '0'
              })
              fs.createReadStream(apkPath).pipe(res)
              return
            }
          }
          next()
        })
      }
    }
  ],
})
