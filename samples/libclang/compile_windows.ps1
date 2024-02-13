param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the llvm installation which include/clang-c")]
  [string]$clangPath
)

jextract `
  --output src `
  -t org.llvm.clang `
  -I "$clangPath\include" `
  -I "$clangPath\include\clang-c" `
  -l libclang `
  "$clangPath\include\clang-c\Index.h"

javac -d classes (ls -r src/*.java)
