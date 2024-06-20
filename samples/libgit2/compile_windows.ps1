param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the libgit2 installation")]
  [string]$libgit2path
)

jextract `
  --output src `
  -t com.github `
  -I "$libgit2path\include" `
  --use-system-load-library `
  -l git2 `
  '<git2.h>'

javac -d classes (ls -r src/*.java)
