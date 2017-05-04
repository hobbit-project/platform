const webpack = require('webpack');
const path = require('path');

const ENV  = process.env.NODE_ENV = 'development';
const HOST = process.env.HOST || 'localhost';
const PORT = process.env.PORT || 8080;

const metadata = {
  env    : ENV,
  host   : HOST,
  port   : PORT
};

// Webpack and its plugins
const CopyWebpackPlugin    = require('copy-webpack-plugin');
const DefinePlugin         = require('webpack/lib/DefinePlugin');

// Webpack Config
var webpackConfig = {
  devServer: {
    contentBase: 'src',
    host: metadata.host,
    port: metadata.port
  },
  entry: {
    'polyfills': './src/polyfills.browser.ts',
    'vendor':    './src/vendor.browser.ts',
    'main':       './src/main.browser.ts',
  },
  module: {
    loaders: [
      { test: /\.ts$/, loaders: ['awesome-typescript-loader', 'angular2-template-loader'] },
      { test: /\.css$/, loaders: ['to-string-loader', 'css-loader'] },
      { test: /\.scss$/, loaders: [ 'style', 'css', 'sass' ] },
      { test: /\.html$/, loader: 'raw-loader' },
      { test: /\.json$/, loader: 'raw-loader'},

      { test: /bootstrap-sass\/assets\/javascripts\//, loader: 'imports?jQuery=jquery' },
      { test: /^assets\/.+\.png$/, loader: 'raw-loader' },

      // Needed for the css-loader when [bootstrap-webpack](https://github.com/bline/bootstrap-webpack)
      // loads bootstrap's css.
      { test: /\.woff(\?v=\d+\.\d+\.\d+)?$/,   loader: "url?limit=10000&minetype=application/font-woff" },
      { test: /\.woff2(\?v=\d+\.\d+\.\d+)?$/,   loader: "url?limit=10000&minetype=application/font-woff2" },
      { test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/,    loader: "url?limit=10000&minetype=application/octet-stream" },
      { test: /\.eot(\?v=\d+\.\d+\.\d+)?$/,    loader: "file" },
      { test: /\.svg(\?v=\d+\.\d+\.\d+)?$/,    loader: "url?limit=10000&minetype=image/svg+xml" }
    ]
  },
  output: {
    path    : 'dist'
  },
  plugins: [
    new webpack.optimize.OccurenceOrderPlugin(true),
    new webpack.optimize.CommonsChunkPlugin({ name: ['main', 'vendor', 'polyfills'], minChunks: Infinity }),
    new CopyWebpackPlugin([
      {from: './src/index.html', to: 'index.html'},
      {from: './src/assets', to: 'assets'},
      {from: 'node_modules/primeng/resources/themes/omega', to: 'assets/primeng/resources/themes/omega'},
      {from: 'node_modules/primeng/resources/images', to: 'assets/primeng/resources/images'},
      {from: 'node_modules/primeng/resources/primeng.min.css', to: 'assets/primeng/resources/primeng.min.css'}]),
    new DefinePlugin({'webpack': {'ENV': JSON.stringify(metadata.env), 'BACKEND_URL': JSON.stringify('http://localhost:8181')}})
  ]
};

// Our Webpack Defaults
var defaultConfig = {
  devtool: 'cheap-module-source-map',
  cache: true,
  debug: true,
  output: {
    filename: '[name].bundle.js',
    sourceMapFilename: '[name].map',
    chunkFilename: '[id].chunk.js'
  },

  resolve: {
    root: [ path.join(__dirname, 'src') ],
    extensions: ['', '.ts', '.js']
  },

  devServer: {
    historyApiFallback: true,
    watchOptions: { aggregateTimeout: 300, poll: 1000 }
  },

  node: {
    global: 1,
    crypto: 'empty',
    module: 0,
    Buffer: 0,
    clearImmediate: 0,
    setImmediate: 0
  }
};

var webpackMerge = require('webpack-merge');
module.exports = webpackMerge(defaultConfig, webpackConfig);
