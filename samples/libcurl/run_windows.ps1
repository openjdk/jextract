param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation")]
  [string]$curlpath,
  [Parameter(Mandatory=$true, HelpMessage="URL to get")]
  [string]$url
)

. ../shared_windows.ps1

$java = find-tool("java")

& $java `
  --enable-native-access=ALL-UNNAMED `
  --add-modules jdk.incubator.foreign `
  -D"java.library.path=$curlpath\bin" `
  CurlMain.java `
  $url
