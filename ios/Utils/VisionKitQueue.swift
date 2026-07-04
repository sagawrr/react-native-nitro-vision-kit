import Foundation

enum VisionKitQueue {
  static let queue = DispatchQueue(label: "com.margelo.nitro.visionkit", qos: .userInitiated)
}
