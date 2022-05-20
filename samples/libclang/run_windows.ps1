param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the llvm installation which include bin/libclang.dll")]
  [string]$clangPath
)

java `
  --enable-native-access=ALL-UNNAMED `
  --enable-preview --source=19 `
  -D"java.library.path=$clangPath\bin" `
  ASTPrinter.java hello.c