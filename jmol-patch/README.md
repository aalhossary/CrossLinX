# Vendored Jmol with the thread-safety patch

CrossLinX depends on `net.sourceforge.jmol:jmol:16.4.1-threadsafe-1`, which is
vendored in `local-maven-repo/`. It is **not** an official Jmol release: it is
the official 16.4.1 binary with `jmol-thread-safety.patch` applied.

## Why

Jmol's `Viewer` is designed to be driven from a single thread. CrossLinX parses
in parallel on a ForkJoinPool, and every worker used to call into the viewer
directly, which corrupted its model set: `group3Lists` null in `BioResolver`,
`bioModelset` null, indices past the end of arrays sized for the previous model.

The application side of that is already fixed — every Jmol call now goes through
the Swing event thread (see `ParsingUI.runOnEdt` and `executeJmolScript`), which
is what actually protects us. This patched jar is defence in depth, and it lets
us run the fix we proposed upstream rather than only proposing it.

## Upstream

Reported as <https://sourceforge.net/p/jmol/bugs/640/> with the same patch and
reproducers attached. **When the fix (or an equivalent) ships in an official
release, delete the vendored artifact and depend on that instead** - it is 19 MB
of binary in this repository.

Jmol is not on Maven Central beyond 14.31.10 (2020), which is why this is
vendored at all rather than being a version bump. See
<https://sourceforge.net/p/jmol/bugs/635/>.

## The patch

One monitor, the per-Viewer `ModelManager` instance, held across every operation
that replaces Viewer-wide state:

| location | change |
|---|---|
| `ModelManager.zap()` | `synchronized` |
| `ModelManager.createModelSet()` | `synchronized` |
| `Viewer.zap(boolean,boolean,boolean)` | `synchronized (mm)` |
| `Viewer.createModelSetAndReturnError` | `synchronized (mm)` |
| `Viewer.openStringInlineParamsAppend` | `synchronized (mm)` |

Each wrapper delegates to the original body unchanged. A load is zap-then-create,
so the lock has to span both halves; locking them separately leaves the pair
non-atomic.

Measured with `Hunt.java` on Jmol 16.4.1, Java 26.0.1: stock fails 110 times in
600 loads over 4 threads; patched shows no failures in 8400 loads (4 and 8
threads), nor in 4000 loads through the rebuilt jar. Single-threaded load time
is unchanged.

## Rebuilding the jar

The official `Jmol.jar` contains duplicate zip entries, so `jar uf` refuses to
update it - use `zip`, which replaces entries in place.

```sh
# 1. sources and binary for the same version
curl -LO 'https://sourceforge.net/projects/jmol/files/Jmol/Version%2016.4/Jmol%2016.4.1/Jmol-16.4.1-full.tar.gz/download'
tar xzf Jmol-16.4.1-full.tar.gz && cd jmol-16.4.1

# 2. apply the patch
patch -p1 < ../jmol-patch/jmol-thread-safety.patch

# 3. compile just the two patched files against the released jar.
#    -sourcepath is required: javajs.J2SIgnoreImport ships in source only.
mkdir /tmp/patched
javac -implicit:none -cp Jmol.jar -sourcepath src -d /tmp/patched \
    src/org/jmol/viewer/Viewer.java src/org/jmol/viewer/ModelManager.java

# 4. graft the classes onto a copy of the official jar
cp Jmol.jar jmol-16.4.1-threadsafe-1.jar
(cd /tmp/patched && zip -q $OLDPWD/jmol-16.4.1-threadsafe-1.jar \
    org/jmol/viewer/Viewer.class 'org/jmol/viewer/Viewer$1.class' \
    'org/jmol/viewer/Viewer$2.class' 'org/jmol/viewer/Viewer$ACCESS.class' \
    org/jmol/viewer/ModelManager.class)

# 5. verify: no failures multi-threaded, and none single-threaded either
javac -cp jmol-16.4.1-threadsafe-1.jar ../jmol-patch/JmolThreadCrash.java
java -cp jmol-16.4.1-threadsafe-1.jar:. JmolThreadCrash

# 6. install into the project repository
mvn install:install-file -Dfile=jmol-16.4.1-threadsafe-1.jar \
    -DgroupId=net.sourceforge.jmol -DartifactId=jmol \
    -Dversion=16.4.1-threadsafe-1 -Dpackaging=jar \
    -DlocalRepositoryPath=local-maven-repo
```

Step 6 writes a descriptor lifted from the pom shaded **inside** Jmol.jar (the
`jna-inchi` one, which depends on an unresolvable `1.4-SNAPSHOT`). Replace it
with the hand-written `.pom` already in `local-maven-repo`, and regenerate the
`.md5`/`.sha1` sidecars.

`META-INF/CROSSLINX-PATCH.txt` inside the jar records the same provenance for
anyone who meets the file on its own.
