param(
  [Parameter(Mandatory=$true, HelpMessage="The path to the tensorflow installation, which contains lib/tensorflow.dll")]
  [string]$tensorflowPath
)

java `
  --enable-native-access=ALL-UNNAMED `
  --enable-preview --source=22 `
  -D"java.library.path=$tensorflowPath\lib" `
  TensorflowLoadSavedModel.java saved_mnist_model