param(
  [Parameter(Mandatory=$true, HelpMessage="The path python install")]
  [string]$pythonPath
)

java `
  --enable-native-access=ALL-UNNAMED `
  --enable-preview --source=22 `
  -D"java.library.path=$pythonPath" `
  PythonMain.java