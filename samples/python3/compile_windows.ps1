param(
  [Parameter(Mandatory=$true, HelpMessage="The path python install")]
  [string]$pythonPath,
  [Parameter(Mandatory=$true, HelpMessage="The name of the python dll to link against. For instance 'python38', for python38.dll")]
  [string]$pythonLibName
)

jextract `
  --output src `
  -I "$pythonPath\include" `
  "-l" $pythonLibName `
  "-t" "org.python" `
  '<Python.h>'

javac -d classes (ls -r src/*.java)
