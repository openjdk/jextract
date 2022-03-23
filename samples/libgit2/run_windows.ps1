param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the libgit2 installation")]
  [string]$libgit2path,
  [Parameter(Mandatory=$true, HelpMessage="URL to clone from")]
  [string]$url,
  [Parameter(Mandatory=$true, HelpMessage="Path to clone into")]
  [string]$path
)

. ../shared_windows.ps1

$java = find-tool("java")

& $java `
  --enable-native-access=ALL-UNNAMED `
  --add-modules jdk.incubator.foreign `
  -D"java.library.path=$libgit2path\bin" `
  GitClone.java `
  $url $path
