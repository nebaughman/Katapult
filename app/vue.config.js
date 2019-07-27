const package = require('./package.json')
process.env.VUE_APP_NAME = package.name
process.env.VUE_APP_VERSION = package.version
process.env.VUE_APP_BUILD_TIME = Date.now()

module.exports = {
  pages: {
    index: {
      entry: 'src/pages/main/main.js',
      title: 'Main Page',
      filename: 'index.html',
    },

    login: {
      entry: 'src/pages/login/login.js',
      title: 'Login',
      filename: 'login/index.html',
    },
  },
}