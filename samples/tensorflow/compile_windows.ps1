param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the tensorflow installation which include/tensorflow/c")]
  [string]$tensorflowPath
)

jextract `
  -t org.tensorflow `
  -I "$tensorflowPath\include" `
  -l tensorflow `
  "$tensorflowPath\include\tensorflow\c\c_api.h"
