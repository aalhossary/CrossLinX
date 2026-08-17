# Known issues

Things found while working on the code that are worth fixing, but were out of scope at the
time. None are urgent. Each says what is wrong, where, and why it has not been fixed yet, so
that the next person to trip over one recognises it instead of rediscovering it.

## Settings have no single source of truth

A default currently lives in up to three places at once, and nothing keeps them agreeing:

| source | example | when it wins |
|---|---|---|
| bundled `res/CrossLinX_settings.ini` | `domainEnabled=true` | only when no file exists in the working directory — it is read through `ClassLoader.getSystemResourceAsStream` |
| hard-coded fallback in `SettingsManager.loadSettings()` | `readBooleanProperty(props, KEY, true)` | when the file exists but lacks that key |
| `./CrossLinX_settings.ini` | whatever the user last saved | whenever it is present |

Two further splits compound it:

- `pdbFilePath`, `fetchBehavior` and `fileFormat` are not `SettingsManager` fields at all. They
  live on the BioJava `UserConfiguration`, which is why `setPdbFilePath` **must** be called
  first — it *constructs* that object, and every later setter dereferences it.
- The command line writes in through a fourth path (`ProteinParser.main`).

And the settings file is resolved relative to the working directory, so which one wins depends
on where the application was launched from.

The consequences are that the bundled ini and the code fallbacks are independent copies which
can drift apart silently; that adding a setting means touching four places; and that "restore
defaults" has no single authority to reset *to*.

The density settings added in 0.9 start the way the rest should end up: every one has a
`DEFAULT_*` constant on `SettingsManager`, and both `loadSettings()` and the Restore defaults
buttons read from it.

**Worth doing:** make the `DEFAULT_*` constants the only authority; generate the bundled ini
from them or delete it, since the fallbacks already cover a missing file; resolve the settings
file to a fixed per-user location instead of the working directory; and move the three
`UserConfiguration`-held values behind ordinary fields.

## Settings that are not honoured

| setting | what actually happens |
|---|---|
| `autoFetch` ("Autofetch Files") | **Inert after start-up.** Both places that would apply it are commented out — `ProteinParser` (`atomCache.setFetchBehavior`) and `ResultManager.getStructureById` (`fileReader.setFetchBehavior`). It reaches `AtomCache` only because the constructor copies `UserConfiguration` once, so changing the checkbox has no effect for the rest of the session, and the viewer's own structure loading ignores it entirely. |
| `fileFormat` | Honoured live by `ResultManager`, which builds a fresh reader per call, but never re-pushed into `AtomCache` — there is no `setFiletype` in `refreshSettings()`. Same start-up-only shape as `autoFetch`. |
| `domainEnabled` ("enable viewing domains") | **Fully inert.** Persisted and restored correctly, but its only reader is a commented-out line inside the commented-out, `@deprecated` `ResultManager.generateJMolScriptString`. The checkbox does nothing at all. |

The density settings deliberately avoid repeating this: `DensityService.newCache` is built per
fetch from the live settings rather than once at start-up, and `ParsingUI.refreshSettings()`
re-applies them to the structure already on screen.

## wwPDB map coefficients cannot be displayed

`DensityMapSource.WWPDB_MAP_COEFFICIENTS` serves structure-factor amplitudes and phases rather
than a sampled grid, so Jmol draws nothing from it. Every fetch therefore asks for renderable
formats only, and the source is listed but greyed out in the density options.

Converting one needs a Fourier transform, which BioJava does not implement.

**Worth doing:** detect `gemmi` on the `PATH`, run `gemmi sf2map` into the density cache, and
enable the source when the conversion is available. CCP4's `cif2mtz` followed by `fft` is the
alternative. Until then the other sources cover every entry that has a grid at all.

## Smaller things

- `ResultManager.generateFileLoadJMolScript` builds a path that omits the
  `data/structures/divided/<format>` segment which BioJava and `prepareFilesList` both use. The
  method is unused, but do not copy it — copy the construction in `prepareFilesList` instead.
- `SettingsManager.saveSettings(true)` deliberately skips notifying listeners. That is right for
  shutdown, but it means any "apply on close" logic would not run.
- `ProteinParser.parseChain_old` is dead code kept beside the live `parseChain`. Two statistics
  counters were incremented only there, which is why "Amino Acids Found" and "Het Groups Found"
  read zero for years.
- `FineTuningDialogue` is an empty shell: every control in it is commented out, so it shows only
  OK and Cancel. It was meant to hold the search parameters (ring diameters, cutoff ratios).
