param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the lib curl installation, which contains bin/liblapacke.dll")]
  [string]$lapackPath,
  [Parameter(Mandatory=$true, HelpMessage="The path to the mingw bin directory which contains libgcc_s_seh-1.dll and libquadmath-0.dll")]
  [string]$mingwBinPath
)

. ../shared_windows.ps1

$java = find-tool("java")

$Env:path+="`;$lapackPath\bin" # libblas.dll
$Env:path+="`;$mingwBinPath" # mingw runtime dlls

& $java `
  --enable-native-access=ALL-UNNAMED `
  --add-modules jdk.incubator.foreign `
  -D"java.library.path=$lapackPath\bin" `
  TestLapack.java `
