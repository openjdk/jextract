@set DIR=%~dp0
@"%DIR%\runtime\bin\java" %JEXTRACT_JAVA_OPTIONS% -m org.openjdk.jextract/org.openjdk.jextract.JextractTool %*
