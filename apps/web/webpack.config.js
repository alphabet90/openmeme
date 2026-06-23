const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');

module.exports = (env, argv) => ({
  entry: './assets/js/app.js',
  output: {
    path: path.resolve(__dirname, 'public/assets'),
    filename: 'app.js',
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
  plugins: [new MiniCssExtractPlugin({ filename: 'app.css' })],
  optimization: {
    minimizer: [new TerserPlugin({ extractComments: false }), new CssMinimizerPlugin()],
  },
  devtool: argv.mode === 'development' ? 'source-map' : false,
  performance: { hints: false },
});
