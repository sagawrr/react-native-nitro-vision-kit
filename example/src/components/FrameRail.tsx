import {
  ActivityIndicator,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
} from 'react-native'
import { ink, line, signal, well } from '../palette'
import { thumbSource, type SamplePhoto } from '../samplePhotos'

type Props = {
  photos: SamplePhoto[]
  selectedId: string | null
  onSelect: (photo: SamplePhoto) => void
  onAdd: () => void
  adding?: boolean
  disabled?: boolean
}

export function FrameRail({
  photos,
  selectedId,
  onSelect,
  onAdd,
  adding,
  disabled,
}: Props) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={styles.row}
      style={styles.rail}>
      {photos.map((photo, index) => {
        const on = selectedId === photo.id
        return (
          <Pressable
            key={photo.id}
            accessibilityLabel={`Photo ${index + 1}`}
            disabled={disabled}
            onPress={() => onSelect(photo)}
            style={[styles.frame, on && styles.frameOn, disabled && styles.dim]}>
            <Image
              source={thumbSource(photo)}
              style={styles.image}
              resizeMode="cover"
            />
          </Pressable>
        )
      })}
      <Pressable
        accessibilityLabel="Add photo"
        disabled={disabled || adding}
        onPress={onAdd}
        style={[styles.add, (disabled || adding) && styles.dim]}>
        {adding ? (
          <ActivityIndicator color={signal} size="small" />
        ) : (
          <Text style={styles.plus}>+</Text>
        )}
      </Pressable>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  rail: {
    marginTop: 28,
  },
  row: {
    paddingHorizontal: 24,
    gap: 10,
    alignItems: 'center',
    paddingBottom: 4,
  },
  frame: {
    width: 48,
    height: 60,
    backgroundColor: well,
    overflow: 'hidden',
    opacity: 0.42,
  },
  frameOn: {
    opacity: 1,
  },
  image: { width: '100%', height: '100%' },
  add: {
    width: 48,
    height: 60,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: line,
  },
  plus: {
    color: ink,
    fontSize: 20,
    fontWeight: '200',
  },
  dim: { opacity: 0.35 },
})
