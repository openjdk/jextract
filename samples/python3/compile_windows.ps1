param(
  [Parameter(Mandatory=$true, HelpMessage="The path python install")]
  [string]$pythonPath,
  [Parameter(Mandatory=$true, HelpMessage="The name of the python dll to link against. For instance 'python38', for python38.dll")]
  [string]$pythonLibName
)

jextract `
  -I "$pythonPath\include" `
  "-l" $pythonLibName `
  "-t" "org.python" `
  "$pythonPath\include\Python.h"