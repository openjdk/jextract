param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation")]
  [string]$curlpath,
  [Parameter(Mandatory=$true, HelpMessage="URL to get")]
  [string]$url
)

java `
  --enable-native-access=ALL-UNNAMED `
  --enable-preview --source=22 `
  -D"java.library.path=$curlpath\bin" `
  CurlMain.java `
  $url
