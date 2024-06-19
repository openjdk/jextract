param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the llvm installation which include/clang-c")]
  [string]$clangPath
)

jextract `
  --output src `
  -t org.llvm.clang `
  -I "$clangPath\include" `
  -I "$clangPath\include\clang-c" `
  --use-system-load-library `
  -l libclang `
  '<Index.h>'

javac -d classes (ls -r src/*.java)
