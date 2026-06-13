const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');

module.exports = (env, argv) => ({
  entry: {
    app: './assets/js/app.js',
    critical: './assets/css/critical.css',
  },
  output: {
    path: path.resolve(__dirname, 'public/assets'),
    filename: '[name].js',
    clean: true,
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [MiniCssExtractPlugin.loader, 'css-loader'],
      },
    ],
  },
  plugins: [new MiniCssExtractPlugin({ filename: '[name].css' })],
  optimization: {
    minimizer: [new TerserPlugin({ extractComments: false }), new CssMinimizerPlugin()],
  },
  devtool: argv.mode === 'development' ? 'source-map' : false,
  performance: { hints: false },
});
