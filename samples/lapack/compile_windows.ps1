param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lapack installation which include/lapacke.h and dependent headers")]
  [string]$lapackPath
)

. ../shared_windows.ps1

$jextract = find-tool("jextract")

& $jextract `
  -t lapack `
  -I "$lapackPath\include" `
  -l liblapacke `
  -- `
  "$lapackPath\include\lapacke.h"
