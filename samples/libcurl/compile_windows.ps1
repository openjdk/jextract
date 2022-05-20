param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation")]
  [string]$curlpath
)

jextract `
  -I "$curlpath\include" `
  -I "$curlpath\include\curl" `
  --dump-includes 'includes_all.conf' `
  "$curlpath\include\curl\curl.h"
  
Select-String -Path 'includes_all.conf' -Pattern 'curl' | %{ $_.Line } | Out-File -FilePath 'includes_filtered.conf' -Encoding ascii

jextract `
  -t org.jextract `
  -I "$curlpath\include" `
  -I "$curlpath\include\curl" `
  -llibcurl `
  '@includes_filtered.conf' `
  "$curlpath\include\curl\curl.h"
