import typescript from '@rollup/plugin-typescript';
import { fromRollup } from '@web/dev-server-rollup';

const ts = fromRollup(typescript);

export default {
  files: ['src/**/*.test.ts', 'src/components/**/*.test.ts'],
  plugins: [ts()],
  nodeResolve: true,
  testFramework: {
    config: {
      ui: 'bdd',
      timeout: '2000',
    },
  },
  testRunnerHtml: (testRunnerImport, config) =>
    `<html>
      <body>
        <script>
          window.process = { env: { NODE_ENV: 'development' } }
        </script>
        <script type="module" src="./test/setup.js"></script>
        <script type="module" src="${testRunnerImport}"></script>
      </body>
    </html>`,
};