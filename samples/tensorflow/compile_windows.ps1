param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the tensorflow installation which include/tensorflow/c")]
  [string]$tensorflowPath
)

jextract `
  --output src `
  -t org.tensorflow `
  -I "$tensorflowPath\include" `
  --use-system-load-library `
  -l tensorflow `
  '<tensorflow\c\c_api.h>'

javac -d classes (ls -r src/*.java)
