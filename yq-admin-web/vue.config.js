const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 5173,
    proxy: {
      '/yq-admin': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
