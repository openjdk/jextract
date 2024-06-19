param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the freeglut installation")]
  [string]$freeglutPath
)

jextract `
  --output src `
  -I "$freeglutPath\include" `
  --use-system-load-library `
  "-l" opengl32 `
  "-l" glu32 `
  "-l" freeglut `
  "-t" "opengl" `
  '<GL\glut.h>'

# Too many sources for command line. Put them into separate file
ls -r src/*.java | %{ $_.FullName } | Out-File sources.txt
javac -d classes '@sources.txt'

