# X-Ray Vision

A client-side **X-Ray mod** for **Minecraft Java Edition 1.21.11** built on the **Fabric** mod loader.

When you press the toggle key, ordinary terrain becomes invisible and the blocks you care about —
ores, chests, spawners, shulker boxes and raw metal blocks — stay visible through walls, each drawn
with a strong, easy-to-read colour highlight. Press the key again and the game goes straight back to
normal vanilla rendering. Nothing is ever written to your world.

---

## Features

* **Toggleable X-Ray** — press <kbd>X</kbd> (default) to switch between normal view and X-Ray view
  instantly. No world reload, no restart, no chunk re-download.
* **Terrain hiding** — non-target blocks stop contributing geometry to the client chunk mesh, so you
  see through them, while water and lava still render so you always know where you are.
* **ESP-style highlighting** — visible blocks get a coloured wireframe outline plus a translucent
  coloured fill, so the ore type is recognisable at a glance.
* **Sensible defaults** — every overworld ore, its deepslate variant, nether quartz / nether gold /
  ancient debris, raw iron/copper/gold blocks, all chests (including copper chests and trapped
  chests), ender chests, spawners (including trial spawners) and all 17 shulker boxes.
* **Small unobtrusive HUD** — an `X-RAY ON` / `X-RAY OFF` badge in the corner of the screen.
* **Persistent configuration** — everything lives in `config/xrayvision.json` and survives restarts.
* **Client side only** — works in singleplayer and on any vanilla server without the server having
  the mod installed.

## Requirements

| Component        | Version            |
|------------------|--------------------|
| Minecraft        | `1.21.11`          |
| Fabric Loader    | `>= 0.19.5`        |
| Fabric API       | required (`0.141.6+1.21.11` tested) |
| Java (to build)  | 25 (Loom 1.18); mod bytecode targets 21 |
| Java (to play)   | 21 (Minecraft 1.21.11) |

## Installation

