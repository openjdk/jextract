param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the freeglut installation")]
  [string]$freeglutPath
)

. ../shared_windows.ps1

$jextract = find-tool("jextract")

& $jextract `
  -I "$freeglutPath\include" `
  "-l" opengl32 `
  "-l" glu32 `
  "-l" freeglut `
  "-t" "opengl" `
  "--" `
  "$freeglutPath\include\GL\glut.h"
