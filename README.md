<img width="256" height="256" alt="icon_128x128@2x" src="https://github.com/user-attachments/assets/90133f83-b4f6-41c6-aab9-25d0859d2a47" />

## Pazabit Mobile — Offline Threat Reporting System

**Early-stage variant** — A decentralized peer-to-peer threat-reporting system for situations where internet and cellular networks are completely unavailable.

Built on the bitchat Android mesh architecture, this variant repurposes the Bluetooth LE / Wi-Fi Aware mesh network for submitting and propagating structured threat reports across a local device mesh without any central infrastructure, accounts, or connectivity.

> ⚠️ **Status**: Beginning stages — core mesh transport works; threat-report data model, submission UI, and mesh propagation logic are implemented and functional. Collector-node sync, media attachments, geohash location embedding, and persistence are not yet implemented.

## License

This project is released into the public domain. See the [LICENSE](LICENSE.md) file for details.

## Currently Implemented

- **Structured Threat Reports**: `SafetyEvent.Report` with body text, severity flag (NONE / ONGOING / URGENT), optional private group targeting
- **Voting**: Up/down votes on reports (`SafetyEvent.Vote`) with real-time tally updates
- **Escalation**: Urgent or highly-upvoted reports can be escalated to community groups (`SafetyEvent.Escalation`)
- **Private Groups**: Create named groups (`#tag`) for targeted report delivery (`SafetyEvent.GroupCreated`)
- **Mesh Propagation**: All events encoded via compact binary codec (`PazabitEventCodec` v1) and sent over the existing bitchat mesh (BLE + Wi-Fi Aware, multi-hop up to 7 hops, Noise XX encryption)
- **Store-and-Forward (in-memory)**: Reports received from peers are merged into local StateFlow and re-broadcast opportunistically via the mesh relay logic
- **Compose UI** (`PazabitScreen`): Report composer with flag selector, group picker, live report feed with vote buttons, escalation affordance, private-group drawer
- **No Accounts / No Servers**: Identity derived from ephemeral Noise static keys; fully decentralized

## Not Yet Implemented

- Persistence across app restarts (StateFlow only)
- Media attachments (photo/audio) in reports
- Geohash location embedding in reports (groups are string tags only)
- Collector-node aggregation and internet/cellular sync when connectivity returns
- Background duty-cycling optimizations specific to threat-report traffic
- Nostr/geohash channel fallback (original bitchat Nostr stack remains in codebase but disconnected)

## Technical Architecture

### Application Layer (New — `com.bitchat.android.pazabit`)

| Module | Responsibility |
|---|---|
| `domain/SafetyEvent.kt` | Sealed event types (Report, Vote, Escalation, GroupCreated) + `SafetyReport` view model |
| `protocol/PazabitEventCodec.kt` | Versioned binary encode/decode (DataOutputStream/InputStream) for mesh packets |
| `protocol/PazabitEventBus.kt` | In-process SharedFlow bridging `PacketProcessor` → `SafetyRepository` |
| `data/SafetyRepository.kt` | Local state (StateFlow<List<SafetyReport>>), event application, outbound send via `MeshService.sendPazabitEvent()` |
| `ui/PazabitScreen.kt` + `PazabitViewModel.kt` | Compose UI + MVVM; flag selector, group chips, vote rows, escalation button, create-group dialog |

### Mesh Transport Layer (Inherited — Unmodified Core)

- **Bluetooth LE Mesh**: Direct peer-to-peer, multi-hop relay (max 7 hops), adaptive duty cycling, connection limits
- **Wi-Fi Aware Transport**: Higher-bandwidth local mesh on supported devices
- **Noise Protocol**: XX pattern, X25519 + ChaCha20-Poly1305, forward secrecy, static-key identities
- **Packet Format**: Compact binary with fragmentation, TTL routing, deduplication
- **Foreground Service**: `MeshForegroundService` keeps mesh alive within Android background limits
- **Transport Selection**: `UnifiedMeshService` chooses BLE vs Wi-Fi Aware automatically

### Integration Points (Minimal)

- `MeshService` interface + implementations (`BluetoothMeshService`, `MeshCore`, `UnifiedMeshService`) expose `sendPazabitEvent(payload: ByteArray)`
- `PacketProcessor.handlePazabitEvent()` decodes inbound packets via `PazabitEventCodec` and publishes to `PazabitEventBus`
- `MainActivity` instantiates `PazabitViewModel` with `unifiedMeshService` and renders `PazabitScreen` after onboarding

### Android Stack

- Kotlin, Jetpack Compose (Material 3), MVVM
- Coroutines and Flow for all networking and state
- Core components reused: `MeshForegroundService`, `BluetoothMeshService` / `WifiAwareMeshService`, `UnifiedMeshService`, `NoiseSessionManager`, `PacketProcessor` (extended for Pazabit event type)

## Building

Requires Android Studio and the Android SDK (API 26+).

```bash
git clone https://github.com/permissionlesstech/bitchat-android.git
cd bitchat-android
./gradlew assembleDebug
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app requests Bluetooth, location (required for BLE scanning), and notification permissions at runtime.

Release APKs and the Android App Bundle can be rebuilt byte-for-byte in the
pinned Linux container. Maintainers should follow the
[Android release guide](docs/maintainer-release-guide.md). See
[Reproducible builds](docs/reproducible-builds.md) for the build trust model
and public GitHub/Google Play verification procedures.

## Testing

```bash
# Unit tests
./gradlew test

# Lint
./gradlew lint

# Instrumented tests (requires a device or emulator)
./gradlew connectedAndroidTest
```

Note that BLE mesh behavior is difficult to emulate; protocol and session logic is covered by unit tests, while radio-level behavior needs real devices.

---

*This variant is a fork of the [bitchat Android](https://github.com/permissionlesstech/bitchat-android) codebase, retaining its mesh networking core while replacing the messaging UI and data model with a threat-reporting workflow. The original bitchat messaging, Nostr, geohash globe, and channel-chat code remains in the repository but is no longer wired into the main UI.*
