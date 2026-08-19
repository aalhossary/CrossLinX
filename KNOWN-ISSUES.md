# Known issues

Things found while working on the code that are worth fixing, but were out of scope at the
time. None are urgent. Each says what is wrong, where, and why it has not been fixed yet, so
that the next person to trip over one recognises it instead of rediscovering it.

## Settings: where a default lives

Every setting's default is now a `DEFAULT_*` constant on `SettingsManager`, and nowhere else.
`loadSettings()` falls back to it when a key is absent, and the "Restore defaults" buttons reset
to it. The bundled `res/CrossLinX_settings.ini` used to carry a second copy of some defaults,
which could drift from the code silently; it now carries only `PdbFolder` and `HomeFolder`,
which stay there as plain strings because a path is the one setting a constant cannot guess for
someone else's machine.

**What is still worth doing:**

- The settings file is resolved relative to the working directory, so which one wins depends on
  where the application was launched from. A fixed per-user location would be better.
- `pdbFilePath`, `fetchBehavior` and `fileFormat` are not `SettingsManager` fields at all. They
  live on the BioJava `UserConfiguration`, which is why `setPdbFilePath` **must** be called
  first — it *constructs* that object, and every later setter dereferences it. Moving them
  behind ordinary fields would remove that ordering trap.
- The command line writes settings in through a fourth path (`ProteinParser.main`).

## Settings that are not honoured

None remain. `autoFetch` and `fileFormat` used to apply only at start-up, because `AtomCache`
is built once in a field initialiser and copies its configuration by value while the two lines
that would have pushed later changes back sat commented out. Both are now re-applied in
`ProteinParser.refreshSettings()`, and `ResultManager.getStructureById` sets the fetch
behaviour on the reader it builds. `HonouredSettingsTest` guards them by asking the loader what
it holds after a refresh rather than asking the settings object what it was told.

`domainEnabled` was removed rather than fixed: nothing read it, and the feature behind it - a
per-structure domain drawn as a sphere - had been commented out and `@deprecated` for years.
Its checkbox now carries "Halo the picked interaction" instead.

**Left behind deliberately:** `encodeDrawSphereCommand`, `decodeDrawSphereCommand` and
`generateDrawEllipsoidCommand` are live methods with no callers, the remains of that feature.
They are harmless and reusable Jmol command builders, so they were not deleted with the
setting. Reviving domains would mean widening `encodeDrawSphereCommand`'s format from
centre-and-radius to three axis vectors, since it cannot express an anisotropic shape.

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
