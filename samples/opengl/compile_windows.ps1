param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the freeglut installation")]
  [string]$freeglutPath
)

jextract `
  --output src `
  -I "$freeglutPath\include" `
  "-l" opengl32 `
  "-l" glu32 `
  "-l" freeglut `
  "-t" "opengl" `
  "$freeglutPath\include\GL\glut.h"

# Too many sources for command line. Put them into separate file
ls -r src/*.java | %{ $_.FullName } | Out-File sources.txt
javac -d classes '@sources.txt'

