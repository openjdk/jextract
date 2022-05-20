param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the llvm installation which include/clang-c")]
  [string]$clangPath
)

jextract `
  -t org.llvm.clang `
  -I "$clangPath\include" `
  -I "$clangPath\include\clang-c" `
  -l libclang `
  "$clangPath\include\clang-c\Index.h"
