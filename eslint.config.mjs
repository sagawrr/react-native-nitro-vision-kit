import rnConfig from '@react-native/eslint-config/flat'
import prettierConfig from 'eslint-config-prettier'

/**
 * ESLint 9 flat config.
 *
 * Replaces the legacy `eslintConfig` block the nitrogen template wrote into
 * package.json (which ESLint 9 cannot read). Uses React Native's official flat
 * config plus Prettier conflict-disabling.
 */
export default [
  {
    ignores: [
      'node_modules/',
      'lib/',
      'nitrogen/generated/',
      'android/build/',
      '.cxx/',
      'bun.lock',
    ],
  },
  ...rnConfig,
  prettierConfig,
]
