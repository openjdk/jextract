param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the freeglut installation")]
  [string]$freeglutPath
)

java `
  -cp classes `
  --enable-native-access=ALL-UNNAMED `
  -D"java.library.path=C:\Windows\System32`;$freeglutPath\bin\x64" `
  Teapot.java
