param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lapack installation which include/cblas.h and dependent headers")]
  [string]$blasPath
)

jextract `
  --output src `
  -t blas `
  -I "$blasPath\include" `
  -l libcblas `
  "$blasPath\include\cblas.h"

javac -d classes (ls -r src/*.java)
