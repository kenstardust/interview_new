import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/ai': {
        target: 'http://localhost:8051',  // Spring Boot后端端口
        changeOrigin: true,
        // SSE特殊配置：不重写路径，保持原始路径以支持SSE流式传输
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'  // 路径别名，方便导入模块
    }
  }
})