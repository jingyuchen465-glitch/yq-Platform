const { defineConfig } = require('@vue/cli-service')
const apiTarget = process.env.YQ_API_TARGET || 'http://localhost:8080'

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 5173,
    proxy: {
      '/yq-admin': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
})
