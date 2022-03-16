param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the freeglut installation")]
  [string]$freeglutPath
)

. ../shared_windows.ps1

$java = find-tool("java")

& $java `
  --enable-native-access=ALL-UNNAMED `
  --add-modules jdk.incubator.foreign `
  -D"java.library.path=C:\Windows\System32`;$freeglutPath\bin\x64" `
  Teapot.java
