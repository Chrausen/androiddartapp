# Sound Effects

Place `.wav` files here. Each file must match the naming conventions below.

## Click sound

| File         | Triggered when                              |
|--------------|---------------------------------------------|
| `click.wav`  | Any button press outside the live game      |

## Throw sounds (random selection)

Name any number of throw files with the prefix `throw` — one is chosen at random
on every dart thrown.

| Example filenames                              |
|------------------------------------------------|
| `throw-1.wav`                                  |
| `throw-2.wav`                                  |
| `throw-3.wav`                                  |
| … (add as many as you like)                    |

## Outcome sounds

| File            | Triggered when                  |
|-----------------|---------------------------------|
| `bust.wav`      | Score goes over (bust)          |
| `checkout.wav`  | Player wins the leg (game shot) |

Missing files are silently ignored — the app will not crash if a file is absent.
