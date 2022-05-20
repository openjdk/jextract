param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the libgit2 installation")]
  [string]$libgit2path
)

jextract `
  -t com.github `
  -I "$libgit2path\include" `
  -l git2 `
  "$libgit2path\include\git2.h"