1. Install **Fabric Loader** for `1.21.11` from [fabricmc.net](https://fabricmc.net/use/installer/).
2. Download the matching **Fabric API** and put it in `.minecraft/mods/`.
3. Put the `xrayvision-*.jar` from a release (or `build/libs/`) into `.minecraft/mods/`.
4. Launch the game. Press <kbd>X</kbd> in a world to try it.

## Keybinds

| Action      | Default | Where to change                                  |
|-------------|---------|--------------------------------------------------|
| Toggle X-Ray| `X`     | Options &rarr; Controls &rarr; Key Binds &rarr; "X-Ray Vision", or `config/xrayvision.json` (`toggleKey`) |

## Configuration

The file `config/xrayvision.json` is created on first launch. Main options:

| Key                 | Type    | Default  | Meaning                                             |
|---------------------|---------|----------|-----------------------------------------------------|
| `enabled`           | boolean | `false`  | Master switch (restored on next launch).            |
| `toggleKey`         | string  | `key.keyboard.x` | Name of the bound key.                        |
| `highlightingEnabled` | boolean | `true` | Draw the ESP overlay while active.                |
| `outlinesEnabled`   | boolean | `true`  | Draw the coloured wireframe box.                   |
| `fillEnabled`       | boolean | `true`  | Draw the translucent coloured fill.                |
| `highlightOpacity`  | float   | `0.22`  | Alpha of the fill, `0.0`–`1.0`.                    |
| `hudEnabled`        | boolean | `true`  | Show the `X-RAY ON/OFF` badge.                     |
| `hudHideWhenOff`    | boolean | `false` | Hide the badge entirely while off.                 |
| `scanRadius`        | int     | `48`    | Blocks around the player scanned for highlights.   |
| `maxHighlights`     | int     | `4096`  | Cap on highlight boxes per frame.                  |
| `blocks`            | map     | all on  | Per-block visibility, e.g. `minecraft:diamond_ore: true`. |

Changing `blocks` lets you enable/disable any individual ore or hide one you don't care about.

## Highlight colours

| Block            | Colour        |   | Block            | Colour   |
|------------------|---------------|---|------------------|----------|
| Coal             | dark gray     |   | Emerald          | green    |
| Copper           | orange        |   | Ancient Debris   | dark brown |
| Iron             | light gray    |   | Nether Quartz    | white    |
| Gold             | yellow        |   | Nether Gold      | golden yellow |
| Redstone         | red           |   | Chests           | tan      |
| Lapis            | blue          |   | Ender Chest      | dark purple |
| Diamond          | cyan          |   | Spawner          | dark blue-gray |

## How it works (briefly)

* A Mixin on `BlockBehaviour.getRenderShape` answers `INVISIBLE` for non-target blocks, which is the
  single decision point used by both the vanilla section compiler and Sodium's chunk mesher — so the
  terrain disappears without editing block states or chunk data.
* A Mixin on `BlockEntityRenderDispatcher.tryExtractRenderState` hides block-entity renders
  (furnaces, signs, beds...) for hidden blocks, so they don't float through walls.
* A Mixin on `LevelRenderer.blockChanged` marks a chunk column stale when a block changes, so the
  highlight cache is updated lazily instead of rescanning.
* Highlights are cached per chunk column and drawn through Fabric's `WorldRenderEvents` using only
  vanilla render types (`RenderTypes.lines()` and `RenderTypes.debugFilledBox()`).
* Turning the feature on flips `Minecraft#smartCull` so occlusion culling can't hide distant ore;
  the previous value is restored when the feature is switched off or the game closes.

## Performance

The mod never scans the world per frame. It scans at most two chunk columns per tick, caches the
result, prunes columns that leave the radius or unload, and iterates the cache only while drawing.
Highlight boxes are emitted with zero per-frame allocations and are capped by `maxHighlights`.

## Compatibility

* **Sodium**: the two rendering hooks (`getRenderShape` and `tryExtractRenderState`) are both on code
  paths that Sodium 1.21.11 calls, so terrain hiding and block-entity hiding work there too. This was
  verified against Sodium's 1.21.11 source; it is not a runtime-tested guarantee.
* **OptiFine**: not supported (and not possible on modern versions).
* Other rendering-heavy mods that replace the chunk mesher entirely may bypass the terrain hiding.

## Known limitations

* Fluids (water/lava) always render, by design, so you can keep your bearings.
* While X-Ray is on, terrain-derived particles and some overlays for hidden blocks are reduced.
* The highlight overlay uses standard (depth-tested) render types; with X-Ray off it can be occluded
  by solid terrain, which is expected.
* In-game behaviour has not been automatically tested — see "Testing".

## Building from source

```sh
git clone https://github.com/AdamJaroui23/X-Ray-Vision.git
cd X-Ray-Vision
./gradlew build     # also runs the unit tests
```

The remapped mod jar lands in `build/libs/xrayvision-<version>.jar`.

## Testing

* `./gradlew build` compiles against the real Minecraft 1.21.11 / Fabric API artifacts and remaps the
  mixins; it also runs the JUnit suite for the configuration model.
* A GitHub Actions workflow (`.github/workflows/build.yml`) runs the same build on every push and
  uploads the jars and the test report.
* Full in-game behaviour (rendering, keybind feel, HUD) is not covered by automation and should be
  confirmed in a real client.

## Troubleshooting

* **Nothing happens when I press the key** — make sure you're in a world (not a menu) and that the
  binding is set under "X-Ray Vision" in the controls screen.
* **Mod crashes on startup** — confirm Fabric API for 1.21.11 is installed and your loader is
  `>= 0.19.5`.
* **Highlights don't match a block I disabled** — `X-Ray Vision` rebuilds the cache when the config
  changes; if you edit the JSON while the game runs, restart.

## License

MIT — see [LICENSE](LICENSE).
