module.exports = {
  pages: {
    index: {
      entry: 'src/pages/main/main.js',
      title: 'Main Page',
      filename: 'index.html',
    },

    admin: {
      entry: 'src/pages/admin/admin.js',
      title: 'Admin Page',
      filename: 'admin/index.html',
    },

    login: {
      entry: 'src/pages/login/login.js',
      title: 'Login',
      filename: 'login/index.html',
    },
  },
}