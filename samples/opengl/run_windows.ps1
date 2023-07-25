param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the freeglut installation")]
  [string]$freeglutPath
)

java `
  --enable-native-access=ALL-UNNAMED `
  --enable-preview --source=21 `
  -D"java.library.path=C:\Windows\System32`;$freeglutPath\bin\x64" `
  Teapot.java
