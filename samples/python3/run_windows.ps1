param(
  [Parameter(Mandatory=$true, HelpMessage="The path python install")]
  [string]$pythonPath
)

java `
  -cp classes `
  --enable-native-access=ALL-UNNAMED `
  -D"java.library.path=$pythonPath" `
  PythonMain.java
