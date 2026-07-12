import { StatusBar, View } from 'react-native'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { PlaygroundScreen } from './src/PlaygroundScreen'
import { paper } from './src/palette'

function App() {
  return (
    <SafeAreaProvider>
      <View style={{ flex: 1, backgroundColor: paper }}>
        <StatusBar barStyle="dark-content" backgroundColor={paper} />
        <PlaygroundScreen />
      </View>
    </SafeAreaProvider>
  )
}

export default App
