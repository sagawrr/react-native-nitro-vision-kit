const path = require('path')
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config')

const projectRoot = __dirname
const libraryRoot = path.resolve(projectRoot, '..')

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * Metro configuration for the local library example.
 * Watches the parent package so `react-native-nitro-vision-kit` source reloads.
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {
  watchFolders: [libraryRoot],
  resolver: {
    // Only block the library's own node_modules — never the example app's.
    blockList: [
      new RegExp(
        `^${escapeRegExp(path.join(libraryRoot, 'node_modules'))}[/\\\\].*`,
      ),
    ],
    nodeModulesPaths: [path.resolve(projectRoot, 'node_modules')],
    extraNodeModules: {
      'react-native-nitro-vision-kit': libraryRoot,
      react: path.resolve(projectRoot, 'node_modules/react'),
      'react-native': path.resolve(projectRoot, 'node_modules/react-native'),
      'react-native-nitro-modules': path.resolve(
        projectRoot,
        'node_modules/react-native-nitro-modules',
      ),
      '@babel/runtime': path.resolve(
        projectRoot,
        'node_modules/@babel/runtime',
      ),
    },
    disableHierarchicalLookup: true,
  },
}

module.exports = mergeConfig(getDefaultConfig(projectRoot), config)
