$jdk = $Env:JAVA_HOME

function find-tool($tool) {
  if (Test-Path "$jdk\bin\$tool.exe") {
    $func = {
      & "$jdk\bin\$tool.exe" $args;
      if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: $tool exited with non-zero exit code: $LASTEXITCODE"
        exit
      }
    }.GetNewClosure()
    & $func.Module Set-Variable jdk $jdk
    return $func
  } else {
    Write-Host "ERROR: Could not find $tool executable in %JAVA_HOME%\bin."
    exit
  }
}

function filter_file($includes_all, $pattern, $output_file) {  
  Select-String -Path $includes_all -Pattern $pattern | %{ $_.Line } | Out-File -FilePath $output_file -Encoding ascii
}
