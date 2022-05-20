param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lapack installation which include/lapacke.h and dependent headers")]
  [string]$lapackPath
)

jextract `
  -t lapack `
  -I "$lapackPath\include" `
  -l liblapacke `
  "$lapackPath\include\lapacke.h"
