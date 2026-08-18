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

## A whole-cell map need not cover the cross-links

Measured on real entries, contouring at the 5 Å clip radius around the interacting atoms:

| entry | source | result |
|---|---|---|
| 1M3Q | RCSB volume server | 306 vertices |
| 1CBS (around its ligand) | RCSB volume server | 4011 vertices |
| 2ATK | RCSB volume server | nothing — not covered |
| 3ALB | RCSB volume server, and PDBe CCP4 | nothing — not covered |

The cause is coverage, not contouring. A map covers one box; the deposited coordinates need
not lie inside it. All four of 3ALB's LYS48–GLY76 cross-links have a negative z, and the map
box runs from the origin to `{58, 76, 134}`.

**The density is not missing, only displaced.** Read from the CCP4 header, 3ALB's map is
64×90×150 samples with start `0,0,0` over a cell of 59.1 × 77.4 × 135.1 Å — exactly one full
cell, and therefore periodic. So the density at z = −24.2 *is* the density at z = 110.9.
Contouring at the lattice-translated position proves it: every cross-link yields a good
surface there.

| atom | deposited | translated into the cell | surface there |
|---|---|---|---|
| LYS48:C.NZ | −9.10, −10.34, −20.08 | 50.00, 67.06, 115.02 | 325 vertices |
| LYS48:B.NZ | 11.60, 6.05, −24.20 | 11.60, 6.05, 110.90 | 294 vertices |
| LYS48:A.NZ | 23.66, 25.90, −14.40 | 23.66, 25.90, 120.70 | 321 vertices |
| LYS48:D.NZ | 15.08, 2.61, −14.61 | 15.08, 2.61, 120.49 | 272 vertices |

What has *not* been found is a way to make Jmol draw that density at the atoms. Tried and
rejected, all still empty:

- `isosurface … periodic …`, which does exist and does wrap voxel lookups
  (`VolumeData.indexLower` wraps the index when `isPeriodic`), but the `within` clip is
  evaluated against the map's box first;
- `offset {0 0 −135.1}` both inside the isosurface command and as a later command on the
  finished surface — the surface does not move;
- setting the cell on the model first. Jmol has no unit cell for these models either way
  (`{atom}.fx` returns the Cartesian x, whether the structure arrives as BioJava-written
  mmCIF or is loaded straight from the original file). `unitcell "a=59.1,b=77.4,c=135.1,
  alpha=90,beta=90,gamma=90"` does set one — `.fx` becomes −0.154 — but the isosurface is
  still empty.

Enlarging the clip radius to 20 Å eventually reaches *into* the box and draws something, but
that something is not the density at the cross-link.

This matters more for CrossLinX than for a general viewer: a cross-link is often at a crystal
contact, which is exactly where an entry's coordinates run outside the cell box.

The application copes rather than fixes: when a map loads but contours to nothing, the
structure's tooltip says the map does not cover those atoms, instead of leaving the user with
"available" and an empty screen.

**Worth doing, upstream, and the cleanest of the options:** BioJava asks the Mol* volume
server for `/volume-server/x-ray/<id>/cell`, which is the whole cell. The same server also
serves `/box/<a,b,c>/<x,y,z>`, an arbitrary region. Asking for a box around the atoms of
interest sidesteps the whole problem — the map arrives already covering them — and is far
smaller: 3ALB's cell response is 5.3 MB.

**The alternative** is to do what Coot and PyMOL do and expand the map by symmetry to the
region the model occupies, rather than expecting the viewer to wrap it. That is real
crystallographic work and does not belong in this application; it belongs wherever the map is
prepared.

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
