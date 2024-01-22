param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation")]
  [string]$curlpath,
  [Parameter(Mandatory=$true, HelpMessage="URL to get")]
  [string]$url
)

java `
  -cp classes `
  --enable-native-access=ALL-UNNAMED `
  -D"java.library.path=$curlpath\bin" `
  CurlMain.java `
  $url
