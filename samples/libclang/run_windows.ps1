param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the llvm installation which include bin/libclang.dll")]
  [string]$clangPath
)

java `
  -cp classes `
  --enable-native-access=ALL-UNNAMED `
  -D"java.library.path=$clangPath\bin" `
  ASTPrinter.java hello.c
