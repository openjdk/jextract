param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation")]
  [string]$curlpath
)

. ../shared_windows.ps1

$jextract = find-tool("jextract")

& $jextract `
  -I "$curlpath\include" `
  -I "$curlpath\include\curl" `
  --dump-includes 'includes_all.conf' `
  -- `
  "$curlpath\include\curl\curl.h"
  
filter_file 'includes_all.conf' 'curl' 'includes_filtered.conf'

& $jextract `
  -t org.jextract `
  -I "$curlpath\include" `
  -I "$curlpath\include\curl" `
  -llibcurl `
  '@includes_filtered.conf' `
  -- `
  "$curlpath\include\curl\curl.h"
