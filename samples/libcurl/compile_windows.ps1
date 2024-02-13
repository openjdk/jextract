param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation")]
  [string]$curlpath
)

jextract `
  -I "$curlpath\include" `
  -I "$curlpath\include\curl" `
  --dump-includes 'includes_all.conf' `
  "$curlpath\include\curl\curl.h"
  
Select-String -Path 'includes_all.conf' -Pattern '(curl|sockaddr )' | %{ $_.Line } | Out-File -FilePath 'includes_filtered.conf' -Encoding ascii

jextract `
  --output src `
  -t org.jextract `
  -I "$curlpath\include" `
  -I "$curlpath\include\curl" `
  -llibcurl `
  '@includes_filtered.conf' `
  "$curlpath\include\curl\curl.h"

javac -d classes (ls -r src/*.java)
