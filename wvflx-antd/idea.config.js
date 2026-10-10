'use strict'
const path = require('path')

/*
// WebStorm识别@符号
File -> Settings
    Language & Frameworks -> JavaScript ->
        JavaScript Language Version
          React JSX
        Webpack
          $AppPath/idea.config.js
*/

function resolve (dir) {
  return path.join(__dirname, '.', dir)
}

module.exports = {
    context: path.resolve(__dirname, './'),
    resolve: {
        extensions: ['.js', '.vue', '.json'],
        alias: {
            '@': resolve('src'),
        }
    },
}