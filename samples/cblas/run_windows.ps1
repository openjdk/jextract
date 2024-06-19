param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation, which contains bin/liblapacke.dll")]
  [string]$blasPath,
  [Parameter(Mandatory=$true, HelpMessage="The path to the mingw bin directory which contains libgcc_s_seh-1.dll and libquadmath-0.dll")]
  [string]$mingwBinPath
)

$Env:path+="`;$blasPath\bin" # libblas.dll
$Env:path+="`;$mingwBinPath" # mingw runtime dlls

java `
  -cp classes `
  --enable-native-access=ALL-UNNAMED `
  TestBlas.java `
