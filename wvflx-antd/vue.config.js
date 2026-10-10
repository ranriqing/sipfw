'use strict'
const path = require('path')
const settings = require('./src/settings')
const webpack = require("webpack")
const TerserPlugin = require('terser-webpack-plugin')
const PreloadPlugin = require("@vue/preload-webpack-plugin");
// Gzip压缩插件，需要服务器开启gzip
// const CompressionPlugin = require('compression-webpack-plugin')
// const productionGzipExtensions = /\.(js|css|json|txt|html|ico|svg)(\?.*)?$/i
// 性能分析插件，查看打包模块大小
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

function resolve(dir) {
  return path.join(__dirname, dir)
}

module.exports = {
  publicPath: settings.WEB_PATH,
  productionSourceMap: false,
  // TipTap/ProseMirror 及其依赖发布产物含可选链语法，Node 12 无法解析，需交给 babel 转译
  transpileDependencies: [
    '@tiptap',
    'prosemirror-',
    'markdown-it',
    'linkifyjs',
    'linkify-it',
    'orderedmap',
    'rope-sequence',
    'w3c-keyname',
  ],
  // 多入口配置部分
  /*pages: {
    index: {
      entry: 'src/main.js',           // 入口文件
      template: 'public/index.html',  // 模板来源
      filename: 'index.html'          // dist输出
    },
    slim: {
      entry: 'src/slim.js',           // 入口文件
      template: 'public/slim.html',   // 模板来源
      filename: 'slim.html'           // dist输出
    }
  },*/
  configureWebpack: (config) => {
    config.devtool = 'none' // 'eval|none|inline-source-map|source-map'
    // 禁用Preload | https://github.com/vuejs/vue-cli/issues/5562
    config.plugins = config.plugins.filter(p => !(p instanceof PreloadPlugin));
    let commonObj = {
      externals:{
        vue : 'Vue',
        moment : 'moment',
      },
    }
    if (process.env.NODE_ENV === 'production') {
      // 选择不同模式，使用Webpack内置的优化，默认: production
      // config.mode = 'production'  // 'development|production|none|production'
      return Object.assign({
        /*output:{
          filename: '[name]-[chunkhash].js',
          libraryTarget: 'umd'       // UniversalModuleDefinition，兼容AMD(浏览器)和CommonJS(Node.js)
        },*/
        plugins: [
          // 分块大小插件
          new webpack.optimize.MinChunkSizePlugin({
            minChunkSize: 200000 // Minimum chunk size 200k
          }),
          // 性能分析插件
          new BundleAnalyzerPlugin({
            analyzerMode: 'static',
            reportFilename: 'report.html',
            openAnalyzer: false,
            generateStatsFile: false,
            statsFilename: 'stats.json',
          }),
          /* Gzip压缩插件
          new CompressionPlugin({
            test: productionGzipExtensions,
            threshold: 10240,
            deleteOriginalAssets: false
          })*/
        ],
        optimization: {
          // 内容压缩插件
          minimizer: [new TerserPlugin({
            terserOptions: {
              output: {
                comments: false,
              },
            }
          })]
        },
        performance: {
          // 文件打包过大警告: 不报错和警告，只做提示
          hints: false   // 'warning|error|false'
        }
      }, commonObj)
    }else{
      // 说明见: config.mode = 'production'
      // config.mode = 'development'
      return Object.assign({}, commonObj)
    }
  },
  chainWebpack: (config) => { // 细粒度的控制内部配置
    config.resolve.alias.set('@', resolve('src'))
    // 更改ant-design的图标引用，减小打包体积
    config.resolve.alias.set('@ant-design/icons/lib/dist$', resolve('src/utils/tiny_icons.js'))
  },
  // Css相关配置
  css: {
    loaderOptions: {
      less: {
        modifyVars: {
          // 更改less中的变量
          // 方案1: 'menu-dark-color': '#fff000',
          // 方案2: src/assets/scss/antd.reset.less
        },
        // 允许Less中写Js函数
        javascriptEnabled: true,
      }
    },
  },
  // Dev开发配置
  devServer: {
    port: 3001,
    // sockHost: '192.168.1.100',
    // 多入口配置部分
    // 测试访问： /wvapp/slim/login
    /*before: function(app) {
      app.get(`${settings.WEB_PATH}/:page/!*`, function(req, res, next) {
        if (['slim'].includes(req.params.page)) {
          // 把 /<base>/<page>/!* 重定向到 /<base>/<page>/
          req.url = `${settings.WEB_PATH}/${req.params.page}.html`;
          next('route');
        } else {
          next();
        }
      });
    },*/
    proxy: {
      '/geo-datav': {
        target: 'https://geo.datav.aliyun.com',
        ws: false,
        secure: false,
        changeOrigin: true,
        pathRewrite: {
          '/geo-datav': ''
        },
        onProxyReq: (proxyReq, req, res) => {
          proxyReq.removeHeader('Referer');
        }
      },
      [settings.BASE_URL]: {
        target: 'http://localhost:18087',
        ws: false,            // 代理WebSockets
        secure:false,         // 支持跨域Https
        changeOrigin: true,   // 更改请求host为target
        pathRewrite: {
          [settings.BASE_URL]: '/gcs-flx'
        }
      },
    }
  },
  // 保存时代码检查
  lintOnSave: true,
}