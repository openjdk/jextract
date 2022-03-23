param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the llvm installation which include bin/libclang.dll")]
  [string]$clangPath
)

. ../shared_windows.ps1

$java = find-tool("java")

& $java `
  --enable-native-access=ALL-UNNAMED `
  --add-modules jdk.incubator.foreign `
  -D"java.library.path=$clangPath\bin" `
  ASTPrinter.java hello.c