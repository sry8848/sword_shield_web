import antfu from '@antfu/eslint-config'

export default antfu(
  { vue: true, typescript: true },
  { ignores: ['**/dist/**', '**/coverage/**', '**/node_modules/**', '**/playwright-report/**', '**/test-results/**'] },
)
