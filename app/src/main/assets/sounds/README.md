# Sound Effects

Place `.wav` files here. Each file must match the filename defined in the
`DartSound` enum inside `SoundEffectsService.kt`.

Expected files:

| File            | Triggered when                  |
|-----------------|---------------------------------|
| `dart_hit.wav`  | A dart scores (normal visit)    |
| `bust.wav`      | Score goes over (bust)          |
| `checkout.wav`  | Player wins the leg (game shot) |

Missing files are silently ignored — the app will not crash if a file is absent.
