$ROOT = "$PSScriptRoot\.."
& "$ROOT\runtime\bin\java" $Env:JEXTRACT_JAVA_OPTIONS -m 'org.openjdk.jextract/org.openjdk.jextract.JextractTool' $args
