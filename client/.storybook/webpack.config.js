const path = require('path');

module.exports = ({ config }) => {
  config.module.rules.push({
    test: /\.ts$/,
    use: [
      {
        loader: require.resolve('ts-loader'),
      },
    ],
  });

  config.resolve.extensions.push('.ts');

  // Map .js imports to .ts files (for ESM-style imports)
  config.resolve.extensionAlias = {
    '.js': ['.ts', '.js'],
  };

  return config;
};
